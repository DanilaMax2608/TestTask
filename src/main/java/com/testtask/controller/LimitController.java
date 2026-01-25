package com.testtask.controller;

import com.testtask.dto.LimitRequestDto;
import com.testtask.dto.LimitResponseDto;
import com.testtask.entity.Limit;
import com.testtask.mapper.AppMapper;
import com.testtask.service.LimitService;
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
@RequestMapping("/api/limits")
@RequiredArgsConstructor
@Tag(name = "Limits API", description = "API для управления месячными лимитами расходов")
public class LimitController {

    private final LimitService limitService;
    private final AppMapper appMapper;

    @PostMapping
    @Operation(
            summary = "Установить новый месячный лимит",
            description = "Создаёт новую запись лимита. Дата и время установки выставляются автоматически. " +
                    "Обновление существующих лимитов запрещено."
    )
    @ApiResponse(responseCode = "201", description = "Лимит успешно создан",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LimitResponseDto.class),
                    examples = @ExampleObject(value = """
                            {
                              "id": 1,
                              "category": "PRODUCT",
                              "limitSum": 1500.00,
                              "limitDatetime": "2026-01-23T16:30:00+03:00",
                              "currency": "USD"
                            }
                            """)))
    @ApiResponse(responseCode = "400", description = "Некорректные входные данные (валидация)")
    public ResponseEntity<LimitResponseDto> createLimit(
            @Valid @RequestBody @Schema(description = "Данные нового лимита") LimitRequestDto requestDto) {

        Limit saved = limitService.createLimit(requestDto);
        LimitResponseDto response = appMapper.toResponseDto(saved);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Получить все установленные лимиты",
            description = "Возвращает историю лимитов, отсортированную по дате установки DESC (новые сверху)."
    )
    @ApiResponse(responseCode = "200", description = "Список лимитов (может быть пустым)",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LimitResponseDto.class)))
    public ResponseEntity<List<LimitResponseDto>> getAllLimits() {
        List<LimitResponseDto> limits = limitService.getAllLimits();
        return ResponseEntity.ok(limits);
    }
}