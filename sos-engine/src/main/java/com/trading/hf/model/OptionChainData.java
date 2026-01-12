package com.trading.hf.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class OptionChainData {

    @JsonProperty("strike")
    private int strike;

    @JsonProperty("call_oi")
    private int callOi;

    @JsonProperty("put_oi")
    private int putOi;

    public int getStrike() {
        return strike;
    }

    public void setStrike(int strike) {
        this.strike = strike;
    }

    public int getCallOi() {
        return callOi;
    }

    public void setCallOi(int callOi) {
        this.callOi = callOi;
    }

    public int getPutOi() {
        return putOi;
    }

    public void setPutOi(int putOi) {
        this.putOi = putOi;
    }
}
