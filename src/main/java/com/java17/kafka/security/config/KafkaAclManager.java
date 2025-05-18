package com.java17.kafka.security.config;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.*;
import org.apache.kafka.common.security.auth.KafkaPrincipal;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class KafkaAclManager {

    private final AdminClient adminClient;

    public KafkaAclManager(Properties adminProps) {
        this.adminClient = AdminClient.create(adminProps);
    }

    public void createProducerAcl(String principalName, String topicName) throws ExecutionException, InterruptedException {
        // Create a resource pattern for the topic
        ResourcePattern resourcePattern = new ResourcePattern(
                ResourceType.TOPIC,
                topicName,
                PatternType.LITERAL
        );

        // Create the ACL entry for write operation
        AccessControlEntry entry = new AccessControlEntry(
                "User:" + principalName,    // Principal
                "*",                        // Host (wildcard)
                AclOperation.WRITE,         // Operation
                AclPermissionType.ALLOW     // Permission type
        );

        AclBinding aclBinding = new AclBinding(resourcePattern, entry);
        
        // Create the ACL
        CreateAclsResult result = adminClient.createAcls(Collections.singletonList(aclBinding));
        
        // Wait for completion
        result.all().get();
        
        System.out.println("Producer ACL created for " + principalName + " on topic " + topicName);
    }

    public void createConsumerAcl(String principalName, String topicName, String groupId) 
            throws ExecutionException, InterruptedException {
        // Create ACLs for both topic read and consumer group
        List<AclBinding> aclBindings = new ArrayList<>();
        
        // 1. Topic read permission
        ResourcePattern topicResource = new ResourcePattern(
                ResourceType.TOPIC,
                topicName,
                PatternType.LITERAL
        );

        AccessControlEntry topicEntry = new AccessControlEntry(
                "User:" + principalName,
                "*",
                AclOperation.READ,
                AclPermissionType.ALLOW
        );
        
        aclBindings.add(new AclBinding(topicResource, topicEntry));
        
        // 2. Consumer group permission
        if (groupId != null && !groupId.isEmpty()) {
            ResourcePattern groupResource = new ResourcePattern(
                    ResourceType.GROUP,
                    groupId,
                    PatternType.LITERAL
            );
    
            AccessControlEntry groupEntry = new AccessControlEntry(
                    "User:" + principalName,
                    "*",
                    AclOperation.READ,
                    AclPermissionType.ALLOW
            );
            
            aclBindings.add(new AclBinding(groupResource, groupEntry));
        }
        
        // Create the ACLs
        CreateAclsResult result = adminClient.createAcls(aclBindings);
        
        // Wait for completion
        result.all().get();
        
        System.out.println("Consumer ACLs created for " + principalName + " on topic " + topicName +
                (groupId != null ? " and group " + groupId : ""));
    }
    
    public void close() {
        if (adminClient != null) {
            adminClient.close();
        }
    }

    public void createAdminAcl(String adminUser) throws ExecutionException, InterruptedException {
        // Create a resource pattern for the Kafka cluster
        ResourcePattern resourcePattern = new ResourcePattern(
                ResourceType.CLUSTER,
                "kafka-cluster",  // Name is not relevant for CLUSTER type
                PatternType.LITERAL
        );

        // Create the ACL entry for ALTER operation (admin permission)
        AccessControlEntry entry = new AccessControlEntry(
                "User:" + adminUser,    // Principal
                "*",                    // Host (wildcard)
                AclOperation.ALTER,     // Operation
                AclPermissionType.ALLOW // Permission type
        );

        AclBinding aclBinding = new AclBinding(resourcePattern, entry);
        
        // Create the ACL
        CreateAclsResult result = adminClient.createAcls(Collections.singletonList(aclBinding));
        
        // Wait for completion
        result.all().get();
        
        System.out.println("Admin ACL created for user: " + adminUser + " with ALTER --cluster permission");
    }
    
    public void createSuperUserAcl(String superUser) throws ExecutionException, InterruptedException {
        List<AclBinding> aclBindings = new ArrayList<>();
        
        // Define all resource types that need permissions
        ResourceType[] resourceTypes = {
            ResourceType.TOPIC, 
            ResourceType.GROUP,
            ResourceType.CLUSTER,
            ResourceType.TRANSACTIONAL_ID,
            ResourceType.DELEGATION_TOKEN
        };
        
        // Define all operations needed for complete access
        AclOperation[] operations = {
            AclOperation.READ,
            AclOperation.WRITE,
            AclOperation.CREATE,
            AclOperation.DELETE,
            AclOperation.ALTER,
            AclOperation.DESCRIBE,
            AclOperation.CLUSTER_ACTION,
            AclOperation.DESCRIBE_CONFIGS,
            AclOperation.ALTER_CONFIGS,
            AclOperation.IDEMPOTENT_WRITE
        };
        
        // For each resource type, grant all operations
        for (ResourceType resourceType : resourceTypes) {
            // Use wildcard pattern for topics, groups, and transactional IDs
            PatternType patternType = (resourceType == ResourceType.CLUSTER || 
                                      resourceType == ResourceType.DELEGATION_TOKEN) 
                                    ? PatternType.LITERAL : PatternType.PREFIXED;
            
            String resourceName = (resourceType == ResourceType.CLUSTER) 
                                ? "kafka-cluster" : "*";
            
            ResourcePattern resourcePattern = new ResourcePattern(
                    resourceType,
                    resourceName,
                    patternType
            );
            
            // Grant all operations for this resource
            for (AclOperation operation : operations) {
                // Skip operations that don't apply to this resource type
                if (!isOperationValidForResource(operation, resourceType)) {
                    continue;
                }
                
                AccessControlEntry entry = new AccessControlEntry(
                        "User:" + superUser,
                        "*",
                        operation,
                        AclPermissionType.ALLOW
                );
                
                aclBindings.add(new AclBinding(resourcePattern, entry));
            }
        }
        
        // Create all ACLs
        CreateAclsResult result = adminClient.createAcls(aclBindings);
        
        // Wait for completion
        result.all().get();
        
        System.out.println("Super User ACLs created for " + superUser + " with ALL permissions");
    }

    // Helper method to check if an operation is valid for a resource type
    private boolean isOperationValidForResource(AclOperation operation, ResourceType resourceType) {
        // Some operations only apply to specific resource types
        if (operation == AclOperation.CLUSTER_ACTION && resourceType != ResourceType.CLUSTER) {
            return false;
        }
        if (operation == AclOperation.IDEMPOTENT_WRITE && 
            (resourceType != ResourceType.CLUSTER && resourceType != ResourceType.TRANSACTIONAL_ID)) {
            return false;
        }
        return true;
    }
       
}
