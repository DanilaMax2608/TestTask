package com.testtask.dto;

import com.testtask.model.ExpenseCategory;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Builder
public record TransactionRequestDto(

        @NotBlank @Size(min = 1, max = 20) String accountFrom,
        @NotBlank @Size(min = 1, max = 20) String accountTo,

        @NotBlank @Size(min = 3, max = 3) String currencyShortname,

        @NotNull @DecimalMin("0.01") @Digits(integer = 13, fraction = 2) BigDecimal sum,

        @NotNull ExpenseCategory expenseCategory,

        @NotNull @PastOrPresent OffsetDateTime datetime

) {
}