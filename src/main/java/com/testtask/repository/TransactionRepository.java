package com.testtask.repository;

import com.testtask.dto.ExceededTransactionResponseDto;
import com.testtask.entity.Transaction;
import com.testtask.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT COALESCE(SUM(t.usdAmount), 0) " +
            "FROM Transaction t " +
            "WHERE t.expenseCategory = :category " +
            "  AND t.datetime >= :limitStart " +
            "  AND t.datetime < :transactionEnd")
    BigDecimal sumUsdAfterLimit(
            @Param("category") ExpenseCategory category,
            @Param("limitStart") OffsetDateTime limitStart,
            @Param("transactionEnd") OffsetDateTime transactionEnd);

    @Query(value = """
    SELECT 
        t.id,
        t.account_from          AS accountFrom,
        t.account_to            AS accountTo,
        t.currency_shortname    AS currencyShortname,
        t.sum                   AS sum,
        t.expense_category      AS expenseCategory,
        t.datetime              AS datetime,
        t.usd_amount            AS usdAmount,
        
        COALESCE(l.limit_sum, 1000.00)          AS limitSum,
        COALESCE(l.limit_datetime, 
                 date_trunc('month', t.datetime)) AS limitDatetime,
        'USD'                                   AS limitCurrencyShortname
        
    FROM transactions t
    
    LEFT JOIN LATERAL (
        SELECT *
        FROM limits l2
        WHERE l2.category = t.expense_category
          AND l2.limit_datetime <= t.datetime
        ORDER BY l2.limit_datetime DESC
        LIMIT 1
    ) l ON true
    
    WHERE t.limit_exceeded = true
    
    ORDER BY t.datetime DESC
    """, nativeQuery = true)
    List<ExceededTransactionResponseDto> findAllExceededWithLimitInfo();
}