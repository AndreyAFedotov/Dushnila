package com.iceekb.dushnila.speller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iceekb.dushnila.message.TransCharReplace;
import com.iceekb.dushnila.message.dto.SpellerIncomingDataWord;
import com.iceekb.dushnila.message.enums.ResponseTypes;
import com.iceekb.dushnila.message.responses.AutoResponseService;
import com.iceekb.dushnila.properties.LastMessageTxt;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@Data
public class SpellerService {

    private static final Integer ERROR_WORDS_LIMIT = 5;
    private static final String SPELLER_URL = "https://speller.yandex.net/services/spellservice.json/checkText";
    private static final String SPELLER_OPTIONS = "518";
    private static final Integer CONNECTION_TIMEOUT = 2;
    private static final Integer READ_TIMEOUT = 2;

    private WebClient webClient;
    private TransCharReplace transCR;
    private ObjectMapper objectMapper;
    private AutoResponseService autoResponseService;

    public SpellerService(TransCharReplace transCR, ObjectMapper objectMapper, AutoResponseService autoResponseService) {
        this.transCR = transCR;
        this.objectMapper = objectMapper;
        this.autoResponseService = autoResponseService;
        this.webClient = WebClient.builder()
                .baseUrl(SPELLER_URL)
                .build();
    }

    public LastMessageTxt speller(LastMessageTxt lastMessage) {
        String message = lastMessage.getReceivedMessage();

        try {
            List<SpellerIncomingDataWord> data = getData(message).block();
            Map<String, String> pairs = extractPairs(data);

            if (!pairs.isEmpty()) {
                handlePossibleTransposition(lastMessage, message, pairs, transCR);
            }
            if (lastMessage.getResponse() != null && !lastMessage.getResponse().isEmpty()) {
                return lastMessage;
            }

            if (!pairs.isEmpty()) {
                handleErrors(lastMessage, pairs);
            }
        } catch (Exception e) {
            log.error("Speller error: {}", e.getMessage());
            lastMessage.setError(true);
        }
        return lastMessage;
    }

    private Mono<List<SpellerIncomingDataWord>> getData(String text) {
        if (text == null || text.isEmpty()) {
            return Mono.just(List.of());
        }

        String query = text.replace(" ", "+");

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("options", SPELLER_OPTIONS)
                        .queryParam("text", query)
                        .build())
                .exchangeToMono(this::getListMono)
                .timeout(Duration.ofSeconds(READ_TIMEOUT))
                .onErrorResume(e -> {
                    log.error("Error querying Speller API: {}", e.getMessage());
                    return Mono.just(List.of());
                });
    }

    @NotNull
    private Mono<List<SpellerIncomingDataWord>> getListMono(ClientResponse response) {
        if (response.statusCode().equals(HttpStatus.OK)) {
            return response.bodyToMono(String.class)
                    .map(body -> {
                        try {
                            SpellerIncomingDataWord[] data = objectMapper.readValue(body, SpellerIncomingDataWord[].class);
                            return filterData(data);
                        } catch (JsonProcessingException e) {
                            log.error("Error parsing Speller response", e);
                            return List.of();
                        }
                    });
        } else {
            log.error("Speller API returned status: {}", response.statusCode());
            return Mono.just(List.of());
        }
    }

    private Map<String, String> extractPairs(List<SpellerIncomingDataWord> data) {
        if (data == null || data.isEmpty()) {
            return Map.of();
        }

        return data.stream()
                .collect(Collectors.toMap(
                        SpellerIncomingDataWord::getWord,
                        word -> word.getS().get(0),
                        (existingValue, newValue) -> existingValue
                ));
    }

    private void handlePossibleTransposition(LastMessageTxt lastMessage,
                                             String message,
                                             Map<String, String> pairs,
                                             TransCharReplace transCR) {
        if (transCR.isTrans(pairs)) {
            lastMessage.setResponse("Я помогу... \"" + transCR.modifyTransString(message) + "\"");
        }
    }

    private void handleErrors(LastMessageTxt lastMessage, Map<String, String> pairs) {
        StringBuilder result = new StringBuilder();
        int wordCount = 0;

        for (Map.Entry<String, String> pair : pairs.entrySet()) {
            if (!pair.getKey().equals(pair.getValue())) {
                result.append(String.format(Objects.requireNonNull(autoResponseService.getMessage(ResponseTypes.PUBLIC)),
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
}