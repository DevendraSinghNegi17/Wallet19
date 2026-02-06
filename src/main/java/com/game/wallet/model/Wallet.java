package com.game.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
@Entity
@Table(name = "wallet")
@IdClass(WalletId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"userId", "asset"})
public class Wallet {

    @Id
    @Column(name = "user_id", length = 100, nullable = false)
    private String userId;

    @Id
    @Column(name = "asset", length = 20, nullable = false)
    private String asset;

    @Column(name = "balance", nullable = false)
    private Long balance = 0L;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void credit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        this.balance += amount;
    }

    public void debit(long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }
        if (this.balance < amount) {
            throw new IllegalStateException("Insufficient balance");
        }
        this.balance -= amount;
    }
}