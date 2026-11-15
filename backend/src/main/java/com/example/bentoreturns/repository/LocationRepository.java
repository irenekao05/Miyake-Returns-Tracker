package com.example.bentoreturns.repository;

import com.example.bentoreturns.model.Location;

import java.util.List;

import org.springframework.stereotype.Repository;
import org.springframework.jdbc.core.simple.JdbcClient;


@Repository
public class LocationRepository {

    private JdbcClient jdbcClient;

    public LocationRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public List<Location> findAll() {
        String sql = """
            SELECT id, marketplace_name, location_name
            FROM locations
            ORDER BY id
            """;
        
        return jdbcClient.sql(sql)
            .query((resultSet, rowNumber) -> {
                Long id = resultSet.getLong("id");
                String marketplaceName = resultSet.getString("marketplace_name");
                String locationName = resultSet.getString("location_name");

                return new Location(id, marketplaceName, locationName);
            }).list();
    }

    public Location findByLocationId(long locationId) {
        String sql = """
            SELECT id, marketplace_name, location_name
            FROM locations
            WHERE id = :locationId
            """;
        
        return jdbcClient.sql(sql).param("locationId", locationId)
            .query((resultSet, rowNumber) -> {
                Long id = resultSet.getLong("id");
                String marketplaceName = resultSet.getString("marketplace_name");
                String locationName = resultSet.getString("location_name");

                return new Location(id, marketplaceName, locationName);
            }).single();
    }
}