package com.game.wallet.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LedgerResponse {
    private String debitUser;
    private String creditUser;
    private String asset;
    private Long amount;
    private String reference;
    private LocalDateTime createdAt;
}