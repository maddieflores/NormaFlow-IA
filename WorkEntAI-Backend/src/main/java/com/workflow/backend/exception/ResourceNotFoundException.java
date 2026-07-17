package com.workflow.backend.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, String id) {
        super(resource + " no encontrado con id: " + id);
    }
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
