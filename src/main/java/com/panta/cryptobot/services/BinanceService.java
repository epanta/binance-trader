package com.panta.cryptobot.services;

import com.panta.cryptobot.dependency.BinanceApiRestClient;

public interface BinanceService {
    BinanceApiRestClient getInstance();
}
