package com.game.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.wallet.dto.BonusRequest;
import com.game.wallet.dto.SpendRequest;
import com.game.wallet.dto.TopUpRequest;
import com.game.wallet.exception.DuplicateRequestException;
import com.game.wallet.exception.InsufficientBalanceException;
import com.game.wallet.service.WalletService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
@DisplayName("WalletController Tests")
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private WalletService walletService;

    @Test
    @DisplayName("Should successfully process top-up request")
    void testTopUp_Success() throws Exception {
        TopUpRequest request = new TopUpRequest("user123", "GOLD", 100L, "topup-001");

        doNothing().when(walletService).topUp(any(TopUpRequest.class));

        mockMvc.perform(post("/v1/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Wallet top-up successful"));

        verify(walletService, times(1)).topUp(any(TopUpRequest.class));
    }

    @Test
    @DisplayName("Should successfully process bonus request")
    void testBonus_Success() throws Exception {
        BonusRequest request = new BonusRequest("user123", "GEMS", 50L, "bonus-001", "Daily reward");

        doNothing().when(walletService).bonus(any(BonusRequest.class));

        mockMvc.perform(post("/v1/wallet/bonus")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Bonus credited successfully"));

        verify(walletService, times(1)).bonus(any(BonusRequest.class));
    }

    @Test
    @DisplayName("Should successfully process spend request")
    void testSpend_Success() throws Exception {
        SpendRequest request = new SpendRequest("user123", "GOLD", 75L, "spend-001", "order-456");

        doNothing().when(walletService).spend(any(SpendRequest.class));

        mockMvc.perform(post("/v1/wallet/spend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Spend successful"));

        verify(walletService, times(1)).spend(any(SpendRequest.class));
    }

    @Test
    @DisplayName("Should return validation error for invalid top-up request")
    void testTopUp_ValidationError() throws Exception {
        TopUpRequest request = new TopUpRequest("", "GOLD", -100L, "");

        mockMvc.perform(post("/v1/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(walletService, never()).topUp(any(TopUpRequest.class));
    }

    @Test
    @DisplayName("Should return conflict error for duplicate request")
    void testTopUp_DuplicateRequest() throws Exception {
        TopUpRequest request = new TopUpRequest("user123", "GOLD", 100L, "duplicate-key");

        doThrow(new DuplicateRequestException()).when(walletService).topUp(any(TopUpRequest.class));

        mockMvc.perform(post("/v1/wallet/topup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DUPLICATE_REQUEST"));
    }

    @Test
    @DisplayName("Should return bad request for insufficient balance")
    void testSpend_InsufficientBalance() throws Exception {
        SpendRequest request = new SpendRequest("user123", "GOLD", 1000L, "spend-002", "order-789");

        doThrow(new InsufficientBalanceException("User wallet has insufficient balance"))
                .when(walletService).spend(any(SpendRequest.class));

        mockMvc.perform(post("/v1/wallet/spend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_BALANCE"));
    }
}
