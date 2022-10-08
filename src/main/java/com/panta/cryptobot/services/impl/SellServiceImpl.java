package com.panta.cryptobot.services.impl;

import com.panta.cryptobot.dependency.domain.OrderSide;
import com.panta.cryptobot.dependency.domain.OrderStatus;
import com.panta.cryptobot.dependency.domain.OrderType;
import com.panta.cryptobot.dependency.domain.account.NewOrder;
import com.panta.cryptobot.dependency.domain.account.NewOrderResponse;
import com.panta.cryptobot.dependency.domain.account.Trade;
import com.panta.cryptobot.dependency.domain.general.SymbolInfo;
import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.dependency.exception.BinanceApiException;
import com.panta.cryptobot.entities.TradeOrder;
import com.panta.cryptobot.repositories.TradeOrderRepository;
import com.panta.cryptobot.services.SellService;
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
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellServiceImpl implements SellService {

    private final TradeOrderRepository tradeOrderRepository;

    @Value("${binance.buyer.order.currency}")
    private String BUYER_CURRENCY;

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
            log.info("");
            orders = getOrders();
        }
    }

    private boolean isKeepInOperation(TradeOrder order, TickerStatistics marketTickerResponse) {
        if (order.getStrategy() == 6) {
            return sellOneCurrencyService.keepInOperation(marketTickerResponse, order);
        }
        return sellDefaultService.keepInOperation(marketTickerResponse, order);
    }

    private List<TradeOrder> getOrders() {
        return listOrders();
    }

    private List<TradeOrder> listOrders() {
        List<TradeOrder> tradeOrders = tradeOrderRepository.findAllByFinishedIsFalseAndSimulatedIsFalse();
        if (!tradeOrders.isEmpty()){
            tradeOrders = tradeOrders.stream().filter(order -> {
                BigDecimal price = order.getUpdatedPrice() == null ? order.getInitialPrice(): order.getUpdatedPrice();
                BigDecimal balance = traderUtil.getBalance(order.getPair().replace(BUYER_CURRENCY, ""), price);
                if ( balance.compareTo(BigDecimal.ZERO) == 0 ||
                        BigDecimal.valueOf(balance.doubleValue() * price.doubleValue()).setScale(8, RoundingMode.DOWN).compareTo(BigDecimal.ONE) < 0) {
                    SymbolInfo symbolInfo = traderUtil.getPairDetails(order.getPair());
                    final NewOrderResponse sellOrder = setOrderAlreadySelled(order, symbolInfo);
                    if (sellOrder != null) {
                        Toolkit.getDefaultToolkit().beep();
                        persistSellOrder(Double.valueOf(sellOrder.getPrice()), order, sellOrder, symbolInfo);
                    }
                    return false;
                }
                return true;
            }).collect(Collectors.toList());
        }
        //int ordersSize = tradeOrders.size();
        //log.info("VENDA - ORDENS ABERTAS: {}", ordersSize);

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

        NewOrderResponse sellOrder = null;

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
                    sellOrder = traderUtil.getInstance().newOrder(new NewOrder(
                            symbol,
                            OrderSide.SELL,
                            OrderType.MARKET,
                            null,
                            amount));

                    log.info("DADOS DA VENDA: {}", traderUtil.getGson().toJson(sellOrder));
                    try {
                        Thread.sleep(500);
                    }catch (Exception e) {}
                } catch (BinanceApiException e) {
                    if (balance.compareTo(BigDecimal.ZERO) == 0) {
                        sellOrder = setOrderAlreadySelled(order, symbolInfo);
                        if (sellOrder != null){
                            lotSizeTry = false;
                            continue;
                        }
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
                        sellOrder = setOrderAlreadySelled(order, symbolInfo);
                        if (sellOrder != null){
                            lotSizeTry = false;
                            continue;
                        }
                    }

                    log.error("NAO FOI POSSIVEL REALIZAR A VENDA DE {}. MOTIVO: {}", symbol, e.getMessage());

                }
                lotSizeTry = false;
            }

        }
        return sellOrder;
    }

    private NewOrderResponse setOrderAlreadySelled(TradeOrder order, SymbolInfo symbolInfo) {
        List<Trade> trades = null;
        NewOrderResponse sellOrder = null;

        try {
            trades = traderUtil.getInstance().getMyTrades(symbolInfo.getSymbol());
            if (trades != null && !trades.isEmpty()) {
                Optional<Trade> initialTradeOptional = trades.stream()
                        .filter(trade -> trade.getOrderId().equalsIgnoreCase(order.getOperatorBuyId()))
                        .findFirst();
                if (initialTradeOptional.isPresent()) {
                    Trade initialTrade = initialTradeOptional.get();

                    trades = trades.stream()
                            .filter(trade -> trade.isBuyer() == false)
                            .filter(trade -> trade.getTime() > initialTrade.getTime()).collect(Collectors.toList());

                    Trade finalTrade = null;

                    if (trades.size() == 0) {
                        return sellOrder;
                    }

                    sellOrder = new NewOrderResponse();

                    if (trades.size() == 1) {
                        finalTrade = trades.stream().findFirst().get();
                        sellOrder.setOrderId(Long.valueOf(finalTrade.getOrderId()));
                        sellOrder.setExecutedQty(finalTrade.getQty());
                        sellOrder.setOrigQty(finalTrade.getQty());
                        sellOrder.setClientOrderId(finalTrade.getOrderId());
                        sellOrder.setCummulativeQuoteQty(finalTrade.getQuoteQty());
                        sellOrder.setPrice(finalTrade.getPrice());
                    }

                    if (trades.size() > 1) {
                        Map<String, Double> groupingByQty = trades.stream()
                                .collect(Collectors.groupingBy(Trade::getOrderId, Collectors.summingDouble(Trade::getQtyFormatted))); //arredondar casas decimais

                        Optional<String> optionalOrderId = groupingByQty.entrySet()
                                .stream()
                                .filter(entry -> verifyBalance(BigDecimal.valueOf(entry.getValue())
                                        .setScale(symbolInfo.getBaseAssetPrecision(), RoundingMode.DOWN), order.getBuyCurrencyAmount())).findFirst().map(Map.Entry::getKey);

                        if (optionalOrderId.isPresent()) {
                            String orderId = optionalOrderId.get();
                            String executedQty = String.valueOf(BigDecimal.valueOf(trades.stream()
                                    .collect(Collectors.summarizingDouble(Trade::getQtyFormatted)).getSum()).setScale(symbolInfo.getBaseAssetPrecision(), RoundingMode.DOWN));
                            String cummulativeQuoteQty = String.valueOf(BigDecimal.valueOf(trades.stream()
                                    .collect(Collectors.summarizingDouble(Trade::getQuoteQtyFormatted)).getSum()).setScale(symbolInfo.getQuotePrecision(), RoundingMode.DOWN));

                            String avaragePrice = String.valueOf(BigDecimal.valueOf(trades.stream()
                                    .collect(Collectors.averagingDouble(Trade::getPriceFormatted))).setScale(symbolInfo.getQuotePrecision(), RoundingMode.DOWN));

                            sellOrder.setOrderId(Long.valueOf(orderId));
                            sellOrder.setExecutedQty(executedQty);
                            sellOrder.setOrigQty(executedQty);
                            sellOrder.setClientOrderId(orderId);
                            sellOrder.setCummulativeQuoteQty(cummulativeQuoteQty);
                            sellOrder.setPrice(avaragePrice);
                        } else {
                            return null;
                        }
                    }
                    sellOrder.setStatus(OrderStatus.FILLED);
                    log.warn("SALDO INSUFICIENTE PARA VENDA DE [{}], A VENDA JA FOI REALIZADA, O BD SERA ATUALIZADO!",
                            traderUtil.getSymbolFormatted(symbolInfo.getSymbol(), BUYER_CURRENCY));
                }
            }
        }catch (Exception e) {
            log.warn("Erro em setOrderAlreadySelled: {}", e.getMessage());
        }
        return sellOrder;
    }

    private boolean verifyBalance(BigDecimal balance, BigDecimal buyCurrencyAmount) {

        if (balance.compareTo(buyCurrencyAmount) == 0) {
            return true;
        }
        if (balance.compareTo(buyCurrencyAmount) != 0) {
            BigDecimal percentual = traderUtil.calculatePercentage(balance.doubleValue(),
                    buyCurrencyAmount.doubleValue(), true);

            if (percentual.compareTo(BigDecimal.ZERO) == 0) {
                return true;
            }
        }
        return false;
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
