package com.iceekb.dushnila.message.enums;

@SuppressWarnings("FieldCanBeLocal")
public enum ChatCommand {
    CREPLACE("замена (/creplace \"слово\" \"на слово/фраза\")"),
    DREPLACE("удалить замену (/dreplace \"слово\")"),
    STAT("статистика группы"),
    HELP("справка"),
    CIGNORE("игнор слова/фразы/маски (/cignore \"слово\" | \"фраза\" | \"маска*\")"),
    DIGNORE(" удалить игнор (/dignore \"слово/фраза/маска*\")"),
    LIGNORE("список игнора"),
    LREPLACE("список замены");

    private final String label;

    ChatCommand(String label) {
        this.label = label;
    }
}
