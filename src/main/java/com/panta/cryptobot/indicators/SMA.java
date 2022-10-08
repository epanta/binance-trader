package com.panta.cryptobot.indicators;

import java.util.LinkedList;

public class SMA {
    private LinkedList values = new LinkedList();

    private double sum = 0;

    private double average = 0;

    /**
     * Compute the moving average.
     * Synchronised so that no changes in the underlying data is made during calculation.
     * @param value The value
     * @return The average
     */
    public synchronized double compute(double value, int length)
    {
        if (values.size() == length && length > 0)
        {
            sum -= ((Double) values.getFirst()).doubleValue();
            values.removeFirst();
        }
        sum += value;
        values.addLast(new Double(value));
        average = sum / values.size();
        return average;
    }
}
