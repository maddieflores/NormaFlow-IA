package com.workflow.backend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
public class CollaborationService {

    // Simula almacenamiento en memoria del estado de un documento en edición
    public static class DocumentSession {
        public String content;
        public LocalDateTime lastUpdated;
        public Map<String, String> activeUsers = new ConcurrentHashMap<>();

        public DocumentSession(String initialContent) {
            this.content = initialContent;
            this.lastUpdated = LocalDateTime.now();
        }
    }

    private final Map<String, DocumentSession> sessions = new ConcurrentHashMap<>();

    public void joinSession(String documentId, String userId, String userName) {
        sessions.computeIfAbsent(documentId, id -> new DocumentSession(""));
        sessions.get(documentId).activeUsers.put(userId, userName != null ? userName : "Usuario Anónimo");
        log.info("Usuario {} ({}) se unió a sesión colaborativa del documento {}", userId, userName, documentId);
    }

    public void leaveSession(String documentId, String userId) {
        DocumentSession session = sessions.get(documentId);
        if (session != null) {
            session.activeUsers.remove(userId);
            log.info("Usuario {} salió de sesión colaborativa del documento {}", userId, documentId);
        }
    }

    public String updateContent(String documentId, String newContent, String userId) {
        DocumentSession session = sessions.get(documentId);
        if (session != null) {
            session.content = newContent;
            session.lastUpdated = LocalDateTime.now();
            return session.content;
        }
        return newContent;
    }

    public String getContent(String documentId) {
        DocumentSession session = sessions.get(documentId);
        return session != null ? session.content : "";
    }
    
    public Map<String, String> getActiveUsers(String documentId) {
        DocumentSession session = sessions.get(documentId);
        return session != null ? session.activeUsers : Map.of();
    }
}
