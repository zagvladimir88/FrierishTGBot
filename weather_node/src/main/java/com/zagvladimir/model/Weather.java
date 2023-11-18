package com.zagvladimir.model;

public record Weather(int id, String main, String description, String icon) {
    public String getEmoji() {
        // Добавьте соответствия между типом погоды и смайликами
        switch (main.toLowerCase()) {
            case "clear":
                return "☀️";
            case "clouds":
                return "☁️";
            case "rain":
                return "🌧️";
            case "snow":
                return "❄️";
            default:
                return "";
        }
    }
}
