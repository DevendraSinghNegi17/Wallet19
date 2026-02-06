package com.game.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entry", indexes = {
    @Index(name = "idx_ledger_idempotency", columnList = "idempotency_key"),
    @Index(name = "idx_ledger_debit_user", columnList = "debit_user"),
    @Index(name = "idx_ledger_credit_user", columnList = "credit_user")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "debit_user", length = 100, nullable = false)
    private String debitUser;

    @Column(name = "credit_user", length = 100, nullable = false)
    private String creditUser;

    @Column(name = "asset", length = 20, nullable = false)
    private String asset;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "idempotency_key", length = 255, nullable = false)
    private String idempotencyKey;

    @Column(name = "reference", length = 500)
    private String reference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
