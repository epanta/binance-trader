package com.panta.cryptobot.services.strategies.pumpV2.impl;

import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.dependency.exception.BinanceApiException;
import com.panta.cryptobot.services.strategies.pumpV2.BuyPumpV2Service;
import com.panta.cryptobot.util.TraderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuyPumpV2ServiceImpl implements BuyPumpV2Service {

    @Value("${binance.buyer.order.currency}")
    private String BUYER_CURRENCY;

    @Value("${binance.buyer.order.volume}")
    private BigDecimal VOLUME_LIMIT;

    private int count;

    private final TraderUtil traderUtil;

    //private final IndicatorsOrigin indicatorsOrigin;

    @Override
    public TickerStatistics getStrategy() {
        List<TickerStatistics> tickersStaticsList = null;
        try {

            tickersStaticsList = traderUtil.getInstance().getAll24HrPriceStatistics();

            tickersStaticsList = tickersStaticsList.stream()
                    .filter(marketTickerResponse -> marketTickerResponse.getSymbol().endsWith(BUYER_CURRENCY))
                    .filter(marketTickerResponse -> isValidToBuy(marketTickerResponse))
                    .collect(Collectors.toList());

            //tickersStaticsList.stream().forEach(tickerStatistics -> tickerStatistics.setPercentage(new BigDecimal(tickerStatistics.getPriceChangePercent()).setScale(2, RoundingMode.DOWN)));

        } catch (BinanceApiException e) {
            log.error("Erro ao consultar dados das moedas na API: {}", e.getMessage());
            return null;
        }

        Optional<TickerStatistics> tickerStatistics = null;

        if (tickersStaticsList != null
                && !tickersStaticsList.isEmpty()) {

            log.info("QTD PARES ENCONTRADOS NA ESTRATEGIA 9: {}", tickersStaticsList.size());
            count = 1;

            tickerStatistics = tickersStaticsList.stream()
                    .sorted(Comparator.comparing(TickerStatistics::getPercentage).reversed())
                    .filter(marketTicker -> traderUtil.isPercentageVariationAbleToBUY(marketTicker))
                    .findFirst();
            if (tickerStatistics.isPresent()) {
                TickerStatistics statistics = tickerStatistics.get();
                statistics.setStrategy(9);
                return statistics;
            }
            log.info("NENHUM PAR APROVADO PARA COMPRA NA ESTRATEGIA 9...");

        }
        return null;
    }

    private boolean isValidToBuy(final TickerStatistics marketTickerResponse) {
        BigDecimal percentage = new BigDecimal(marketTickerResponse.getPriceChangePercent());
        marketTickerResponse.setPercentage(percentage);

        BigDecimal openPrice = new BigDecimal(marketTickerResponse.getOpenPrice());

        BigDecimal lastPrice = new BigDecimal(marketTickerResponse.getLastPrice());
        if (lastPrice.compareTo(openPrice) >= 0) {
            log.info("lastPrice={} >= openPrice={}", lastPrice, openPrice);
            BigDecimal percent = traderUtil.calculatePercentage(openPrice.doubleValue(), lastPrice.doubleValue(), false);

            if (percent.compareTo(BigDecimal.valueOf(6)) >=0) {
                log.info("percent={} >= 6", percent);
                return true;
            }
        }
        return false;
    }
}
