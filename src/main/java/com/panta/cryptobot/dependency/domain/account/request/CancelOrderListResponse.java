package com.panta.cryptobot.dependency.domain.account.request;

import com.panta.cryptobot.dependency.domain.account.NewOCOResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CancelOrderListResponse extends NewOCOResponse {

}