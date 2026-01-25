package com.testtask.service;

import com.testtask.entity.ExchangeRate;
import com.testtask.repository.ExchangeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final WebClient exchangeRateWebClient;
    private final ExchangeRateRepository exchangeRateRepository;

    @Value("${alphavantage.api-key}")
    private String apiKey;

    private static final String BASE_CURRENCY = "USD";
    private static final String[] TARGET_CURRENCIES = {"KZT", "RUB"};

    @Transactional(readOnly = true)
    public Optional<BigDecimal> getRate(String targetCurrency, LocalDate date) {
        if (!isSupportedCurrency(targetCurrency)) {
            throw new IllegalArgumentException("Unsupported currency: " + targetCurrency);
        }

        Optional<ExchangeRate> exact = exchangeRateRepository.findByBaseCurrencyAndTargetCurrencyAndRateDate(
                BASE_CURRENCY, targetCurrency, date);

        if (exact.isPresent()) {
            return Optional.of(exact.get().getRate());
        }

        Optional<ExchangeRate> latest = exchangeRateRepository
                .findFirstByBaseCurrencyAndTargetCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                        BASE_CURRENCY, targetCurrency, date);

        if (latest.isPresent()) {
            return Optional.of(latest.get().getRate());
        }

        return Optional.empty();
    }

    @Transactional
    public BigDecimal fetchAndSaveRate(String targetCurrency, LocalDate date) {
        Optional<BigDecimal> existing = getRate(targetCurrency, date);
        if (existing.isPresent()) {
            return existing.get();
        }

        return exchangeRateWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/query")
                        .queryParam("function", "FX_DAILY")
                        .queryParam("from_symbol", BASE_CURRENCY)
                        .queryParam("to_symbol", targetCurrency)
                        .queryParam("apikey", apiKey)
                        .build())
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                        clientResponse -> clientResponse.bodyToMono(String.class)
                                .map(body -> {
                                    return new RuntimeException("Alpha Vantage error: " + clientResponse.statusCode() + " - " + body);
                                }))
                .bodyToMono(Map.class)
                .flatMap(response -> {
                    if (response.containsKey("Error Message") || response.containsKey("Note")) {
                        String errorMsg = (String) response.getOrDefault("Error Message", response.get("Note"));
                        return Mono.error(new RuntimeException("Alpha Vantage API error: " + errorMsg));
                    }

                    @SuppressWarnings("unchecked")
                    Map<String, Map<String, String>> timeSeries =
                            (Map<String, Map<String, String>>) response.get("Time Series FX (Daily)");

                    if (timeSeries == null || timeSeries.isEmpty()) {
                        return Mono.error(new RuntimeException("No time series data for USD/" + targetCurrency));
                    }

                    Map<String, String> dayData = timeSeries.get(date.toString());
                    BigDecimal rate = null;

                    if (dayData != null) {
                        String closeStr = dayData.get("4. close");
                        if (closeStr != null) {
                            rate = new BigDecimal(closeStr);
                        }
                    }

                    if (rate == null) {
                        Optional<Map.Entry<String, Map<String, String>>> closest = timeSeries.entrySet().stream()
                                .filter(e -> {
                                    try {
                                        LocalDate entryDate = LocalDate.parse(e.getKey());
                                        return !entryDate.isAfter(date);
                                    } catch (Exception ex) {
                                        return false;
                                    }
                                })
                                .max(Comparator.comparing(e -> LocalDate.parse(e.getKey())));

                        if (closest.isPresent()) {
                            String closestDate = closest.get().getKey();
                            String closeStr = closest.get().getValue().get("4. close");
                            if (closeStr != null) {
                                rate = new BigDecimal(closeStr);
                            }
                        }
                    }

                    if (rate == null) {
                        return Mono.error(new RuntimeException("No suitable close rate found for or before " + date));
                    }

                    ExchangeRate entity = ExchangeRate.builder()
                            .baseCurrency(BASE_CURRENCY)
                            .targetCurrency(targetCurrency)
                            .rateDate(date)
                            .rate(rate)
                            .source("alphavantage.co")
                            .build();

                    exchangeRateRepository.save(entity);

                    return Mono.just(rate);
                })
                .block();
    }

    public BigDecimal getOrFetchRate(String targetCurrency, LocalDate date) {
        return getRate(targetCurrency, date)
                .orElseGet(() -> fetchAndSaveRate(targetCurrency, date));
    }

    private boolean isSupportedCurrency(String currency) {
        for (String supported : TARGET_CURRENCIES) {
            if (supported.equals(currency)) return true;
        }
        return false;
    }
}