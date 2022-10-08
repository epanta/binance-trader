package com.panta.cryptobot.services.strategies.onecurrency.impl;

import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.dependency.exception.BinanceApiException;
import com.panta.cryptobot.entities.TradeOrder;
import com.panta.cryptobot.repositories.TradeOrderRepository;
import com.panta.cryptobot.services.strategies.onecurrency.BuyOneCurrencyService;
import com.panta.cryptobot.util.TraderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuyOneCurrencyServiceImpl implements BuyOneCurrencyService {

    @Value("${binance.currency.to.buy}")
    private String CURRENCY_TO_BUY;

    private final TraderUtil traderUtil;

    private final TradeOrderRepository tradeOrderRepository;

    @Value("${binance.simulated.active}")
    private boolean SIMULATED_ACTIVE;

    @Value("${binance.currency.percentage.limit}")
    private Double PERCENTAGE_LIMIT;

    @Override
    public TickerStatistics getStrategy() {
        TickerStatistics tickersStatics = null;
        String[] currencies = CURRENCY_TO_BUY.split(",");

        if (currencies != null && currencies.length > 0) {
            final List<String> currencyList = Arrays.stream(currencies).collect(Collectors.toList());
            for (String currency: currencyList) {
                log.info("Iniciando a tentativa de compra do par: {}", currency.trim().toUpperCase());
                tickersStatics = getTickerStatistics(currency.trim().toUpperCase());
                if (tickersStatics != null) {
                    return tickersStatics;
                }
            }
        }
        return tickersStatics;
    }

    @Nullable
    private TickerStatistics getTickerStatistics(final String currencyToBuy) {
        TickerStatistics tickersStatics = null;
        try {
            if(containsPairUnfinished(currencyToBuy)) {
                return tickersStatics;
            }

            tickersStatics = traderUtil.getInstance().get24HrPriceStatistics(currencyToBuy);
            tickersStatics.setStrategy(6);
            BigDecimal percentage = getLastPercentageOrdered(tickersStatics);

            if (!shouldBuy(percentage, tickersStatics.getSymbol(), tickersStatics)) {
                tickersStatics = null;
            }
        }catch (BinanceApiException e) {
            log.error("Erro ao consultar dados das moedas na API: {}" , e.getMessage());
        }
        return tickersStatics;
    }

    private BigDecimal getLastPercentageOrdered(TickerStatistics tickerStatistics) {
        Optional<TradeOrder> tradeOrderOptional = getTradeOrder(tickerStatistics);

        if (tradeOrderOptional.isPresent()) {
            TradeOrder tradeOrder = tradeOrderOptional.get();
            return traderUtil.calculatePercentage(tradeOrder.getInitialPrice().doubleValue(),
                    Double.valueOf(tickerStatistics.getLastPrice()), false);
        }

        Double limit = PERCENTAGE_LIMIT + 1;

        return BigDecimal.valueOf(-limit);
    }

    private Optional<TradeOrder> getTradeOrder(TickerStatistics tickerStatistics) {
        if (SIMULATED_ACTIVE) {
            return tradeOrderRepository
                    .findFirstByPairAndFinishedIsTrueAndSimulatedIsTrueOrderByBuyDateDesc(tickerStatistics.getSymbol());
        }

        return tradeOrderRepository
                .findFirstByPairAndFinishedIsTrueAndSimulatedIsFalseOrderByBuyDateDesc(tickerStatistics.getSymbol());
    }

    private boolean containsPairUnfinished(String pair) {
        if (SIMULATED_ACTIVE) {
            return tradeOrderRepository.findFirstByPairAndFinishedIsFalseAndSimulatedIsTrue(pair).isPresent();
        }

        return tradeOrderRepository.findFirstByPairAndFinishedIsFalseAndSimulatedIsFalse(pair).isPresent();
    }

    private boolean shouldBuy(BigDecimal percentage, String pair, TickerStatistics tickerStatistics){
        if (percentage.compareTo(BigDecimal.valueOf(-PERCENTAGE_LIMIT)) < 0) {
            log.info("Percentual minimo = [{}] atingindo. Atual = [{}%]. Comprando o ativo [{}].",
                    -PERCENTAGE_LIMIT,  percentage, pair);
            return true;
        }
        return shouldBuyForced(percentage, pair, tickerStatistics);
    }

    private boolean shouldBuyForced(final BigDecimal percentage, final String pair, final TickerStatistics tickerStatistics) {
        if (new BigDecimal(tickerStatistics.getPriceChangePercent())
                .compareTo(BigDecimal.valueOf(-PERCENTAGE_LIMIT)) < 0) {
            log.info("Percentual minimo = [{}] atingindo. Atual = [{}%] Comprando o ativo [{}].",
                    -PERCENTAGE_LIMIT,  tickerStatistics.getPriceChangePercent(), pair);
            return true;
        }
        log.info("Percentual minimo = [{}] nao atingindo. Atual [{}%]. Compra do ativo [{}] nao realizada.",
                -PERCENTAGE_LIMIT,
                percentage.doubleValue() < new BigDecimal(tickerStatistics.getPriceChangePercent())
                        .doubleValue() ? percentage : new BigDecimal(tickerStatistics.getPriceChangePercent()), pair);
        return false;
    }
}
