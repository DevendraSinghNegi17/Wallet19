package com.game.wallet.model;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class WalletId implements Serializable {

    private String userId;
    private String asset;
}
