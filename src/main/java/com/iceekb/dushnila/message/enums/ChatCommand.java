package com.iceekb.dushnila.message.enums;

public enum ChatCommand {
    CREPLACE("замена (/creplace \"слово\" \"на слово/фраза\")"),
    DREPLACE("удалить замену (/dreplace \"слово\")"),
    STAT("статистика группы"),
    HELP("справка"),
    CIGNORE("игнор слова (/cignore \"слово\")"),
    DIGNORE(" удалить игнор (/dignore \"слово\")"),
    LIGNORE("список игнора"),
    LREPLACE("список замены");

    private final String label;

    ChatCommand(String label) {
        this.label = label;
    }
}
