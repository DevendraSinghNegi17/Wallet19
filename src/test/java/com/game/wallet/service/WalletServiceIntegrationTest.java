package com.game.wallet.service;

import com.game.wallet.dto.BonusRequest;
import com.game.wallet.dto.SpendRequest;
import com.game.wallet.dto.TopUpRequest;
import com.game.wallet.exception.DuplicateRequestException;
import com.game.wallet.exception.InsufficientBalanceException;
import com.game.wallet.model.Asset;
import com.game.wallet.model.Wallet;
import com.game.wallet.repository.AssetRepository;
import com.game.wallet.repository.IdempotencyRepository;
import com.game.wallet.repository.LedgerRepository;
import com.game.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("WalletService Integration Tests")
class WalletServiceIntegrationTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    @BeforeEach
    @Transactional
    void setUp() {
        idempotencyRepository.deleteAll();
        ledgerRepository.deleteAll();
        walletRepository.deleteAll();
        assetRepository.deleteAll();

        Asset goldAsset = new Asset("GOLD", true);
        Asset gemsAsset = new Asset("GEMS", false);
        assetRepository.save(goldAsset);
        assetRepository.save(gemsAsset);

        Wallet systemGold = new Wallet();
        systemGold.setUserId("SYSTEM");
        systemGold.setAsset("GOLD");
        systemGold.setBalance(100000L);
        walletRepository.save(systemGold);

        Wallet userGold = new Wallet();
        userGold.setUserId("user1");
        userGold.setAsset("GOLD");
        userGold.setBalance(1000L);
        walletRepository.save(userGold);
    }

    @Test
    @DisplayName("Should handle concurrent top-ups correctly")
    void testConcurrentTopUps() throws InterruptedException, ExecutionException {
        int threadCount = 10;
        int amountPerTopUp = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                TopUpRequest request = new TopUpRequest(
                        "user1", "GOLD", (long) amountPerTopUp, "topup-" + index
                );
                walletService.topUp(request);
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        Wallet userWallet = walletRepository.findByUserIdAndAsset("user1", "GOLD").orElseThrow();
        Wallet systemWallet = walletRepository.findByUserIdAndAsset("SYSTEM", "GOLD").orElseThrow();

        assertEquals(1000L + (threadCount * amountPerTopUp), userWallet.getBalance());
        assertEquals(100000L - (threadCount * amountPerTopUp), systemWallet.getBalance());
        assertEquals(threadCount, ledgerRepository.count());
    }

    @Test
    @DisplayName("Should handle concurrent spends correctly")
    void testConcurrentSpends() throws InterruptedException, ExecutionException {
        int threadCount = 5;
        int amountPerSpend = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            futures.add(executor.submit(() -> {
                SpendRequest request = new SpendRequest(
                        "user1", "GOLD", (long) amountPerSpend, "spend-" + index, "order-" + index
                );
                walletService.spend(request);
            }));
        }

        for (Future<?> future : futures) {
            future.get();
        }
        executor.shutdown();

        Wallet userWallet = walletRepository.findByUserIdAndAsset("user1", "GOLD").orElseThrow();
        Wallet systemWallet = walletRepository.findByUserIdAndAsset("SYSTEM", "GOLD").orElseThrow();

        assertEquals(1000L - (threadCount * amountPerSpend), userWallet.getBalance());
        assertEquals(100000L + (threadCount * amountPerSpend), systemWallet.getBalance());
    }

    @Test
    @DisplayName("Should reject duplicate idempotency keys")
    void testDuplicateIdempotencyKey() {
        TopUpRequest request1 = new TopUpRequest("user1", "GOLD", 100L, "same-key");
        TopUpRequest request2 = new TopUpRequest("user1", "GOLD", 200L, "same-key");

        walletService.topUp(request1);
        assertThrows(DuplicateRequestException.class, () -> walletService.topUp(request2));

        Wallet userWallet = walletRepository.findByUserIdAndAsset("user1", "GOLD").orElseThrow();
        assertEquals(1100L, userWallet.getBalance());
    }

    @Test
    @DisplayName("Should handle unlimited supply assets correctly")
    void testUnlimitedSupplyAsset() {
        Wallet userGems = new Wallet();
        userGems.setUserId("user1");
        userGems.setAsset("GEMS");
        userGems.setBalance(0L);
        walletRepository.save(userGems);

        TopUpRequest topUpRequest = new TopUpRequest("user1", "GEMS", 500L, "gems-topup");
        walletService.topUp(topUpRequest);

        SpendRequest spendRequest = new SpendRequest("user1", "GEMS", 200L, "gems-spend", "order-gems");
        walletService.spend(spendRequest);

        Wallet updatedWallet = walletRepository.findByUserIdAndAsset("user1", "GEMS").orElseThrow();
        assertEquals(300L, updatedWallet.getBalance());

        assertFalse(walletRepository.findByUserIdAndAsset("SYSTEM", "GEMS").isPresent());
    }

    @Test
    @DisplayName("Should throw exception when spending more than balance")
    void testSpendMoreThanBalance() {
        SpendRequest request = new SpendRequest("user1", "GOLD", 2000L, "overspend", "order-999");
        assertThrows(InsufficientBalanceException.class, () -> walletService.spend(request));
    }

    @Test
    @DisplayName("Should correctly record ledger entries")
    void testLedgerEntries() {
        TopUpRequest topUpRequest = new TopUpRequest("user1", "GOLD", 100L, "ledger-test-1");
        walletService.topUp(topUpRequest);

        BonusRequest bonusRequest = new BonusRequest("user1", "GOLD", 50L, "ledger-test-2", "Test bonus");
        walletService.bonus(bonusRequest);

        SpendRequest spendRequest = new SpendRequest("user1", "GOLD", 30L, "ledger-test-3", "order-123");
        walletService.spend(spendRequest);

        assertEquals(3, ledgerRepository.count());
    }
}
