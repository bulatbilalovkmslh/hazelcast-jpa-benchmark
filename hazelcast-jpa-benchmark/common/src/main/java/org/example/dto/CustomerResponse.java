package org.example.dto;

public record CustomerResponse(
        Long id,
        String name,
        String email,
        String status,
        String nodeId) {
}
