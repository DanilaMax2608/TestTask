package com.testtask.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.testtask.dto.ExceededTransactionResponseDto;
import com.testtask.dto.LimitRequestDto;
import com.testtask.dto.LimitResponseDto;
import com.testtask.dto.TransactionRequestDto;
import com.testtask.entity.Transaction;
import com.testtask.model.ExpenseCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/transactions";
    }

    private String getLimitsUrl() {
        return "http://localhost:" + port + "/api/limits";
    }

    public TransactionControllerIntegrationTest() {
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(HttpStatusCode statusCode) {
                return statusCode.is5xxServerError();
            }
        });
    }

    @BeforeEach
    void cleanDatabaseBeforeEach() {
        jdbcTemplate.execute("DELETE FROM transactions");
        jdbcTemplate.execute("DELETE FROM limits");
    }

    @Test
    void createTransactionValidRequest() {
        TransactionRequestDto requestDto = TransactionRequestDto.builder()
                .accountFrom("11111111111111111111")
                .accountTo("99999999999999999999")
                .currencyShortname("KZT")
                .sum(new BigDecimal("50000.00"))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .datetime(OffsetDateTime.now().minusDays(1))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TransactionRequestDto> entity = new HttpEntity<>(requestDto, headers);

        ResponseEntity<Transaction> response = restTemplate.exchange(
                getBaseUrl(),
                HttpMethod.POST,
                entity,
                Transaction.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Transaction saved = response.getBody();
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull().isPositive();
        assertThat(saved.getUsdAmount()).isNotNull().isGreaterThan(BigDecimal.ZERO);
        assertThat(saved.isLimitExceeded()).isIn(true, false);
        assertThat(saved.getExpenseCategory()).isEqualTo(ExpenseCategory.PRODUCT);
    }

    @Test
    void createTransactionInvalidSum() {
        TransactionRequestDto invalidDto = TransactionRequestDto.builder()
                .accountFrom("12345678901234567890")
                .accountTo("09876543210987654321")
                .currencyShortname("KZT")
                .sum(BigDecimal.ZERO)
                .expenseCategory(ExpenseCategory.SERVICE)
                .datetime(OffsetDateTime.now().minusDays(1))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TransactionRequestDto> entity = new HttpEntity<>(invalidDto, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl(),
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).contains("\"status\":400");
        assertThat(body).contains("\"message\":\"Validation failed\"");
        assertThat(body).contains("\"sum\":");
        assertThat(body).contains("должно быть больше, чем или равно 0.01");
    }

    @Test
    void createTransactionMissingCurrency() {
        TransactionRequestDto brokenDto = TransactionRequestDto.builder()
                .accountFrom("12345678901234567890")
                .accountTo("09876543210987654321")
                .sum(new BigDecimal("1000.00"))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .datetime(OffsetDateTime.now().minusDays(1))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TransactionRequestDto> entity = new HttpEntity<>(brokenDto, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl(),
                HttpMethod.POST,
                entity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        String body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body).contains("\"status\":400");
        assertThat(body).contains("\"message\":\"Validation failed\"");
        assertThat(body).contains("\"currencyShortname\":");
        assertThat(body).contains("не должно быть пустым");
    }

    @Test
    void createSeveralTransactionsLimitChangedMidMonth() {
        LimitRequestDto limit1 = new LimitRequestDto(ExpenseCategory.PRODUCT, new BigDecimal("1000.00"));
        restTemplate.postForEntity(getLimitsUrl(), limit1, LimitResponseDto.class);

        TransactionRequestDto tx1 = TransactionRequestDto.builder()
                .accountFrom("1111222233334444")
                .accountTo("9999888877776666")
                .currencyShortname("KZT")
                .sum(new BigDecimal("200000.00"))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .datetime(OffsetDateTime.parse("2026-01-05T12:00:00Z"))
                .build();
        restTemplate.postForEntity(getBaseUrl(), tx1, Transaction.class);

        TransactionRequestDto tx2 = TransactionRequestDto.builder()
                .accountFrom("1111222233334444")
                .accountTo("9999888877776666")
                .currencyShortname("KZT")
                .sum(new BigDecimal("350000.00"))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .datetime(OffsetDateTime.parse("2026-01-10T14:00:00Z"))
                .build();
        restTemplate.postForEntity(getBaseUrl(), tx2, Transaction.class);

        LimitRequestDto limit2 = new LimitRequestDto(ExpenseCategory.PRODUCT, new BigDecimal("2000.00"));
        restTemplate.postForEntity(getLimitsUrl(), limit2, LimitResponseDto.class);

        TransactionRequestDto tx3 = TransactionRequestDto.builder()
                .accountFrom("1111222233334444")
                .accountTo("9999888877776666")
                .currencyShortname("KZT")
                .sum(new BigDecimal("250000.00"))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .datetime(OffsetDateTime.parse("2026-01-12T15:00:00Z"))
                .build();
        restTemplate.postForEntity(getBaseUrl(), tx3, Transaction.class);

        ResponseEntity<ExceededTransactionResponseDto[]> response = restTemplate.getForEntity(
                getBaseUrl() + "/exceeded",
                ExceededTransactionResponseDto[].class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ExceededTransactionResponseDto[] exceeded = response.getBody();
        assertThat(exceeded)
                .as("Должны попасть две транзакции, превысившие лимит на момент своей даты")
                .hasSize(2);

        assertThat(exceeded[0].id()).isGreaterThan(exceeded[1].id());
        assertThat(exceeded[0].usdAmount()).isEqualByComparingTo("487.47");
        assertThat(exceeded[0].limitSum()).isEqualByComparingTo("1000.00");

        assertThat(exceeded[1].usdAmount()).isEqualByComparingTo("682.46");
        assertThat(exceeded[1].limitSum()).isEqualByComparingTo("1000.00");
    }
}