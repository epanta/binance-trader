package com.panta.cryptobot.indicators;

import com.panta.cryptobot.dependency.domain.market.Candlestick;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

@Component
@Slf4j
@RequiredArgsConstructor
public class SimpleMovingAverage {

    public double getSMA(List<Candlestick> candlesticks, int period) {
        Double result = 0.0;
        for (Candlestick candlestick : candlesticks) {
            result += Double.valueOf(candlestick.getClose());
        }
        return result/period;
    }

    public double[] calculateMaValues(List<Candlestick> candlesticks, double n){

        double[] results = new double[candlesticks.size()];

        calculateMasHelper(candlesticks, n, candlesticks.size()-1, results);
        return results;
    }

    public double calculateMasHelper(List<Candlestick> candlesticks, double n, int i, double[] results){

        if(i == 0){
            results[0] = Double.parseDouble(candlesticks.stream().findFirst().get().getClose());
            return results[0];
        }else {
            double close = Double.parseDouble(candlesticks.get(i).getClose());
            double ema =  close + calculateMasHelper(candlesticks, n, i-1, results) ;
            results[i] = ema;
            return ema;
        }
    }

}
