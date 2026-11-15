package com.zagvladimir.fraerish.command;

public record CommandRequest(long chatId, String args) {

    public boolean hasArgs() {
        return !args.isBlank();
    }

    public String firstArg() {
        return args.split("\\s+", 2)[0];
    }
}