package com.telegrambot;

import java.util.regex.Pattern;

public class TimeValidator {
    private static final Pattern pattern = Pattern.compile("^([0-1]?\\d|2[0-3]):[0-5]\\d$");

    public static boolean isValid(String time) {
        return pattern.matcher(time).matches();
    }
}