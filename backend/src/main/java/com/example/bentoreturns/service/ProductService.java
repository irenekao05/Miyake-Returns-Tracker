package com.example.bentoreturns.service;

import com.example.bentoreturns.model.Product;
import com.example.bentoreturns.repository.ProductRepository;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Service;


@Service
public class ProductService {
    private ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Map<String, String> getProducts() {
        List<Product> products = productRepository.findAll();

        Map<String, String> productsByBarcode = new HashMap<>();

        for (Product product: products) {
            productsByBarcode.put(product.barcode(), product.name());
        }

        return productsByBarcode;
    }

    
    public Map<String, String> getProducts(long locationId) {
        List<Product> products = productRepository.findByLocation(locationId);

        Map<String, String> productsByBarcode = new HashMap<>();

        for (Product product: products) {
            productsByBarcode.put(product.barcode(), product.name());
        }

        return productsByBarcode;
    }
}
