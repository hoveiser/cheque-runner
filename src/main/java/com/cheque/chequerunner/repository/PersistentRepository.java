package com.cheque.chequerunner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;


@NoRepositoryBean
public interface PersistentRepository<T> extends JpaRepository<T, Long> {
}
