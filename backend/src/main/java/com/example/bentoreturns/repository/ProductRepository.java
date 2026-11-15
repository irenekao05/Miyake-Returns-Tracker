package com.example.bentoreturns.repository;

import com.example.bentoreturns.model.Product;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.simple.JdbcClient;


@Repository
public class ProductRepository {
    
    private JdbcClient jdbcClient;

    public ProductRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    // fetches product master list (including discontinued products)
    public List<Product> findAll() {
        String sql = """
                SELECT id, barcode, name
                FROM products
                """;

        return jdbcClient.sql(sql)
                .query((resultSet, rowNumber) -> {
                    Long id = resultSet.getLong("id");
                    String barcode = resultSet.getString("barcode");
                    String name = resultSet.getString("name");

                    return new Product(id, barcode, name);
                }).list();
    }
    
    // fetches product list based on location 
    public List<Product> findByLocation(long locationId) {
        String sql = """
            SELECT p.id, p.barcode, p.name
            FROM products p
            JOIN location_products lp ON lp.product_id = p.id
            WHERE lp.location_id = :locationId
            """;
        
        return jdbcClient.sql(sql).param("locationId", locationId)
                .query((resultSet, rowNumber) -> {
                    Long id = resultSet.getLong("id");
                    String barcode = resultSet.getString("barcode");
                    String name = resultSet.getString("name");

                    return new Product(id, barcode, name);
                }).list();
    }
}
