package com.example.bentoreturns.service;

import com.example.bentoreturns.model.ResolvedReturns;
import com.example.bentoreturns.model.ScanMode;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.springframework.stereotype.Service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.ValueRange;

@Service
public class GoogleSheetsService {

    private Sheets sheets;

    public GoogleSheetsService(Sheets sheets) {
        this.sheets = sheets;
    }

    // helper: get spreadsheet column letter matching day of week
    public String getDateColumn(LocalDate date, ScanMode mode) {
        if (mode == ScanMode.PRODUCTION) {
            return switch (date.getDayOfWeek()) {
                case MONDAY -> "C";
                case TUESDAY -> "D";
                case WEDNESDAY -> "E";
                case THURSDAY -> "F";
                case FRIDAY -> "G";
                case SATURDAY -> "H";
                case SUNDAY -> "I";
            };
        }

        return switch (date.getDayOfWeek()) {
            case MONDAY -> "K";
            case TUESDAY -> "L";
            case WEDNESDAY -> "M";
            case THURSDAY -> "N";
            case FRIDAY -> "O";
            case SATURDAY -> "P";
            case SUNDAY -> "Q";
        };
    }

    public void addReturns(String spreadsheetId, String sheetName, LocalDate date, ScanMode mode, List<ResolvedReturns> resolvedReturns) throws IOException {
        if (resolvedReturns.isEmpty()) {
            return;
        }

        // batch retrieve 'Product Name' column and existing return counts column
        String column = getDateColumn(date, mode);
        String quotedSheetName = "'" + sheetName + "'";
        String productColName = quotedSheetName + "!A4:A";
        String countColName = quotedSheetName + "!" + column + "4:" + column;

        BatchGetValuesResponse response = sheets.spreadsheets()
                .values()
                .batchGet(spreadsheetId)
                .setRanges(List.of(productColName, countColName))
                .setValueRenderOption("UNFORMATTED_VALUE")
                .execute();
        
        List<ValueRange> returnedRanges = response.getValueRanges();
        List<List<Object>> nameCol = returnedRanges.get(0).getValues();
        List<List<Object>> countsCol = returnedRanges.get(1).getValues();

        if (nameCol == null) {
            throw new IllegalStateException("No products listed in spreadsheet");
        }

        // map product names to their spreadsheet row index
        Map<String, Integer> productRows = new HashMap<>();

        for(int i = 0; i < nameCol.size(); i++) {
            List<Object> row = nameCol.get(i);

            if (row.isEmpty()) {
                continue;
            }

            String productName = row.get(0).toString().trim();

            if (!productName.isEmpty()) {
                productRows.put(productName, i + 4);
            }
        }

        // calculate updated return counts
        List<ValueRange> updates = new ArrayList<>();

        for (ResolvedReturns item : resolvedReturns) {
            String productName = item.productName();
            int returnQuantity = item.quantity();

            if (!productRows.containsKey(productName)) {
                throw new IllegalStateException("Product missing from spreadsheet:" + productName);
            }

            // find existing count value if any
            int rowIndex = productRows.get(productName);
            int countIndex = rowIndex - 4;

            int existing_count = 0;

            if (countsCol != null && countIndex < countsCol.size() && !countsCol.get(countIndex).isEmpty()) {
                existing_count = ((Number) countsCol.get(countIndex).get(0)).intValue();
            }

            int newCount = existing_count + returnQuantity;

            // prepare return count update for this item
            ValueRange update = new ValueRange();
            update.setRange(quotedSheetName +"!" + column + rowIndex);
            update.setValues(List.of(List.of(newCount)));

            updates.add(update);
        }

        // push updates 
        BatchUpdateValuesRequest request = new BatchUpdateValuesRequest()
            .setValueInputOption("RAW")
            .setData(updates);

        sheets.spreadsheets()
            .values()
            .batchUpdate(spreadsheetId, request)
            .execute();
    }
}
