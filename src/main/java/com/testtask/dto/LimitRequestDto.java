package com.testtask.dto;

import com.testtask.model.ExpenseCategory;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record LimitRequestDto(

        @NotNull(message = "Категория обязательна")
        ExpenseCategory category,

        @NotNull(message = "Сумма лимита обязательна")
        @DecimalMin(value = "0.01", message = "Лимит должен быть больше 0")
        @Digits(integer = 13, fraction = 2, message = "Неверный формат суммы")
        BigDecimal limitSum

) {
}