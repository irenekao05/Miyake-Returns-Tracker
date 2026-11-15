package com.example.bentoreturns.repository;

import com.example.bentoreturns.model.ResolvedReturns;
import com.example.bentoreturns.model.ScanMode;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;


@Repository 
public class ReturnsRepository {

    private JdbcTemplate jdbcTemplate;

    public ReturnsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void addQuantities(long locationId, LocalDate businessDate, ScanMode mode, List<ResolvedReturns> resolvedReturns) {
        if (resolvedReturns.isEmpty()) {
            return;
        }

        String quantityColumn = switch (mode) {
            case PRODUCTION -> "produced_quantity";
            case RETURNS -> "returned_quantity";
        };

        String sql = """
            INSERT INTO daily_product_counts (
                product_id, location_id, business_date, %s
            )
            VALUES (?, ?, ?, ?)
            ON CONFLICT (product_id, location_id, business_date)
            DO UPDATE SET
                %s = COALESCE(daily_product_counts.%s, 0) + EXCLUDED.%s
            """.formatted(quantityColumn, quantityColumn, quantityColumn, quantityColumn);

        jdbcTemplate.batchUpdate(
            sql,
            resolvedReturns,
            100,
            (ps, item) -> {
                ps.setLong(1, item.productId());
                ps.setLong(2, locationId);
                ps.setDate(3, Date.valueOf(businessDate));
                ps.setInt(4, item.quantity());
            }
        );

        
    }
}
