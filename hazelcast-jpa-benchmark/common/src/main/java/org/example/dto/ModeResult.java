package org.example.dto;


public record ModeResult(Mode mode, long elapsedMs, long hits, long misses, String nodeId) {
}

