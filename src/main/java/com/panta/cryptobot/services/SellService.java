package com.panta.cryptobot.services;

public interface SellService {

    void initSellProcess() throws InterruptedException;
    void sellProcess() throws InterruptedException;
}
