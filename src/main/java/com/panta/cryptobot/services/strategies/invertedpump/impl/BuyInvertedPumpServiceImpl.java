package com.panta.cryptobot.services.strategies.invertedpump.impl;

import com.panta.cryptobot.dependency.domain.market.Candlestick;
import com.panta.cryptobot.dependency.domain.market.CandlestickInterval;
import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.dependency.exception.BinanceApiException;
import com.panta.cryptobot.services.strategies.invertedpump.BuyInvertedPumpService;
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
public class BuyInvertedPumpServiceImpl implements BuyInvertedPumpService {

    @Value("${binance.buyer.order.currency}")
    private String BUYER_CURRENCY;

    @Value("${binance.buyer.order.volume}")
    private BigDecimal VOLUME_LIMIT;

    private int count;

    private final TraderUtil traderUtil;

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

            log.info("QTD PARES ENCONTRADOS NA ESTRATEGIA 4: {}", tickersStaticsList.size());
            count = 1;

            tickerStatistics = tickersStaticsList.stream()
                    .sorted(Comparator.comparing(TickerStatistics::getPercentage).reversed())
                    .filter(marketTicker -> isCandleStickLimitAbletoBuy(marketTicker))
                    .filter(marketTicker -> traderUtil.isPercentageVariationAbleToBUY(marketTicker))
                    .findFirst();
            if (tickerStatistics.isPresent()) {
                TickerStatistics statistics = tickerStatistics.get();
                statistics.setStrategy(4);
                return statistics;
            }
            log.info("NENHUM PAR APROVADO PARA COMPRA NA ESTRATEGIA 4...");

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
            BigDecimal openPrice3 = new BigDecimal(marketKlineResponseList.get(2).getOpen());

            BigDecimal closePrice1 = new BigDecimal(marketKlineResponseList.get(0).getClose());
            BigDecimal closePrice2 = new BigDecimal(marketKlineResponseList.get(1).getClose());
            BigDecimal closePrice3 = new BigDecimal(marketKlineResponseList.get(2).getClose());

            high = new BigDecimal(marketKlineResponseList.get(2).getHigh());
            low = new BigDecimal(marketKlineResponseList.get(2).getLow());

            if (volume3.compareTo(VOLUME_LIMIT) > 0) {
                    if (closePrice1.compareTo(openPrice1) < 0) {
                    if (closePrice2.compareTo(openPrice2) < 0 &&
                            volume2.compareTo(volume1) < 0) {
                        if (closePrice3.compareTo(openPrice3) > 0 ){
                                result = true;
                                status = "APROVADO PARCIALMENTE";
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
        int limit = 3;
        Date from = new Date();
        from.setTime(traderUtil.getDateTimeMinusHours(currentTime, -24));

        Date to = new Date();
        //to.setTime(getDateTimeMinusHours(currentTime, -7));

        List<Candlestick> marketKlineResponseList = null;
        try {
            List<Candlestick> candlestickList = traderUtil.getInstance().getCandlestickBars(symbol, CandlestickInterval.FOUR_HOURLY, limit,
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
