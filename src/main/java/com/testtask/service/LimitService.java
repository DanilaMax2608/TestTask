package com.testtask.service;

import com.testtask.dto.LimitRequestDto;
import com.testtask.dto.LimitResponseDto;
import com.testtask.entity.Limit;
import com.testtask.mapper.AppMapper;
import com.testtask.model.ExpenseCategory;
import com.testtask.repository.LimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LimitService {

    private final LimitRepository limitRepository;
    private final AppMapper appMapper;

    @Transactional
    public Limit createLimit(LimitRequestDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        Limit limit = appMapper.toEntity(dto);

        Limit saved = limitRepository.save(limit);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<LimitResponseDto> getAllLimits() {
        List<Limit> limits = limitRepository.findAllByOrderByLimitDatetimeDesc();

        List<LimitResponseDto> dtos = limits.stream()
                .map(appMapper::toResponseDto)
                .collect(Collectors.toList());

        return dtos;
    }
}