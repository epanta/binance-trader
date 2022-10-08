package com.panta.cryptobot.services.strategies.onecurrency.impl;

import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.entities.TradeOrder;
import com.panta.cryptobot.repositories.TradeOrderRepository;
import com.panta.cryptobot.services.strategies.onecurrency.SellOneCurrencyService;
import com.panta.cryptobot.util.TraderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellOneCurrencyServiceImpl implements SellOneCurrencyService {

    private final TradeOrderRepository tradeOrderRepository;

    private final TraderUtil traderUtil;

    @Value("${binance.currency.percentage.limit}")
    private Double PERCENTAGE_LIMIT;

    @Override
    public boolean keepInOperation(final TickerStatistics marketTickerResponse, TradeOrder order) {
        boolean result;
        Double initialPrice = Double.valueOf(order.getInitialPrice().doubleValue());

        BigDecimal profitPercentage = traderUtil.calculatePercentage(initialPrice,
        Double.valueOf(marketTickerResponse.getLastPrice()), false);

        result = shouldKeep(profitPercentage, order.getPair());

        updateOrderData(order, initialPrice, Double.valueOf(marketTickerResponse.getLastPrice()), profitPercentage);

        return result;
    }

    private void updateOrderData(TradeOrder order, Double initialPrice, Double currentPrice, BigDecimal profitPercentage) {
        order.setProfitPercentage(profitPercentage.setScale(2, RoundingMode.DOWN));
        order.setUpdatedPrice(BigDecimal.valueOf(currentPrice));
        order.setIncreasedValue(new BigDecimal(currentPrice-initialPrice).setScale(2, RoundingMode.DOWN));

        Double currentProfit = order.getBuyCurrencyAmount().doubleValue() * currentPrice;
        order.setProfitValue(new BigDecimal(currentProfit - order.getBuyValue().doubleValue()).setScale(8, RoundingMode.DOWN));

        tradeOrderRepository.save(order);
    }

    private boolean shouldKeep(BigDecimal percentage, String pair){
        if (percentage.compareTo(BigDecimal.valueOf(PERCENTAGE_LIMIT)) > 0) {
            log.info("Percentual de ganho atingindo [{}%]. Vendendo o ativo [{}].",
                    percentage, pair);
            return false;
        }
        log.info("Percentual de ganho nao atingindo [{}%]. Permanecendo com o ativo [{}].",
                percentage, pair);
        return true;
    }
}