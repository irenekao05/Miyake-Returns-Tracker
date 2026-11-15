package com.example.bentoreturns.service;

import com.example.bentoreturns.model.Location;
import com.example.bentoreturns.repository.LocationRepository;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class LocationService {
    private LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public List<Location> getLocations() {
        return locationRepository.findAll();
    }
}
