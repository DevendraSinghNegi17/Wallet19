package com.game.wallet.controller;

import com.game.wallet.dto.*;
import com.game.wallet.service.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

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

    @Operation(summary = "View Balance", description = "Get current balance for a specific user and asset type")
    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<BalanceResponse>> getBalance(
            @Parameter(description = "User ID to fetch balance for", required = true, example = "user123")
            @RequestParam String userId,
            @Parameter(description = "Asset code (e.g., GOLD, GEMS, DIAMOND)", required = true, example = "GOLD")
            @RequestParam String asset
    ) {
        return ResponseEntity.ok(ApiResponse.success("Balance fetched successfully", walletService.getBalance(userId, asset)));
    }

    @Operation(summary = "View Transactions", description = "Get paginated transaction history for a user. Returns both debit (money out) and credit (money in) transactions, sorted by newest first by default.")
    @GetMapping("/transactions")
    public ResponseEntity<Page<LedgerResponse>> getTransactions(
            @Parameter(description = "User ID to fetch transactions for", required = true, example = "user123")
            @RequestParam String userId,
            @Parameter(description = "Page number (0-indexed). First page is 0.", required = false, schema = @Schema(defaultValue = "0", minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page (max 100)", required = false, schema = @Schema(defaultValue = "20", minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort criteria in format: property(,asc|desc). Supported properties: createdAt, amount, asset. Examples: 'createdAt,desc' or 'amount,asc'", required = false, schema = @Schema(defaultValue = "createdAt,desc"))
            @RequestParam(defaultValue = "createdAt,desc") String[] sort
    ) {
        if (size > 100) {
            size = 100;
        }
        if (size < 1) {
            size = 20;
        }

        Sort.Order[] orders = Arrays.stream(sort)
                .map(this::parseSortParameter)
                .toArray(Sort.Order[]::new);

        Pageable pageable = PageRequest.of(page, size, Sort.by(orders));
        Page<LedgerResponse> transactions = walletService.getTransactions(userId, pageable);

        return ResponseEntity.ok(transactions);
    }

    private Sort.Order parseSortParameter(String sortParam) {
        String[] parts = sortParam.split(",");
        String property = parts[0].trim();
        Sort.Direction direction = Sort.Direction.DESC;

        if (parts.length > 1 && parts[1].trim().equalsIgnoreCase("asc")) {
            direction = Sort.Direction.ASC;
        }

        if (!isValidSortProperty(property)) {
            property = "createdAt";
        }

        return new Sort.Order(direction, property);
    }

    private boolean isValidSortProperty(String property) {
        return property.matches("^[a-zA-Z]+$") &&
                (property.equals("createdAt") ||
                        property.equals("amount") ||
                        property.equals("asset") ||
                        property.equals("id"));
    }
}