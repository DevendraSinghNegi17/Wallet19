package com.game.wallet.service;

import com.game.wallet.model.LedgerEntry;
import com.game.wallet.model.Wallet;
import com.game.wallet.repository.LedgerRepository;
import com.game.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LedgerReplayService {

    private static final String GENESIS = "GENESIS";

    private final LedgerRepository ledgerRepository;
    private final WalletRepository walletRepository;

    @Transactional
    public void replayAll() {
        log.info("Starting TRUE ledger rebuild");

        List<Wallet> wallets = walletRepository.findAll();
        wallets.forEach(w -> w.setBalance(0L));

        Map<String, Wallet> walletMap = wallets.stream()
                .collect(Collectors.toMap(
                        w -> key(w.getUserId(), w.getAsset()),
                        w -> w
                ));

        List<LedgerEntry> entries = ledgerRepository.findAllByOrderByIdAsc();
        log.info("Replaying {} ledger entries", entries.size());

        for (LedgerEntry entry : entries) {

            Wallet credit = walletMap.get(key(entry.getCreditUser(), entry.getAsset()));
            if (credit == null) {
                throw new IllegalStateException(
                        "Missing credit wallet during replay. EntryId=" + entry.getId()
                );
            }

            if (!GENESIS.equals(entry.getDebitUser())) {

                Wallet debit = walletMap.get(key(entry.getDebitUser(), entry.getAsset()));
                if (debit == null) {
                    throw new IllegalStateException(
                            "Missing debit wallet during replay. EntryId=" + entry.getId()
                    );
                }

                long newDebitBalance = debit.getBalance() - entry.getAmount();

                if (newDebitBalance < 0) {
                    throw new IllegalStateException(
                            "Negative balance during replay. User=" + debit.getUserId() +
                                    ", Asset=" + debit.getAsset() +
                                    ", EntryId=" + entry.getId()
                    );
                }

                debit.setBalance(newDebitBalance);
            }

            credit.setBalance(credit.getBalance() + entry.getAmount());
        }

        walletRepository.saveAll(wallets);

        log.info("Ledger rebuild completed successfully");
    }

    private String key(String user, String asset) {
        return user + "|" + asset;
    }
}