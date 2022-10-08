package com.panta.cryptobot.services.simulated.impl;

import com.panta.cryptobot.dependency.domain.OrderSide;
import com.panta.cryptobot.dependency.domain.OrderStatus;
import com.panta.cryptobot.dependency.domain.OrderType;
import com.panta.cryptobot.dependency.domain.account.NewOrder;
import com.panta.cryptobot.dependency.domain.account.NewOrderResponse;
import com.panta.cryptobot.dependency.domain.general.SymbolInfo;
import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.dependency.exception.BinanceApiException;
import com.panta.cryptobot.entities.TradeOrder;
import com.panta.cryptobot.repositories.TradeOrderRepository;
import com.panta.cryptobot.services.simulated.SellSimulatedService;
import com.panta.cryptobot.services.strategies.generic.SellDefaultService;
import com.panta.cryptobot.services.strategies.onecurrency.SellOneCurrencyService;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class SellSimulatedServiceImpl implements SellSimulatedService {

    private final TradeOrderRepository tradeOrderRepository;

    @Value("${binance.buyer.order.currency}")
    private String BUYER_CURRENCY;

    @Value("${binance.seller.percentage.limit.loss}")
    private BigDecimal MAX_PERCENTAGE_LOSS;

    @Value("${binance.seller.percentage.limit.gain}")
    private BigDecimal MAX_PERCENTAGE_GAIN;

    @Value("${binance.seller.percentage.limit.hold}")
    private BigDecimal HOLD_PERCENTAGE;

    @Value("${binance.seller.percentage.limit.loss.related.gain}")
    private BigDecimal MAX_PERCENTAGE_LOSS_RELATED_GAIN;

    @Value("${binance.simulated.active}")
    private boolean SIMULATED_ACTIVE;

    private final TraderUtil traderUtil;

    private final SellDefaultService sellDefaultService;

    private final SellOneCurrencyService sellOneCurrencyService;

    @Override
    public void initSellProcess() throws InterruptedException {
        while(true) {
            log.info("Iniciando o processo de venda. Aguarde...");
            sellProcess();
            log.info("Reiniciando o processo de venda. Aguarde...");
            Thread.sleep(30000);
        }
    }

    @Override
    public void sellProcess() throws InterruptedException {
        List<TradeOrder> orders = getOrders();

        while (orders.size() > 0) {
            for (TradeOrder order : orders) {
                TickerStatistics marketTickerResponse = null;
                try{
                    marketTickerResponse = traderUtil.getInstance().get24HrPriceStatistics(order.getPair());
                }catch (BinanceApiException e) {
                    log.error("Erro ao buscar informacoes na API: {}", e.getMessage());
                    continue;
                }

                boolean keepInOperation = isKeepInOperation(order, marketTickerResponse);

                if (!keepInOperation) {
                    createSELLOrder(marketTickerResponse, order);
                }
                Thread.sleep(1000);
            }
            orders = getOrders();
        }
    }

    private boolean isKeepInOperation(TradeOrder order, TickerStatistics marketTickerResponse) {
        if (order.getStrategy() != 6) {
            return sellDefaultService.keepInOperation(marketTickerResponse, order);
        } else {
            return sellOneCurrencyService.keepInOperation(marketTickerResponse, order);
        }
    }

    @Override
    public Boolean getSimulated() {
        return SIMULATED_ACTIVE;
    }

    private List<TradeOrder> getOrders() {
        return listOrders();
    }

    private List<TradeOrder> listOrders() {
        List<TradeOrder> tradeOrders = tradeOrderRepository.findAllByFinishedIsFalseAndSimulatedIsTrue();
        int ordersSize = tradeOrders.size();
        log.info("VENDA - ORDENS SIMULADAS ABERTAS: {}", ordersSize);

        return tradeOrders;
    }

    private void createSELLOrder(TickerStatistics marketTickerResponse, TradeOrder order) {
        SymbolInfo symbolInfo = traderUtil.getPairDetails(order.getPair());
        final NewOrderResponse sellOrder = orderSELLRequest(order, symbolInfo);
        if (sellOrder != null) {
            Toolkit.getDefaultToolkit().beep();
            persistSellOrder(Double.valueOf(marketTickerResponse.getLastPrice()), order, sellOrder, symbolInfo);
        }else {
            log.error("Problema ao criar a ordem de venda.");
        }
    }

    private NewOrderResponse orderSELLRequest(TradeOrder order, SymbolInfo symbolInfo) {
        log.info("SELLING PAIR = {} FROM ORDER = {}", symbolInfo.getSymbol(), order.getOperatorBuyId());

        NewOrderResponse sellOrder = new NewOrderResponse();

        if (symbolInfo != null) {
            String symbol = symbolInfo.getSymbol();
            Integer precision = symbolInfo.getBaseAssetPrecision();

            boolean lotSizeTry = true;
            int maxTry = precision+1;
            String amount = String.valueOf(order.getBuyCurrencyAmount().setScale(precision, RoundingMode.UNNECESSARY));

            BigDecimal balance = traderUtil.getBalance(symbol.replace(BUYER_CURRENCY, ""),
                    order.getUpdatedPrice() == null ? order.getInitialPrice() : order.getUpdatedPrice());

            if (balance.compareTo(order.getBuyCurrencyAmount()) < 0) {
                BigDecimal percentual = traderUtil.calculatePercentage(balance.doubleValue(),
                        order.getBuyCurrencyAmount().doubleValue(), true);

                if (percentual.compareTo(BigDecimal.ZERO) == 0) {
                    order.setBuyCurrencyAmount(balance);
                    amount = String.valueOf(balance);
                }
            }

            while (lotSizeTry && maxTry > 0) {

                try {
                    traderUtil.getInstance().newOrderTest(new NewOrder(
                            symbol,
                            OrderSide.SELL,
                            OrderType.MARKET,
                            null,
                            amount));
                    sellOrder.setOrderId(new Random().nextLong());
                    sellOrder.setStatus(OrderStatus.FILLED);
                    sellOrder.setOrigQty(amount);
                    sellOrder.setExecutedQty(amount);
                    sellOrder.setType(OrderType.MARKET);
                    Double cummulativeQuoty = Double.valueOf(amount) * order.getUpdatedPrice().doubleValue();
                    sellOrder.setCummulativeQuoteQty(String.valueOf(BigDecimal.valueOf(cummulativeQuoty).setScale(precision, RoundingMode.DOWN)));
                    log.info("DADOS DA VENDA: {}", traderUtil.getGson().toJson(sellOrder));
                    try {
                        Thread.sleep(500);
                    }catch (Exception e) {}
                } catch (BinanceApiException e) {
                    if (balance.compareTo(BigDecimal.ZERO) == 0) {
                        lotSizeTry = false;
                        continue;
                    }
                    if (e.getMessage().toUpperCase().contains("LOT_SIZE")) {
                        precision--;
                        maxTry--;
                        amount = String.valueOf(order.getBuyCurrencyAmount().setScale(precision, RoundingMode.DOWN));
                        continue;
                    }
                    if (e.getMessage().toUpperCase().contains("MIN_NOTIONAL")) {
                        log.warn("QUANTIDADE INVALIDA PARA VENDA DE [{}], DIMINUINDO O PERCENTUAL", traderUtil.getSymbolFormatted(symbol, BUYER_CURRENCY));
                        maxTry--;
                        amount = String.valueOf(BigDecimal.valueOf(Double.valueOf(amount) * 0.08).setScale(precision, RoundingMode.DOWN));
                        continue;
                    }

                    if (e.getMessage().toUpperCase().contains("INSUFFICIENT BALANCE")) {
                        lotSizeTry = false;
                        continue;
                    }

                    log.error("NAO FOI POSSIVEL REALIZAR A VENDA DE {}. MOTIVO: {}", symbol, e.getMessage());

                }
                lotSizeTry = false;
            }

        }
        return sellOrder;
    }

    private void persistSellOrder(Double lastPrice, TradeOrder tradeOrder,
                                  NewOrderResponse newOrderResponse, SymbolInfo symbolInfo) {

        log.info("SAVING PAIR = {} FROM ORDER = {} IN DATABASE",
                traderUtil.getSymbolFormatted(symbolInfo.getSymbol(), BUYER_CURRENCY), tradeOrder.getOperatorBuyId());

        tradeOrder.setSellCurrencyAmount(new BigDecimal(newOrderResponse.getExecutedQty()));

        Date sellDate = new Date();

        tradeOrder.setSellDate(sellDate);
        tradeOrder.setOperatorSellId(String.valueOf(newOrderResponse.getOrderId()));

        tradeOrder.setSellValue(new BigDecimal(newOrderResponse.getCummulativeQuoteQty()));

        BigDecimal tax = BigDecimal.valueOf(Double.valueOf(newOrderResponse
                .getCummulativeQuoteQty()) - (lastPrice * Double.valueOf(newOrderResponse.getExecutedQty())))
                .setScale(symbolInfo.getBaseAssetPrecision(), RoundingMode.DOWN);

        tradeOrder.setSellTax(tax);
        tradeOrder.setSellStatus(newOrderResponse.getStatus().name());
        tradeOrder.setFinished(true);
        tradeOrder.setLastUpdate(new Date());
        tradeOrderRepository.save(tradeOrder);
        log.info("SAVED SELL ORDER = {}", tradeOrder);
    }
}
