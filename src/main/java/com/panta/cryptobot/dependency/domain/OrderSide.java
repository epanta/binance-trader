package com.panta.cryptobot.dependency.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Buy/Sell order side.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public enum OrderSide {
  BUY,
  SELL
}
