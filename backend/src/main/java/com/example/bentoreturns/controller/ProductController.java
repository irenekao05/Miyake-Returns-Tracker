package com.example.bentoreturns.controller;

import com.example.bentoreturns.service.ProductService;

import java.util.Map;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController 
public class ProductController {
    private ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/api/products")
    public Map<String, Map<String, String>> getProducts(
        @RequestParam(value = "locationId") long locationId
    ) {
        Map<String, String> products = productService.getProducts(locationId);
        return Map.of("products", products);
    }
}
