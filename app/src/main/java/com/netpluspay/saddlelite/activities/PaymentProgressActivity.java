package com.netpluspay.saddlelite.activities;

import com.netpluspay.saddlelite.R;
import com.netpluspay.saddlelite.fragments.BluetoothDialogFragment;
import com.netpluspay.saddlelite.fragments.BluetoothDialogFragmentPrinter;
import com.netpluspay.saddlelite.fragments.PrinterDialogFragment;
import com.netpluspay.saddlelite.fragments.TerminalDialogFragment;
import com.netpluspay.saddlelite.utils.LogTransactionOnServer;
import com.netpluspay.saddlelite.utils.SPConstants;
import com.netpluspay.saddlelite.utils.SmartPesaTransactionType;
import com.netpluspay.saddlelite.utils.UIHelper;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import smartpesa.sdk.ServiceManager;
import smartpesa.sdk.SmartPesa;
import smartpesa.sdk.devices.SpTerminal;
import smartpesa.sdk.core.error.SpException;
import smartpesa.sdk.error.SpCardTransactionException;
import smartpesa.sdk.error.SpTransactionException;
import smartpesa.sdk.models.currency.Currency;
import smartpesa.sdk.models.merchant.TransactionType;
import smartpesa.sdk.models.transaction.Balance;
import smartpesa.sdk.scanner.PrinterScanningCallback;
import smartpesa.sdk.scanner.TerminalScanningCallback;
import smartpesa.sdk.interfaces.TransactionCallback;
import smartpesa.sdk.interfaces.TransactionData;
import smartpesa.sdk.models.loyalty.Loyalty;
import smartpesa.sdk.models.loyalty.LoyaltyTransaction;
import smartpesa.sdk.models.printing.AbstractPrintingDefinition;
import smartpesa.sdk.models.transaction.Card;
import smartpesa.sdk.models.transaction.Transaction;
import smartpesa.sdk.models.receipt.*;
import smartpesa.sdk.devices.SpPrinterDevice;
import smartpesa.sdk.error.SpPrinterException;
import smartpesa.sdk.error.SpSessionException;
import smartpesa.sdk.interfaces.PrintingCallback;
import smartpesa.sdk.models.printing.AbstractPrintingDefinition;
import smartpesa.sdk.models.receipt.GetReceiptCallback;
import smartpesa.sdk.models.transaction.SendNotificationCallback;
//import smartpesa.sdk.scanner.PrinterScanningCallback;


public class PaymentProgressActivity extends AppCompatActivity {

    public static final String LOG_TAG = "Payment";
    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_ORDER_NO = "orderNo";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    private static final String BLUETOOTH_FRAGMENT_TAG = "bluetooth";
    public SmartPesaTransactionType transactionType;
    ProgressDialog mProgressDialog;
    protected int fromAccount, toAccount;
    public int transactionFinished = 0;
    public String deviceID="";
    @Bind(R.id.amount_tv) TextView amountTv;
    @Bind(R.id.progress_tv) TextView progressTv;
    @Bind(R.id.back_btn) Button backBtn;
    @Bind(R.id.receipt_btn) Button receiptBtn;
    @Bind(R.id.email_receipt_btn) Button emailReceiptBtn;
    @Bind(R.id.emailEtxt)
    EditText emailEtxt;
    double amount;
    String orderNo;
    String email;
    String password;
    ServiceManager mServiceManager;
    public  Transaction transaction = null;
    String finalEmail = "";
    String finalPhone = "";
    boolean sendEmail = false;
    public SpTransactionException exception = null;
    private LogTransactionOnServer logTransactionOnServer;
    UUID transactionID;
    List<AbstractPrintingDefinition> dataToPrint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_progress);
        ButterKnife.bind(this);

        receiptBtn.setVisibility(View.INVISIBLE);
        backBtn.setVisibility(View.INVISIBLE);
        emailReceiptBtn.setVisibility(View.INVISIBLE);
        emailEtxt.setVisibility(View.INVISIBLE);
        //initialise service manager
        mServiceManager = ServiceManager.get(PaymentProgressActivity.this);

        amount = getIntent().getExtras().getDouble(KEY_AMOUNT);
        email = getIntent().getExtras().getString(KEY_EMAIL);
        password = getIntent().getExtras().getString(KEY_PASSWORD);
        orderNo = getIntent().getExtras().getString(KEY_ORDER_NO);
        amountTv.setText("Amount: "+amount);
        progressTv.setText("Enabling blueetooth..");

        //amount = new BigDecimal(this.getIntent().getDoubleExtra(SPConstants.AMOUNT, 0.00));
        //transactionType = SmartPesaTransactionType.fromEnumId(1);
        //fromAccount = this.getIntent().getIntExtra(SPConstants.FROM_ACCOUNT, 0);
        //toAccount = this.getIntent().getIntExtra(SPConstants.TO_ACCOUNT, 10);
        //qrScannedData = this.getIntent().getStringExtra(SPConstants.QR_ALI_PAY_SCAN_TEXT);
        //isScan = this.getIntent().getBooleanExtra(SPConstants.IS_ALI_PAY_SCAN, false);

        transactionType = SmartPesaTransactionType.fromEnumId(1);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setCancelable(false);

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

    public SmartPesa.TransactionParam buildTransactionParam(SpTerminal terminal) {

        if (transactionType != null) {
            SmartPesa.TerminalTransactionParam.Builder builder = SmartPesa.TransactionParam.newBuilder(terminal)
                    .transactionType(transactionType.getEnumId())
                    .terminal(terminal)
                    .amount(new BigDecimal(Double.valueOf(amount)))
                    .from(SmartPesa.AccountType.DEFAULT)
                    .to(SmartPesa.AccountType.SAVINGS);

            SmartPesa.TransactionParam param = builder.build();

            return param;
        } else {
            return null;
        }
    }

    private void performPayment(SpTerminal spTerminal) {

        //start the transaction
        /*SmartPesa.TransactionParam param = SmartPesa.TransactionParam.newBuilder()
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
                .build();*/
        SmartPesa.TransactionParam param = buildTransactionParam(spTerminal);
        if (param != null) {
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
                /*if (transactionFinished == 0) {
                    Intent intent = new Intent();
                    intent.putExtra("status", "FAILED");
                    setResult(RESULT_CANCELED, intent);
                    finish();
                } else if (transactionFinished == 2) {
                    Intent intent = new Intent();
                    //intent.putExtra("status", "FAILED");
                    setResult(RESULT_CANCELED, intent);
                    finish();
                }*/
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
            public void onTransactionFinished(TransactionType transactionType, boolean isSuccess, @Nullable Transaction transaction2, @Nullable SmartPesa.Verification verification, @Nullable SpCardTransactionException exception2) {

            }


            @Override
            public void onTransactionApproved(TransactionData transaction2) {
                transaction = transaction2.getTransaction();
                //exception = exception2;
                transactionFinished = 1;
                logTransactionOnServer = new LogTransactionOnServer(email, password, orderNo, amount + "", "SUCCESS", 1, getBaseContext());
                logTransactionOnServer.logCashTransaction();
                progressTv.setText("Transaction success");
                receiptBtn.setVisibility(View.VISIBLE);
                backBtn.setVisibility(View.VISIBLE);
                emailEtxt.setVisibility(View.INVISIBLE);
                emailReceiptBtn.setVisibility(View.INVISIBLE);
                //backBtn.setVisibility(View.INVISIBLE);
                backBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.putExtra("amount", amount);

                        intent.putExtra("transactionReference", transaction.getTransactionResult().getTransactionReference());
                        intent.putExtra("transactionId", transaction.getTransactionResult().getTransactionId());
                        intent.putExtra("transactionDateTime", transaction.getTransactionResult().getTransactionDatetime().toString());
                        intent.putExtra("currency", transaction.getTransactionResult().getCurrencyName());
                        intent.putExtra("responseCode", transaction.getTransactionResult().getResponseCode());
                        intent.putExtra("responseDescription", transaction.getTransactionResult().getResponseDescription());
                        intent.putExtra("status", "SUCCESS");
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                });

                receiptBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        printReceipt(transaction.getTransactionResult().getTransactionId());
                    }
                });

                emailReceiptBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        sendReceipt(transaction.getTransactionResult().getTransactionId());
                    }
                });

            }


            @Override
            public void onTransactionDeclined(SpTransactionException e, TransactionData transactionData) {

                transactionFinished = 2;
                exception = e;
                transaction = transactionData.getTransaction();
                emailEtxt.setVisibility(View.INVISIBLE);
                emailReceiptBtn.setVisibility(View.INVISIBLE);
                backBtn.setVisibility(View.VISIBLE);
                if (exception.getMessage().equalsIgnoreCase("DECLINED")) {
                    if (transactionData != null && transactionData.getTransaction() != null && transactionData.getTransaction().getTransactionResult() != null) {
                        //try {
                        //SmartPesa.ReceiptFormat receiptFormat = SmartPesa.ReceiptFormat.CUSTOMER;
                        //fetchReceiptAndPrint(transactionData.getTransaction().getTransactionResult().getTransactionId(), receiptFormat);
                        progressTv.setText(e.getMessage());
                        //receiptBtn.setVisibility(View.INVISIBLE);

                        receiptBtn.setVisibility(View.VISIBLE);
                        emailReceiptBtn.setVisibility(View.VISIBLE);
                        emailEtxt.setVisibility(View.VISIBLE);

                        receiptBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //SmartPesa.ReceiptFormat receiptFormat = SmartPesa.ReceiptFormat.CUSTOMER;
                                printReceipt(transaction.getTransactionResult().getTransactionId());
                            }
                        });

                        emailReceiptBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                sendReceipt(transaction.getTransactionResult().getTransactionId());

                            }
                        });


                    }
                }
                else{
                    progressTv.setText(exception.getMessage());
                    logTransactionOnServer = new LogTransactionOnServer(email, password, orderNo, amount + "", "FAILED", 1, getBaseContext());
                    logTransactionOnServer.logCashTransaction();
                }
                backBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.putExtra("amount", amount);
//                            intent.putExtra("cardHolderName", transaction.getTransactionResult().getCardHolderName());
//                            intent.putExtra("cardBrand", transaction.getTransactionResult().getCardBrand());
//                            intent.putExtra("cardNumber", transaction.getTransactionResult().getCardNumber());
//                            intent.putExtra("transactionReference", transaction.getTransactionResult().getTransactionReference());
                            intent.putExtra("transactionId", "FAILED");
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


            @Override
            public void onError(SpException exception) {
                transactionFinished = 2;
                progressTv.setText(exception.getMessage());
                Log.d("Exception Message", exception.getMessage());
                //SmartPesa.ReceiptFormat receiptFormat = SmartPesa.ReceiptFormat.CUSTOMER;
                //fetchReceiptAndPrint(transaction.getTransactionResult().getTransactionId(), receiptFormat);
               // if (!exception.getMessage().equalsIgnoreCase("DECLINED")) {
                    //logTransactionOnServer = new LogTransactionOnServer(email, password, orderNo, amount + "", "FAILED", 1);
                    //logTransactionOnServer.logCashTransaction();
               // }
                //SystemClock.sleep(500000);
                Intent intent = new Intent();
                //intent.putExtra("exception", exception.getMessage());
                intent.putExtra("amount", amount);
                intent.putExtra("status", "FAILED");
                intent.putExtra("transactionId", "FAILED");
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

                @Override
                public void onShowConfirmFeePrompt(TransactionType.FeeChargeType feeChargeType, Currency currency, BigDecimal feeAmount, BigDecimal finalAmount) {

                }

                @Override
                public void onRequestForInput() {

                }


                @Override
            public void onShowBalance(Balance balance) {

            }
        });
    }
    }

    //print receipt
    private void printReceipt(final UUID transactionId) {

        final SmartPesa.ReceiptFormat[] receiptFormats = {
                SmartPesa.ReceiptFormat.CUSTOMER,
                SmartPesa.ReceiptFormat.MERCHANT
        };
        new AlertDialog.Builder(PaymentProgressActivity.this)
                .setTitle(getString(R.string.select_receipt_format))
                .setAdapter(
                        new ArrayAdapter<SmartPesa.ReceiptFormat>(
                                PaymentProgressActivity.this,
                                android.R.layout.simple_list_item_1,
                                receiptFormats
                        ),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SmartPesa.ReceiptFormat receiptFormat = receiptFormats[which];
                                fetchReceiptAndPrint(transactionId, receiptFormat);
                            }
                        })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();

    }


    //print receipt start here
    protected void fetchReceiptAndPrint(UUID transactionId, SmartPesa.ReceiptFormat receiptFormat) {
        HashMap<String, Object> config = new HashMap<>();
        final ProgressDialog mp = new ProgressDialog(this);
        mp.setTitle(getString(R.string.app_name));
        mp.setMessage(getString(R.string.loading_receipt));
        mp.show();
        mServiceManager.getReceipt(transactionId, receiptFormat, config, new GetReceiptCallback() {
            @Override
            public void onSuccess(List<AbstractPrintingDefinition> abstractPrintingDefinitions) {
                //if (isActivityDestroyed()) return;
                mp.dismiss();
                dataToPrint = abstractPrintingDefinitions;
                final boolean[] processCalled = {false};
                final boolean[] listDisplayed = {false};
                mServiceManager.scanPrinter(new PrinterScanningCallback() {
                    @Override
                    public void onDeviceListRefresh(Collection<SpPrinterDevice> collection) {

                        Log.d("No of Printer", collection.size()+"");
                            if (collection.size() > 1) {

                                displayPrinterDevice(collection);
                            }
                            else {

                                for (SpPrinterDevice device : collection) {
                                    Log.d("Printer name", device.getName());
                                    performPrint(device);
                                }

                            }
                        //if (isActivityDestroyed()) return;

//                        if (!listDisplayed[0]) {
//                            SpPrinterDevice defaultPrinter = null;
//                            for (final SpPrinterDevice device : collection) {
//                                if (device.getName().equalsIgnoreCase(deviceID)) {
//                                    defaultPrinter = device;
//                                    break;
//                                }
//                            }
//
//                            if (defaultPrinter != null && !processCalled[0]) {
//
//                                Log.d("Printer name", defaultPrinter.getName());
//                                processCalled[0] = true;
//                                performPrint(defaultPrinter);
//                                if (mServiceManager != null) {
//                                    mServiceManager.stopScan();
//                                }
//                            }
//                           // else {
//                           //     displayPrinterDevice(collection);
//                           //     listDisplayed[0] = true;
//                           // }
//                        } //else if (!processCalled[0]) {
//                        //    displayPrinterDevice(collection);
//                      //  }
                       // displayPrinterDevice(collection);
                    }

                    @Override
                    public void onScanStopped() {
                        // if (isActivityDestroyed()) return;
                    }

                    @Override
                    public void onScanTimeout() {
                        //if (isActivityDestroyed()) return;
                    }

                    @Override
                    public void onEnablingBluetooth(String s) {

                    }

                    @Override
                    public void onBluetoothPermissionDenied(String[] strings) {

                    }
                });
            }

            @Override
            public void onError(SpException exception) {
                // if (isActivityDestroyed()) return;

                if (exception instanceof SpSessionException) {
                    mp.dismiss();
                    //show the expired message
                    UIHelper.showToast(PaymentProgressActivity.this, getResources().getString(R.string.session_expired));
                    //finish the current activity
                    finish();

                    //start the splash screen
                    //startActivity(new Intent(AliPayReceiptActivity.this, SplashActivity.class));
                } else {
                    mp.dismiss();
                    UIHelper.showErrorDialog(PaymentProgressActivity.this, getResources().getString(R.string.app_name), exception.getMessage());
                }
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
            deviceID = device.getName();
            Log.d("Device name", deviceID);
            performPayment(device);
        }
    }

    private void displayPrinterDevice(Collection<SpPrinterDevice> devices) {
        //try {
            PrinterDialogFragment dialog;
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(BLUETOOTH_FRAGMENT_TAG);
            if (fragment == null) {

                dialog = new PrinterDialogFragment();
                dialog.show(getSupportFragmentManager(), BLUETOOTH_FRAGMENT_TAG);
            } else {
                dialog = (PrinterDialogFragment) fragment;
            }
            dialog.setSelectedListener(new PrinterSelectedImpl());
            dialog.updateDevices(devices);
       // }
        //catch (Exception ex){

        //}
    }

    private class PrinterSelectedImpl implements BluetoothDialogFragmentPrinter.TerminalSelectedListener<SpPrinterDevice> {
        @Override
        public void onSelected(SpPrinterDevice device)
        {
            performPrint(device);
            if (mServiceManager != null) {
                mServiceManager.stopScan();
            }
        }

        @Override
        public void onCancelled() {

        }
    }

    private void performPrint(SpPrinterDevice device) {
        closeDialogFragment();
        mServiceManager.performPrint(SmartPesa.PrintingParam.withData(dataToPrint).printerDevice(device).build(), new PrintingCallback() {
            @Override
            public void onPrinterError(SpPrinterException errorMessage) {
                //if (isActivityDestroyed()) return;
                UIHelper.showErrorDialog(PaymentProgressActivity.this, getResources().getString(R.string.app_name), errorMessage.getMessage());
            }

            @Override
            public void onPrinterSuccess() {
                //if (isActivityDestroyed()) return;
                UIHelper.log("here");
                closeDialogFragment();
            }
        });
    }

    //close the printer bluetooth list if already one is present
    private void closeDialogFragment() {
        Fragment dialogBluetoothList = getSupportFragmentManager().findFragmentByTag(BLUETOOTH_FRAGMENT_TAG);
        if (dialogBluetoothList != null) {
            DialogFragment dialogFragment = (DialogFragment) dialogBluetoothList;
            if (dialogFragment != null) {
                dialogFragment.dismiss();
            }
        }
    }

    //send receipt
    public void sendReceipt(final UUID transactionId) {

        HashMap<String, Object> config = new HashMap<>();

        String email = emailEtxt.getText().toString();
        Log.d("Email", email);
        sendEmail = true;
        if (validateSending(email)) {
            Log.d("Email", finalEmail);
            mProgressDialog.setMessage(getResources().getString(R.string.sending_receipt));
            mProgressDialog.show();

            mServiceManager.sendNotification(transactionId, finalEmail, finalPhone, config, new SendNotificationCallback() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    //if (isActivityDestroyed()) return;

                    mProgressDialog.dismiss();

                    UIHelper.showToast(PaymentProgressActivity.this, getResources().getString(R.string.sending_success));
                    emailEtxt.setText("");
                }

                @Override
                public void onError(SpException exception) {
                    if (exception instanceof SpSessionException) {
                        mProgressDialog.dismiss();
                        UIHelper.showToast(PaymentProgressActivity.this, getResources().getString(R.string.session_expired));
                        //finish the current activity
                        finish();

                        //start the splash screen
                        //startActivity(new Intent(AliPayReceiptActivity.this, SplashActivity.class));
                    } else {
                        // if (isActivityDestroyed()) return;

                        UIHelper.log("Error = " + exception.getMessage());
                        mProgressDialog.dismiss();
                        UIHelper.showErrorDialog(PaymentProgressActivity.this, getResources().getString(R.string.app_name), exception.getMessage());
                    }
                }

            });
        }
    }

    //validate the phone and email entered
    public boolean validateSending(String email) {


        if (sendEmail) {
            if (TextUtils.isEmpty(email)) {
                finalEmail = "";
                UIHelper.showToast(PaymentProgressActivity.this, getResources().getString(R.string.enter_valid_email));
                return false;
            } else {
                finalEmail = email;
            }
        } else {
            finalEmail = "";
        }
        return true;


    }

}
