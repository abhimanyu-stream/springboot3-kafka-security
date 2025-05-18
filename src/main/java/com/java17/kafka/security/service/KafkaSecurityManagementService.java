package com.java17.kafka.security.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.java17.kafka.security.config.KafkaAclManager;

import java.util.*;

@Service
public class KafkaSecurityManagementService {

    private final KafkaUserManager userManager;
    private final KafkaAclManager aclManager;
    private final KafkaBrokerSecurityService brokerSecurityService;
    
    @Autowired
    public KafkaSecurityManagementService(KafkaUserManager userManager, 
                                        KafkaAclManager aclManager,
                                        KafkaBrokerSecurityService brokerSecurityService) {
        this.userManager = userManager;
        this.aclManager = aclManager;
        this.brokerSecurityService = brokerSecurityService;
    }
    
    /**
     * Complete Kafka security setup for a new environment
     * This handles broker config, user creation, and ACL setup
     */
    public void setupKafkaSecurity(String brokerConfigPath, String jaasConfigPath, 
                                 List<String> superUsers, Map<String, String> users,
                                 Map<String, List<String>> producerTopics,
                                 Map<String, Map<String, String>> consumerTopicsGroups) throws Exception {
        
        // 1. Generate broker security config (to be applied manually before broker start)
        brokerSecurityService.generateBrokerJaasConfig(jaasConfigPath, 
                                                    superUsers.get(0), // First user as admin
                                                    users.get(superUsers.get(0)), 
                                                    superUsers);
        
        brokerSecurityService.generateSuperUserConfig(superUsers);
        
        System.out.println("\n==== IMPORTANT MANUAL STEPS ====");
        System.out.println("1. Add the generated super.users config to server.properties");
        System.out.println("2. Configure your broker to use the JAAS file: -Djava.security.auth.login.config=" + jaasConfigPath);
        System.out.println("3. Restart your Kafka broker");
        System.out.println("4. Run this application again with setupBrokerSecurityOnly=false");
        System.out.println("===============================\n");
        
        // For existing brokers, continue with user and ACL setup
        
        // 2. Create SCRAM users
        for (Map.Entry<String, String> userEntry : users.entrySet()) {
            userManager.createScramUser(userEntry.getKey(), userEntry.getValue());
        }
        
        // 3. Setup ACLs for producers
        for (Map.Entry<String, List<String>> producerEntry : producerTopics.entrySet()) {
            String username = producerEntry.getKey();
            for (String topic : producerEntry.getValue()) {
                aclManager.createProducerAcl(username, topic);
            }
        }
        
        // 4. Setup ACLs for consumers
        for (Map.Entry<String, Map<String, String>> consumerEntry : consumerTopicsGroups.entrySet()) {
            String username = consumerEntry.getKey();
            for (Map.Entry<String, String> topicGroup : consumerEntry.getValue().entrySet()) {
                aclManager.createConsumerAcl(username, topicGroup.getKey(), topicGroup.getValue());
            }
        }
        
        // 5. Grant admin permissions to super users
        for (String adminUser : superUsers) {
            if (!adminUser.equals(superUsers.get(0))) { // Skip the first one who already has admin rights
                aclManager.createAdminAcl(adminUser);
            }
        }
    }
}
