package com.example.bentoreturns.model;

public record SaveReturnsResponse(
    boolean sheetsSaved,
    boolean databaseSaved,
    String message
) {}
