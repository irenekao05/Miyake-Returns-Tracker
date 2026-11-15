package com.example.bentoreturns.model;

import java.util.Map;

public record SaveReturnsRequest(
    Long locationId,
    ScanMode mode,
    Map<String, Integer> scannedCounts
) {}
