package com.netpluspay.saddlelite.utils;

public enum SmartPesaTransactionType {
    SALE(1),
    REFUND(2),
    CASH_BACK(3),
    BALANCE_INQUIRY(4),
    ACCOUNT_TRANSFER(6),
    PAYMENT(7),
    LOYALTY(8),
    WITHDRAWAL(9),
    VOID(10),
    REVERSAL(11),
    GENERAL_TRANSFER(12),
    CASH_DEPOSIT(13),
    CASH_ADVANCE(14);

    public final int id;

    SmartPesaTransactionType(int id) {
        this.id = id;
    }

    public static SmartPesaTransactionType fromEnumId(int id) {
        if (id == SALE.id) {
            return SALE;
        } else if (id == LOYALTY.id) {
            return LOYALTY;
        } else if (id == VOID.id) {
            return VOID;
        } else if (id == REVERSAL.id) {
            return REVERSAL;
        } else if (id == GENERAL_TRANSFER.id) {
            return GENERAL_TRANSFER;
        } else if (id == PAYMENT.id) {
            return PAYMENT;
        } else if (id == BALANCE_INQUIRY.id) {
            return BALANCE_INQUIRY;
        } else if (id == REFUND.id) {
            return REFUND;
        } else if (id == ACCOUNT_TRANSFER.id) {
            return ACCOUNT_TRANSFER;
        } else if (id == CASH_ADVANCE.id) {
            return CASH_ADVANCE;
        } else if (id == CASH_BACK.id) {
            return CASH_BACK;
        } else if (id == CASH_DEPOSIT.id) {
            return CASH_DEPOSIT;
        } else if (id == WITHDRAWAL.id) {
            return WITHDRAWAL;
        } else {
            return SALE;
        }
    }

    public final int getEnumId() {
        return this.id;
    }

    @Override
    public String toString() {
        if (id == SALE.id) {
            return "Sale";
        } else if (id == LOYALTY.id) {
            return "Loyalty";
        } else if (id == VOID.id) {
            return "Void";
        } else if (id == REVERSAL.id) {
            return "Reversal";
        } else if (id == GENERAL_TRANSFER.id) {
            return "Transfer";
        } else if (id == PAYMENT.id) {
            return "Payment";
        } else if (id == BALANCE_INQUIRY.id) {
            return "Balance inquiry";
        } else if (id == REFUND.id) {
            return "Refund";
        } else if (id == ACCOUNT_TRANSFER.id) {
            return "Account transfer";
        } else if (id == CASH_ADVANCE.id) {
            return "Cash advance";
        } else if (id == CASH_BACK.id) {
            return "Cash back";
        } else if (id == CASH_DEPOSIT.id) {
            return "Cash deposit";
        } else if (id == WITHDRAWAL.id) {
            return "Withdrawal";
        } else {
            return "Sale";
        }
    }
}
