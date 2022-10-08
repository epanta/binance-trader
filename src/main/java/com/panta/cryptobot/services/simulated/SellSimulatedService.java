package com.panta.cryptobot.services.simulated;

public interface SellSimulatedService {

    void initSellProcess() throws InterruptedException;
    void sellProcess() throws InterruptedException;
    Boolean getSimulated();
}
