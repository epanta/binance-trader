package com.panta.cryptobot.indicators;

import com.panta.cryptobot.dependency.domain.market.Candlestick;
import com.panta.cryptobot.dependency.domain.market.TickerStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class MA {

    public List<Double> getCalc(List<Candlestick> candlesticks, int period) {

        int idx = 0;

        List<Double> values = new ArrayList<>();

        List<Candlestick> tempCandles = new ArrayList<>();
        tempCandles.addAll(candlesticks);

        List<Candlestick> candles = new ArrayList<>();
        candles.addAll(candlesticks.stream()
                .sorted(Comparator.comparing(Candlestick::getCloseTime)
                        .reversed()).collect(Collectors.toList()));

        for (int i = 0; i<10; i++) {
            candles = tempCandles.stream()
                    .limit(period).collect(Collectors.toList());

            values.add(getSMA(candles));

            tempCandles.remove(idx);
            idx++;
        }
        return values;
    }

    public double getSMA(List<Candlestick> candlesticks) {
        Double result = 0.0;
        for (Candlestick candlestick : candlesticks) {
            result += Double.valueOf(candlestick.getClose());
        }
        return result/candlesticks.size();
    }
}
