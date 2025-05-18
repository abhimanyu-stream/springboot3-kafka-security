package com.java17.kafka.security.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.java17.kafka.security.service.KafkaAclService;

@RestController
@RequestMapping("/api/kafka/acls")
public class KafkaAclController {

    @Autowired
    private KafkaAclService kafkaAclService;
    
    @PostMapping("/producer")
    public ResponseEntity<String> createProducerAcl(
            @RequestParam String username,
            @RequestParam String topicName) {
        
        kafkaAclService.createProducerAcl(username, topicName);
        return ResponseEntity.ok("Producer ACL created successfully");
    }
    
    @PostMapping("/consumer")
    public ResponseEntity<String> createConsumerAcl(
            @RequestParam String username,
            @RequestParam String topicName,
            @RequestParam String groupId) {
        
        kafkaAclService.createConsumerAcl(username, topicName, groupId);
        return ResponseEntity.ok("Consumer ACL created successfully");
    }
    
    @PostMapping("/admin")
    public ResponseEntity<String> createAdminAcl(@RequestParam String username) {
        try {
            kafkaAclService.createAdminAcl(username);
            return ResponseEntity.ok("Admin ACL created successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create admin ACL: " + e.getMessage());
        }
    }
}
