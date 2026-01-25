package com.testtask.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ExceededTransactionResponseDto(
        Long id,
        String accountFrom,
        String accountTo,
        String currencyShortname,
        BigDecimal sum,
        String expenseCategory,
        Instant datetime,
        BigDecimal usdAmount,

        BigDecimal limitSum,
        Instant limitDatetime,
        String limitCurrencyShortname
) {
}