package com.panta.cryptobot.events;

import com.panta.cryptobot.services.BuyService;
import com.panta.cryptobot.services.simulated.BuySimulatedService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventBuySubscriber implements DisposableBean, Runnable {

    private Thread thread;
    private final BuyService buyService;
    private final BuySimulatedService buySimulatedService;

    EventBuySubscriber(BuyService buyService, BuySimulatedService buySimulatedService){
        this.buyService = buyService;
        this.buySimulatedService = buySimulatedService;
        this.thread = new Thread(this);
        this.thread.start();
    }

    @SneakyThrows
    @Override
    public void run(){
        while(true) {
            //log.info("Iniciando o processo de compra. Aguarde...");
            if (buySimulatedService.getSimulated()) {
                buySimulatedService.buyProcess();
            } else {
                buyService.buyProcess();
            }
            //log.info("Reiniciando o processo de compra. Aguarde...");
            Thread.sleep(4000);
        }
    }

    @Override
    public void destroy() throws Exception {

    }
}
