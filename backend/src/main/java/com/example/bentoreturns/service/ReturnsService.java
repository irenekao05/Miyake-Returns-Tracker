package com.example.bentoreturns.service;

import com.example.bentoreturns.repository.LocationRepository;
import com.example.bentoreturns.repository.ProductRepository;
import com.example.bentoreturns.repository.ReturnsRepository;
import com.example.bentoreturns.model.ResolvedReturns;
import com.example.bentoreturns.model.Location;
import com.example.bentoreturns.model.Product;
import com.example.bentoreturns.model.SaveReturnsRequest;
import com.example.bentoreturns.model.SaveReturnsResponse;
import com.example.bentoreturns.model.ScanMode;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ReturnsService {
    private LocationRepository locationRepository;
    private ProductRepository productRepository;
    private GoogleDriveService googleDriveService;
    private GoogleSheetsService googleSheetsService;
    private ReturnsRepository returnsRepository;
    private Logger log = LoggerFactory.getLogger(ReturnsService.class);

    public ReturnsService(
        LocationRepository locationRepository,
        ProductRepository productRepository,
        GoogleDriveService googleDriveService,
        GoogleSheetsService googleSheetsService,
        ReturnsRepository returnsRepository
    ) {
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.googleDriveService = googleDriveService;
        this.googleSheetsService = googleSheetsService;
        this.returnsRepository = returnsRepository;
    }

    private LocalDate getBusinessDate(ScanMode mode) {
        ZoneId timeZone = ZoneId.of("America/Los_Angeles");
        LocalDate today = LocalDate.now(timeZone);

        return switch (mode) {
            case PRODUCTION -> today;
            case RETURNS -> today.minusDays(1);
        };
    }

    // get spreadsheet name return date maps to 
    private String getSpreadsheetName(LocalDate businessDate) {
        int year = businessDate.get(IsoFields.WEEK_BASED_YEAR);
        int weekNumber = businessDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);

        return String.format(Locale.ENGLISH, "MarketReturns_%d-W%02d", year, weekNumber);
    }

    // get sheet tab name in spreadsheet 
    // format example: Mitsuwa (Cupertino) 
    private String getSheetName(Location location) {
        String marketplaceName = location.marketplaceName();
        String cityName = location.locationName().split(",")[0].trim();

        return marketplaceName + " (" + cityName + ")";
    }

    // remaps scanned barcodes to Product objects
    private List<ResolvedReturns> resolveReturns(long locationId, Map<String, Integer> scannedCounts) {
        List<Product> products = productRepository.findByLocation(locationId);

        Map<String, Product> productsByBarcode = new HashMap<>();
        for (Product product : products) {
            productsByBarcode.put(product.barcode(), product);
        }

        List<ResolvedReturns> resolvedReturns = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : scannedCounts.entrySet()) {
            String barcode = entry.getKey();
            Integer quantity = entry.getValue();

            Product product = productsByBarcode.get(barcode);

            if (product == null) {
                throw new IllegalArgumentException("Product not available at this location: " + barcode);
            }

            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for barcode: " + barcode);
            }

            ResolvedReturns singleReturn = new ResolvedReturns(
                product.id(),
                product.barcode(),
                product.name(),
                quantity
            );

            resolvedReturns.add(singleReturn);
        }
        return resolvedReturns;
    }

    public SaveReturnsResponse saveReturns(SaveReturnsRequest request) throws IOException {
        long locationId = request.locationId();
        Map<String, Integer> scannedCounts = request.scannedCounts();
        ScanMode mode = request.mode();

        if (scannedCounts.isEmpty()) {
            throw new IllegalArgumentException("Must scan at least one item");
        }

        // getting spreadsheet name 
        LocalDate businessDate = getBusinessDate(mode);
        String spreadsheetName = getSpreadsheetName(businessDate);

        // getting sheet name for corresponding marketplace + location
        Location location = locationRepository.findByLocationId(locationId);
        String sheetName = getSheetName(location);

        // remap scanned barcodes to Product objects
        List<ResolvedReturns> resolvedReturns = resolveReturns(locationId, scannedCounts);

        String spreadsheetId = googleDriveService.findSpreadsheetId(spreadsheetName);
        googleSheetsService.addReturns(spreadsheetId, sheetName, businessDate, mode, resolvedReturns);

        try {
            returnsRepository.addQuantities(locationId, businessDate, mode, resolvedReturns);
        } 
        catch (DataAccessException | TransactionException exception) {
            log.error(
                    "Sheets saved, but database saving failed for location {} on {}",
                    locationId,
                    businessDate,
                    exception
            );

            return new SaveReturnsResponse(
                    true,
                    false,
                    "Saved to Google Sheets, but database saving failed. "
            );
        }

        return new SaveReturnsResponse(
                true,
                true,
                "Returns saved to Google Sheets and the database"
        );

    }

}
