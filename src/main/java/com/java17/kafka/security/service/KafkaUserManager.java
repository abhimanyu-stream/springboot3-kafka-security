package com.java17.kafka.security.service;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.ConfigResource.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class KafkaUserManager {

    private final AdminClient adminClient;

    @Autowired
    public KafkaUserManager(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                           @Value("${spring.kafka.security.protocol}") String securityProtocol,
                           @Value("${spring.kafka.ssl.trust-store-location}") String trustStorePath,
                           @Value("${spring.kafka.ssl.trust-store-password}") String trustStorePassword,
                           @Value("${spring.kafka.properties.sasl.mechanism}") String saslMechanism,
                           @Value("${kafka.admin.username}") String adminUsername,
                           @Value("${kafka.admin.password}") String adminPassword,
                           ResourceLoader resourceLoader) {
                           
        try {
            // Convert Spring resource path to physical file path
            Resource resource = resourceLoader.getResource(trustStorePath);
            File trustStoreFile = resource.getFile();
            String absolutePath = trustStoreFile.getAbsolutePath();
            
            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, securityProtocol);
            props.put("ssl.truststore.location", absolutePath);  // Use resolved path
            props.put("ssl.truststore.password", trustStorePassword);
            props.put("sasl.mechanism", saslMechanism);
            props.put("sasl.jaas.config", 
                    "org.apache.kafka.common.security.scram.ScramLoginModule required " +
                    "username=\"" + adminUsername + "\" password=\"" + adminPassword + "\";");
            
            this.adminClient = AdminClient.create(props);
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize Kafka user manager: " + e.getMessage(), e);
        }
    }
    
    /**
     * Create or update a Kafka user with SCRAM-SHA-512 credentials
     * Equivalent to: kafka-configs.sh --bootstrap-server localhost:9092 --alter --add-config 
     * 'SCRAM-SHA-512=[password=password]' --entity-type users --entity-name username
     */
    public void createScramUser(String username, String password) throws ExecutionException, InterruptedException {
        // Define the configuration resource for the user
        ConfigResource userResource = new ConfigResource(Type.TOPIC, username);
        
        // Create the SCRAM config with the encoded password
        String saslScramConfig = String.format("SCRAM-SHA-512=[password=%s]", password);
        // Create the AlterConfigOp operation
        Map<ConfigResource, Collection<AlterConfigOp>> alterConfigs = new HashMap<>();
        AlterConfigOp configOp = new AlterConfigOp(
                new ConfigEntry("SCRAM-SHA-512", "[password=" + password + "]"),
                AlterConfigOp.OpType.SET
        );
        
        alterConfigs.put(userResource, Collections.singletonList(configOp));
        
        // Execute the operation
        AlterConfigsResult result = adminClient.incrementalAlterConfigs(alterConfigs);
        result.all().get(); // Wait for completion
        
        System.out.println("Created SCRAM-SHA-512 credentials for user: " + username);
    }
    
    public void close() {
        if (adminClient != null) {
            adminClient.close();
        }
    }
}
