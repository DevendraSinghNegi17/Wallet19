package com.game.wallet.dto;


import lombok.*;


@Data
@Builder
public class BalanceResponse {

    private String userId;
    private String asset;
    private Long balance;
    private Long version;
}
