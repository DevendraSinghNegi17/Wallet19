package com.game.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request object for spending funds from the wallet")
public class SpendRequest {

    @NotBlank(message = "User ID is required")
    @Schema(description = "Unique identifier of the user", example = "user_12345")
    private String userId;

    @NotBlank(message = "Asset code is required")
    @Schema(description = "Asset code to be spent", example = "GOLD")
    private String asset;

    @Positive(message = "Amount must be positive")
    @Schema(description = "Amount to deduct", example = "50")
    private Long amount;

    @NotBlank(message = "Idempotency key is required")
    @Schema(description = "Unique key to prevent duplicate processing", example = "spend-uuid-002")
    private String idempotencyKey;

    @NotBlank(message = "Order ID is required")
    @Schema(description = "External order or transaction ID", example = "ORD-998877")
    private String orderId;
}
