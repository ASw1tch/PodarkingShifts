package com.telegrambot;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "InfoBot";
    private static final String SPREADSHEET_ID = "1Z1cXREsuank6PijxIw4W-LJZrH2B0-f6QqT0A_8MF1k"; // Заменить на свой
    private static final String RANGE = "Sheet1!A:L";
    private static Sheets sheetsService;

    public static void init() throws IOException, GeneralSecurityException {
        FileInputStream serviceAccountStream = new FileInputStream("info-bot-credentials.json");

        GoogleCredential credential = GoogleCredential.fromStream(serviceAccountStream)
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(),
                credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void appendRow(List<String> rowData) throws IOException {
        ValueRange body = new ValueRange()
                .setValues(Collections.singletonList(new ArrayList<>(rowData)));

        sheetsService.spreadsheets().values()
                .append(SPREADSHEET_ID, RANGE, body)
                .setValueInputOption("RAW")
                .execute();
    }
}