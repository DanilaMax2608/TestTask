package com.testtask.service;

import com.testtask.dto.ExceededTransactionResponseDto;
import com.testtask.entity.Limit;
import com.testtask.entity.Transaction;
import com.testtask.model.ExpenseCategory;
import com.testtask.repository.LimitRepository;
import com.testtask.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private static final BigDecimal DEFAULT_LIMIT_SUM = new BigDecimal("1000.00");

    private final TransactionRepository transactionRepository;
    private final LimitRepository limitRepository;
    private final ExchangeRateService exchangeRateService;

    @Transactional
    public Transaction processAndSave(Transaction transaction) {
        ExpenseCategory category = transaction.getExpenseCategory();
        OffsetDateTime txDateTime = transaction.getDatetime();

        BigDecimal rate = exchangeRateService.getOrFetchRate(
                transaction.getCurrencyShortname(),
                txDateTime.toLocalDate()
        );
        BigDecimal usdAmount = transaction.getSum()
                .divide(rate, 2, RoundingMode.HALF_UP);
        transaction.setUsdAmount(usdAmount);

        Limit applicableLimit = limitRepository
                .findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                        category, txDateTime)
                .orElse(null);
        transaction.setLimit(applicableLimit);

        OffsetDateTime monthStart = txDateTime
                .with(TemporalAdjusters.firstDayOfMonth())
                .withHour(0).withMinute(0).withSecond(0).withNano(0);

        BigDecimal effectiveLimitSum;
        OffsetDateTime limitEffectiveFrom;

        if (applicableLimit != null) {
            effectiveLimitSum = applicableLimit.getLimitSum();
            Limit previousLimit = limitRepository
                    .findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(
                            category, applicableLimit.getLimitDatetime())
                    .orElse(null);

            if (previousLimit != null) {
                limitEffectiveFrom = previousLimit.getLimitDatetime();
            } else {
                limitEffectiveFrom = applicableLimit.getLimitDatetime()
                        .with(TemporalAdjusters.firstDayOfMonth())
                        .withHour(0).withMinute(0).withSecond(0).withNano(0);
            }
        } else {
            effectiveLimitSum = DEFAULT_LIMIT_SUM;
            limitEffectiveFrom = monthStart;
        }

        BigDecimal spentInMonth = transactionRepository.sumUsdAfterLimit(
                category,
                monthStart,
                txDateTime
        );

        boolean exceeded = spentInMonth.add(usdAmount).compareTo(effectiveLimitSum) > 0;
        transaction.setLimitExceeded(exceeded);

        Transaction saved = transactionRepository.save(transaction);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<ExceededTransactionResponseDto> getExceededTransactions() {
        return transactionRepository.findAllExceededWithLimitInfo();
    }
}