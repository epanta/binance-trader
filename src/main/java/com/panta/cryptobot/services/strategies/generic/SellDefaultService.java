package com.panta.cryptobot.services.strategies.generic;

import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import com.panta.cryptobot.entities.TradeOrder;

public interface SellDefaultService {

    boolean keepInOperation(TickerStatistics marketTickerResponse, TradeOrder order);
}