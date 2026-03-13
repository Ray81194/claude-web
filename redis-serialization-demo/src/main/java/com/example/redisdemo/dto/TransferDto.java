package com.example.redisdemo.dto;

public class TransferDto {

    private AccountId fromAccount;
    private AccountId toAccount;
    private Money amount;
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
