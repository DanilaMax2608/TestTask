package com.testtask.repository;

import com.testtask.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByBaseCurrencyAndTargetCurrencyAndRateDate(
            String baseCurrency, String targetCurrency, LocalDate rateDate);

    Optional<ExchangeRate> findFirstByBaseCurrencyAndTargetCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
            String baseCurrency, String targetCurrency, LocalDate date);
}