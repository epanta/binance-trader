package com.panta.cryptobot.services.strategies.futures.impl;

import com.panta.cryptobot.dependency.BinanceApiClientFactory;
import com.panta.cryptobot.dependency.domain.market.Candlestick;
import com.panta.cryptobot.dependency.domain.market.CandlestickInterval;
import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.dependency.exception.BinanceApiException;
import com.panta.cryptobot.indicators.EMA;
import com.panta.cryptobot.indicators.RSI;
import com.panta.cryptobot.indicators.SimpleMovingAverage;
import com.panta.cryptobot.services.strategies.futures.BuyFuturesService;
import com.panta.cryptobot.util.TraderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuyFuturesServiceImpl implements BuyFuturesService {

    @Value("${binance.buyer.order.currency}")
    private String BUYER_CURRENCY;

    @Value("${binance.buyer.order.volume}")
    private BigDecimal VOLUME_LIMIT;

    private int count;

    private final TraderUtil traderUtil;

    private final RSI rsi;

    private final SimpleMovingAverage simpleMovingAverage;

    @Override
    public TickerStatistics getStrategy() {
        List<TickerStatistics> tickersStaticsList = null;
        try {

            tickersStaticsList = traderUtil.getInstance().getAll24HrPriceStatistics();

            tickersStaticsList = tickersStaticsList.stream()
                    .filter(marketTickerResponse -> marketTickerResponse.getSymbol().endsWith("AXSBUSD"))
                    .collect(Collectors.toList());

            tickersStaticsList.stream().forEach(tickerStatistics -> tickerStatistics.setPercentage(new BigDecimal(tickerStatistics.getPriceChangePercent()).setScale(2, RoundingMode.DOWN)));

        } catch (BinanceApiException e) {
            log.error("Erro ao consultar dados das moedas na API: {}", e.getMessage());
            return null;
        }

        Optional<TickerStatistics> tickerStatistics = null;

        if (tickersStaticsList != null
                && !tickersStaticsList.isEmpty()) {

            log.info("QTD PARES ENCONTRADOS NA ESTRATEGIA 8: {}", tickersStaticsList.size());
            count = 1;

            tickerStatistics = tickersStaticsList.stream()
                    .sorted(Comparator.comparing(TickerStatistics::getPercentage).reversed())
                    .filter(marketTicker -> isCandleStickLimitAbletoBuy(marketTicker))
                    .filter(marketTicker -> traderUtil.isPercentageVariationAbleToBUY(marketTicker))
                    .findFirst();
            if (tickerStatistics.isPresent()) {
                TickerStatistics statistics = tickerStatistics.get();
                statistics.setStrategy(8);
                return statistics;
            }
            log.info("NENHUM PAR APROVADO PARA COMPRA NA ESTRATEGIA 2...");

        }
        return null;
    }

    private boolean isCandleStickLimitAbletoBuy(TickerStatistics marketTicker) {
        final String symbol = marketTicker.getSymbol();
        final BigDecimal lastPrice = new BigDecimal(marketTicker.getLastPrice());
        final BigDecimal perce24h = new BigDecimal(marketTicker.getPriceChangePercent()).setScale(2, RoundingMode.DOWN);
        boolean result = false;

        traderUtil.sleep(500);

        Long currentTime = traderUtil.getServerTime();

        if (currentTime == null) return false;

        List<Candlestick> marketKlineResponseList = getCandlesticks(symbol, currentTime);

        if (marketKlineResponseList == null || marketKlineResponseList.isEmpty()) {
            return false;
        }

        marketKlineResponseList = marketKlineResponseList.stream().sorted(Comparator.comparing(Candlestick::getOpenTime)).collect(Collectors.toList());
        String status = "REPROVADO";

        BigDecimal high = BigDecimal.ZERO;
        BigDecimal low = BigDecimal.ZERO;
        BigDecimal volume3 = BigDecimal.ZERO;

        if (marketKlineResponseList.size() >= 3) {

            Date openTime1 = new Date();
            openTime1.setTime(marketKlineResponseList.get(0).getOpenTime());

            Date openTime2 = new Date();
            openTime2.setTime(marketKlineResponseList.get(1).getOpenTime());

            Date openTime3 = new Date();
            openTime3.setTime(marketKlineResponseList.get(2).getOpenTime());

            BigDecimal volume1 = new BigDecimal(marketKlineResponseList.get(0).getVolume());
            BigDecimal volume2 = new BigDecimal(marketKlineResponseList.get(1).getVolume());
            volume3 = new BigDecimal(marketKlineResponseList.get(2).getVolume());

            BigDecimal openPrice1 = new BigDecimal(marketKlineResponseList.get(0).getOpen());

            BigDecimal openPrice2 = new BigDecimal(marketKlineResponseList.get(1).getOpen());

            BigDecimal closePrice1 = new BigDecimal(marketKlineResponseList.get(0).getClose());
            BigDecimal closePrice2 = new BigDecimal(marketKlineResponseList.get(1).getClose());
            BigDecimal closePrice3 = new BigDecimal(marketKlineResponseList.get(2).getClose());

            high = new BigDecimal(marketKlineResponseList.get(2).getHigh());
            low = new BigDecimal(marketKlineResponseList.get(2).getLow());

            if (volume3.compareTo(VOLUME_LIMIT) > 0) {
                    if (closePrice1.compareTo(openPrice1) > 0) { //TODO fazer o comparativo com o closePrice2 > openPrice2
                    if (closePrice2.compareTo(openPrice2) > 0 &&
                            volume2.compareTo(volume1) > 0) {
                        if (closePrice3.compareTo(closePrice2) > 0 && closePrice2.compareTo(closePrice1) > 0) {
                            if (lastPrice.compareTo(closePrice3) >= 0 && lastPrice.compareTo(traderUtil.avgPrice(high, low)) > 0) {
                                result = true;
                                status = "APROVADO PARCIALMENTE";
                            }
                        }
                    }
                }
            }
        }

        log.info("{} - {} [{}] Var24h: {}% Media: {} Vol.: {}", count++, status, traderUtil.getSymbolFormatted(symbol, BUYER_CURRENCY),
                perce24h, traderUtil.avgPrice(high, low), volume3.setScale(2, RoundingMode.HALF_DOWN));
        return result;
    }

    @Nullable
    private List<Candlestick> getCandlesticks(String symbol, Long currentTime) {
        int limit = 1000;
        Date from = new Date();
        from.setTime(traderUtil.getDateTimeMinusDays(currentTime, -17));

        Date to = new Date();
        to.setTime(traderUtil.getDateTimeMinusHours(currentTime, 0));

        List<Candlestick> marketKlineResponseList = null;
        try {
            List<Candlestick> candlestickList = traderUtil.getInstance()
                    .getCandlestickBars(symbol, CandlestickInterval.FIFTEEN_MINUTES, limit,
                    from.getTime(), to.getTime());
            if (candlestickList != null && !candlestickList.isEmpty()
                    && candlestickList.size() >= limit) {
                marketKlineResponseList = candlestickList;
            }

            //double[] doubles17 = RSI.calculateRSIValues(candlestickList, 17);
            double doubles17 = simpleMovingAverage.getSMA(candlestickList, 17);
            log.info("PERIODO 17 ={}", doubles17 );

            double doubles55 = simpleMovingAverage.getSMA(candlestickList, 55);

            log.info("PERIODO 55 ={}", doubles55);

        }catch (BinanceApiException e) {
            log.error("Erro ao consultar candle: {}", e.getMessage());
        }
        return marketKlineResponseList;
    }
}
