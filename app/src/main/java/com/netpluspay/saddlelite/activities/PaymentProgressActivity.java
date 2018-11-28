package com.netpluspay.saddlelite.activities;

import com.netpluspay.saddlelite.R;
import com.netpluspay.saddlelite.fragments.BluetoothDialogFragment;
import com.netpluspay.saddlelite.fragments.TerminalDialogFragment;
import com.netpluspay.saddlelite.utils.LogTransactionOnServer;

import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import smartpesa.sdk.ServiceManager;
import smartpesa.sdk.SmartPesa;
import smartpesa.sdk.devices.SpTerminal;
import smartpesa.sdk.error.SpException;
import smartpesa.sdk.error.SpTransactionException;
import smartpesa.sdk.interfaces.TerminalScanningCallback;
import smartpesa.sdk.interfaces.TransactionCallback;
import smartpesa.sdk.models.loyalty.Loyalty;
import smartpesa.sdk.models.loyalty.LoyaltyTransaction;
import smartpesa.sdk.models.printing.AbstractPrintingDefinition;
import smartpesa.sdk.models.transaction.Card;
import smartpesa.sdk.models.transaction.Transaction;
import smartpesa.sdk.models.receipt.*;

public class PaymentProgressActivity extends AppCompatActivity {

    public static final String LOG_TAG = "Payment";
    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_ORDER_NO = "orderNo";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    private static final String BLUETOOTH_FRAGMENT_TAG = "bluetooth";
    public int transactionFinished = 0;

    @Bind(R.id.amount_tv) TextView amountTv;
    @Bind(R.id.progress_tv) TextView progressTv;
    @Bind(R.id.back_btn) Button backBtn;
    @Bind(R.id.receipt_btn) Button receiptBtn;
    double amount;
    String orderNo;
    String email;
    String password;
    ServiceManager mServiceManager;
    public  Transaction transaction = null;
    public SpTransactionException exception = null;
    private LogTransactionOnServer logTransactionOnServer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_progress);
        ButterKnife.bind(this);

        receiptBtn.setVisibility(View.INVISIBLE);
        backBtn.setVisibility(View.INVISIBLE);
        //initialise service manager
        mServiceManager = ServiceManager.get(PaymentProgressActivity.this);

        amount = getIntent().getExtras().getDouble(KEY_AMOUNT);
        email = getIntent().getExtras().getString(KEY_EMAIL);
        password = getIntent().getExtras().getString(KEY_PASSWORD);
        orderNo = getIntent().getExtras().getString(KEY_ORDER_NO);
        amountTv.setText("Amount: "+amount);
        progressTv.setText("Enabling blueetooth..");



        //scan for bluetooth device
        mServiceManager.scanTerminal(new TerminalScanningCallback() {
            @Override
            public void onDeviceListRefresh(Collection<SpTerminal> collection) {
                displayBluetoothDevice(collection);
            }

            @Override
            public void onScanStopped() {

            }

            @Override
            public void onScanTimeout() {

                Intent intent = new Intent();
                //intent.putExtra("status", "FAILED");
                setResult(RESULT_CANCELED, intent);
                finish();
            }

            @Override
            public void onEnablingBluetooth(String s) {

            }

            @Override
            public void onBluetoothPermissionDenied(String[] strings) {

                Intent intent = new Intent();
                //intent.putExtra("status", "FAILED");
                setResult(RESULT_CANCELED, intent);
                finish();
            }

        });
    }

    private void performPayment(SpTerminal spTerminal) {

        //start the transaction
        SmartPesa.TransactionParam param = SmartPesa.TransactionParam.newBuilder()
                .transactionType(SmartPesa.TransactionType.GOODS_AND_SERVICES)
                .terminal(spTerminal)
                .amount(new BigDecimal(Double.valueOf(amount)))
                .from(SmartPesa.AccountType.DEFAULT)
                .to(SmartPesa.AccountType.SAVINGS)
                .cashBack(BigDecimal.ZERO)
                .cardMode(SmartPesa.CardMode.SWIPE_OR_INSERT)

                //uncomment below if testing with PAX
//                .withApduExtension(new MyApduExtension())

                .extraParams(new HashMap<String, Object>())
                .build();

        mServiceManager.performTransaction(param, new TransactionCallback() {
            @Override
            public void onProgressTextUpdate(String s) {
                progressTv.setText(s);
            }

            @Override
            public void onDeviceConnected(SpTerminal spTerminal) {

            }

            @Override
            public void onDeviceDisconnected(SpTerminal spTerminal) {
                if (transactionFinished == 0) {
                    Intent intent = new Intent();
                    intent.putExtra("status", "FAILED");
                    setResult(RESULT_CANCELED, intent);
                    finish();
                }
                else if (transactionFinished == 2) {
                    Intent intent = new Intent();
                    //intent.putExtra("status", "FAILED");
                    setResult(RESULT_CANCELED, intent);
                    finish();
                }
            }

            @Override
            public void onBatteryStatus(SmartPesa.BatteryStatus batteryStatus) {

            }

            @Override
            public void onShowSelectApplicationPrompt(List<String> list) {

            }

            @Override
            public void onWaitingForCard(String s, SmartPesa.CardMode cardMode) {
                progressTv.setText("Insert/swipe card");
            }

            @Override
            public void onShowInsertChipAlertPrompt() {
                progressTv.setText("Insert chip card");
            }

            @Override
            public void onReadCard(Card card) {

            }

            @Override
            public void onShowPinAlertPrompt() {
                progressTv.setText("Enter PIN on NETPOS");
            }

            @Override
            public void onPinEntered() {

            }

            @Override
            public void onShowInputPrompt() {

            }

            @Override
            public void onReturnInputStatus(SmartPesa.InputStatus inputStatus, String s) {

            }

            @Override
            public void onShowConfirmAmountPrompt() {
                progressTv.setText("Confirm amount on NETPOS");
            }

            @Override
            public void onAmountConfirmed(boolean b) {

            }

            @Override
            public void onTransactionFinished(SmartPesa.TransactionType transactionType, boolean isSuccess, @Nullable Transaction transaction2, @Nullable SmartPesa.Verification verification, @Nullable SpTransactionException exception2) {
                transaction = transaction2;
                exception = exception2;
                transactionFinished = 1;
                if(isSuccess) {
                    logTransactionOnServer = new LogTransactionOnServer(email,password, orderNo, amount+"", "SUCCESS", 1);
                    logTransactionOnServer.logCashTransaction();
                    progressTv.setText("Transaction success");
                    receiptBtn.setVisibility(View.VISIBLE);
                    backBtn.setVisibility(View.VISIBLE);
                    //backBtn.setVisibility(View.INVISIBLE);
                    backBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            //intent.putExtra("amount", amount);
                            intent.putExtra("cardHolderName", transaction.getTransactionResult().getCardHolderName());
                            intent.putExtra("cardBrand", transaction.getTransactionResult().getCardBrand());
                            intent.putExtra("cardNumber", transaction.getTransactionResult().getCardNumber());
                            intent.putExtra("transactionReference", transaction.getTransactionResult().getTransactionReference());
                            intent.putExtra("transactionId", "Card Transaction");
                            intent.putExtra("transactionDateTime", transaction.getTransactionResult().getTransactionDatetime().toString());
                            intent.putExtra("currency", transaction.getTransactionResult().getCurrencyName());
                            intent.putExtra("responseCode", transaction.getTransactionResult().getResponseCode());
                            intent.putExtra("responseDescription", transaction.getTransactionResult().getResponseDescription());
                            intent.putExtra("status", "CAPTURED");
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    });

                    receiptBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ((ServiceManager)PaymentProgressActivity.this.mServiceManager).getReceipt(transaction.getTransactionResult().getTransactionId(), SmartPesa.ReceiptFormat.CUSTOMER, null, new GetReceiptCallback(){


                                @Override
                                public void onSuccess(List<AbstractPrintingDefinition> abstractPrintingDefinitions) {
                                    Toast toast = Toast.makeText(getApplicationContext(),
                                            "Sent to  printer",
                                            Toast.LENGTH_SHORT);

                                    toast.show();
                                }

                                public void onError(SpException paramAnonymous2SpException)
                                {
                                    Toast toast = Toast.makeText(getApplicationContext(),
                                            "Unable to print",
                                            Toast.LENGTH_SHORT);

                                    toast.show();
                                }


                            });
                        }
                    });


                } else {
                    progressTv.setText(exception.getMessage());
                    //receiptBtn.setVisibility(View.INVISIBLE);
                    backBtn.setVisibility(View.VISIBLE);
                    backBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            //intent.putExtra("amount", amount);
//                            intent.putExtra("cardHolderName", transaction.getTransactionResult().getCardHolderName());
//                            intent.putExtra("cardBrand", transaction.getTransactionResult().getCardBrand());
//                            intent.putExtra("cardNumber", transaction.getTransactionResult().getCardNumber());
//                            intent.putExtra("transactionReference", transaction.getTransactionResult().getTransactionReference());
//                            intent.putExtra("transactionId", "Card Transaction");
//                            intent.putExtra("transactionDateTime", transaction.getTransactionResult().getTransactionDatetime().toString());
//                            intent.putExtra("currency", transaction.getTransactionResult().getCurrencyName());
//                            intent.putExtra("responseCode", transaction.getTransactionResult().getResponseCode());
//                            intent.putExtra("responseDescription", transaction.getTransactionResult().getResponseDescription());
                            intent.putExtra("exception", exception.getMessage());
                            intent.putExtra("status", "FAILED");
                            setResult(RESULT_CANCELED, intent);
                            finish();
                        }
                    });

                }
            }



            @Override
            public void onError(SpException exception) {
                transactionFinished = 2;
                progressTv.setText(exception.getMessage());
                //SystemClock.sleep(500000);
                Intent intent = new Intent();
                //intent.putExtra("exception", exception.getMessage());
                //intent.putExtra("status", "FAILED");
                setResult(RESULT_CANCELED, intent);
                finish();
                //Log.d(LOG_TAG, "I got here for erroe");
            }


            @Override
            public void onStartPostProcessing(String providerName, Transaction transaction) {

            }

            @Override
            public void onReturnLoyaltyBalance(Loyalty loyalty) {

            }

            @Override
            public void onShowLoyaltyRedeemablePrompt(LoyaltyTransaction loyaltyTransaction) {

            }

            @Override
            public void onLoyaltyCancelled() {

            }

            @Override
            public void onLoyaltyApplied(LoyaltyTransaction loyaltyTransaction) {

            }
        });
    }

    //display the list of bluetooth devices
    public void displayBluetoothDevice(Collection<SpTerminal> devices) {
        TerminalDialogFragment dialog;
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(BLUETOOTH_FRAGMENT_TAG);
        if (fragment == null) {
            dialog = new TerminalDialogFragment();
            dialog.show(getSupportFragmentManager(), BLUETOOTH_FRAGMENT_TAG);
        } else {
            dialog = (TerminalDialogFragment) fragment;
        }
        dialog.setSelectedListener(new DeviceSelectedListenerImpl());
        dialog.updateDevices(devices);
    }

    //start the transaction when the bluetooth device is selected
    private class DeviceSelectedListenerImpl implements BluetoothDialogFragment.DeviceSelectedListener<SpTerminal> {
        @Override
        public void onSelected(SpTerminal device) {
            performPayment(device);
        }
    }

}
