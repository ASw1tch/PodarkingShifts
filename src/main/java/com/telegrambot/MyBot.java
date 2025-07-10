package com.telegrambot;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class MyBot extends TelegramLongPollingBot {

    private final Map<Long, UserSession> sessions = new HashMap<>();
    private final GoogleSheetsService googleSheetsService = new GoogleSheetsService();

    // ✅ Карта для замены Telegram имён на полные
private static final Map<String, String> nameMap = Map.of(
    "ARTEM", "Артём Пушков",
    "Anatoliy Switch", "Анатолий Петров",
    "Валерия", "Герасимчук Лера",
    "Darja Timošenko", "Дарья Тимошенко",
    "Evgenya", "Евгения Лисица",
    "Jane157", "Евгения Орлова"
);

    @Override
    public String getBotUsername() {
        return "PodarkingShiftBot"; // ← замени на username из BotFather (например: info_collector_java_bot)
    }

    @Override
    public String getBotToken() {
        return System.getenv("BOT_TOKEN"); // ← вставь сюда токен, полученный от BotFather
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        Message msg = update.getMessage();
        long chatId = msg.getChatId();
        String text = msg.getText().trim();

        if (text.equalsIgnoreCase("/dinner") && sessions.containsKey(chatId)) {
            UserSession session = sessions.get(chatId);
            session.step = BotStep.LUNCH;
            sendWithButtons(chatId, "Сколько длился обед?", "00:15", "00:30", "00:45", "01:00", "Без обеда");
            return;
        }

        // Если человек ввёл /start — сбрасываем сессию и начинаем заново
        if (text.equalsIgnoreCase("/start")) {
            UserSession newSession = new UserSession();
            String rawFirstName = msg.getFrom().getFirstName();
            String rawLastName = msg.getFrom().getLastName();
            String tgFullName = (rawLastName != null) ? rawFirstName + " " + rawLastName : rawFirstName;

            String correctedName = nameMap.getOrDefault(tgFullName, tgFullName);

            newSession.fullName = correctedName;
            newSession.date = LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            newSession.step = BotStep.PROJECT;

            sessions.put(chatId, newSession);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText("Привет, " + newSession.fullName + "! 👋\n\n" +
                "Сегодняшняя дата: *" + newSession.date + "*\n\n" +
                "*Начнём!* 🚀\nНа каком проекте ты работал(а)? (или напиши 'Другое')");
            message.setParseMode("Markdown");
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
            return;
        }

        // Если нет активной сессии — просим написать /start
        if (!sessions.containsKey(chatId)) {
            sendMessage(chatId, "Начни отчёт командой /start");
            return;
        }

        UserSession session = sessions.get(chatId);

        switch (session.step) {
            case LUNCH -> {
    session.lunchDuration = text;

    try {
        List<Object> row = List.of(
                safe(session.fullName),
                safe(session.date),
                "", // workHours
                safe(session.lunchDuration),
                "", // project
                "", // activity
                "", // activityTime
                "Был добавлен обед", // comment
                "", // пустая колонка I
                java.time.ZonedDateTime.now(java.time.ZoneId.of("Europe/Belgrade"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) // timestamp колонка J
        );
        String timestamp = java.time.ZonedDateTime.now(java.time.ZoneId.of("Europe/Belgrade"))
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        List<Object> rowForThirdSheet = new ArrayList<>();
        rowForThirdSheet.add(safe(session.fullName));     // A
        rowForThirdSheet.add(safe(session.date));         // B
        rowForThirdSheet.add("");                          // C
        rowForThirdSheet.add(safe(session.lunchDuration));// D
        rowForThirdSheet.add("");                          // E пустой
        rowForThirdSheet.add("");                          // F
        rowForThirdSheet.add("");                          // G
        rowForThirdSheet.add("");                          // H
        rowForThirdSheet.add("Был добавлен обед");        // I
        rowForThirdSheet.add("");                          // J пустой
        rowForThirdSheet.add("");                          // K пустой
        rowForThirdSheet.add(timestamp);                   // L
        googleSheetsService.appendRow(row);
        googleSheetsService.appendRowToSecondSheet(row);
        googleSheetsService.appendRowToThirdSheet(rowForThirdSheet);
        sendMessage(chatId, "✅ Обед успешно добавлен! ✅");
    } catch (Exception e) {
        sendMessage(chatId, "⚠️ Ошибка при записи обеда в таблицу: " + e.getMessage());
        e.printStackTrace();
    }

    session.step = BotStep.DONE;
}
            case PROJECT -> {
                session.project = text;
                session.step = BotStep.ACTIVITY;
                sendWithButtons(chatId, "Выбери деятельность:", "Пошив", "Крой", "Нанесение", "ОТК", "Переделки", "Другое", "Закупки", "Дизайн", "ППО", "Печать");
            }
            case ACTIVITY -> {
                session.activity = text;
                session.step = BotStep.ACTIVITY_TIME;
                sendMessage(chatId, "Сколько времени заняла эта деятельность? (формат HH:MM)");
            }
            case ACTIVITY_TIME -> {
                if (TimeValidator.isInvalid(text)) {
                    sendMessage(chatId, "Пожалуйста, введи время в формате HH:MM (например, 01:20)");
                    return;
                }
                session.activityTime = text;
                session.step = BotStep.COMMENT;
                sendMessage(chatId, "Напиши, пожалуйста, комментарий к своей деятельности:");
            }
            case COMMENT -> {
                session.comment = text;
                try {
                    List<Object> row = List.of(
                            safe(session.fullName),
                            safe(session.date),
                            safe(session.workHours),
                            safe(session.lunchDuration),
                            safe(session.project),
                            safe(session.activity),
                            safe(session.activityTime),
                            safe(session.comment),
                            "", // пустая колонка I
                            java.time.ZonedDateTime.now(java.time.ZoneId.of("Europe/Belgrade"))
                                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) // timestamp колонка J
                    );
                    String timestamp = java.time.ZonedDateTime.now(java.time.ZoneId.of("Europe/Belgrade"))
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                    List<Object> rowForThirdSheet = new ArrayList<>();
                    rowForThirdSheet.add(safe(session.fullName));     // A
                    rowForThirdSheet.add(safe(session.date));         // B
                    rowForThirdSheet.add(safe(session.workHours));    // C
                    rowForThirdSheet.add(safe(session.lunchDuration));// D
                    rowForThirdSheet.add("");                          // E пустой
                    rowForThirdSheet.add(safe(session.project));      // F
                    rowForThirdSheet.add(safe(session.activity));     // G
                    rowForThirdSheet.add(safe(session.activityTime)); // H
                    rowForThirdSheet.add(safe(session.comment));      // I
                    rowForThirdSheet.add("");                          // J пустой
                    rowForThirdSheet.add("");                          // K пустой
                    rowForThirdSheet.add(timestamp);                   // L
                    googleSheetsService.appendRow(row);
                    googleSheetsService.appendRowToSecondSheet(row);
                    googleSheetsService.appendRowToThirdSheet(rowForThirdSheet);
                    sendMessage(chatId, "✅ Отчёт успешно записан! ✅");
                } catch (Exception e) {
                    sendMessage(chatId, "⚠️ Ошибка при записи в таблицу: " + e.getMessage());
                    e.printStackTrace();
                }

                session.step = BotStep.DONE;
            }
            case DONE ->
                    sendMessage(chatId, "Хочешь добавить новый проект, деятельность и время? Снова жми /start или жми /dinner чтобы добавить обед");
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(null); // по умолчанию — без клавиатуры
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendWithButtons(long chatId, String text, String... options) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        for (int i = 0; i < options.length; i++) {
            row.add(options[i]);
            if ((i + 1) % 3 == 0) { // 3 кнопки в ряд
                rows.add(row);
                row = new KeyboardRow();
            }
        }
        if (!row.isEmpty()) rows.add(row);

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(true);
        message.setReplyMarkup(keyboard);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

}
