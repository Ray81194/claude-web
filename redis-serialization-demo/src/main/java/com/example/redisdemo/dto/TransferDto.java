package com.example.redisdemo.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class TransferDto {

    private AccountId fromAccount;
    private AccountId toAccount;
    private Money amount;
    @JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
    @JsonSerialize(using = OrderStatus.Serializer.class)
    @JsonDeserialize(using = OrderStatus.Deserializer.class)
    private OrderStatus status;

    public TransferDto() {}

    public AccountId getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(AccountId fromAccount) {
        this.fromAccount = fromAccount;
    }

    public AccountId getToAccount() {
        return toAccount;
    }

    public void setToAccount(AccountId toAccount) {
        this.toAccount = toAccount;
    }

    public Money getAmount() {
        return amount;
    }

    public void setAmount(Money amount) {
        this.amount = amount;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "TransferDto{fromAccount=" + fromAccount + ", toAccount=" + toAccount + ", amount=" + amount + ", status=" + status + "}";
    }
}
