package com.cheque.chequerunner.repository;

import com.cheque.chequerunner.domain.Cheque;

import java.util.Optional;

public interface ChequeRepository extends PersistentRepository<Cheque> {

    Optional<Cheque> findByNumberAndDrawerId(String number, Long drawerId);
}
