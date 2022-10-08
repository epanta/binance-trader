package com.panta.cryptobot.services.strategies.dump.impl;

import com.panta.cryptobot.dependency.domain.market.Candlestick;
import com.panta.cryptobot.dependency.domain.market.CandlestickInterval;
import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.dependency.exception.BinanceApiException;
import com.panta.cryptobot.services.strategies.dump.BuyDumpService;
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
public class BuyDumpServiceImpl implements BuyDumpService {

    @Value("${binance.buyer.order.currency}")
    private String BUYER_CURRENCY;

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
                    .sorted(Comparator.comparing(TickerStatistics::getPercentage))
                    .collect(Collectors.toList());

        }catch (BinanceApiException e) {
            log.error("Erro ao consultar dados das moedas na API: {}" , e.getMessage());
            return null;
        }

        Optional<TickerStatistics> tickerStatistics = null;

        if (tickersStaticsList != null
                && !tickersStaticsList.isEmpty()) {

            log.info("QTD PARES ENCONTRADOS NA ESTRATEGIA 3: {}", tickersStaticsList.size());

            log.info("BUSCANDO MELHOR PAR PARA COMPRA NA ESTRATEGIA 3...");

            tickerStatistics = tickersStaticsList.stream()
                    .sorted(Comparator.comparing(TickerStatistics::getVolumeFormatted).reversed())
                    .filter(marketTicker -> isCandleStickLimitAbletoBuy(marketTicker))
                    .filter(marketTicker -> traderUtil.isPercentageVariationAbleToBUY(marketTicker))
                    .findFirst();

            if (tickerStatistics.isPresent()) {
                TickerStatistics statistics = tickerStatistics.get();
                statistics.setStrategy(3);
                return statistics;
            }
        }
        return null;
    }

    private boolean isValidToBuy(final TickerStatistics marketTickerResponse) {
        BigDecimal percentage = new BigDecimal(marketTickerResponse.getPriceChangePercent());
        marketTickerResponse.setPercentage(percentage);
        //if (percentage.compareTo(BigDecimal.ZERO) < 0) {
            return true;
        //}
        //return false;
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

        marketKlineResponseList = marketKlineResponseList.stream().sorted(Comparator.comparing(Candlestick::getOpenTime)).collect(Collectors.toList());

        Candlestick marketKlineResponse = marketKlineResponseList.get(0);

        Candlestick marketKlineCurrentResponse = marketKlineResponseList.get(1);

        BigDecimal percentageIncreased = traderUtil.calculatePercentage(Double.valueOf(marketKlineResponse.getClose()),
                lastPrice.doubleValue(), true);
        if (percentageIncreased.compareTo(BigDecimal.valueOf(3)) >=0) {
            log.info("PERCENTUAL DE AUMENTO APROVADO DO PAR {}: {}", traderUtil.getSymbolFormatted(symbol, BUYER_CURRENCY), percentageIncreased);
            log.info("LIMITE ATUAL = {}, LIMITE MINIMO = {}", new BigDecimal(marketKlineCurrentResponse.getVolume()),  VOLUME_LIMIT);
            if (new BigDecimal(marketKlineCurrentResponse.getVolume()).compareTo(VOLUME_LIMIT) > 0) {
                log.info("LIMITE DE VOLUME APROVADO PARA {}: {}. VALIDANDO VELA...", traderUtil.getSymbolFormatted(symbol, BUYER_CURRENCY),
                        marketTicker.getVolumeFormatted().setScale(2, RoundingMode.HALF_DOWN));
                if (new BigDecimal(marketKlineCurrentResponse.getClose())
                        .compareTo(new BigDecimal(marketKlineCurrentResponse.getOpen())) > 0) {
                    log.info("PAR APROVADO NA ESTRATEGIA 3: {}", traderUtil.getSymbolFormatted(symbol, BUYER_CURRENCY));
                    log.info("Maior preço negociado do ativo: {}", marketKlineCurrentResponse.getHigh());
                    return true;
                } else {
                    log.info("CONDICOES DA VELA INVALIDA, CLOSE = {} < OPEN = {}", new BigDecimal(marketKlineCurrentResponse.getClose()),
                            new BigDecimal(marketKlineCurrentResponse.getOpen()));
                }
            }
        }
        log.info("PAR REPROVADO NA ESTRATEGIA 3: [{}] P24h {}% Vol.: {} Aumento: {}%", traderUtil.getSymbolFormatted(symbol, BUYER_CURRENCY),
                marketTicker.getPercentage().setScale(1, RoundingMode.DOWN),
                new BigDecimal(marketKlineCurrentResponse.getVolume()).setScale(2, RoundingMode.HALF_DOWN), percentageIncreased);
        return false;
    }

    @Nullable
    private List<Candlestick> getCandlesticks(String symbol, Long currentTime) {
        int limit = 2;
        Date from = new Date();
        from.setTime(traderUtil.getDateTimeMinusMinutes(currentTime, -50));

        Date to = new Date();
        //to.setTime(traderUtil.getDateTimeMinusHours(currentTime, -3));

        List<Candlestick> marketKlineResponseList = null;

        try {
            List<Candlestick> candlestickList = traderUtil.getInstance().getCandlestickBars(symbol, CandlestickInterval.FIFTEEN_MINUTES, limit,
                    from.getTime(), to.getTime());
            if (candlestickList != null && !candlestickList.isEmpty()
                    && candlestickList.size() >= limit) {
                marketKlineResponseList = candlestickList;
            }
        }catch (BinanceApiException e) {
            log.error("Erro ao consultar candle: {}", e.getMessage());
        }
        return marketKlineResponseList;
    }

}
