package com.example.bentoreturns.service;

import java.io.IOException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

@Service
public class GoogleDriveService {

    @Value("${app.google.returns-folder-id}")
    private String returnsFolderId;

    private Drive drive;

    public GoogleDriveService(Drive drive) {
        this.drive = drive;
    }

    public String findSpreadsheetId(String spreadsheetName) throws IOException {
        String escapedName = spreadsheetName.replace("'", "\\'");
        String escapedFolderId = returnsFolderId.replace("'", "\\'");

        String query = String.format(
                "name = '%s'" + 
                " and '%s' in parents" +
                " and mimeType = 'application/vnd.google-apps.spreadsheet'" + 
                " and trashed = false",
                escapedName,
                escapedFolderId
        );

        FileList response = drive.files()
                .list()
                .setQ(query)
                .setFields("nextPageToken, files(id, name)")
                .setPageSize(100)
                .execute();
        
        List<File> matchingFiles = response.getFiles();

        if (matchingFiles == null || matchingFiles.isEmpty()) {
            throw new IllegalStateException("Spreadsheet not found: " + spreadsheetName);
        }

        if (matchingFiles.size() > 1) {
            throw new IllegalStateException("Multiple spreadsheets with same name found" + spreadsheetName);
        }

        File spreadsheet = matchingFiles.get(0);
        return spreadsheet.getId();
    }
}
