package com.panta.cryptobot.indicators;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class LeastSquaresGradient {

    public List<BigDecimal> xarray = new ArrayList<BigDecimal>();
    public List<BigDecimal> yarray = new ArrayList<BigDecimal>();

    public void teste() {

        xarray.add(new BigDecimal(-9));
        yarray.add(new BigDecimal(13.7));

        System.out.println(leastSquaresGradient()); // 0

        xarray.add(new BigDecimal(-10));
        yarray.add(new BigDecimal(14.52));
        System.out.println(leastSquaresGradient()); // -0.8200000001

        xarray.add(new BigDecimal(-9));
        yarray.add(new BigDecimal(14.41));
        System.out.println(leastSquaresGradient()); // -0.4650000000

        xarray.add(new BigDecimal(-4));
        yarray.add(new BigDecimal(13.67));
        System.out.println(leastSquaresGradient()); // -0.1122727273

        xarray.add(new BigDecimal(-5));
        yarray.add(new BigDecimal(14.29));
        System.out.println(leastSquaresGradient()); // -0.0669178083



    }

    public BigDecimal leastSquaresGradient() {

        // Variáveis utilizadas para guardar as variancias de cada ponto de sua
        // média
        List<BigDecimal> varX = new ArrayList<>();
        List<BigDecimal> varY = new ArrayList<BigDecimal>();

        for (BigDecimal x : xarray) {
            varX.add(x.subtract(averageX()));
        }
        for (BigDecimal y : yarray) {
            varY.add(y.subtract(averageY()));
        }

        BigDecimal topLine = BigDecimal.ZERO;
        BigDecimal bottomLine = BigDecimal.ZERO;

        for (int i = 0; i < xarray.size(); i++) {
            topLine = topLine.add(varX.get(i).multiply(varY.get(i)));
            bottomLine = bottomLine.add(varX.get(i).multiply(varX.get(i)));
        }

        if (bottomLine.compareTo(BigDecimal.ZERO)!=0) {
            return topLine.divide(bottomLine,10, RoundingMode.FLOOR);
        } else {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Intercept Y
     * @return
     */

    public BigDecimal leastSquaresYIntercept() {
        return averageY().subtract(leastSquaresGradient().multiply(averageX()));
    }

    /**
     * Média de X
     *
     * @return média do array xarray
     */
    public BigDecimal averageX() {
        BigDecimal temp = BigDecimal.ZERO;
        for (BigDecimal t : xarray) {
            temp = temp.add(t);
        }

        if (xarray.size() == 0) {
            return BigDecimal.ZERO;
        }
        return temp.divide(BigDecimal.valueOf(xarray.size()),10,RoundingMode.FLOOR);
    }

    /**
     * Média de Y
     *
     * @return média do array yarray
     */

    public BigDecimal averageY() {

        BigDecimal temp = BigDecimal.ZERO;
        for (BigDecimal t : yarray) {
            temp = temp.add(t);
        }

        if (yarray.size() == 0) {
            return BigDecimal.ZERO;
        }
        return temp.divide(BigDecimal.valueOf(yarray.size()),10,RoundingMode.FLOOR);
    }
}
