package com.panta.cryptobot.services.strategies.engolfo.impl;

import com.panta.cryptobot.dependency.domain.market.Candlestick;
import com.panta.cryptobot.dependency.domain.market.CandlestickInterval;
import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.dependency.exception.BinanceApiException;
import com.panta.cryptobot.indicators.EMA;
import com.panta.cryptobot.indicators.RSI;
import com.panta.cryptobot.services.strategies.engolfo.BuyEngolfoService;
import com.panta.cryptobot.util.TraderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuyEngolfoServiceImpl implements BuyEngolfoService {

    @Value("${binance.buyer.order.currency}")
    private String BUYER_CURRENCY;

    @Value("${binance.buyer.order.volume}")
    private BigDecimal VOLUME_LIMIT;

    private int count;

    private final TraderUtil traderUtil;

    private final EMA ema;

    @Override
    public TickerStatistics getStrategy() {
        List<TickerStatistics> tickersStaticsList = null;
        try {

            tickersStaticsList = traderUtil.getInstance().getAll24HrPriceStatistics();

            tickersStaticsList = tickersStaticsList.stream()
                    .filter(marketTickerResponse -> marketTickerResponse.getSymbol().endsWith(BUYER_CURRENCY))
                    .collect(Collectors.toList());

            tickersStaticsList.stream().forEach(tickerStatistics -> tickerStatistics.setPercentage(new BigDecimal(tickerStatistics.getPriceChangePercent()).setScale(2, RoundingMode.DOWN)));

        } catch (BinanceApiException e) {
            log.error("Erro ao consultar dados das moedas na API: {}", e.getMessage());
            return null;
        }

        Optional<TickerStatistics> tickerStatistics = null;

        if (tickersStaticsList != null
                && !tickersStaticsList.isEmpty()) {

            log.info("QTD PARES ENCONTRADOS NA ESTRATEGIA 5: {}", tickersStaticsList.size());
            count = 1;

            tickerStatistics = tickersStaticsList.stream()
                    .sorted(Comparator.comparing(TickerStatistics::getPercentage).reversed())
                    .filter(marketTicker -> isCandleStickLimitAbletoBuy(marketTicker))
                    .filter(marketTicker -> traderUtil.isPercentageVariationAbleToBUY(marketTicker))
                    .findFirst();
            if (tickerStatistics.isPresent()) {
                TickerStatistics statistics = tickerStatistics.get();
                statistics.setStrategy(5);
                return statistics;
            }
            log.info("NENHUM PAR APROVADO PARA COMPRA NA ESTRATEGIA 5...");

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

        if (marketKlineResponseList.size() >= 14) {

            Date previousOpenTime = new Date();
            previousOpenTime.setTime(marketKlineResponseList.get(13).getOpenTime());

            Date currentOpenTime = new Date();
            currentOpenTime.setTime(marketKlineResponseList.get(14).getOpenTime());

            BigDecimal previousVolume = new BigDecimal(marketKlineResponseList.get(13).getVolume());
            BigDecimal currentVolume = new BigDecimal(marketKlineResponseList.get(14).getVolume());

            BigDecimal currentOpenPrice = new BigDecimal(marketKlineResponseList.get(14).getOpen());
            BigDecimal currentClosePrice = new BigDecimal(marketKlineResponseList.get(14).getClose());

            BigDecimal previousOpenPrice = new BigDecimal(marketKlineResponseList.get(13).getOpen());
            BigDecimal previousClosePrice = new BigDecimal(marketKlineResponseList.get(13).getClose());

            Boolean increasedVolume = currentVolume.doubleValue() > (previousVolume.doubleValue() * 1.5);

            List<Candlestick> marketKlineRSIList = marketKlineResponseList.stream().limit(14).collect(Collectors.toList());

            Double rsi = calculate2(marketKlineRSIList);
            //TODO calculate EMA 200
            //RSI < 30 and
           // if (rsi < 30 && currentVolume.compareTo(VOLUME_LIMIT) > 0) {
           double[] ema200 = ema.calculateEmaValues(marketKlineResponseList, 200);

            //System.out.println("Primeiro"+ema200[0]);
            Double ema = ema200[ema200.length -1];
           //System.out.println("Ultimo"+ema);

            //if (rsi < 30 && currentVolume.compareTo(VOLUME_LIMIT) > 0) {
            if (rsi > 70 && currentVolume.compareTo(VOLUME_LIMIT) > 0) {
                System.out.println("Check RSI - OK");
                if (increasedVolume) {
                    System.out.println("Check Volume - OK");
                    if (lastPrice.doubleValue() > ema) {
                        System.out.println("Check EMA - OK");
                        if (lastPrice.compareTo(currentOpenPrice) > 0
                            //&& previousClosePrice.compareTo(previousOpenPrice) > 0
                        ) {
                            System.out.println("Check GREEN LAST CANDLE - OK BUY IT!");
                            result = true;
                            status = "COMPRAR";

                        }
                    }
                }
                //printResult(symbol, perce24h, status, currentVolume, currentClosePrice, rsi);
            }
            printResult(symbol, perce24h, status, currentVolume, currentClosePrice, rsi);
        }


        return result;
    }

    private void printResult(String symbol, BigDecimal perce24h, String status, BigDecimal currentVolume, BigDecimal currentClosePrice, Double rsi) {
        log.info("Par no: {} - {} - [{}] RSI: {} - Var24h: {}% - closePrice: {}  - Vol.: {}", count++, status, traderUtil.getSymbolFormatted(symbol, BUYER_CURRENCY),
                rsi, perce24h, currentClosePrice, currentVolume.setScale(2, RoundingMode.HALF_DOWN));
    }

    @Nullable
    private List<Candlestick> getCandlesticks(String symbol, Long currentTime) {
        //int limit = 15;
        int limit = 200;
        Date from = new Date();
        //from.setTime(traderUtil.getDateTimeMinusHours(currentTime, -15)); // 1 hour
        //from.setTime(traderUtil.getDateTimeMinusMinutes(currentTime, -225)); // 15 min

        from.setTime(traderUtil.getDateTimeMinusMinutes(currentTime, -3000)); // 15 min
        //from.setTime(traderUtil.getDateTimeMinusMinutes(currentTime, -75)); //  5 min
        //Date to = new Date();
        //to.setTime(traderUtil.getDateTimeMinusHours(currentTime, 21));
        //to.setTime(currentTime);

        List<Candlestick> marketKlineResponseList = null;
        try {
            List<Candlestick> candlestickList = traderUtil.getInstance().getCandlestickBars(symbol,
                    CandlestickInterval.FIFTEEN_MINUTES, limit,
                    from.getTime(), currentTime);
            if (candlestickList != null && !candlestickList.isEmpty()
                    && candlestickList.size() >= limit) {
                marketKlineResponseList = candlestickList;
            }
        }catch (BinanceApiException e) {
            log.error("Erro ao consultar candle: {}", e.getMessage());
        }
        return marketKlineResponseList;
    }

    public double calculate(List<Candlestick> qh) {
        int periodLength = 14;
        int qhSize = qh.size();
        int lastBar = qhSize - 1;
        int firstBar = lastBar - periodLength + 1;

        double gains = 0, losses = 0;

        for (int bar = firstBar + 1; bar <= lastBar; bar++) {
            double change = Double.parseDouble(qh.get(bar).getClose())
                    - Double.parseDouble(qh.get(bar - 1).getClose());
            gains += Math.max(0, change);
            losses += Math.max(0, -change);
        }

        double change = gains + losses;

        double value = (change == 0) ? 50 : (100 * gains / change);
        System.out.println("RSI1: " + value);
        return value;
    }

    public double calculate2(List<Candlestick> data)  {
        int periodLength = 14;
        int lastBar = data.size() - 1;
        int firstBar = lastBar - periodLength + 1;
        if (firstBar < 0) {
            String msg = "Quote history length " + data.size() + " is insufficient to calculate the indicator.";
            System.out.println(msg);
        }

        double aveGain = 0, aveLoss = 0;
        for (int bar = firstBar + 1; bar <= lastBar; bar++) {
            double change = Double.parseDouble(data.get(bar).getClose()) - Double.parseDouble(data.get(bar - 1).getClose());
            if (change >= 0) {
                aveGain += change;
            } else {
                aveLoss += change;
            }
        }

        double rs = aveGain / Math.abs(aveLoss);
        double rsi = 100 - 100 / (1 + rs);

        //System.out.println("RSI2: " + rsi);'

        return rsi -5;
    }
}
