package com.java17.kafka.security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

@Service
public class KafkaBrokerSecurityService {
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaBrokerSecurityService.class);
    
    /**
     * Generate a JAAS file for Kafka broker with superuser configuration
     * This must be applied before broker startup
     */
    public void generateBrokerJaasConfig(String configPath, String adminUsername, String adminPassword, 
                                         List<String> superUsers) throws IOException {
        StringBuilder jaasConfig = new StringBuilder();
        
        // Build KafkaServer section with SCRAM credentials
        jaasConfig.append("KafkaServer {\n");
        jaasConfig.append("    org.apache.kafka.common.security.scram.ScramLoginModule required\n");
        jaasConfig.append("    username=\"").append(adminUsername).append("\"\n");
        jaasConfig.append("    password=\"").append(adminPassword).append("\"\n");
        
        // Add user entries
        jaasConfig.append("    user_").append(adminUsername).append("=\"")
                 .append(adminPassword).append("\"");
        
        jaasConfig.append(";\n};\n\n");
        
        // Client section for tools
        jaasConfig.append("KafkaClient {\n");
        jaasConfig.append("    org.apache.kafka.common.security.scram.ScramLoginModule required\n");
        jaasConfig.append("    username=\"").append(adminUsername).append("\"\n");
        jaasConfig.append("    password=\"").append(adminPassword).append("\";\n");
        jaasConfig.append("};\n");
        
        // Write to file
        try (FileWriter writer = new FileWriter(configPath)) {
            writer.write(jaasConfig.toString());
        }
        
        logger.info("Generated JAAS config file at: {}", configPath);
        logger.info("To apply this configuration, set -Djava.security.auth.login.config={} " +
                "when starting your Kafka broker", configPath);
    }
    
    /**
     * Generate the superusers configuration for server.properties
     */
    public String generateSuperUserConfig(List<String> superUsers) {
        StringBuilder config = new StringBuilder("super.users=");
        
        for (int i = 0; i < superUsers.size(); i++) {
            config.append("User:").append(superUsers.get(i));
            if (i < superUsers.size() - 1) {
                config.append(";");
            }
        }
        
        String superUserConfig = config.toString();
        logger.info("Generated super.users configuration: {}", superUserConfig);
        logger.info("Add this line to your server.properties file");
        
        return superUserConfig;
    }
    
    /**
     * Updates server.properties file with super user configuration
     * Note: This requires file system access to Kafka broker configuration
     */
    public void updateServerProperties(String serverPropertiesPath, List<String> superUsers) throws IOException {
        Properties properties = new Properties();
        
        // Load existing properties
        try (FileInputStream fis = new FileInputStream(serverPropertiesPath)) {
            properties.load(fis);
        }
        
        // Generate super.users config
        String superUsersEntry = generateSuperUserConfig(superUsers)
                .substring("super.users=".length());
        
        // Update properties
        properties.setProperty("super.users", superUsersEntry);
        properties.setProperty("authorizer.class.name", "kafka.security.authorizer.AclAuthorizer");
        properties.setProperty("allow.everyone.if.no.acl.found", "false");
        
        // Save properties
        try (FileOutputStream fos = new FileOutputStream(serverPropertiesPath)) {
            properties.store(fos, "Updated by KafkaBrokerSecurityService");
        }
        
        logger.info("Updated server.properties file at: {}", serverPropertiesPath);
        logger.info("Restart Kafka broker to apply changes");
    }
}
