package com.testtask.controller;

import com.testtask.dto.ExceededTransactionResponseDto;
import com.testtask.dto.TransactionRequestDto;
import com.testtask.entity.Transaction;
import com.testtask.mapper.AppMapper;
import com.testtask.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "Transactions API", description = "API для приёма расходных операций и получения превысивших лимит")
public class TransactionController {

    private final TransactionService transactionService;
    private final AppMapper appMapper;

    @PostMapping
    @Operation(
            summary = "Принять и обработать новую расходную операцию",
            description = "Принимает транзакцию, конвертирует сумму в USD по курсу на день операции, " +
                    "определяет превышение лимита и сохраняет в БД. " +
                    "Если лимит не установлен — используется дефолт 1000 USD."
    )
    @ApiResponse(responseCode = "201", description = "Транзакция успешно создана",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Transaction.class),
                    examples = @ExampleObject(value = """
                            {
                              "id": 5,
                              "accountFrom": "0000000123",
                              "accountTo": "9999999999",
                              "currencyShortname": "KZT",
                              "sum": 500000.00,
                              "expenseCategory": "PRODUCT",
                              "datetime": "2026-01-10T08:00:00Z",
                              "usdAmount": 986.00,
                              "limit": null,
                              "limitExceeded": false,
                              "createdAt": "2026-01-23T16:45:00Z",
                              "updatedAt": "2026-01-23T16:45:00Z"
                            }
                            """)))
    @ApiResponse(responseCode = "400", description = "Некорректные входные данные (валидация)")
    public ResponseEntity<Transaction> createTransaction(
            @Valid @RequestBody @Schema(description = "Данные новой транзакции") TransactionRequestDto requestDto) {

        Transaction transaction = appMapper.toEntity(requestDto);
        Transaction saved = transactionService.processAndSave(transaction);

        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/exceeded")
    @Operation(
            summary = "Получить список транзакций, превысивших лимит",
            description = "Возвращает только те транзакции, у которых limit_exceeded=true, " +
                    "с дополнительными полями лимита, который был превышен (или дефолтного 1000 USD)."
    )
    @ApiResponse(responseCode = "200", description = "Список превысивших транзакций (может быть пустым)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ExceededTransactionResponseDto.class)))
    public ResponseEntity<List<ExceededTransactionResponseDto>> getExceededTransactions() {
        List<ExceededTransactionResponseDto> exceeded = transactionService.getExceededTransactions();
        return ResponseEntity.ok(exceeded);
    }
}