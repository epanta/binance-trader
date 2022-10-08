package com.panta.cryptobot.services.impl;

import com.panta.cryptobot.dependency.BinanceApiClientFactory;
import com.panta.cryptobot.dependency.BinanceApiRestClient;
import com.panta.cryptobot.services.BinanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BinanceServiceImpl implements BinanceService {

    @Value("${binance.apikey}")
    private String ACCESS_KEY;

    @Value("${binance.secret}")
    private String SECRET_KEY;

    private BinanceApiRestClient binanceApiRestClient;

    @Override
    public BinanceApiRestClient getInstance() {
        if (binanceApiRestClient == null) {
            BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(ACCESS_KEY, SECRET_KEY);
            binanceApiRestClient = factory.newRestClient();
        }
        return binanceApiRestClient;
    }
}
