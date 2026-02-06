package com.game.wallet.service;

import com.game.wallet.dto.*;
import com.game.wallet.exception.AssetNotFoundException;
import com.game.wallet.exception.DuplicateRequestException;
import com.game.wallet.exception.InsufficientBalanceException;
import com.game.wallet.exception.WalletNotFoundException;
import com.game.wallet.model.Asset;
import com.game.wallet.model.IdempotencyKey;
import com.game.wallet.model.LedgerEntry;
import com.game.wallet.model.Wallet;
import com.game.wallet.repository.AssetRepository;
import com.game.wallet.repository.IdempotencyRepository;
import com.game.wallet.repository.LedgerRepository;
import com.game.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private static final String SYSTEM_USER = "SYSTEM";

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final AssetRepository assetRepository;

    @Transactional
    public void topUp(TopUpRequest request) {

        checkDuplicate(request.getIdempotencyKey(), "TOPUP");

        Asset asset = getAsset(request.getAsset());

        if (asset.getLimitedSupply()) {
            Wallet systemWallet = getWalletForUpdate(SYSTEM_USER, request.getAsset());

            if (systemWallet.getBalance() < request.getAmount()) {
                throw new InsufficientBalanceException("System wallet has insufficient balance");
            }

            systemWallet.debit(request.getAmount());
        }

        Wallet userWallet = getOrCreateWallet(request.getUserId(), request.getAsset());
        userWallet.credit(request.getAmount());

        saveLedger(SYSTEM_USER, request.getUserId(), request.getAsset(),
                request.getAmount(), request.getIdempotencyKey(), "TOP-UP");

        saveIdempotency(request.getIdempotencyKey(), "TOPUP");
    }

    @Transactional
    public void bonus(BonusRequest request) {

        checkDuplicate(request.getIdempotencyKey(), "BONUS");

        Asset asset = getAsset(request.getAsset());

        if (asset.getLimitedSupply()) {
            Wallet systemWallet = getWalletForUpdate(SYSTEM_USER, request.getAsset());

            if (systemWallet.getBalance() < request.getAmount()) {
                throw new InsufficientBalanceException("System wallet has insufficient balance");
            }

            systemWallet.debit(request.getAmount());
        }

        Wallet userWallet = getOrCreateWallet(request.getUserId(), request.getAsset());
        userWallet.credit(request.getAmount());

        saveLedger(SYSTEM_USER, request.getUserId(), request.getAsset(),
                request.getAmount(), request.getIdempotencyKey(), request.getReason());

        saveIdempotency(request.getIdempotencyKey(), "BONUS");
    }

    @Transactional
    public void spend(SpendRequest request) {

        checkDuplicate(request.getIdempotencyKey(), "SPEND");

        Wallet userWallet = getWalletForUpdate(request.getUserId(), request.getAsset());

        if (userWallet.getBalance() < request.getAmount()) {
            throw new InsufficientBalanceException("User wallet has insufficient balance");
        }

        userWallet.debit(request.getAmount());

        Asset asset = getAsset(request.getAsset());

        if (asset.getLimitedSupply()) {
            Wallet systemWallet = getOrCreateWallet(SYSTEM_USER, request.getAsset());
            systemWallet.credit(request.getAmount());
        }

        saveLedger(request.getUserId(), SYSTEM_USER, request.getAsset(),
                request.getAmount(), request.getIdempotencyKey(), request.getOrderId());

        saveIdempotency(request.getIdempotencyKey(), "SPEND");
    }


    private void checkDuplicate(String key, String operation) {
        if (idempotencyRepository.existsByIdempotencyKeyAndOperation(key, operation)) {
            throw new DuplicateRequestException();
        }
    }

    private void saveIdempotency(String key, String operation) {
        try {
            idempotencyRepository.save(new IdempotencyKey(key, operation));
        }
        catch(DataIntegrityViolationException ex){
            throw new DuplicateRequestException();
        }
    }


    private Asset getAsset(String assetCode) {
        return assetRepository.findById(assetCode)
                .orElseThrow(() -> new AssetNotFoundException("Asset not found: " + assetCode));
    }

    private Wallet getWalletForUpdate(String userId, String asset) {
        return walletRepository.findByUserIdAndAssetForUpdate(userId, asset)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                String.format("Wallet not found for user: %s, asset: %s", userId, asset)
                        )
                );
    }

    private Wallet getOrCreateWallet(String userId, String asset) {
        return walletRepository.findByUserIdAndAssetForUpdate(userId, asset)
                .orElseGet(() -> {
                    Wallet wallet = new Wallet();
                    wallet.setUserId(userId);
                    wallet.setAsset(asset);
                    wallet.setBalance(0L);
                    wallet.setVersion(0L);
                    return walletRepository.save(wallet);
                });
    }

    private void saveLedger(String debitUser, String creditUser, String asset,
                            Long amount, String idempotencyKey, String reference) {

        LedgerEntry entry = LedgerEntry.builder()
                .debitUser(debitUser)
                .creditUser(creditUser)
                .asset(asset)
                .amount(amount)
                .idempotencyKey(idempotencyKey)
                .reference(reference)
                .build();

        ledgerRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public Page<LedgerResponse> getTransactions(String userId, Pageable pageable) {
        return ledgerRepository
                .findByDebitUserOrCreditUser(userId, userId, pageable)
                .map(entry -> LedgerResponse.builder()
                        .debitUser(entry.getDebitUser())
                        .creditUser(entry.getCreditUser())
                        .asset(entry.getAsset())
                        .amount(entry.getAmount())
                        .reference(entry.getReference())
                        .createdAt(entry.getCreatedAt())
                        .build());
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String userId, String asset) {

        Wallet wallet = walletRepository.findByUserIdAndAsset(userId, asset)
                .orElseThrow(() ->
                        new WalletNotFoundException(
                                "Wallet not found for user=" + userId + ", asset=" + asset));

        return BalanceResponse.builder()
                .userId(wallet.getUserId())
                .asset(wallet.getAsset())
                .balance(wallet.getBalance())
                .version(wallet.getVersion())
                .build();
    }
}