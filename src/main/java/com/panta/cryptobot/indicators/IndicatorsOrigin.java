package com.panta.cryptobot.indicators;

import com.panta.cryptobot.dependency.domain.market.Candlestick;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class IndicatorsOrigin {

    public List<Double> mma(List<Candlestick> dados, int periodos) {

        List<Double> result = new ArrayList<>();
        List<Double> pFech = dados.stream()
                .map(candlestick -> Double.valueOf(candlestick.getClose()))
                .collect(Collectors.toList());
        int numDados = dados.size();
        for (int i = periodos - 1; i < numDados; i++) {
            double soma = 0.0D;
            for (int j = i - periodos + 1; j < i + 1; j++)
                soma += ((Double)pFech.get(j)).doubleValue();
            soma /= periodos;
            result.add(soma);
        }
        return result;
    }

    public List<Double> mme(List<Candlestick> dados, int periodos) {
        List<Double> result = new ArrayList<>();
        List<Double> pFech = dados.stream()
                .map(candlestick -> Double.valueOf(candlestick.getClose()))
                .collect(Collectors.toList());
        int numDados = dados.size();
        double k = 2.0D / (periodos + 1);
        double mm = 0.0D;
        int i;
        for (i = 0; i < periodos; i++)
            mm += ((Double)pFech.get(i)).doubleValue();
        mm /= periodos;

        for (i = periodos; i < numDados; i++) {
            mm = k * ((Double)pFech.get(i)).doubleValue() + (1.0D - k) * mm;
            result.add(mm);
        }
        return result;
    }
}
