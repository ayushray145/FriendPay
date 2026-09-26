package com.splitledger.payment;

public class FriendPaymentUnavailableException extends RuntimeException {
    public FriendPaymentUnavailableException() {
        super("This friend has not shared a UPI ID for payments");
    }
}
