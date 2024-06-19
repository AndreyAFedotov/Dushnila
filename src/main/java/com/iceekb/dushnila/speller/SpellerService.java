package com.iceekb.dushnila.speller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceekb.dushnila.properties.LastMessage;
import com.iceekb.dushnila.message.TransCharReplace;
import com.iceekb.dushnila.message.enums.ResponseTypes;
import com.iceekb.dushnila.message.util.TextUtil;
import com.iceekb.dushnila.message.dto.SpellerIncomingDataWord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class SpellerService {

    private static final Integer ERROR_WORDS_LIMIT = 5;
    private static final String SPELLER_URL = "https://speller.yandex.net/services/spellservice.json/checkText";
    private static final String SPELLER_OPTIONS = "518";
    private static final Integer CONNECTION_TIMEOUT = 2;
    private static final Integer READ_TIMEOUT = 2;

    private final RestTemplate restTemplate = restTemplate(new RestTemplateBuilder());
    private final TransCharReplace transCR;

    public LastMessage speller(LastMessage lastMessage) {
        String message = lastMessage.getReceivedMessage();

        try {
            List<SpellerIncomingDataWord> data = getData(message);
            Map<String, String> pairs = extractPairs(data);

            if (!pairs.isEmpty()) {
                handlePossibleTransposition(lastMessage, message, pairs, transCR);
            }
            if (!lastMessage.getResponse().isEmpty()) return lastMessage;

            if (!pairs.isEmpty()) {
                handleErrors(lastMessage, pairs);
            }
        } catch (JsonProcessingException e) {
            log.error("Speller error!", e);
            lastMessage.setError(true);
        }
        return lastMessage;
    }

    private List<SpellerIncomingDataWord> getData(String text) throws JsonProcessingException {
        String query = text.replace(" ", "+");
        String url = SPELLER_URL + "?options=" + SPELLER_OPTIONS + "&text=" + query;

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            SpellerIncomingDataWord[] data = objectMapper.readValue(response.getBody(), SpellerIncomingDataWord[].class);

            return filterData(data);
        } catch (RestClientException | IOException e) {
            log.error("Error querying Speller API: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private Map<String, String> extractPairs(List<SpellerIncomingDataWord> data) {
        return data.stream()
                .collect(Collectors.toMap(SpellerIncomingDataWord::getWord, word -> word.getS().get(0)));
    }

    private void handlePossibleTransposition(LastMessage lastMessage,
                                             String message,
                                             Map<String, String> pairs,
                                             TransCharReplace transCR) {
        if (transCR.isTrans(pairs)) {
            lastMessage.setResponse("Я помогу... \"" + transCR.modifyTransString(message) + "\"");
        }
    }

    private void handleErrors(LastMessage lastMessage, Map<String, String> pairs) {
        StringBuilder result = new StringBuilder();
        int wordCount = 0;

        for (Map.Entry<String, String> pair : pairs.entrySet()) {
            if (!pair.getKey().equals(pair.getValue())) {
                result.append(String.format(Objects.requireNonNull(TextUtil.nextAutoMessage(ResponseTypes.SPELLER)),
                                pair.getKey(),
                                pair.getValue()))
                        .append(" ");
                wordCount++;
            }
        }

        if (wordCount > ERROR_WORDS_LIMIT) {
            result.setLength(0);
            result.append("Тут слишком много ошибок ;-) (")
                    .append(wordCount)
                    .append(" слов)");
        }

        if (StringUtils.isNotBlank(result)) {
            lastMessage.setResponse(result.toString());
        }
    }

    private List<SpellerIncomingDataWord> filterData(SpellerIncomingDataWord[] data) {
        List<SpellerIncomingDataWord> result = new ArrayList<>();
        for (SpellerIncomingDataWord word : data) {
            if (!word.getWord().equals(word.getS().get(0))) {
                result.add(word);
            }
        }
        return result;
    }

    private RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Duration.of(CONNECTION_TIMEOUT, ChronoUnit.SECONDS))
                .setReadTimeout(Duration.of(READ_TIMEOUT, ChronoUnit.SECONDS))
                .build();
    }
}
