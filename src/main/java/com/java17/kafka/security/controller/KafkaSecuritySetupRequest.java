package com.java17.kafka.security.controller;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KafkaSecuritySetupRequest {

    private String brokerConfigPath;
    private String jaasConfigPath;
    private List<String> superUsers;
    private List<String> users;
    private Map<String, List<String>> producerTopics;
    private Map<String, Map<String, String>> consumerTopicsGroups;

    
    // Getters and setters
    public String getBrokerConfigPath() {
        return brokerConfigPath;
    }

    
}
