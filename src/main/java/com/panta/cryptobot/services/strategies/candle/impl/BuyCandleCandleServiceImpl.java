package com.panta.cryptobot.services.strategies.candle.impl;

import com.panta.cryptobot.dependency.domain.market.Candlestick;
import com.panta.cryptobot.dependency.domain.market.CandlestickInterval;
import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.dependency.exception.BinanceApiException;
import com.panta.cryptobot.services.strategies.candle.BuyCandleService;
import com.panta.cryptobot.util.TraderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuyCandleCandleServiceImpl implements BuyCandleService {

    @Value("${binance.buyer.order.currency}")
    private String BUYER_CURRENCY;

    @Value("${binance.buyer.min.percentage.increase.currency}")
    private BigDecimal MIN_CURRENCY_PERCENTAGE_LIMIT;

    @Value("${binance.buyer.order.volume}")
    private BigDecimal VOLUME_LIMIT;

    private final TraderUtil traderUtil;

    @Override
    public TickerStatistics getStrategy() {
        List<TickerStatistics> tickersStaticsList = null;
        try {

            tickersStaticsList = traderUtil.getInstance().getAll24HrPriceStatistics();

            tickersStaticsList = tickersStaticsList.stream()
                    .filter(marketTickerResponse -> marketTickerResponse.getSymbol().endsWith(BUYER_CURRENCY))
                    .filter(marketTickerResponse -> isValidToBuy(marketTickerResponse))
                    .sorted(Comparator.comparing(TickerStatistics::getPercentage).reversed())
                    .collect(Collectors.toList());

        }catch (BinanceApiException e) {
            log.error("Erro ao consultar dados das moedas na API: {}" , e.getMessage());
            return null;
        }

        Optional<TickerStatistics> tickerStatistics = null;

        if (tickersStaticsList != null
                && !tickersStaticsList.isEmpty()) {

            log.info("QTD PARES ENCONTRADOS NA ESTRATEGIA 1: {}", tickersStaticsList.size());
            tickersStaticsList.forEach(ticker -> log.info(" (PERCENTUAL 24H {}% - {})",
                    ticker.getPercentage().setScale(1, RoundingMode.DOWN),
                    traderUtil.getSymbolFormatted(ticker.getSymbol(), BUYER_CURRENCY)));

            tickerStatistics = tickersStaticsList.stream()
                    .sorted(Comparator.comparing(TickerStatistics::getVolumeFormatted).reversed())
                    .filter(marketTicker -> isCandleStickLimitAbletoBuy(marketTicker))
                    .filter(marketTicker -> traderUtil.isPercentageVariationAbleToBUY(marketTicker))
                    .findFirst();

            if (tickerStatistics.isPresent()) {
                TickerStatistics statistics = tickerStatistics.get();
                statistics.setStrategy(1);
                return statistics;
            }
            log.info("NENHUM PAR APROVADO PARA COMPRA NA ESTRATEGIA 1...");
        }
        return null;
    }

    private boolean isValidToBuy(TickerStatistics marketTickerResponse) {
        Double startValue = Double.valueOf(marketTickerResponse.getOpenPrice());
        Double currentValue = Double.valueOf(marketTickerResponse.getLastPrice());
        BigDecimal percentage = traderUtil.calculatePercentage(startValue, currentValue, true);

        if (percentage.compareTo(MIN_CURRENCY_PERCENTAGE_LIMIT) > 0) {
            marketTickerResponse.setPercentage(percentage);
            return true;
        }
        return false;
    }

    private boolean isCandleStickLimitAbletoBuy(TickerStatistics marketTicker) {

        final String symbol = marketTicker.getSymbol();
        final BigDecimal lastPrice = new BigDecimal(marketTicker.getLastPrice());

        traderUtil.sleep(500);

        Long currentTime = traderUtil.getServerTime();

        if (currentTime == null) return false;

        List<Candlestick> marketKlineResponseList = getCandlesticks(symbol, currentTime);

        if (marketKlineResponseList == null || marketKlineResponseList.isEmpty()) {
            return false;
        }

        Candlestick marketKlineResponse = marketKlineResponseList.stream().max(Comparator.comparing(Candlestick::getHighFormatted)).get();

        if (lastPrice.compareTo(marketKlineResponse.getHighFormatted()) >=0) {
            if (new BigDecimal(marketKlineResponse.getVolume()).compareTo(VOLUME_LIMIT) > 0) {
                if (new BigDecimal(marketKlineResponse.getClose())
                        .compareTo(new BigDecimal(marketKlineResponse.getOpen())) > 0) {
                    log.info("PAR APROVADO NA ESTRATEGIA 1: {}", traderUtil.getSymbolFormatted(symbol, BUYER_CURRENCY));
                    log.info("Maior preço negociado do ativo: {}", marketKlineResponse.getHigh());
                    return true;
                }
            }
        }
        log.info("PAR REPROVADO NA ESTRATEGIA 1: [{}] P24h {}% Vol.: {}", traderUtil.getSymbolFormatted(symbol, BUYER_CURRENCY),
                marketTicker.getPercentage().setScale(1, RoundingMode.DOWN), new BigDecimal(marketTicker.getVolume()).setScale(2, RoundingMode.HALF_DOWN));
        return false;
    }

    @Nullable
    private List<Candlestick> getCandlesticks(String symbol, Long currentTime) {
        int limit = 3;
        Date from = new Date();
        from.setTime(traderUtil.getDateTimeMinusDays(currentTime, -4));

        Date to = new Date();
        to.setTime(traderUtil.getDateTimeMinusMinutes(currentTime, -1));

        List<Candlestick> marketKlineResponseList = null;
        try {
            marketKlineResponseList = traderUtil.getInstance().getCandlestickBars(symbol, CandlestickInterval.FOUR_HOURLY, limit,
                    from.getTime(), to.getTime() );
        }catch (BinanceApiException e) {
            log.error("Erro ao consultar candle: {}", e.getMessage());
            return null;
        }
        return marketKlineResponseList;
    }
}
