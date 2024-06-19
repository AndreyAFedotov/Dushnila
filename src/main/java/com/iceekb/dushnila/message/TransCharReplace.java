package com.iceekb.dushnila.message;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class TransCharReplace {

    private final Map<Character, Character> transData = new HashMap<>();

    public TransCharReplace() {
        initializeTransData();
    }

    public void initializeTransData() {
        addMapping('Q', 'Й');
        addMapping('q', 'й');
        addMapping('W', 'Ц');
        addMapping('w', 'ц');
        addMapping('E', 'У');
        addMapping('e', 'у');
        addMapping('R', 'К');
        addMapping('r', 'к');
        addMapping('T', 'Е');
        addMapping('t', 'е');
        addMapping('Y', 'Н');
        addMapping('y', 'н');
        addMapping('U', 'Г');
        addMapping('u', 'г');
        addMapping('I', 'Ш');
        addMapping('i', 'ш');
        addMapping('O', 'Щ');
        addMapping('o', 'щ');
        addMapping('P', 'З');
        addMapping('p', 'з');
        addMapping('{', 'Х');
        addMapping('[', 'х');
        addMapping('}', 'Ъ');
        addMapping(']', 'ъ');
        addMapping('A', 'Ф');
        addMapping('a', 'ф');
        addMapping('S', 'Ы');
        addMapping('s', 'ы');
        addMapping('D', 'В');
        addMapping('d', 'в');
        addMapping('F', 'А');
        addMapping('f', 'а');
        addMapping('G', 'П');
        addMapping('g', 'п');
        addMapping('H', 'Р');
        addMapping('h', 'р');
        addMapping('J', 'О');
        addMapping('j', 'о');
        addMapping('K', 'Л');
        addMapping('k', 'л');
        addMapping('L', 'Д');
        addMapping('l', 'д');
        addMapping(':', 'Ж');
        addMapping(';', 'ж');
        addMapping('"', 'Э');
        addMapping('\'', 'э');
        addMapping('|', '/');
        addMapping('~', 'Ë');
        addMapping('`', 'ё');
        addMapping('Z', 'Я');
        addMapping('z', 'я');
        addMapping('X', 'Ч');
        addMapping('x', 'ч');
        addMapping('C', 'С');
        addMapping('c', 'с');
        addMapping('V', 'М');
        addMapping('v', 'м');
        addMapping('B', 'И');
        addMapping('b', 'и');
        addMapping('N', 'Т');
        addMapping('n', 'т');
        addMapping('M', 'Ь');
        addMapping('m', 'ь');
        addMapping('<', 'Б');
        addMapping(',', 'б');
        addMapping('>', 'Ю');
        addMapping('.', 'ю');
        addMapping('?', ',');
        addMapping('/', '.');
    }

    private void addMapping(char from, char to) {
        transData.put(from, to);
    }

    public char getChar(char chr) {
        return transData.getOrDefault(chr, chr);
    }

    public String modifyTransString(String message) {
        return message.chars()
                .mapToObj(c -> String.valueOf(getChar((char) c)))
                .collect(Collectors.joining());
    }

    public boolean isTrans(Map<String, String> pairs) {
        long targetPairsCount = pairs.entrySet().stream()
                .filter(pair -> isEnglishChar(pair.getKey().charAt(0)) && !isEnglishChar(pair.getValue().charAt(0)))
                .count();
        return targetPairsCount >= pairs.size() * 0.7;
    }

    private boolean isEnglishChar(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }
}