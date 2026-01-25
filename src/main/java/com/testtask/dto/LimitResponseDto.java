package com.testtask.dto;

import com.testtask.model.ExpenseCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record LimitResponseDto(

        Long id,
        ExpenseCategory category,
        BigDecimal limitSum,
        OffsetDateTime limitDatetime,
        String currency

) {
}