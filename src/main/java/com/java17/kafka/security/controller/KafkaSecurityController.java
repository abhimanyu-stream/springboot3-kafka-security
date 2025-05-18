package com.java17.kafka.security.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.java17.kafka.security.config.KafkaAclManager;
import com.java17.kafka.security.service.KafkaSecurityManagementService;
import com.java17.kafka.security.service.KafkaUserManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/kafka/security")
public class KafkaSecurityController {

    private final KafkaSecurityManagementService securityService;
    private final KafkaUserManager userManager;
    private final KafkaAclManager aclManager;
    
    @Autowired
    public KafkaSecurityController(KafkaSecurityManagementService securityService,
                                 KafkaUserManager userManager,
                                 KafkaAclManager aclManager) {
        this.securityService = securityService;
        this.userManager = userManager;
        this.aclManager = aclManager;
    }
    
    @PostMapping("/setup")
    public ResponseEntity<String> setupKafkaSecurity(@RequestBody KafkaSecuritySetupRequest request) {
        try {
            securityService.setupKafkaSecurity(
                request.getBrokerConfigPath(),
                request.getJaasConfigPath(),
                request.getSuperUsers(),
                convertUsersListToMap(request.getUsers()),
                request.getProducerTopics(),
                request.getConsumerTopicsGroups()
            );
            return ResponseEntity.ok("Kafka security setup completed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Failed to setup Kafka security: " + e.getMessage());
        }
    }
    
    @PostMapping("/users")
    public ResponseEntity<String> createUser(@RequestParam String username, @RequestParam String password) {
        try {
            userManager.createScramUser(username, password);
            return ResponseEntity.ok("User created successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body("Failed to create user: " + e.getMessage());
        }
    }

    private Map<String, String> convertUsersListToMap(List<String> users) {
        Map<String, String> userMap = new HashMap<>();
        // Populate map as needed - this depends on your data structure
        return userMap;
    }
}
