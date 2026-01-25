package com.testtask.repository;

import com.testtask.entity.Limit;
import com.testtask.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LimitRepository extends JpaRepository<Limit, Long> {

    Optional<Limit> findFirstByCategoryAndLimitDatetimeLessThanEqualOrderByLimitDatetimeDesc(
            ExpenseCategory category,
            OffsetDateTime datetime);

    List<Limit> findAllByOrderByLimitDatetimeDesc();

    Optional<Limit> findFirstByCategoryAndLimitDatetimeLessThanOrderByLimitDatetimeDesc(
            ExpenseCategory category,
            OffsetDateTime datetime);
}