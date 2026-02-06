package com.game.wallet.repository;

import com.game.wallet.model.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerRepository extends JpaRepository<LedgerEntry, Long> {

    Page<LedgerEntry> findByDebitUserOrCreditUser(
            String debitUser,
            String creditUser,
            Pageable pageable
    );

    List<LedgerEntry> findAllByOrderByIdAsc();
}