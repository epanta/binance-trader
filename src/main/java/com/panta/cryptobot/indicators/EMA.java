package com.panta.cryptobot.indicators;

import com.panta.cryptobot.dependency.domain.market.Candlestick;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class EMA {

    public double[] calculateEmaValues(List<Candlestick> candlesticks, double n){

        double[] results = new double[candlesticks.size()];

        calculateEmasHelper(candlesticks, n, candlesticks.size()-1, results);
        return results;
    }

    public double calculateEmasHelper(List<Candlestick> candlesticks, double n, int i, double[] results){

        if(i == 0){
            results[0] = Double.parseDouble(candlesticks.stream().findFirst().get().getClose());
            return results[0];
        }else {
            double close = Double.parseDouble(candlesticks.get(i).getClose());
            double factor = ( 2.0 / (n +1) );
            double ema =  close * factor + (1 - factor) * calculateEmasHelper(candlesticks, n, i-1, results) ;
            results[i] = ema;
            return ema;
        }
   }
}
