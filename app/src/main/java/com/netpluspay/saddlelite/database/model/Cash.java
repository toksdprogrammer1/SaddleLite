package com.netpluspay.saddlelite.database.model;

public class Cash {
    public static final String TABLE_NAME = "cash";

    public static final String COLUMN_ID = "id";
    public static final String COLUMN_MERCHANT_ID = "merchantId";
    public static final String COLUMN_ORDER_NO = "orderNo";
    public static final String COLUMN_AMOUNT = "amount";
    public static final String COLUMN_NARRATIVE = "narrative";
    public static final String COLUMN_EMAIL = "email";
    public static final String COLUMN_STATUS = "status";

    private int id;
    private String merchantId;
    private String orderNo;
    private String amount;
    private String narrative;
    private String email;
    private String status;

    // Create table SQL query
    public static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + "("
                    + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + COLUMN_MERCHANT_ID + " TEXT,"
                    + COLUMN_ORDER_NO + " TEXT,"
                    + COLUMN_AMOUNT + " TEXT,"
                    + COLUMN_NARRATIVE + " TEXT,"
                    + COLUMN_EMAIL + " TEXT,"
                    + COLUMN_STATUS + " TEXT"
                    + ")";

    public Cash() {
    }

    public Cash(int id, String merchantId, String orderNo, String amount, String narrative, String email, String status) {
        this.id = id;
        this.merchantId = merchantId;
        this.orderNo = orderNo;
        this.amount = amount;
        this.narrative = narrative;
        this.email = email;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
