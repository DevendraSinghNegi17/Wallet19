package com.game.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "idempotency_key",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_idempotency_key_operation", columnNames = {"idempotency_key", "operation"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", length = 255, nullable = false)
    private String idempotencyKey;

    @Column(name = "operation", length = 50, nullable = false)
    private String operation;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public IdempotencyKey(String key, String operation) {
        this.idempotencyKey = key ;
        this.operation = operation ;
    }
}