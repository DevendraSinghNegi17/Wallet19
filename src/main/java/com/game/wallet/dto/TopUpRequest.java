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
@Schema(description = "Request object for topping up a user's wallet")
public class TopUpRequest {

    @NotBlank(message = "User ID is required")
    @Schema(description = "Unique identifier of the user", example = "user_12345")
    private String userId;

    @NotBlank(message = "Asset code is required")
    @Schema(description = "Asset code (e.g., GOLD, GEMS)", example = "GOLD")
    private String asset;

    @Positive(message = "Amount must be positive")
    @Schema(description = "Amount to add to the wallet", example = "100")
    private Long amount;

    @NotBlank(message = "Idempotency key is required")
    @Schema(description = "Unique key to prevent duplicate processing", example = "topup-uuid-001")
    private String idempotencyKey;
}
