package com.smartwater.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.smartwater.web.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/** Razorpay Orders API over plain HTTPS: POST /v1/orders with HTTP Basic auth (key id : key secret). */
@Component
@Slf4j
public class RazorpayHttpGateway implements RazorpayGateway {
    private final String keyId;
    private final String keySecret;
    private final RestClient client;

    public RazorpayHttpGateway(@Value("${app.razorpay.key-id:}") String keyId,
                               @Value("${app.razorpay.key-secret:}") String keySecret,
                               @Value("${app.razorpay.base-url:https://api.razorpay.com/v1}") String baseUrl) {
        this.keyId = keyId;
        this.keySecret = keySecret;
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override public boolean configured() { return !keyId.isBlank() && !keySecret.isBlank(); }

    @Override public String keyId() { return keyId; }

    @Override
    public Order createOrder(long amountPaise, String currency, String receipt, Map<String, String> notes) {
        if (!configured()) throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Online payments are not configured");
        Map<String, Object> body = Map.of("amount", amountPaise, "currency", currency, "receipt", receipt, "notes", notes);
        try {
            JsonNode res = client.post().uri("/orders")
                    .headers(h -> h.setBasicAuth(keyId, keySecret))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            if (res == null || !res.hasNonNull("id"))
                throw new ApiException(HttpStatus.BAD_GATEWAY, "Payment gateway returned no order");
            return new Order(res.get("id").asText(), res.path("amount").asLong(amountPaise), res.path("currency").asText(currency));
        } catch (RestClientResponseException e) {
            log.warn("Razorpay rejected the order request: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The payment gateway rejected the request");
        } catch (RestClientException e) {
            log.warn("Razorpay is unreachable: {}", e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The payment gateway is unreachable, please try again");
        }
    }
}
