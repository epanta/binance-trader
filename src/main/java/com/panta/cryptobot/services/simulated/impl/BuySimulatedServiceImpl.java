package com.panta.cryptobot.services.simulated.impl;

import com.panta.cryptobot.dependency.domain.OrderSide;
import com.panta.cryptobot.dependency.domain.OrderStatus;
import com.panta.cryptobot.dependency.domain.OrderType;
import com.panta.cryptobot.dependency.domain.account.NewOrder;
import com.panta.cryptobot.dependency.domain.account.NewOrderResponse;
import com.panta.cryptobot.dependency.domain.general.FilterType;
import com.panta.cryptobot.dependency.domain.general.SymbolFilter;
import com.panta.cryptobot.dependency.domain.general.SymbolInfo;
import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.dependency.exception.BinanceApiException;
import com.panta.cryptobot.entities.TradeOrder;
import com.panta.cryptobot.repositories.TradeOrderRepository;
import com.panta.cryptobot.services.simulated.BuySimulatedService;
import com.panta.cryptobot.services.strategies.candle.BuyCandleService;
import com.panta.cryptobot.services.strategies.currencylist.BuyCurrencyListService;
import com.panta.cryptobot.services.strategies.dump.BuyDumpService;
import com.panta.cryptobot.services.strategies.engolfo.BuyEngolfoService;
import com.panta.cryptobot.services.strategies.futures.BuyFuturesService;
import com.panta.cryptobot.services.strategies.invertedpump.BuyInvertedPumpService;
import com.panta.cryptobot.services.strategies.onecurrency.BuyOneCurrencyService;
import com.panta.cryptobot.services.strategies.pump.BuyPumpService;
import com.panta.cryptobot.services.strategies.pumpV2.BuyPumpV2Service;
import com.panta.cryptobot.util.TraderUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class BuySimulatedServiceImpl implements BuySimulatedService {

    @Value("${binance.buyer.order.limit}")
    private int ORDER_LIMIT;

    @Value("${binance.buyer.order.currency}")
    private String BUYER_CURRENCY;

    @Value("${binance.buyer.order.value}")
    private BigDecimal VALUE_TO_BUY;

    @Value("${binance.seller.percentage.limit.loss}")
    private BigDecimal MAX_PERCENTAGE_LOSS;

    @Value("${operator}")
    private String OPERATOR_NAME;

    @Value("${binance.buyer.strategy}")
    private String BUY_STRATEGY;

    @Value("${binance.simulated.total.available}")
    private BigDecimal TOTAL_TO_USE;

    @Value("${binance.simulated.active}")
    private boolean SIMULATED_ACTIVE;

    private final TraderUtil traderUtil;

    private final TradeOrderRepository tradeOrderRepository;

    private final BuyDumpService buyDumpService;

    private final BuyPumpService buyPumpService;

    private final BuyCandleService buyCandleService;

    private final BuyInvertedPumpService buyInvertedPumpService;

    private final BuyEngolfoService buyEngolfoService;

    private final BuyOneCurrencyService buyOneCurrencyService;

    private final BuyCurrencyListService buyCurrencyListService;

    private final BuyFuturesService buyFuturesService;

    private final BuyPumpV2Service buyPumpV2Service;

    @Override
    public void initSellProcess() throws InterruptedException {
        VALUE_TO_BUY = setValueToBuy();
        while(true) {
            //log.info("Iniciando o processo de compra. Aguarde...");
            buyProcess();
            //log.info("Reiniciando o processo de compra. Aguarde...");
            Thread.sleep(4000);
        }
    }

    @Override
    public void buyProcess() throws InterruptedException {
        while (listOrders().size() <= ORDER_LIMIT) {
            BigDecimal totalAvailable = totalCurrencyAvailable();
            if (totalAvailable.compareTo(VALUE_TO_BUY) >= 0) {
                TickerStatistics bestPairToBuy = getBestToBuy();
                if (bestPairToBuy != null
                        && !containsPairUnfinished(bestPairToBuy.getSymbol())) {
                    log.info("Melhor par de moedas para comprar é: {}", bestPairToBuy.getSymbol());
                    final SymbolInfo symbolInfo = traderUtil.getPairDetails(bestPairToBuy.getSymbol());
                    if (symbolInfo != null) {
                        if (validateConditionToBuy(symbolInfo, bestPairToBuy, totalAvailable)) {
                            createBUYOrder(symbolInfo, bestPairToBuy);
                        } else {
                            log.info("Condicoes para compra do par {} nao foram atendidas.", bestPairToBuy.getSymbol());
                        }
                    }
                } else {
                    log.info("NENHUM PAR APROVADO. TENTANDO NOVAMENTE EM ALGUNS SEGUNDOS...");
                }
            }
            Thread.sleep(10000);
        }

        if (listOrders().size() >= ORDER_LIMIT) {
            log.info("Total de ordens abertas ultrapassa o limite de {}. ", ORDER_LIMIT);
        }
    }

    private TickerStatistics getBestToBuy() {
        TickerStatistics tickerStatisticsResult = null;
        if (BUY_STRATEGY.contains("1")) {
            tickerStatisticsResult = buyCandleService.getStrategy();
        }
        if (BUY_STRATEGY.contains("2") && tickerStatisticsResult == null) {
            tickerStatisticsResult = buyPumpService.getStrategy();
        }
        if (BUY_STRATEGY.contains("3") && tickerStatisticsResult == null) {
            tickerStatisticsResult = buyDumpService.getStrategy();
        }
        if (BUY_STRATEGY.contains("4") && tickerStatisticsResult == null) {
            tickerStatisticsResult = buyInvertedPumpService.getStrategy();
        }
        if (BUY_STRATEGY.contains("5") && tickerStatisticsResult == null) {
            tickerStatisticsResult = buyEngolfoService.getStrategy();
        }

        if (BUY_STRATEGY.contains("6") && tickerStatisticsResult == null) {
            tickerStatisticsResult = buyOneCurrencyService.getStrategy();
        }

        if (BUY_STRATEGY.contains("7") && tickerStatisticsResult == null) {
            tickerStatisticsResult = buyCurrencyListService.getStrategy();
        }

        if (BUY_STRATEGY.contains("8") && tickerStatisticsResult == null) {
            tickerStatisticsResult = buyFuturesService.getStrategy();
        }

        if (BUY_STRATEGY.contains("9") && tickerStatisticsResult == null) {
            tickerStatisticsResult = buyPumpV2Service.getStrategy();
        }

        return tickerStatisticsResult;
    }

    @Override
    public Boolean getSimulated() {
        return SIMULATED_ACTIVE;
    }

    private List<TradeOrder> listOrders() {
        final List<TradeOrder> tradeOrders = tradeOrderRepository.findAllByFinishedIsFalseAndSimulatedIsTrue();
        int ordersSize = tradeOrders.size();
        log.info("QTD DE ORDENS SIMULADAS ABERTAS: {}", ordersSize);

        return tradeOrders;
    }

    private BigDecimal setValueToBuy() {
        Double percentage = Math.abs(MAX_PERCENTAGE_LOSS.doubleValue()) / 100;
        Double value = percentage * VALUE_TO_BUY.doubleValue();
        return BigDecimal.valueOf(VALUE_TO_BUY.doubleValue() + value);
    }

    private boolean containsPairUnfinished(String pair) {
        return tradeOrderRepository.findFirstByPairAndFinishedIsFalseAndSimulatedIsTrue(pair).isPresent();
    }

    private boolean validateConditionToBuy(SymbolInfo symbolInfo,
                                           TickerStatistics bestPairToBuy, BigDecimal totalAvailable) {
        SymbolFilter sbFilter = symbolInfo.getFilters().stream().filter(symbolFilter -> symbolFilter.getFilterType().equals(FilterType.PRICE_FILTER)).findFirst().get();

        if (new BigDecimal(sbFilter.getMinPrice()).compareTo(totalAvailable) > 0) {
            log.info("O saldo de [{}] disponivel em conta na moeda {} eh insuficiente para comprar o ativo. " +
                    "Valor minimo para compra de {} eh: {}", totalAvailable, BUYER_CURRENCY, symbolInfo.getSymbol(), sbFilter.getMinPrice());
            return false;
        }

        if (new BigDecimal(sbFilter.getMinPrice()).compareTo(VALUE_TO_BUY) > 0) {
            log.info("O valor de [{}] definido para compra eh insuficiente para comprar o ativo. " +
                    "Valor minimo para compra de {} eh: {}", VALUE_TO_BUY, symbolInfo.getSymbol(), sbFilter.getMinPrice());
            return false;
        }

        log.info( "Valor minimo para compra de {} eh: {}", symbolInfo.getSymbol(), sbFilter.getMinPrice());

        SymbolFilter sbFilterLS = symbolInfo.getFilters().stream()
                .filter(symbolFilter -> symbolFilter.getFilterType().equals(FilterType.LOT_SIZE)).findFirst().get();

        BigDecimal amount = traderUtil.getAmountToBuy(Double.valueOf(VALUE_TO_BUY.toString()),
                        Double.valueOf(bestPairToBuy.getLastPrice()),
                        symbolInfo.getBaseAssetPrecision());

        if (new BigDecimal(sbFilterLS.getMinQty()).compareTo(amount) > 0) {
            log.info("A quantidade [{}] insuficiente para comprar o ativo. Quantidade minima para compra de {} eh: {}",
                    amount,  symbolInfo.getSymbol(), sbFilterLS.getMinQty());
            return false;
        }

        return true;
    }

    private boolean createBUYOrder(SymbolInfo symbolInfo, TickerStatistics pairData) {
        final NewOrderResponse buyOrder = buyOrderRequest(pairData, symbolInfo);
        if (buyOrder != null) {
            Toolkit.getDefaultToolkit().beep();
            return persistBuyOrder(symbolInfo, pairData, buyOrder);
        } else {
            log.info("Problema ao criar a ordem de compra.");
        }

        return false;
    }

    private NewOrderResponse buyOrderRequest(TickerStatistics pairData, SymbolInfo symbolInfo) {
        NewOrderResponse buyOrder = new NewOrderResponse();

        if (symbolInfo != null) {

            Integer precision = symbolInfo.getBaseAssetPrecision();
            boolean lotSizeTry = true;
            int maxTry = precision+1;
            String amount = String.valueOf(traderUtil.getAmountToBuy(Double.valueOf(VALUE_TO_BUY.toString()),
                    Double.valueOf(pairData.getLastPrice()), precision).setScale(precision, RoundingMode.DOWN));

            while (lotSizeTry && maxTry > 0) {
                try {
                    traderUtil.getInstance().newOrderTest(new NewOrder(
                            symbolInfo.getSymbol(),
                            OrderSide.BUY,
                            OrderType.MARKET,
                            null,
                            amount));

                    buyOrder.setOrderId(new Random().nextLong());
                    buyOrder.setSymbol(pairData.getSymbol());
                    buyOrder.setStatus(OrderStatus.FILLED);
                    buyOrder.setOrigQty(amount);
                    buyOrder.setType(OrderType.MARKET);
                    Double cummulativeQuoty = Double.valueOf(amount) * Double.valueOf(pairData.getLastPrice());
                    buyOrder.setCummulativeQuoteQty(String.valueOf(BigDecimal.valueOf(cummulativeQuoty).setScale(precision, RoundingMode.DOWN)));
                    log.info("DADOS DA COMPRA: {}", traderUtil.getGson().toJson(buyOrder));
                    try {
                        Thread.sleep(500);
                    }catch (Exception e) {}
                } catch (BinanceApiException e) {
                    if (e.getMessage().toUpperCase().contains("LOT_SIZE")) {
                        precision--;
                        maxTry--;
                        amount = String.valueOf(traderUtil.getAmountToBuy(Double.valueOf(VALUE_TO_BUY.toString()),
                                Double.valueOf(pairData.getLastPrice()), precision).setScale(precision, RoundingMode.DOWN));
                        continue;
                    }
                    if (e.getMessage().toUpperCase().contains("MIN_NOTIONAL")) {
                        log.warn("QUANTIDADE INVALIDA, AUMENTANDO O PERCENTUAL");
                        maxTry--;
                        amount = String.valueOf(BigDecimal.valueOf(Double.valueOf(amount) * 1.02).setScale(precision, RoundingMode.DOWN));
                        continue;
                    }
                    log.error("Nao foi possivel comprar a moeda. Razao: {}", e.getMessage());
                }
                lotSizeTry = false;
            }
        } else {
            log.warn("buyOrderRequest - SYMBOL NULL");
        }


        return buyOrder;
    }

    private boolean persistBuyOrder(SymbolInfo symbolInfo,
                                    TickerStatistics pairData, NewOrderResponse buyOrder) {
        // Order order = getOrderData(symbolInfo.getSymbol(), buyOrder.getOrderId());

        TradeOrder tradeOrder = new TradeOrder();
        tradeOrder.setPair(buyOrder.getSymbol());
        tradeOrder.setUpdatedPrice(new BigDecimal(pairData.getLastPrice()));
        tradeOrder.setInitialPrice(new BigDecimal(pairData.getLastPrice()));
        tradeOrder.setType(buyOrder.getType().name());

        tradeOrder.setBuyCurrencyAmount(new BigDecimal(buyOrder.getOrigQty()).setScale(8, RoundingMode.DOWN));

        tradeOrder.setBuyValue(new BigDecimal(buyOrder.getCummulativeQuoteQty()));

        BigDecimal tax = BigDecimal.valueOf(Double.valueOf(buyOrder.getCummulativeQuoteQty()) - (Double.valueOf(pairData.getLastPrice()) * Double.valueOf(buyOrder.getOrigQty())))
                .setScale(symbolInfo.getBaseAssetPrecision(), RoundingMode.DOWN);

        log.info("persistBuyOrder - Tax = {}", tax);

        tradeOrder.setBuyTax(tax);

        Date buyDate = new Date();
        tradeOrder.setBuyDate(buyDate);
        tradeOrder.setBuyStatus(buyOrder.getStatus().name());
        tradeOrder.setOperator(OPERATOR_NAME);
        tradeOrder.setOperatorBuyId(String.valueOf(buyOrder.getOrderId()));
        tradeOrder.setFinished(false);
        tradeOrder.setProfitPercentage(BigDecimal.ZERO);
        tradeOrder.setLastUpdate(new Date());
        tradeOrder.setStrategy(pairData.getStrategy());
        tradeOrder.setSimulated(Boolean.TRUE);
        tradeOrderRepository.save(tradeOrder);
        log.info("SAVED BUY ORDER = {}", tradeOrder);

        return true;
    }

    private BigDecimal totalCurrencyAvailable() {
        Double totalInUse = 0d;
        List<Double> tradeOrder = tradeOrderRepository
                .findAllByFinishedIsFalseAndSimulatedIsTrue()
                .stream().map(tOrder -> tOrder.getBuyValue().doubleValue())
                .collect(Collectors.toList());
        if (!tradeOrder.isEmpty()) {
            totalInUse = tradeOrder.stream()
                    .reduce(0d, Double::sum);
            log.info("M=totalCurrencyAvailable, totalInUse = {}", totalInUse);
        }

        log.info("M=totalCurrencyAvailable, TOTAL_TO_USE = {}", TOTAL_TO_USE);

        Double totalAvailable = TOTAL_TO_USE.doubleValue() - totalInUse;

        log.info("M=totalCurrencyAvailable, totalAvailable (TOTAL_TO_USE - totalInUse) = {}", totalAvailable);

        return BigDecimal.valueOf(totalAvailable);
    }
}
