package com.splitledger.payment;

public class PaymentProfileNotConfiguredException extends RuntimeException {
    public PaymentProfileNotConfiguredException() { super("Save a UPI ID in your payment profile first"); }
}
