package com.cheque.chequerunner.repository;

import com.cheque.chequerunner.domain.BounceRecord;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BounceRecordRepository extends PersistentRepository<BounceRecord> {

    @Query("SELECT COUNT(br) FROM BounceRecord br JOIN Cheque c ON br.chequeId = c.id " +
            "WHERE c.drawer.id = :drawerId AND br.date >= :specificTime")
    Long countBouncesByDrawerAndDateAfter(
            @Param("drawerId") Long drawerId,
            @Param("specificTime") LocalDate specificTime);
}
