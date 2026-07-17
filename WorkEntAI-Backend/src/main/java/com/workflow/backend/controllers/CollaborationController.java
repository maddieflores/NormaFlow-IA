package com.workflow.backend.controllers;

import com.workflow.backend.services.CollaborationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Set;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CollaborationController {

    private final CollaborationService collaborationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Data
    public static class CollabMessage {
        private String type; // JOIN, EDIT, LEAVE
        private String userId;
        private String userName; // New field for names
        private String content; // used in EDIT
    }

    @MessageMapping("/collab/{docId}")
    public void handleCollaborationMessage(@DestinationVariable String docId, @Payload CollabMessage message) {
        log.info("Mensaje collab recibido en doc {}: type={}, userId={}, userName={}", docId, message.getType(), message.getUserId(), message.getUserName());

        switch (message.getType()) {
            case "JOIN":
                collaborationService.joinSession(docId, message.getUserId(), message.getUserName());
                broadcastState(docId);
                break;
            case "EDIT":
                String updatedContent = collaborationService.updateContent(docId, message.getContent(), message.getUserId());
                message.setContent(updatedContent);
                // Broadcast the change to everyone
                messagingTemplate.convertAndSend("/topic/collab/" + docId, message);
                break;
            case "LEAVE":
                collaborationService.leaveSession(docId, message.getUserId());
                broadcastState(docId);
                break;
        }
    }

    private void broadcastState(String docId) {
        java.util.Map<String, String> activeUsers = collaborationService.getActiveUsers(docId);
        String currentContent = collaborationService.getContent(docId);
        
        CollabMessage stateMsg = new CollabMessage();
        stateMsg.setType("STATE");
        stateMsg.setContent(currentContent);
        
        // Return a comma-separated list of names instead of IDs
        String names = String.join(", ", activeUsers.values());
        stateMsg.setUserId(names); // Using userId field to send the names string to not break frontend too much
        
        messagingTemplate.convertAndSend("/topic/collab/" + docId, stateMsg);
    }
}
