package com.panta.cryptobot.services.strategies.onecurrency;

import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.entities.TradeOrder;

public interface SellOneCurrencyService {

    boolean keepInOperation(TickerStatistics marketTickerResponse, TradeOrder order);
}
