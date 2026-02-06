package com.game.wallet.repository;

import com.game.wallet.model.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRepository extends JpaRepository<IdempotencyKey, Long> {

    boolean existsByIdempotencyKeyAndOperation(String idempotencyKey, String operation);
}