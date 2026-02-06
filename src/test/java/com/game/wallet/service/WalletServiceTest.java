package com.game.wallet.service;

import com.game.wallet.dto.BonusRequest;
import com.game.wallet.dto.SpendRequest;
import com.game.wallet.dto.TopUpRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @Mock
    private AssetRepository assetRepository;

    @InjectMocks
    private WalletService walletService;

    private Asset limitedAsset;
    private Asset unlimitedAsset;
    private Wallet userWallet;
    private Wallet systemWallet;

    @BeforeEach
    void setUp() {
        limitedAsset = new Asset("GOLD", true);
        unlimitedAsset = new Asset("GEMS", false);

        userWallet = new Wallet();
        userWallet.setUserId("user123");
        userWallet.setAsset("GOLD");
        userWallet.setBalance(500L);

        systemWallet = new Wallet();
        systemWallet.setUserId("SYSTEM");
        systemWallet.setAsset("GOLD");
        systemWallet.setBalance(10000L);
    }

    @Test
    void testTopUp_LimitedSupply_Success() {
        TopUpRequest request = new TopUpRequest("user123", "GOLD", 100L, "idem-1");

        when(assetRepository.findById("GOLD")).thenReturn(Optional.of(limitedAsset));
        when(walletRepository.findByUserIdAndAssetForUpdate("SYSTEM", "GOLD")).thenReturn(Optional.of(systemWallet));
        when(walletRepository.findByUserIdAndAssetForUpdate("user123", "GOLD")).thenReturn(Optional.of(userWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        walletService.topUp(request);

        assertEquals(9900L, systemWallet.getBalance());
        assertEquals(600L, userWallet.getBalance());
        verify(ledgerRepository).save(any(LedgerEntry.class));
        verify(idempotencyRepository).save(any(IdempotencyKey.class));
    }

    @Test
    void testTopUp_UnlimitedSupply_Success() {
        TopUpRequest request = new TopUpRequest("user123", "GEMS", 100L, "idem-2");

        Wallet gemsWallet = new Wallet();
        gemsWallet.setUserId("user123");
        gemsWallet.setAsset("GEMS");
        gemsWallet.setBalance(500L);

        when(assetRepository.findById("GEMS")).thenReturn(Optional.of(unlimitedAsset));
        when(walletRepository.findByUserIdAndAssetForUpdate("user123", "GEMS")).thenReturn(Optional.of(gemsWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        walletService.topUp(request);

        assertEquals(600L, gemsWallet.getBalance());
        verify(ledgerRepository).save(any(LedgerEntry.class));
        verify(idempotencyRepository).save(any(IdempotencyKey.class));
    }

    @Test
    void testBonus_LimitedSupply_Success() {
        BonusRequest request = new BonusRequest("user123", "GOLD", 50L, "idem-3", "bonus");

        when(assetRepository.findById("GOLD")).thenReturn(Optional.of(limitedAsset));
        when(walletRepository.findByUserIdAndAssetForUpdate("SYSTEM", "GOLD")).thenReturn(Optional.of(systemWallet));
        when(walletRepository.findByUserIdAndAssetForUpdate("user123", "GOLD")).thenReturn(Optional.of(userWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        walletService.bonus(request);

        assertEquals(9950L, systemWallet.getBalance());
        assertEquals(550L, userWallet.getBalance());
        verify(ledgerRepository).save(any(LedgerEntry.class));
        verify(idempotencyRepository).save(any(IdempotencyKey.class));
    }

    @Test
    void testSpend_LimitedSupply_Success() {
        SpendRequest request = new SpendRequest("user123", "GOLD", 30L, "idem-4", "order");

        when(assetRepository.findById("GOLD")).thenReturn(Optional.of(limitedAsset));
        when(walletRepository.findByUserIdAndAssetForUpdate("user123", "GOLD")).thenReturn(Optional.of(userWallet));
        when(walletRepository.findByUserIdAndAssetForUpdate("SYSTEM", "GOLD")).thenReturn(Optional.of(systemWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        walletService.spend(request);

        assertEquals(470L, userWallet.getBalance());
        assertEquals(10030L, systemWallet.getBalance());
        verify(ledgerRepository).save(any(LedgerEntry.class));
        verify(idempotencyRepository).save(any(IdempotencyKey.class));
    }

    @Test
    void testSpend_UnlimitedSupply_Success() {
        SpendRequest request = new SpendRequest("user123", "GEMS", 30L, "idem-5", "order");

        Wallet gemsWallet = new Wallet();
        gemsWallet.setUserId("user123");
        gemsWallet.setAsset("GEMS");
        gemsWallet.setBalance(500L);

        when(assetRepository.findById("GEMS")).thenReturn(Optional.of(unlimitedAsset));
        when(walletRepository.findByUserIdAndAssetForUpdate("user123", "GEMS")).thenReturn(Optional.of(gemsWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        walletService.spend(request);

        assertEquals(470L, gemsWallet.getBalance());
        verify(ledgerRepository).save(any(LedgerEntry.class));
        verify(idempotencyRepository).save(any(IdempotencyKey.class));
    }

    @Test
    void testDuplicateIdempotencyKey() {
        TopUpRequest request = new TopUpRequest("user123", "GOLD", 100L, "dup");

        when(assetRepository.findById("GOLD")).thenReturn(Optional.of(limitedAsset));
        when(walletRepository.findByUserIdAndAssetForUpdate("SYSTEM", "GOLD"))
                .thenReturn(Optional.of(systemWallet));
        when(walletRepository.findByUserIdAndAssetForUpdate("user123", "GOLD"))
                .thenReturn(Optional.of(userWallet));

        doThrow(DataIntegrityViolationException.class)
                .when(idempotencyRepository).save(any());

        assertThrows(DuplicateRequestException.class, () -> walletService.topUp(request));
    }

    @Test
    void testSpend_InsufficientBalance() {
        SpendRequest request = new SpendRequest("user123", "GOLD", 1000L, "idem-6", "order");

        when(walletRepository.findByUserIdAndAssetForUpdate("user123", "GOLD")).thenReturn(Optional.of(userWallet));

        assertThrows(InsufficientBalanceException.class, () -> walletService.spend(request));
    }

    @Test
    void testTopUp_AssetNotFound() {
        TopUpRequest request = new TopUpRequest("user123", "INVALID", 100L, "idem-7");

        when(assetRepository.findById("INVALID")).thenReturn(Optional.empty());

        assertThrows(AssetNotFoundException.class, () -> walletService.topUp(request));
    }

    @Test
    void testSpend_WalletNotFound() {
        SpendRequest request = new SpendRequest("user999", "GOLD", 100L, "idem-8", "order");

        when(walletRepository.findByUserIdAndAssetForUpdate("user999", "GOLD")).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.spend(request));
    }

    @Test
    void testTopUp_CreatesWalletIfNotExists() {
        TopUpRequest request = new TopUpRequest("newuser", "GOLD", 100L, "idem-9");

        when(assetRepository.findById("GOLD")).thenReturn(Optional.of(limitedAsset));
        when(walletRepository.findByUserIdAndAssetForUpdate("SYSTEM", "GOLD")).thenReturn(Optional.of(systemWallet));
        when(walletRepository.findByUserIdAndAssetForUpdate("newuser", "GOLD")).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArgument(0));

        walletService.topUp(request);

        verify(walletRepository).save(any(Wallet.class));
        verify(ledgerRepository).save(any(LedgerEntry.class));
    }
}