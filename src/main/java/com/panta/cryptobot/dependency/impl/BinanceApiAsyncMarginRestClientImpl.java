package com.panta.cryptobot.dependency.impl;

import com.panta.cryptobot.dependency.BinanceApiAsyncMarginRestClient;
import com.panta.cryptobot.dependency.BinanceApiCallback;
import com.panta.cryptobot.dependency.constant.BinanceApiConstants;
import com.panta.cryptobot.dependency.domain.TransferType;
import com.panta.cryptobot.dependency.domain.account.*;
import com.panta.cryptobot.dependency.domain.account.request.CancelOrderRequest;
import com.panta.cryptobot.dependency.domain.account.request.CancelOrderResponse;
import com.panta.cryptobot.dependency.domain.account.request.OrderRequest;
import com.panta.cryptobot.dependency.domain.account.request.OrderStatusRequest;
import com.panta.cryptobot.dependency.domain.event.ListenKey;

import java.util.List;

import static com.panta.cryptobot.dependency.impl.BinanceApiServiceGenerator.createService;

/**
 * Implementation of Binance's Margin REST API using Retrofit with asynchronous/non-blocking method calls.
 */
public class BinanceApiAsyncMarginRestClientImpl implements com.panta.cryptobot.dependency.BinanceApiAsyncMarginRestClient {

    private final BinanceApiService binanceApiService;

    public BinanceApiAsyncMarginRestClientImpl(String apiKey, String secret) {
        binanceApiService = createService(BinanceApiService.class, apiKey, secret);
    }

    // Margin Account endpoints

    @Override
    public void getAccount(Long recvWindow, Long timestamp, com.panta.cryptobot.dependency.BinanceApiCallback<MarginAccount> callback) {
        binanceApiService.getMarginAccount(recvWindow, timestamp).enqueue(new BinanceApiCallbackAdapter<>(callback));
    }

    @Override
    public void getAccount(com.panta.cryptobot.dependency.BinanceApiCallback<MarginAccount> callback) {
        long timestamp = System.currentTimeMillis();
        binanceApiService.getMarginAccount(BinanceApiConstants.DEFAULT_MARGIN_RECEIVING_WINDOW, timestamp).enqueue(new BinanceApiCallbackAdapter<>(callback));
    }

    @Override
    public void getOpenOrders(OrderRequest orderRequest, com.panta.cryptobot.dependency.BinanceApiCallback<List<Order>> callback) {
        binanceApiService.getOpenMarginOrders(orderRequest.getSymbol(), orderRequest.getRecvWindow(),
                orderRequest.getTimestamp()).enqueue(new BinanceApiCallbackAdapter<>(callback));
    }

    @Override
    public void newOrder(MarginNewOrder order, com.panta.cryptobot.dependency.BinanceApiCallback<MarginNewOrderResponse> callback) {
        binanceApiService.newMarginOrder(order.getSymbol(), order.getSide(), order.getType(), order.getTimeInForce(),
                order.getQuantity(), order.getPrice(), order.getNewClientOrderId(), order.getStopPrice(), order.getIcebergQty(),
                order.getNewOrderRespType(), order.getSideEffectType(), order.getRecvWindow(), order.getTimestamp()).enqueue(new BinanceApiCallbackAdapter<>(callback));
    }

    @Override
    public void cancelOrder(CancelOrderRequest cancelOrderRequest, com.panta.cryptobot.dependency.BinanceApiCallback<CancelOrderResponse> callback) {
        binanceApiService.cancelMarginOrder(cancelOrderRequest.getSymbol(),
                cancelOrderRequest.getOrderId(), cancelOrderRequest.getOrigClientOrderId(), cancelOrderRequest.getNewClientOrderId(),
                cancelOrderRequest.getRecvWindow(), cancelOrderRequest.getTimestamp()).enqueue(new BinanceApiCallbackAdapter<>(callback));
    }

    @Override
    public void getOrderStatus(OrderStatusRequest orderStatusRequest, com.panta.cryptobot.dependency.BinanceApiCallback<Order> callback) {
        binanceApiService.getMarginOrderStatus(orderStatusRequest.getSymbol(),
                orderStatusRequest.getOrderId(), orderStatusRequest.getOrigClientOrderId(),
                orderStatusRequest.getRecvWindow(), orderStatusRequest.getTimestamp()).enqueue(new BinanceApiCallbackAdapter<>(callback));
    }

    @Override
    public void getMyTrades(String symbol, com.panta.cryptobot.dependency.BinanceApiCallback<List<Trade>> callback) {
        binanceApiService.getMyTrades(symbol, null, null, BinanceApiConstants.DEFAULT_RECEIVING_WINDOW, System.currentTimeMillis()).enqueue(new BinanceApiCallbackAdapter<>(callback));
    }

    // user stream endpoints

    @Override
    public void startUserDataStream(com.panta.cryptobot.dependency.BinanceApiCallback<ListenKey> callback) {
        binanceApiService.startMarginUserDataStream().enqueue(new BinanceApiCallbackAdapter<>(callback));
    }

    @Override
    public void keepAliveUserDataStream(String listenKey, com.panta.cryptobot.dependency.BinanceApiCallback<Void> callback) {
        binanceApiService.keepAliveMarginUserDataStream(listenKey).enqueue(new BinanceApiCallbackAdapter<>(callback));
    }

    @Override
    public void transfer(String asset, String amount, TransferType type, com.panta.cryptobot.dependency.BinanceApiCallback<MarginTransaction> callback) {
        long timestamp = System.currentTimeMillis();
        binanceApiService.transfer(asset, amount, type.getValue(), BinanceApiConstants.DEFAULT_MARGIN_RECEIVING_WINDOW, timestamp).enqueue(new BinanceApiCallbackAdapter<>(callback));
    }

    @Override
    public void borrow(String asset, String amount, com.panta.cryptobot.dependency.BinanceApiCallback<MarginTransaction> callback) {
        long timestamp = System.currentTimeMillis();
        binanceApiService.borrow(asset, amount, BinanceApiConstants.DEFAULT_MARGIN_RECEIVING_WINDOW, timestamp).enqueue(new BinanceApiCallbackAdapter<>(callback));
    }

    @Override
    public void repay(String asset, String amount, com.panta.cryptobot.dependency.BinanceApiCallback<MarginTransaction> callback) {
        long timestamp = System.currentTimeMillis();
        binanceApiService.repay(asset, amount, BinanceApiConstants.DEFAULT_MARGIN_RECEIVING_WINDOW, timestamp).enqueue(new BinanceApiCallbackAdapter<>(callback));
    }
}
