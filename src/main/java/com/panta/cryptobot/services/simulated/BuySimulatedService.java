package com.panta.cryptobot.services.simulated;

public interface BuySimulatedService {

    void initSellProcess() throws InterruptedException;
    void buyProcess() throws InterruptedException;
    Boolean getSimulated();
}
