package org.example.dto;

public record BenchmarkResult(
        long noCacheMs,
        long secondLevelCacheMs,
        long distributedCacheMs,
        TtlEffect ttlEffect) {
}
