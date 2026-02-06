package com.game.wallet.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "asset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "code")
public class Asset {

    @Id
    @Column(name = "code", length = 20, nullable = false)
    private String code;

    @Column(name = "limited_supply", nullable = false)
    private Boolean limitedSupply = false;
}
