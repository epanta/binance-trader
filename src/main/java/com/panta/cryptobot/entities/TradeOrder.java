package com.panta.cryptobot.entities;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "trade_order_binance_spring")
@Data
@ToString
@EqualsAndHashCode
public class TradeOrder implements Serializable {

    @Id
    @Column(name = "operatorBuyId", nullable = false)
    private String operatorBuyId;

    @Column(name = "pair", nullable = false)
    private String pair;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "operator", nullable = false)
    private String operator;

    @Column(name = "buyValue", precision = 16, scale=8, nullable = false)
    private BigDecimal buyValue;

    @Column(name = "initialPrice", precision = 16, scale=8, nullable = false)
    private BigDecimal initialPrice;

    @Column(name = "profitValue", precision = 16, scale=8, nullable = true)
    private BigDecimal profitValue;

    @Column(name = "buyDate", nullable = false)
    private Date buyDate;

    @Column(name = "buyStatus", nullable = true)
    private String buyStatus;

    @Column(name = "buyTax", precision = 16, scale=8, nullable = true)
    private BigDecimal buyTax;

    @Column(name = "buyCurrencyAmount", precision = 16, scale=8, nullable = false)
    private BigDecimal buyCurrencyAmount;

    @Column(name = "sellValue", precision = 16, scale=8, nullable = true)
    private BigDecimal sellValue;

    @Column(name = "updatedPrice", precision = 16, scale=8, nullable = true)
    private BigDecimal updatedPrice;

    @Column(name = "sellDate", nullable = true)
    private Date sellDate;

    @Column(name = "sellStatus", nullable = true)
    private String sellStatus;

    @Column(name = "sellTax", precision = 16, scale=8, nullable = true)
    private BigDecimal sellTax;

    @Column(name = "lastUpdate", nullable = true)
    private Date lastUpdate;

    @Column(name = "sellCurrencyAmount", precision = 16, scale=8, nullable = true)
    private BigDecimal sellCurrencyAmount;

    @Column(name = "operatorSellId", nullable = true)
    private String operatorSellId;

    @Column(name = "profitPercentage", precision = 16, scale=4, nullable = true)
    private BigDecimal profitPercentage;

    @Column(name = "increasedValue", precision = 16, scale=4, nullable = true)
    private BigDecimal increasedValue;

    @Column(name = "finished", nullable = false)
    private boolean finished;

    @Column(name = "strategy", nullable = true)
    private Integer strategy;

    @Column(name = "maxPercentage", precision = 16, scale=4, nullable = true)
    private BigDecimal maxPercentage;

    @Column(name = "simulated", nullable = false)
    private boolean simulated;

    @PrePersist
    public void prePersist() {
        buyDate = new Date();
        finished = false;
    }

    @PreUpdate
    public void preUpdate() {
        lastUpdate = new Date();
    }

}
