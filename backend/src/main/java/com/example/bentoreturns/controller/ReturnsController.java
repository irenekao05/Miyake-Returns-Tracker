package com.example.bentoreturns.controller;

import com.example.bentoreturns.model.SaveReturnsRequest;
import com.example.bentoreturns.service.ReturnsService;
import com.example.bentoreturns.model.SaveReturnsResponse;

import java.io.IOException;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController 
public class ReturnsController {
    private ReturnsService returnsService;

    public ReturnsController(ReturnsService returnsService) {
        this.returnsService = returnsService;
    }

    @PostMapping("/api/saveReturns") 
    public SaveReturnsResponse saveReturns (@RequestBody SaveReturnsRequest request) {
        if (request.locationId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "locationId not provided");
        }

        if (request.mode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mode must be provided");
        }

        if (request.scannedCounts() == null || request.scannedCounts().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "must scan at least one item");
        }

        // try to save to sheets and db 
        try {
            return returnsService.saveReturns(request);
        }
        catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                exception
            );
        }
        catch (IOException exception) {
            throw new ResponseStatusException(
                HttpStatus.BAD_GATEWAY,
                "Google API request failed",
                exception
            );
        }
    }
}
