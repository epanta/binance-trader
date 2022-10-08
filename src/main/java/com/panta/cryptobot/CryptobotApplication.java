package com.panta.cryptobot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
@ConfigurationPropertiesScan
@Slf4j
public class CryptobotApplication {
    public static void main(final String[] args) {
        ApplicationContext ctx = SpringApplication.run(CryptobotApplication.class, args);
       // Thread buySubscriber = new Thread(ctx.getBean(EventBuySubscriber.class));
       // buySubscriber.start();

        //Thread sellSubscriber = new Thread(ctx.getBean(EventSellSubscriber.class));
        //sellSubscriber.start();
    }
}
