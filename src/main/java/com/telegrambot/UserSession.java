package com.telegrambot;

public class UserSession {
    public String fullName;
    public String date;
    public String workHours;
    public String lunchDuration;
    public String project;
    public String activity;
    public String activityTime;
    public String comment;

    public BotStep step = BotStep.WORK_HOURS;
}