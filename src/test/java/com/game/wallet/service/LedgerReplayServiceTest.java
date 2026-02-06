package com.game.wallet.service;

import com.game.wallet.model.LedgerEntry;
import com.game.wallet.model.Wallet;
import com.game.wallet.repository.LedgerRepository;
import com.game.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LedgerReplayService Tests")
class LedgerReplayServiceTest {

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private LedgerReplayService ledgerReplayService;

    private Wallet userWallet;
    private Wallet systemWallet;

    @BeforeEach
    void setUp() {
        userWallet = new Wallet();
        userWallet.setUserId("user1");
        userWallet.setAsset("GOLD");
        userWallet.setBalance(500L);

        systemWallet = new Wallet();
        systemWallet.setUserId("SYSTEM");
        systemWallet.setAsset("GOLD");
        systemWallet.setBalance(10000L);
    }

    @Test
    @DisplayName("Should replay all ledger entries correctly")
    void testReplayAll() {

        LedgerEntry genesis = LedgerEntry.builder()
                .id(0L)
                .debitUser("GENESIS")
                .creditUser("SYSTEM")
                .asset("GOLD")
                .amount(10000L)
                .idempotencyKey("init")
                .reference("INITIAL_MINT")
                .build();

        LedgerEntry entry1 = LedgerEntry.builder()
                .id(1L)
                .debitUser("SYSTEM")
                .creditUser("user1")
                .asset("GOLD")
                .amount(100L)
                .idempotencyKey("key1")
                .reference("TOP-UP")
                .build();

        LedgerEntry entry2 = LedgerEntry.builder()
                .id(2L)
                .debitUser("user1")
                .creditUser("SYSTEM")
                .asset("GOLD")
                .amount(50L)
                .idempotencyKey("key2")
                .reference("SPEND")
                .build();

        List<LedgerEntry> entries = Arrays.asList(genesis, entry1, entry2);

        when(walletRepository.findAll()).thenReturn(Arrays.asList(userWallet, systemWallet));
        when(ledgerRepository.findAllByOrderByIdAsc()).thenReturn(entries);

        // ACT
        ledgerReplayService.replayAll();

        // ASSERT
        assertEquals(50L, userWallet.getBalance());
        assertEquals(9950L, systemWallet.getBalance());

        verify(walletRepository).saveAll(any());
    }
    @Test
    @DisplayName("Should handle empty ledger gracefully")
    void testReplayAll_EmptyLedger() {

        when(walletRepository.findAll()).thenReturn(Arrays.asList(userWallet, systemWallet));
        when(ledgerRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

        // ACT
        ledgerReplayService.replayAll();

        assertEquals(0L, userWallet.getBalance());
        assertEquals(0L, systemWallet.getBalance());

        verify(walletRepository).saveAll(any());
    }
}