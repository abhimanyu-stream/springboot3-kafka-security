package com.java17.kafka.security.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

@Configuration
@EnableKafka
public class KafkaConfig {
    public static final String NOTIFICATION_TOPIC = "notification-events";
    public static final String NOTIFICATION_GROUP = "notification-group";
    public static final String NOTIFICATION_DLQ = "notification-events-dlq";
    @Value("${spring.kafka.consumer.retry.max-attempts:3}")
    private int maxRetryAttempts;

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, String> template) {
        return new DeadLetterPublishingRecoverer(template,
                (record, ex) -> new TopicPartition(KafkaConfig.NOTIFICATION_DLQ, -1));
    }

    @Bean
    public KafkaAclManager kafkaAclManager(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.security.protocol:PLAINTEXT}") String securityProtocol,
            ResourceLoader resourceLoader) throws IOException {
            
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        if ("SASL_SSL".equals(securityProtocol)) {
            Resource resource = resourceLoader.getResource("${spring.kafka.ssl.trust-store-location}");
            File trustStoreFile = resource.getFile();
            String absolutePath = trustStoreFile.getAbsolutePath();
            
            props.put(AdminClientConfig.SECURITY_PROTOCOL_CONFIG, securityProtocol);
            props.put("ssl.truststore.location", absolutePath);
            props.put("ssl.truststore.password", "${spring.kafka.ssl.trust-store-password}");
            props.put("sasl.mechanism", "${spring.kafka.properties.sasl.mechanism}");
            props.put("sasl.jaas.config", 
                  "org.apache.kafka.common.security.scram.ScramLoginModule required " +
                  "username=\"${kafka.admin.username}\" password=\"${kafka.admin.password}\";");
        }
        
        return new KafkaAclManager(props);
    }
} 