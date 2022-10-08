package com.panta.cryptobot.events;

import com.panta.cryptobot.services.SellService;
import com.panta.cryptobot.services.simulated.SellSimulatedService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventSellSubscriber implements DisposableBean, Runnable {

    private Thread thread;
    private final SellSimulatedService sellSimulatedService;
    private final SellService sellService;

    EventSellSubscriber(SellService sellService, SellSimulatedService sellSimulatedService){
        this.sellService = sellService;
        this.sellSimulatedService = sellSimulatedService;
        this.thread = new Thread(this);
        this.thread.start();
    }

    @SneakyThrows
    @Override
    public void run(){
        while(true) {
            log.info("Iniciando o processo de venda. Aguarde...");
            if (sellSimulatedService.getSimulated()) {
                sellSimulatedService.sellProcess();
            } else {
                sellService.sellProcess();
            }
            log.info("Reiniciando o processo de venda. Aguarde...");
            Thread.sleep(30000);
        }
    }

    @Override
    public void destroy() throws Exception {

    }
}
