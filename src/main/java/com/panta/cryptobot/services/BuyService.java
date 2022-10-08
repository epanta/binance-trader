package com.panta.cryptobot.services;

public interface BuyService {

    void initSellProcess() throws InterruptedException;
    void buyProcess() throws InterruptedException;
}
