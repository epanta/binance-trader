package com.panta.cryptobot.indicators;

import com.panta.cryptobot.dependency.domain.market.Candlestick;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/*
 * fonte: https://nullbeans.com/how-to-calculate-the-relative-strength-index-rsi/
 * https://stackoverflow.com/questions/72330608/how-to-recreate-the-exact-rsi-calculation-from-tradingview-in-java
 */

@Component
@Slf4j
@RequiredArgsConstructor
public class RSI {

    public static double calcSmmaUp(List<Candlestick> candlesticks, double n, int i, double avgUt1){

        if(avgUt1==0){
            double sumUpChanges = 0;

            for(int j = 0; j < n; j++){
                double change = candlesticks.get(i - j).getCloseFormatted() - candlesticks.get(i - j).getOpenFormatted();

                if(change > 0){
                    sumUpChanges+= change;
                }
            }
            return sumUpChanges / n;
        }else {
            double change = candlesticks.get(i).getCloseFormatted() - candlesticks.get(i).getOpenFormatted();
            if(change < 0){
                change = 0;
            }
            return ((avgUt1 * (n-1)) + change) / n ;
        }

    }

    public static double calcSmmaDown(List<Candlestick> candlesticks, double n, int i, double avgDt1){
        if(avgDt1==0){
            double sumDownChanges = 0;

            for(int j = 0; j < n; j++){
                double change = candlesticks.get(i - j).getCloseFormatted() - candlesticks.get(i - j).getOpenFormatted();

                if(change < 0){
                    sumDownChanges-= change;
                }
            }
            return sumDownChanges / n;
        }else {
            double change = candlesticks.get(i).getCloseFormatted() - candlesticks.get(i).getOpenFormatted();
            if(change > 0){
                change = 0;
            }
            return ((avgDt1 * (n-1)) - change) / n ;
        }

    }

    public static double[] calculateRSIValues(List<Candlestick> candlesticks, int n){

        double[] results = new double[candlesticks.size()];

        double ut1 = 0;
        double dt1 = 0;
        for(int i = 0; i < candlesticks.size(); i++){
            if(i<(n)){
                continue;
            }

            ut1 = calcSmmaUp(candlesticks, n, i, ut1);
            dt1 = calcSmmaDown(candlesticks, n, i, dt1);

            results[i] = 100.0 - 100.0 / (1.0 +
                    calculateRS(ut1,
                            dt1));

        }

        return results;
    }

    private static double calculateRS(double ut1, double dt1) {
        return ut1/dt1;
    }
}
