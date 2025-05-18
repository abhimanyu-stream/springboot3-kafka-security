package com.java17.kafka.security;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.acl.*;
import org.apache.kafka.common.resource.*;
import org.apache.kafka.common.security.auth.KafkaPrincipal;

import java.util.*;
import java.util.concurrent.ExecutionException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.java17.kafka.security.config.KafkaAclManager;
//https://chatgpt.com/share/67f863c3-73c8-800c-98ec-44bc5a30f838

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
		Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
        props.put("ssl.truststore.location", "/path/to/client.truststore.jks");
        props.put("ssl.truststore.password", "truststore_password");
        props.put("sasl.mechanism", "SCRAM-SHA-512");
        props.put("sasl.jaas.config", 
                "org.apache.kafka.common.security.scram.ScramLoginModule required " +
                "username=\"admin_user\" password=\"admin_password\";");
        
        KafkaAclManager aclManager = new KafkaAclManager(props);
        
        try {
            // Create producer ACL
            aclManager.createProducerAcl("producer_name", "notification-events");
            
            // Create consumer ACL
            aclManager.createConsumerAcl("consumer_name", "notification-events", "notification-group");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            aclManager.close();
        }
    }
}
	