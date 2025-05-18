package com.java17.kafka.security.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.java17.kafka.security.config.KafkaAclManager;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * 
 */

@Service
public class KafkaAclService {

    private KafkaAclManager aclManager;
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.security.protocol}")
    private String securityProtocol;
    //spring.kafka.ssl.trust-store-location
    @Value("${spring.kafka.ssl.trust-store-location}")
    private String trustStoreLocation;
    
    @Value("${spring.kafka.ssl.trust-store-password}")
    private String trustStorePassword;
    
    @Value("${spring.kafka.properties.sasl.mechanism}")
    private String saslMechanism;
    
    @Value("${kafka.admin.username}")
    private String adminUsername;
    
    @Value("${kafka.admin.password}")
    private String adminPassword;
    
    @Value("${kafka.broker.superusers:admin_user}")
    private String superusers;
    
    @Value("${kafka.skip.permission.check:false}")
    private boolean skipPermissionCheck;
    
    @Autowired
    private ResourceLoader resourceLoader;
    
    @PostConstruct
    public void init() throws IOException {
        // Convert Spring resource to actual file path
        Resource resource = resourceLoader.getResource(trustStoreLocation);
        File trustStoreFile = resource.getFile();
        String absolutePath = trustStoreFile.getAbsolutePath();
        /**For bundled resources in JAR files, you may need to copy the resource to a temporary file first:
         * 
         *  File tempFile = File.createTempFile("keystore", ".p12");
        tempFile.deleteOnExit();
        try (InputStream is = resource.getInputStream();
             FileOutputStream fos = new FileOutputStream(tempFile)) {
            IOUtils.copy(is, fos);
        }
        String absolutePath = tempFile.getAbsolutePath();
         */
        
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        props.put("ssl.truststore.location", absolutePath); // Use actual file path
        props.put("ssl.truststore.password", trustStorePassword);
        props.put("sasl.mechanism", saslMechanism);
        props.put("sasl.jaas.config", 
                "org.apache.kafka.common.security.scram.ScramLoginModule required " +
                "username=\"" + adminUsername + "\" password=\"" + adminPassword + "\";");
        
        aclManager = new KafkaAclManager(props);
        
        // Use the skip permission flag from properties
        if (skipPermissionCheck) {
            System.out.println("Skipping admin permission verification");
        } else if (!verifyAdminPermissions()) {
            System.out.println("Warning: Admin lacks ALTER permission, attempting bootstrap");
            bootstrapSuperUserPermissions();
        }
    }
    
    private void bootstrapSuperUserPermissions() {
        // Split superusers string into array
        String[] superUserArray = superusers.split(",");
        
        try {
            for (String superUser : superUserArray) {
                // Create admin permissions for each superuser
                System.out.println("Setting up superuser permissions for: " + superUser);
                createAdminAcl(superUser.trim());
            }
        } catch (Exception e) {
            System.out.println("Could not create superuser permissions: " + e.getMessage());
        }
    }
    
    /**
     * Verify that the configured admin user has the necessary permissions
     * by attempting to describe cluster ACLs
     */
    private boolean verifyAdminPermissions() {
        try {
            // Try to describe cluster ACLs - this will fail if we don't have ALTER --cluster permission
            AdminClient adminClient = AdminClient.create(createAdminConfig());
            adminClient.describeAcls(AclBindingFilter.ANY).values().get();
            adminClient.close();
            return true;
        } catch (ExecutionException e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.SecurityDisabledException) {
                // ACLs are not enabled, so no permission is required
                return true;
            }
            if (e.getCause() instanceof org.apache.kafka.common.errors.ClusterAuthorizationException) {
                return false;
            }
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Create producer ACL for a specified user and topic
     */
    public void createProducerAcl(String username, String topicName) {
        try {
            aclManager.createProducerAcl(username, topicName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create producer ACL: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create consumer ACL for a specified user, topic and consumer group
     */
    public void createConsumerAcl(String username, String topicName, String groupId) {
        try {
            aclManager.createConsumerAcl(username, topicName, groupId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create consumer ACL: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create admin ACL for a specified user
     * This grants ALTER --cluster permission needed to manage ACLs
     */
    public void createAdminAcl(String username) throws ExecutionException, InterruptedException {
        ResourcePattern resourcePattern = new ResourcePattern(
                ResourceType.CLUSTER,
                "kafka-cluster",  // The name doesn't matter for CLUSTER type
                PatternType.LITERAL
        );

        AccessControlEntry entry = new AccessControlEntry(
                "User:" + username,
                "*",
                AclOperation.ALTER,
                AclPermissionType.ALLOW
        );

        AclBinding aclBinding = new AclBinding(resourcePattern, entry);
        
        AdminClient adminClient = AdminClient.create(createAdminConfig());
        CreateAclsResult result = adminClient.createAcls(Collections.singletonList(aclBinding));
        result.all().get();
        adminClient.close();
        
        System.out.println("Admin ACL created for " + username + " with ALTER --cluster permission");
    }
    
    private Properties createAdminConfig() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        props.put("ssl.truststore.location", trustStoreLocation);
        props.put("ssl.truststore.password", trustStorePassword);
        props.put("sasl.mechanism", saslMechanism);
        props.put("sasl.jaas.config", 
                "org.apache.kafka.common.security.scram.ScramLoginModule required " +
                "username=\"" + adminUsername + "\" password=\"" + adminPassword + "\";");
        return props;
    }
    
    @PreDestroy
    public void cleanup() {
        if (aclManager != null) {
            aclManager.close();
        }
    }
}
