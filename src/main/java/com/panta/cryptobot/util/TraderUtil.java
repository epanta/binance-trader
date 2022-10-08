package com.panta.cryptobot.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.panta.cryptobot.dependency.BinanceApiRestClient;
import com.panta.cryptobot.dependency.domain.account.Account;
import com.panta.cryptobot.dependency.domain.general.SymbolInfo;
import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.dependency.exception.BinanceApiException;
import com.panta.cryptobot.services.BinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;

@Component
@Slf4j
@RequiredArgsConstructor
public class TraderUtil {

    private final BinanceService binanceService;

    private Gson gson;

    public BigDecimal getBalance(final String symbol) {
        BigDecimal balance = BigDecimal.ZERO;
        try {
            final Account account = getInstance().getAccount(60_000L, System.currentTimeMillis());
            balance = new BigDecimal(account.getAssetBalance(symbol).getFree());
        }catch (Exception e) {
            log.error("Erro ao consultar saldo na API: {}", e.getMessage());
        }
        return balance;
    }

    public BigDecimal getBalance(final String symbol, final BigDecimal price) {
        BigDecimal balance = BigDecimal.ZERO;
        try {
            Account account = getInstance().getAccount(60_000L, System.currentTimeMillis());
            balance = new BigDecimal(account.getAssetBalance(symbol).getFree());
/*            log.info("SALDO DISPONIVEL EM {} = {}", traderUtil.getSymbolFormatted(symbol, BUYER_CURRENCY),
                    BigDecimal.valueOf(balance.doubleValue() * price.doubleValue()).setScale(8, RoundingMode.DOWN));
            log.info("TOTAL DE MOEDAS EM {} = {}", traderUtil.getSymbolFormatted(symbol, BUYER_CURRENCY), balance);*/
        }catch (Exception e) {
            log.info("Erro ao consultar saldo na API: {}", e.getMessage());
        }
        return balance;
    }

    public SymbolInfo getPairDetails(final String symbol) {
        try {
            return getInstance().getExchangeInfo().getSymbolInfo(symbol);
        }catch (BinanceApiException e) {
            log.error("Erro ao buscar dados do par {} API: {}", symbol,  e.getMessage());
        }
        return null;
    }

    public BigDecimal totalCurrencyAvailable(final String symbol) {
        BigDecimal totalAvailable = getBalance(symbol);
        if (totalAvailable.compareTo(BigDecimal.ZERO) > 0) {
            log.info("SALDO DISPONIVEL PARA TRADES {}: {}", symbol, totalAvailable);

            return totalAvailable;
        }
        log.info("Saldo insuficiente para realizar trades.");
        return BigDecimal.ZERO;
    }

    public BigDecimal getAmountToBuy(Double value, Double lastPrice, int precision) {
        return BigDecimal.valueOf(value / lastPrice).setScale(precision, RoundingMode.DOWN);
    }

    public BinanceApiRestClient getInstance() {
        return binanceService.getInstance();
    }

    public long getDateTimeMinusMinutes(final long currentTime, int minutes) {
        return getDateTimeMinusTime(currentTime, minutes, Calendar.MINUTE);
    }

    public long getDateTimeMinusHours(final long currentTime, int hours) {
        return getDateTimeMinusTime(currentTime, hours, Calendar.HOUR_OF_DAY);
    }

    public long getDateTimeMinusDays(final long currentTime, int days) {
        return getDateTimeMinusTime(currentTime, days, Calendar.DAY_OF_MONTH);
    }

    private long getDateTimeMinusTime(long currentTime, int minutes, int time) {
        Date currentDate = new Date();
        currentDate.setTime(currentTime);

        Calendar c = Calendar.getInstance();
        c.setTime(currentDate);

        c.add(time, minutes);

        return c.getTime().getTime();
    }

    public boolean isPercentageVariationAbleToBUY(final TickerStatistics tickerStatistics) {
        boolean result = false;

        Double currentValue = Double.valueOf(tickerStatistics.getLastPrice());
        TickerStatistics newMarketTickerResponse = null;
        BigDecimal percentageIncreased = BigDecimal.ZERO;
        int verification = 1;

        while (verification <= 3) {
            try {
                Thread.sleep(500);
            }catch (Exception e) {}
            try {
                newMarketTickerResponse = getInstance().get24HrPriceStatistics(tickerStatistics.getSymbol());
            } catch (BinanceApiException e) {
                log.error("Erro ao consultar dado de negociacao do par: {}", e.getMessage());
                return false;
            }

            Double currentValueUpdated = Double.valueOf(newMarketTickerResponse.getLastPrice());
            percentageIncreased = calculatePercentage(currentValue, currentValueUpdated, false);

            if (currentValueUpdated.compareTo(currentValue) > 0) {
                log.info("VERIFICANDO FORCA DE SUBIDA DE [{}]... {}/3. OK", tickerStatistics.getSymbol(), verification);
                result = true;
            } else {
                log.info("VERIFICANDO FORCA DE SUBIDA DE [{}]... {}/3. FALHOU!", tickerStatistics.getSymbol(), verification);
                return false;
            }
            verification++;
        }

        printLimitMessage(tickerStatistics, result, percentageIncreased);

        return result;
    }

    private void printLimitMessage(TickerStatistics tickerStatistics, boolean result, BigDecimal percentageIncreased) {
        if (result) {
            log.warn("PERCENTUAL DE {}% ESTA DENTRO DO LIMITE PARA ENTRADA EM {}",
                    percentageIncreased.setScale(4, RoundingMode.DOWN), tickerStatistics.getSymbol());
        } else {
            log.warn("O PERCENTUAL DE {}% ESTA ABAIXO DO LIMITE PARA ENTRADA EM {}",
                    percentageIncreased.setScale(4, RoundingMode.DOWN), tickerStatistics.getSymbol());
        }
    }

    public BigDecimal calculatePercentage(Double startValue, Double currentValue, boolean shortValue) {

        Double calculatePartOne = currentValue - startValue;
        Double calculatePartTwo = calculatePartOne / startValue;

        Double calculatePartThree = calculatePartTwo * 100;

        if (shortValue) {
            return BigDecimal.valueOf(calculatePartThree.shortValue());
        }

        return BigDecimal.valueOf(calculatePartThree.longValue()).setScale(8, RoundingMode.HALF_UP);
    }

    public Long getServerTime() {
        try {
            return getInstance().getServerTime();
        }catch (BinanceApiException e) {
            log.error("Erro ao consultar o server time: {}", e.getMessage());
        }
        return null;
    }

    public void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            log.error("Erro ao colocar o sleep.");
        }
    }

    public Gson getGson() {
        if (gson == null) {
             gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .serializeNulls()
                    .create();
        }
        return gson;
    }

    public String getSymbolFormatted(final String symbol, final String buyerCurrency) {
        return symbol.replace(buyerCurrency, "_"+buyerCurrency);
    }

    public BigDecimal avgPrice(BigDecimal high, BigDecimal low) {
        return BigDecimal.valueOf((high.doubleValue() + low.doubleValue()) / 2);
    }

    public void getPairInfo(final String symbol) {
        try {
            TickerStatistics statistics = getInstance().get24HrPriceStatistics(symbol);
            log.info("getAskPrice: {}", statistics.getAskPrice());
            log.info("getBidPrice: {}", statistics.getBidPrice());
            log.info("getLastPrice: {}", statistics.getLastPrice());
            log.info("getPrevClosePrice: {}", statistics.getPrevClosePrice());
            log.info("getPriceChange: {}", statistics.getPriceChange());
            log.info("getCount: {}", statistics.getCount());
            log.info("getWeightedAvgPrice: {}", statistics.getWeightedAvgPrice());
            log.info("avg: {}", avgPrice(new BigDecimal(statistics.getHighPrice()), new BigDecimal(statistics.getLowPrice())));
        }catch (BinanceApiException e) {
            log.error("Erro ao buscar dados do par {} API: {}", symbol,  e.getMessage());
        }
    }
}
