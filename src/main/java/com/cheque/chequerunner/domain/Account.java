package com.cheque.chequerunner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account extends PersistentEntity {

    @Column(nullable = false)
    @NotNull
    private BigDecimal balance;

    @Enumerated(EnumType.ORDINAL)
    @NotNull
    private AccountStatus accountStatus;

    public enum AccountStatus {
        ACTIVE,
        BLOCKED
    }
}