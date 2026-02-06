package com.game.wallet.repository;

import com.game.wallet.model.Wallet;
import com.game.wallet.model.WalletId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, WalletId> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId AND w.asset = :asset")
    Optional<Wallet> findByUserIdAndAssetForUpdate(@Param("userId") String userId, @Param("asset") String asset);

    Optional<Wallet> findByUserIdAndAsset(String userId, String asset);
}
