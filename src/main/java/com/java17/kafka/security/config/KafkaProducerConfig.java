package com.java17.kafka.security.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.transaction.KafkaTransactionManager;
import org.springframework.kafka.annotation.EnableKafka;

import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
@EnableTransactionManagement
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.transaction-id-prefix:tx-}")
    private String transactionIdPrefix;

    @Bean
    public Map<String, Object> producerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Security configurations
        props.put("security.protocol", "SASL_SSL");
        props.put("ssl.truststore.location", "/path/to/client.truststore.jks");
        props.put("ssl.truststore.password", "truststore_password");
        props.put("ssl.protocol", "TLSv1.2");
        props.put("sasl.mechanism", "SCRAM-SHA-512");
        props.put("sasl.jaas.config", 
                  "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"producer_name\" password=\"producer_password\";");

        // Safe producer userSettings
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "5");

        // High throughput userSettings
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        props.put(ProducerConfig.LINGER_MS_CONFIG, "20");
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32 * 1024)); // 32KB
        props.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, transactionIdPrefix);

        // Error handling
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000); // 2 minutes
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); // 30 seconds
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000); // 1 second

        return props;
    }

    @Bean
    @Primary
    public ProducerFactory<String, String> producerFactory() {
        DefaultKafkaProducerFactory<String, String> factory =
            new DefaultKafkaProducerFactory<>(producerConfig());
        factory.setTransactionIdPrefix(transactionIdPrefix);
        return factory;
    }

   /* @Bean("producerFactoryPayRequest")
    public ProducerFactory<String, PaymentRequest> producerFactoryPayRequest() {
        return new DefaultKafkaProducerFactory<>(producerConfig());
    }

    @Bean("producerFactoryMoneyTransferEvent")
    public ProducerFactory<String, MoneyTransferEvent> producerFactoryMoneyTransferEvent() {
        return new DefaultKafkaProducerFactory<>(producerConfig());
    }*/
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(
            ProducerFactory<String, String> producerFactory) {
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);
        template.setDefaultTopic(KafkaConfig.NOTIFICATION_TOPIC);
        return template;
    }

   /* @Bean("kafkaTemplateMoneyTransferEvent")
    public KafkaTemplate<String, MoneyTransferEvent> kafkaTemplateMoneyTransferEvent() {
        KafkaTemplate<String, MoneyTransferEvent> kafkaTemplate = new KafkaTemplate<>(producerFactoryMoneyTransferEvent());
        kafkaTemplate.setTransactionIdPrefix("tx-");
        return kafkaTemplate;
    }

    @Bean("kafkaTemplatePayRequest")
    public KafkaTemplate<String, PaymentRequest> kafkaTemplatePayRequest() {
        KafkaTemplate<String, PaymentRequest> kafkaTemplate = new KafkaTemplate<>(producerFactoryPayRequest());
        kafkaTemplate.setTransactionIdPrefix("tx-");
        return kafkaTemplate;
    }*/

    @Bean
    public KafkaTransactionManager<String, String> kafkaTransactionManager(
            ProducerFactory<String, String> producerFactory) {
        return new KafkaTransactionManager<>(producerFactory);
    }
   /* @Bean("kafkaTransactionManagerPayRequest")
    public KafkaTransactionManager<String, PaymentRequest> kafkaTransactionManagerPayRequest(ProducerFactory<String, PaymentRequest> producerFactory) {
        return new KafkaTransactionManager<>(producerFactory);
    }

    @Bean("kafkaTransactionManagerMoneyTransferEvent")
    public KafkaTransactionManager<String, MoneyTransferEvent> kafkaTransactionManagerMoneyTransferEvent(ProducerFactory<String, MoneyTransferEvent> producerFactoryMoneyTransferEvent) {
        return new KafkaTransactionManager<>(producerFactoryMoneyTransferEvent);
    }*/
}
