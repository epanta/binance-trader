package com.panta.cryptobot.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Data
@ConstructorBinding
@ConfigurationProperties(prefix = "binance")
public class BinanceProperties {

    private String apikey;

    private String secret;

}
