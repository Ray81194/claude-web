package com.example.redisdemo.dto.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Money {

    private final int value;
    private final String currency;

    private Money(int value, String currency) {
        this.value = value;
        this.currency = currency;
    }

    @JsonCreator
    public static Money of(
            @JsonProperty("value") int value,
            @JsonProperty("currency") String currency) {
        return new Money(value, currency);
    }

    public int getValue() {
        return value;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return value == money.value && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return 31 * value + currency.hashCode();
    }

    @Override
    public String toString() {
        return value + " " + currency;
    }
}
