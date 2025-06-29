package com.telegrambot;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class GoogleSheetsService {
    private static final String APPLICATION_NAME = "InfoBot";
    private static final String SPREADSHEET_ID = System.getenv("SPREADSHEET_ID"); // ✅ берёт ID из переменной
    private static final String RANGE = "Sheet1!A:L";
    private static Sheets sheetsService;

    public GoogleSheetsService() {
        String base64Json = System.getenv("GOOGLE_CREDENTIALS_JSON");

        if (base64Json == null || base64Json.isEmpty()) {
            throw new IllegalStateException("GOOGLE_CREDENTIALS_JSON is missing or empty");
        }

        byte[] decoded = Base64.getDecoder().decode(base64Json);
        InputStream stream = new ByteArrayInputStream(decoded);

        GoogleCredential credential;
        try {
            credential = GoogleCredential.fromStream(stream)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create GoogleCredential from stream", e);
        }

        try {
            sheetsService = new Sheets.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to create Sheets service", e);
        }
    }


    public void appendRow(List<Object> rowData) throws Exception {
        ValueRange body = new ValueRange().setValues(List.of(rowData));
        sheetsService.spreadsheets()
                .values()
                .append(SPREADSHEET_ID, RANGE, body)
                .setValueInputOption("RAW")
                .execute();
    }
}