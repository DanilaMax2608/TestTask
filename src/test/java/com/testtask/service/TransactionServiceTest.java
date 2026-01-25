package com.testtask.service;

import com.testtask.entity.Limit;
import com.testtask.entity.Transaction;
import com.testtask.model.ExpenseCategory;
import com.testtask.repository.LimitRepository;
import com.testtask.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LimitRepository limitRepository;

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private TransactionService transactionService;

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        transaction = Transaction.builder()
                .accountFrom("123")
                .accountTo("456")
                .currencyShortname("KZT")
                .sum(new BigDecimal("500.00"))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .datetime(OffsetDateTime.of(
                        2026, 1, 10, 12, 0, 0, 0, ZoneOffset.UTC))
                .build();
    }

    @Test
    void processAndSaveNoLimitDefaultLimitNotExceeded() {
        when(exchangeRateService.getOrFetchRate(eq("KZT"), any()))
                .thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                any(), any()))
                .thenReturn(java.util.Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(
                any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getUsdAmount()).isEqualByComparingTo("1.00");
        assertThat(saved.isLimitExceeded()).isFalse();
        assertThat(saved.getLimit()).isNull();
    }

    @Test
    void processAndSaveNoLimitDefaultLimitExceeded() {
        when(exchangeRateService.getOrFetchRate(eq("KZT"), any()))
                .thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                any(), any()))
                .thenReturn(java.util.Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(
                any(), any(), any()))
                .thenReturn(new BigDecimal("999.00"));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transaction.setSum(new BigDecimal("1000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getUsdAmount()).isEqualByComparingTo("2.00");
        assertThat(saved.isLimitExceeded()).isTrue();
        assertThat(saved.getLimit()).isNull();
    }

    @Test
    void processAndSaveWithLimitNotExceeded() {
        var limit = com.testtask.entity.Limit.builder()
                .id(1L)
                .category(ExpenseCategory.PRODUCT)
                .limitSum(new BigDecimal("1500.00"))
                .limitDatetime(OffsetDateTime.of(
                        2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .currency("USD")
                .build();

        when(exchangeRateService.getOrFetchRate(eq("KZT"), any()))
                .thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), any()))
                .thenReturn(java.util.Optional.of(limit));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), eq(limit.getLimitDatetime())))
                .thenReturn(java.util.Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(
                any(), any(), any()))
                .thenReturn(new BigDecimal("1000.00"));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transaction.setSum(new BigDecimal("200000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getUsdAmount()).isEqualByComparingTo("400.00");
        assertThat(saved.isLimitExceeded()).isFalse();
        assertThat(saved.getLimit()).isEqualTo(limit);
    }

    @Test
    void processAndSaveWithLimitExceeded() {
        var limit = com.testtask.entity.Limit.builder()
                .id(1L)
                .category(ExpenseCategory.PRODUCT)
                .limitSum(new BigDecimal("1500.00"))
                .limitDatetime(OffsetDateTime.of(
                        2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .currency("USD")
                .build();

        when(exchangeRateService.getOrFetchRate(eq("KZT"), any()))
                .thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), any()))
                .thenReturn(Optional.of(limit));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), eq(limit.getLimitDatetime())))
                .thenReturn(Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(any(), any(), any()))
                .thenReturn(new BigDecimal("1200.00"));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transaction.setSum(new BigDecimal("200000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getUsdAmount()).isEqualByComparingTo("400.00");
        assertThat(saved.isLimitExceeded()).isTrue();
        assertThat(saved.getLimit()).isEqualTo(limit);
    }

    @Test
    void processAndSaveWithLimitExactlyOnLimitNotExceeded() {
        var limit = com.testtask.entity.Limit.builder()
                .id(1L)
                .category(ExpenseCategory.PRODUCT)
                .limitSum(new BigDecimal("1500.00"))
                .limitDatetime(OffsetDateTime.of(
                        2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .currency("USD")
                .build();

        when(exchangeRateService.getOrFetchRate(eq("KZT"), any()))
                .thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), any()))
                .thenReturn(Optional.of(limit));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), eq(limit.getLimitDatetime())))
                .thenReturn(Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(any(), any(), any()))
                .thenReturn(new BigDecimal("1100.00"));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        transaction.setSum(new BigDecimal("200000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getUsdAmount()).isEqualByComparingTo("400.00");
        assertThat(saved.isLimitExceeded()).isFalse();
        assertThat(saved.getLimit()).isEqualTo(limit);
    }

    @Test
    void processAndSaveTransactionBeforeFirstLimitInMonthUsesDefaultLimitFromMonthStart() {
        when(exchangeRateService.getOrFetchRate(eq("KZT"), any()))
                .thenReturn(new BigDecimal("500.00"));

        OffsetDateTime txDateTime = OffsetDateTime.of(2026, 1, 5, 14, 0, 0, 0, ZoneOffset.UTC);

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), eq(txDateTime)))
                .thenReturn(Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(
                eq(ExpenseCategory.PRODUCT),
                argThat(start -> start.getDayOfMonth() == 1 && start.getHour() == 0),
                eq(txDateTime)))
                .thenReturn(BigDecimal.ZERO);

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transaction.setDatetime(txDateTime);
        transaction.setSum(new BigDecimal("600000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getUsdAmount()).isEqualByComparingTo("1200.00");
        assertThat(saved.isLimitExceeded()).isTrue();
        assertThat(saved.getLimit()).isNull();
    }


    @Test
    void processAndSaveTwoLimitsInSameMonthSecondLimitAppliedAfterItsDate() {
        OffsetDateTime txDate2 = OffsetDateTime.of(2026, 1, 12, 14, 30, 0, 0, ZoneOffset.UTC);

        Limit limit1 = Limit.builder()
                .id(1L)
                .category(ExpenseCategory.PRODUCT)
                .limitSum(new BigDecimal("800.00"))
                .limitDatetime(OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .currency("USD")
                .build();

        Limit limit2 = Limit.builder()
                .id(2L)
                .category(ExpenseCategory.PRODUCT)
                .limitSum(new BigDecimal("2000.00"))
                .limitDatetime(OffsetDateTime.of(2026, 1, 10, 9, 0, 0, 0, ZoneOffset.UTC))
                .currency("USD")
                .build();

        when(exchangeRateService.getOrFetchRate(anyString(), any())).thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), eq(txDate2)))
                .thenReturn(Optional.of(limit2));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), eq(limit2.getLimitDatetime())))
                .thenReturn(Optional.of(limit1));

        when(transactionRepository.sumUsdAfterLimit(any(), any(), eq(txDate2)))
                .thenReturn(new BigDecimal("700.00"));

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transaction.setDatetime(txDate2);
        transaction.setSum(new BigDecimal("800000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getUsdAmount()).isEqualByComparingTo("1600.00");
        assertThat(saved.isLimitExceeded()).isTrue();
        assertThat(saved.getLimit()).isEqualTo(limit2);
    }


    @Test
    void processAndSaveTransactionExactlyOnNewLimitDateUsesNewLimit() {
        OffsetDateTime limitChangeTime = OffsetDateTime.of(2026, 1, 10, 14, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime txTime = OffsetDateTime.of(2026, 1, 10, 14, 0, 0, 0, ZoneOffset.UTC);

        Limit oldLimit = Limit.builder()
                .limitSum(new BigDecimal("1000.00"))
                .limitDatetime(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .currency("USD")
                .build();

        Limit newLimit = Limit.builder()
                .limitSum(new BigDecimal("3000.00"))
                .limitDatetime(limitChangeTime)
                .currency("USD")
                .build();

        when(exchangeRateService.getOrFetchRate(anyString(), any())).thenReturn(new BigDecimal("500"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                any(), eq(txTime)))
                .thenReturn(Optional.of(newLimit));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(
                any(), eq(newLimit.getLimitDatetime())))
                .thenReturn(Optional.of(oldLimit));

        when(transactionRepository.sumUsdAfterLimit(any(), any(), eq(txTime)))
                .thenReturn(new BigDecimal("2500.00"));

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transaction.setDatetime(txTime);
        transaction.setSum(new BigDecimal("600000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getLimit()).isEqualTo(newLimit);
        assertThat(saved.isLimitExceeded()).isTrue();
        assertThat(saved.getUsdAmount()).isEqualByComparingTo("1200.00");
    }

    @Test
    void processAndSaveExactlyEqualsRemainingLimitShouldNotExceed() {
        var limit = Limit.builder()
                .id(1L)
                .category(ExpenseCategory.PRODUCT)
                .limitSum(new BigDecimal("1500.00"))
                .limitDatetime(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .currency("USD")
                .build();

        when(exchangeRateService.getOrFetchRate(eq("KZT"), any()))
                .thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                any(), any())).thenReturn(Optional.of(limit));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(
                any(), any())).thenReturn(Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(any(), any(), any()))
                .thenReturn(new BigDecimal("1100.00"));

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transaction.setSum(new BigDecimal("200000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getUsdAmount()).isEqualByComparingTo("400.00");
        assertThat(saved.isLimitExceeded()).isFalse();
        assertThat(saved.getLimit()).isEqualTo(limit);
    }


    @Test
    void processAndSaveExceedsByOneCentShouldSetExceededTrue() {
        var limit = Limit.builder()
                .id(1L)
                .category(ExpenseCategory.PRODUCT)
                .limitSum(new BigDecimal("1500.00"))
                .limitDatetime(OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC))
                .currency("USD")
                .build();

        when(exchangeRateService.getOrFetchRate(eq("KZT"), any()))
                .thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                any(), any())).thenReturn(Optional.of(limit));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(
                any(), any())).thenReturn(Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(any(), any(), any()))
                .thenReturn(new BigDecimal("1499.99"));

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transaction.setSum(new BigDecimal("10.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getUsdAmount()).isEqualByComparingTo("0.02");
        assertThat(saved.isLimitExceeded()).isTrue();
        assertThat(saved.getLimit()).isEqualTo(limit);
    }

    @Test
    void limitChangedMidMonthPreviousExceededRemainExceededNewTransactionsUseNewLimit() {
        when(exchangeRateService.getOrFetchRate(anyString(), any()))
                .thenReturn(new BigDecimal("500.00"));

        when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation -> {
                    Transaction tx = invocation.getArgument(0);
                    tx.setId((long) (100 + Math.random() * 9000));
                    return tx;
                });

        Limit limitJan1 = Limit.builder()
                .id(1L)
                .category(ExpenseCategory.PRODUCT)
                .limitSum(new BigDecimal("1000.00"))
                .limitDatetime(OffsetDateTime.parse("2026-01-01T00:00:00Z"))
                .currency("USD")
                .build();

        Limit limitJan11 = Limit.builder()
                .id(2L)
                .category(ExpenseCategory.PRODUCT)
                .limitSum(new BigDecimal("2000.00"))
                .limitDatetime(OffsetDateTime.parse("2026-01-11T09:00:00Z"))
                .currency("USD")
                .build();

        OffsetDateTime dt1 = OffsetDateTime.parse("2026-01-05T12:00:00Z");

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), eq(dt1)))
                .thenReturn(Optional.of(limitJan1));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), eq(limitJan1.getLimitDatetime())))
                .thenReturn(Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(
                eq(ExpenseCategory.PRODUCT),
                argThat(d -> d.getDayOfMonth() == 1 && d.getMonthValue() == 1),
                eq(dt1)))
                .thenReturn(BigDecimal.ZERO);

        Transaction tx1 = Transaction.builder()
                .accountFrom("123")
                .accountTo("456")
                .currencyShortname("KZT")
                .sum(new BigDecimal("300000.00"))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .datetime(dt1)
                .build();

        Transaction saved1 = transactionService.processAndSave(tx1);
        assertThat(saved1.isLimitExceeded()).isFalse();


        OffsetDateTime dt2 = OffsetDateTime.parse("2026-01-10T14:00:00Z");

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), eq(dt2)))
                .thenReturn(Optional.of(limitJan1));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), eq(limitJan1.getLimitDatetime())))
                .thenReturn(Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(
                eq(ExpenseCategory.PRODUCT),
                argThat(d -> d.getDayOfMonth() == 1 && d.getMonthValue() == 1),
                eq(dt2)))
                .thenReturn(new BigDecimal("600.00"));

        Transaction tx2 = Transaction.builder()
                .accountFrom("123")
                .accountTo("456")
                .currencyShortname("KZT")
                .sum(new BigDecimal("250000.00"))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .datetime(dt2)
                .build();

        Transaction saved2 = transactionService.processAndSave(tx2);
        assertThat(saved2.isLimitExceeded()).isTrue();


        OffsetDateTime dt3 = OffsetDateTime.parse("2026-01-12T15:00:00Z");

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), eq(dt3)))
                .thenReturn(Optional.of(limitJan11));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(
                eq(ExpenseCategory.PRODUCT), eq(limitJan11.getLimitDatetime())))
                .thenReturn(Optional.of(limitJan1));

        when(transactionRepository.sumUsdAfterLimit(
                eq(ExpenseCategory.PRODUCT),
                argThat(d -> d.getDayOfMonth() == 1 && d.getMonthValue() == 1),
                eq(dt3)))
                .thenReturn(new BigDecimal("1100.00"));

        Transaction tx3 = Transaction.builder()
                .accountFrom("123")
                .accountTo("456")
                .currencyShortname("KZT")
                .sum(new BigDecimal("400000.00"))
                .expenseCategory(ExpenseCategory.PRODUCT)
                .datetime(dt3)
                .build();

        Transaction saved3 = transactionService.processAndSave(tx3);
        assertThat(saved3.isLimitExceeded()).isFalse();
        assertThat(saved3.getLimit()).isEqualTo(limitJan11);
    }

    @Test
    void exactlyEqualsRemainingLimitShouldBeFalse() {
        var limit = Limit.builder()
                .id(1L)
                .category(ExpenseCategory.PRODUCT)
                .limitSum(new BigDecimal("1500.00"))
                .limitDatetime(OffsetDateTime.parse("2026-01-01T00:00:00Z"))
                .currency("USD")
                .build();

        when(exchangeRateService.getOrFetchRate(anyString(), any()))
                .thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(any(), any()))
                .thenReturn(Optional.of(limit));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(any(), any()))
                .thenReturn(Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(any(), any(), any()))
                .thenReturn(new BigDecimal("1100.00"));

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transaction.setSum(new BigDecimal("200000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getUsdAmount()).isEqualByComparingTo("400.00");
        assertThat(saved.isLimitExceeded()).isFalse();
        assertThat(saved.getLimit()).isEqualTo(limit);
    }


    @Test
    void transactionExactlyOnNewLimitDateShouldUseNewLimit() {
        OffsetDateTime changeTime = OffsetDateTime.parse("2026-01-10T14:00:00Z");

        Limit oldLimit = Limit.builder()
                .limitSum(new BigDecimal("1000.00"))
                .limitDatetime(OffsetDateTime.parse("2026-01-01T00:00:00Z"))
                .currency("USD")
                .build();

        Limit newLimit = Limit.builder()
                .limitSum(new BigDecimal("3000.00"))
                .limitDatetime(changeTime)
                .currency("USD")
                .build();

        when(exchangeRateService.getOrFetchRate(anyString(), any()))
                .thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(any(), eq(changeTime)))
                .thenReturn(Optional.of(newLimit));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(any(), eq(changeTime)))
                .thenReturn(Optional.of(oldLimit));

        when(transactionRepository.sumUsdAfterLimit(any(), any(), eq(changeTime)))
                .thenReturn(new BigDecimal("2500.00"));

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transaction.setDatetime(changeTime);
        transaction.setSum(new BigDecimal("300000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getLimit()).isEqualTo(newLimit);
        assertThat(saved.isLimitExceeded()).isTrue();
    }


    @Test
    void transactionBeforeFirstLimitSameDayShouldUseDefault1000() {
        OffsetDateTime txTime   = OffsetDateTime.parse("2026-01-05T10:00:00Z");
        OffsetDateTime limitTime = OffsetDateTime.parse("2026-01-05T14:30:00Z");

        when(exchangeRateService.getOrFetchRate(anyString(), any()))
                .thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(any(), eq(txTime)))
                .thenReturn(Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(any(), any(), eq(txTime)))
                .thenReturn(BigDecimal.ZERO);

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transaction.setDatetime(txTime);
        transaction.setSum(new BigDecimal("750000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.isLimitExceeded()).isTrue();
        assertThat(saved.getLimit()).isNull();
        assertThat(saved.getUsdAmount()).isEqualByComparingTo("1500.00");
    }


    @Test
    void multipleLimitChangesSameDayLastOneWins() {
        OffsetDateTime t10 = OffsetDateTime.parse("2026-01-10T10:00:00Z");
        OffsetDateTime t11 = OffsetDateTime.parse("2026-01-10T11:00:00Z");
        OffsetDateTime t12 = OffsetDateTime.parse("2026-01-10T12:00:00Z");

        Limit lim10 = Limit.builder().limitSum(new BigDecimal("500.00")).limitDatetime(t10).build();
        Limit lim11 = Limit.builder().limitSum(new BigDecimal("1500.00")).limitDatetime(t11).build();
        Limit lim12 = Limit.builder().limitSum(new BigDecimal("3000.00")).limitDatetime(t12).build();

        when(exchangeRateService.getOrFetchRate(anyString(), any())).thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(any(), eq(t12.plusSeconds(1))))
                .thenReturn(Optional.of(lim12));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(any(), eq(t12)))
                .thenReturn(Optional.of(lim11));

        when(transactionRepository.sumUsdAfterLimit(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transaction.setDatetime(t12.plusSeconds(1));
        transaction.setSum(new BigDecimal("2000000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getLimit()).isEqualTo(lim12);
        assertThat(saved.isLimitExceeded()).isTrue();
    }


    @Test
    void hugeTransactionOneMillionUsdExceedsImmediately() {
        when(exchangeRateService.getOrFetchRate(anyString(), any())).thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(any(), any()))
                .thenReturn(Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(any(), any(), any())).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transaction.setSum(new BigDecimal("500000000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getUsdAmount()).isEqualByComparingTo("1000000.00");
        assertThat(saved.isLimitExceeded()).isTrue();
        assertThat(saved.getLimit()).isNull();
    }


    @Test
    void limitSetAfterTransactionInSameMonthTransactionUsesDefaultOrPrevious() {
        OffsetDateTime txTime   = OffsetDateTime.parse("2026-01-05T12:00:00Z");
        OffsetDateTime limitTime = OffsetDateTime.parse("2026-01-10T09:00:00Z");

        when(exchangeRateService.getOrFetchRate(anyString(), any())).thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(any(), eq(txTime)))
                .thenReturn(Optional.empty());

        when(transactionRepository.sumUsdAfterLimit(any(), any(), eq(txTime))).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transaction.setDatetime(txTime);
        transaction.setSum(new BigDecimal("800000.00"));

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.isLimitExceeded()).isTrue();
        assertThat(saved.getLimit()).isNull();
    }


    @Test
    void spentExactlyLimitBeforeNewLimitNewTransactionUsesNewLimit() {
        OffsetDateTime limit2Time = OffsetDateTime.parse("2026-01-10T09:00:00Z");
        OffsetDateTime tx2Time    = OffsetDateTime.parse("2026-01-12T14:00:00Z");

        Limit limit1 = Limit.builder()
                .limitSum(new BigDecimal("1000.00"))
                .limitDatetime(OffsetDateTime.parse("2026-01-01T00:00:00Z"))
                .category(ExpenseCategory.PRODUCT)
                .currency("USD")
                .build();

        Limit limit2 = Limit.builder()
                .limitSum(new BigDecimal("5000.00"))
                .limitDatetime(limit2Time)
                .category(ExpenseCategory.PRODUCT)
                .currency("USD")
                .build();

        when(exchangeRateService.getOrFetchRate(anyString(), any()))
                .thenReturn(new BigDecimal("500.00"));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
                any(), eq(tx2Time)))
                .thenReturn(Optional.of(limit2));

        when(limitRepository.findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(
                any(), eq(limit2Time)))
                .thenReturn(Optional.of(limit1));

        when(transactionRepository.sumUsdAfterLimit(any(), any(), eq(tx2Time)))
                .thenReturn(new BigDecimal("1000.00"));

        when(transactionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        transaction.setDatetime(tx2Time);
        transaction.setSum(new BigDecimal("1500000.00"));
        transaction.setExpenseCategory(ExpenseCategory.PRODUCT);

        Transaction saved = transactionService.processAndSave(transaction);

        assertThat(saved.getLimit()).isEqualTo(limit2);
        assertThat(saved.isLimitExceeded()).isFalse();
        assertThat(saved.getUsdAmount()).isEqualByComparingTo("3000.00");
    }
}
