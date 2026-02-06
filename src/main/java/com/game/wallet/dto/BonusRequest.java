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
@Schema(description = "Request object for crediting a bonus to a user")
public class BonusRequest {

    @NotBlank(message = "User ID is required")
    @Schema(description = "Unique identifier of the user", example = "user_12345")
    private String userId;

    @NotBlank(message = "Asset code is required")
    @Schema(description = "Asset code for the bonus", example = "GEMS")
    private String asset;

    @Positive(message = "Amount must be positive")
    @Schema(description = "Bonus amount to credit", example = "10")
    private Long amount;

    @NotBlank(message = "Idempotency key is required")
    @Schema(description = "Unique key to prevent duplicate processing", example = "bonus-uuid-003")
    private String idempotencyKey;

    @NotBlank(message = "Reason is required")
    @Schema(description = "Reason for the bonus (used as ledger reference)", example = "Daily login reward")
    private String reason;
}
