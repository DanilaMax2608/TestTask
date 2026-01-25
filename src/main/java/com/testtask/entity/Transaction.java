package com.testtask.entity;

import com.testtask.model.ExpenseCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_from", nullable = false, length = 20)
    private String accountFrom;

    @Column(name = "account_to", nullable = false, length = 20)
    private String accountTo;

    @Column(name = "currency_shortname", nullable = false, length = 3)
    private String currencyShortname;

    @Column(name = "sum", precision = 15, scale = 2, nullable = false)
    private BigDecimal sum;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_category", nullable = false)
    private ExpenseCategory expenseCategory;

    @Column(name = "datetime", nullable = false)
    private OffsetDateTime datetime;

    @Column(name = "usd_amount", precision = 15, scale = 2)
    private BigDecimal usdAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "limit_id", foreignKey = @ForeignKey(name = "fk_transactions_limit"))
    private Limit limit;

    @Column(name = "limit_exceeded", nullable = false)
    private boolean limitExceeded = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}