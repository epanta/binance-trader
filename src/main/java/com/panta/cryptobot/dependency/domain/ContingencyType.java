package com.panta.cryptobot.dependency.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public enum ContingencyType {
    OCO
}
