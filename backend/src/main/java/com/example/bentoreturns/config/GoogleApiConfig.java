package com.example.bentoreturns.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;


@Configuration
public class GoogleApiConfig {

    @Value("${app.google.credentials-path}")
    private String credentialsPath;

    @Bean 
    public GoogleCredentials googleCredentials() throws IOException {
        try (InputStream input = Files.newInputStream(Path.of(credentialsPath))) {
            return ServiceAccountCredentials
                    .fromStream(input)
                    .createScoped(List.of(
                            SheetsScopes.SPREADSHEETS,
                            DriveScopes.DRIVE_READONLY
                    ));
        }
    }

    @Bean 
    public Sheets sheetsClient(GoogleCredentials credentials) throws IOException, GeneralSecurityException {
        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("Sushi and Bento Returns").build();
    }

    @Bean 
    public Drive driveClient(GoogleCredentials credentials) throws IOException, GeneralSecurityException {
        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("Sushi and Bento Returns").build();
    }
        
}
