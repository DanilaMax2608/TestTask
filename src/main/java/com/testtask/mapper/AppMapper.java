package com.testtask.mapper;

import com.testtask.dto.*;
import com.testtask.entity.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.OffsetDateTime;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AppMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "usdAmount", ignore = true)
    @Mapping(target = "limit", ignore = true)
    @Mapping(target = "limitExceeded", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Transaction toEntity(TransactionRequestDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "limitDatetime", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "currency", constant = "USD")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Limit toEntity(LimitRequestDto dto);

    LimitResponseDto toResponseDto(Limit entity);
}