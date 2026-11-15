package com.example.bentoreturns.model;

public record ResolvedReturns(
    Long productId,
    String barcode,
    String productName,
    int quantity
) {}
