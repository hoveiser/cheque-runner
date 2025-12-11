package com.cheque.chequerunner.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

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
    private AccountStatus status;

    public enum AccountStatus {
        ACTIVE,
        BLOCKED
    }
}