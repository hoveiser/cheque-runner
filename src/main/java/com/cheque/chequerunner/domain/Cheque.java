package com.cheque.chequerunner.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"number", "drawer_id"})
        }
)
public class Cheque extends PersistentEntity {

    @Column(nullable = false)
    @NotNull
    private String number;

    @ManyToOne
    @JoinColumn(name = "drawer_id", nullable = false)
    private Account drawer;


    @Column(nullable = false)
    @NotNull
    private BigDecimal amount;

    @Column(nullable = false)
    @NotNull
    private LocalDate issueDate;

    @Enumerated(EnumType.ORDINAL)
    private ChequeStatus chequeStatus;

    public enum ChequeStatus {
        ISSUED,
        PAID,
        BOUNCED
    }
}