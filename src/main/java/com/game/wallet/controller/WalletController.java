package com.game.wallet.controller;

import com.game.wallet.dto.*;
import com.game.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/wallet")
@RequiredArgsConstructor
@Tag(name = "Wallet Operations", description = "Endpoints for managing user balances and transactions")
public class WalletController {

    private final WalletService walletService;

    @Operation(summary = "Top up wallet", description = "Adds funds from the system or unlimited supply to a user's wallet")
    @PostMapping("/topup")
    public ResponseEntity<ApiResponse<Void>> topUp(@Valid @RequestBody TopUpRequest request) {
        walletService.topUp(request);
        return ResponseEntity.ok(ApiResponse.success("Wallet top-up successful"));
    }

    @Operation(summary = "Credit bonus", description = "Credits a bonus to a user's wallet with a specific reason")
    @PostMapping("/bonus")
    public ResponseEntity<ApiResponse<Void>> bonus(@Valid @RequestBody BonusRequest request) {
        walletService.bonus(request);
        return ResponseEntity.ok(ApiResponse.success("Bonus credited successfully"));
    }

    @Operation(summary = "Spend funds", description = "Deducts funds from a user's wallet for a purchase")
    @PostMapping("/spend")
    public ResponseEntity<ApiResponse<Void>> spend(@Valid @RequestBody SpendRequest request) {
        walletService.spend(request);
        return ResponseEntity.ok(ApiResponse.success("Spend successful"));
    }


    @Operation(summary = "View Balance", description = "Balance of the user with assetType asset")
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(@RequestParam String userId, @RequestParam String asset) {
        return ResponseEntity.ok(ApiResponse.success("Balance fetched succesfully", walletService.getBalance(userId, asset)));
    }

    @Operation(summary = "View Transaction", description = "Transactions of a user")
    @GetMapping("/transactions")
    public ResponseEntity<Page<LedgerResponse>> getTransactions(@RequestParam String userId, @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {
        return ResponseEntity.ok(walletService.getTransactions(userId, pageable));
    }
}
