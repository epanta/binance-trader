package com.panta.cryptobot.indicators;

import com.panta.cryptobot.dependency.domain.market.Candlestick;
import org.jfree.data.time.Day;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class Indicators {

    public TimeSeries mma(List<Candlestick> dados, int periodos, String legenda) {
        TimeSeries serie = new TimeSeries(legenda, Day.class);
        Date tempDate = new Date();

        List<Date> datas = dados.stream()
                .map(candlestick -> {
                    tempDate.setTime(candlestick.getCloseTime());
                    return tempDate;
                })
                .collect(Collectors.toList());

        List<Double> pFech = dados.stream()
                .map(candlestick -> Double.valueOf(candlestick.getClose()))
                .collect(Collectors.toList());
        int numDados = dados.size();
        for (int i = periodos - 1; i < numDados; i++) {
            double soma = 0.0D;
            for (int j = i - periodos + 1; j < i + 1; j++)
                soma += ((Double)pFech.get(j)).doubleValue();
            soma /= periodos;
            serie.addOrUpdate((RegularTimePeriod) new Day(datas.get(i)), soma);
        }
        return serie;
    }

    public TimeSeries mma(TimeSeries serieIn, int periodos, String legenda) {
        TimeSeries serieOut = new TimeSeries(legenda, Day.class);
        ArrayList<RegularTimePeriod> datas = new ArrayList<RegularTimePeriod>();
        ArrayList<Double> valor = new ArrayList<Double>();
        int numDados = serieIn.getItemCount();
        int i;
        for (i = 0; i < numDados; i++) {
            datas.add(serieIn.getTimePeriod(i));
            valor.add(Double.valueOf(serieIn.getValue(i).doubleValue()));
        }
        for (i = periodos - 1; i < numDados; i++) {
            double soma = 0.0D;
            for (int j = i - periodos + 1; j < i + 1; j++)
                soma += ((Double)valor.get(j)).doubleValue();
            soma /= periodos;
            serieOut.addOrUpdate(datas.get(i), soma);
        }
        return serieOut;
    }

    public TimeSeries mme(List<Candlestick> dados, int periodos, String legenda) {
        TimeSeries serie = new TimeSeries(legenda, Day.class);

        Date tempDate = new Date();

        List<Date> datas = dados.stream()
                .map(candlestick -> {
                     tempDate.setTime(candlestick.getCloseTime());
                     return tempDate;
                })
                .collect(Collectors.toList());

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
        serie.addOrUpdate((RegularTimePeriod)new Day(datas.get(periodos - 1)), mm);
        for (i = periodos; i < numDados; i++) {
            mm = k * ((Double)pFech.get(i)).doubleValue() + (1.0D - k) * mm;
            serie.addOrUpdate((RegularTimePeriod)new Day(datas.get(i)), mm);
        }
        return serie;
    }

    public TimeSeries mme(TimeSeries serieIn, int periodos, String legenda) {
        TimeSeries serieOut = new TimeSeries(legenda, Day.class);
        ArrayList<RegularTimePeriod> datas = new ArrayList<RegularTimePeriod>();
        ArrayList<Double> valor = new ArrayList<Double>();
        int numDados = serieIn.getItemCount();
        for (int i = 0; i < numDados; i++) {
            datas.add(serieIn.getTimePeriod(i));
            valor.add(Double.valueOf(serieIn.getValue(i).doubleValue()));
        }
        double k = 2.0D / (periodos + 1);
        double mm = 0.0D;
        int j;
        for (j = 0; j < periodos; j++)
            mm += ((Double)valor.get(j)).doubleValue();
        mm /= periodos;
        serieOut.addOrUpdate(datas.get(periodos - 1), mm);
        for (j = periodos; j < numDados; j++) {
            mm = k * ((Double)valor.get(j)).doubleValue() + (1.0D - k) * mm;
            serieOut.addOrUpdate(datas.get(j), mm);
        }
        return serieOut;
    }
}
