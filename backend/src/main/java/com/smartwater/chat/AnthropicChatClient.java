package com.smartwater.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartwater.web.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Calls the Anthropic Messages API (POST /v1/messages) over plain HTTPS - no extra library. */
@Component
@Slf4j
public class AnthropicChatClient implements ChatClient {
    private final String apiKey;
    private final String model;
    private final int maxTokens;
    private final RestClient client;

    public AnthropicChatClient(@Value("${app.chatbot.api-key:}") String apiKey,
                               @Value("${app.chatbot.model:claude-haiku-5-5}") String model,
                               @Value("${app.chatbot.max-tokens:600}") int maxTokens,
                               @Value("${app.chatbot.base-url:https://api.anthropic.com}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(30_000);                     // never let a slow model call hang a request thread forever
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override public boolean configured() { return !apiKey.isBlank(); }

    @Override
    public String complete(String system, List<Turn> messages) {
        if (!configured()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "The assistant is not configured");
        List<Map<String, String>> msgs = new ArrayList<>();
        for (Turn t : messages) msgs.add(Map.of("role", t.role(), "content", t.content()));
        Map<String, Object> body = Map.of("model", model, "max_tokens", maxTokens, "system", system, "messages", msgs);
        try {
            JsonNode res = client.post().uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            return extractText(res);
        } catch (RestClientResponseException e) {
            log.warn("Anthropic API error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The assistant is unavailable right now");
        } catch (RestClientException e) {
            log.warn("Anthropic API unreachable: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The assistant is unreachable right now");
        }
    }

    /** The reply is a list of content blocks; we only use the text ones. */
    static String extractText(JsonNode response) {
        StringBuilder sb = new StringBuilder();
        if (response != null) {
            for (JsonNode block : response.path("content")) {
                if ("text".equals(block.path("type").asText()) && block.hasNonNull("text")) {
                    if (sb.length() > 0) sb.append('\n');
                    sb.append(block.get("text").asText());
                }
            }
        }
        if (sb.toString().isBlank()) throw new ApiException(HttpStatus.BAD_GATEWAY, "The assistant returned an empty answer");
        return sb.toString().trim();
    }
}
