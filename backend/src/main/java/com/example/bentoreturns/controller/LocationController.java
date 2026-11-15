package com.example.bentoreturns.controller;

import com.example.bentoreturns.model.Location;
import com.example.bentoreturns.service.LocationService;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController 
public class LocationController {
    private LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    @GetMapping("/api/locations")
    public Map<String, List<Location>>getLocations() {
        List<Location> locations =  locationService.getLocations();
        return Map.of("locations", locations);
    }
}
