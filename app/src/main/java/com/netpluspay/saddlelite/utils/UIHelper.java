package com.netpluspay.saddlelite.utils;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

import java.math.BigDecimal;

import smartpesa.sdk.SmartPesa;

import static smartpesa.sdk.SmartPesa.AccountType.DEFAULT;

//import com.smartpesa.smartpesa.models.SmartPesaTransactionType;

public class UIHelper {

    public Typeface regularFont, boldFont,boldItalicFont,italicFont,ocrFont;

    public UIHelper(Context context){
        regularFont = Typeface.createFromAsset(context.getAssets(), "fonts/SmartPesa-Regular.ttf");
        boldFont = Typeface.createFromAsset(context.getAssets(), "fonts/SmartPesa-Bold.ttf");
        boldItalicFont = Typeface.createFromAsset(context.getAssets(), "fonts/SmartPesa-BoldItalic.ttf");
        italicFont = Typeface.createFromAsset(context.getAssets(), "fonts/SmartPesa-Italic.ttf");
        ocrFont = Typeface.createFromAsset(context.getAssets(), "fonts/ocr.ttf");
    }

    public static void showMessageDialogWithCallback(Context ctx, String message, String positiveText, String negativeText, MaterialDialog.ButtonCallback callback){
        if (ctx == null) return;

        new MaterialDialog.Builder(ctx)
                .theme(Theme.LIGHT)
                .content(message)
                .callback(callback)
                .positiveText(positiveText)
                .negativeText(negativeText)
                .show();
    }


    public static void showToast(Context ctx, String message){
        if (ctx == null) return;
        Toast.makeText(ctx, message, Toast.LENGTH_LONG).show();
    }

    public static void showMessageDialog(Context ctx, String message){
        if (ctx == null) return;

        new MaterialDialog.Builder(ctx)
                .content(message)
                .positiveText("OK")
                .cancelable(false)
                .show();
    }

    public static void showErrorDialog(Context ctx, String title, String message){
        if (ctx == null) return;
        new MaterialDialog.Builder(ctx)
                .title(title)
                .theme(Theme.LIGHT)
                .content(message)
                //.iconRes(R.drawable.ic_error_red)
                .positiveText("OK")
                .show();
    }

    public static void showErrorDialog(Context ctx, String title, int messageResId){
        if (ctx == null) return;
        new MaterialDialog.Builder(ctx)
                .title(title)
                .theme(Theme.LIGHT)
                .content(messageResId)
                .title("This is ")
                //.iconRes(R.drawable.ic_error_red)
                .positiveText("OK")
                .show();
    }

    public static void showMessageDialogWithCallback(Context ctx, String message, String positiveText, MaterialDialog.ButtonCallback callback){
        if (ctx == null) return;

        new MaterialDialog.Builder(ctx)
                .content(message)
                .theme(Theme.LIGHT)
                .cancelable(false)
                .callback(callback)
                .positiveText(positiveText)
                .show();
    }

    public static void showMessageDialogWithTitleCallback(Context ctx, String title, String message, String positiveText, MaterialDialog.ButtonCallback callback){
        if (ctx == null) return;

        new MaterialDialog.Builder(ctx)
                .title(title)
                .theme(Theme.LIGHT)
                .content(message)
                .callback(callback)
                .positiveText(positiveText)
                .cancelable(false)
                .show();
    }

    public static void showMessageDialogWithTitleTwoButtonCallback(Context ctx, String title, String message, String positiveText,String negativeText, MaterialDialog.ButtonCallback callback){
        if (ctx == null) return;

        new MaterialDialog.Builder(ctx)
                .title(title)
                .content(message)
                .theme(Theme.LIGHT)
                .callback(callback)
                .positiveText(positiveText)
                .negativeText(negativeText)
                .cancelable(false)
                .show();
    }


    public static boolean isOnline(Context mContext) {
        ConnectivityManager cm =
                (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public static void openBrowser(Context context, String url){
        if (context == null) return;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        if (!(url.startsWith("http") || url.startsWith("https"))){
            url = "http://" + url;
        }
        intent.setData(Uri.parse(url));
        context.startActivity(intent);
    }

    public static void log(String log){
        Log.e("HERE I AM --> ", " " + log);
    }

    public static String getAccountNameFromType(SmartPesa.AccountType accountType) {
        String s;
        switch (accountType) {
            case DEFAULT:
                s = "Default account";
                break;
            case SAVINGS:
                s = "Savings account";
                break;
            case CURRENT:
                s = "Current account";
                break;
            case CREDIT:
                s = "Credit account";
                break;
            default:
                s = "Default account";
                break;

        }
        return s;
    }


    public static SmartPesa.AccountType getAccountTypeFromInt (int i){

        SmartPesa.AccountType accountType;

        switch (i){
            case 0:
                accountType = DEFAULT;
                break;
            case 1:
                accountType = SmartPesa.AccountType.SAVINGS;
                break;
            case 2:
                accountType = SmartPesa.AccountType.CURRENT;
                break;
            case 3:
                accountType = SmartPesa.AccountType.CREDIT;
                break;
            default:
                accountType = DEFAULT;
                break;

        }
        return accountType;
    }

    public static int getAccountPositionFromEnum(SmartPesa.AccountType accountType) {

        int i;

        switch (accountType) {
            case DEFAULT:
                i = 0;
                break;
            case SAVINGS:
                i = 1;
                break;
            case CURRENT:
                i = 2;
                break;
            case CREDIT:
                i = 3;
                break;
            default:
                i = 0;
                break;

        }
        return i;
    }

//    public static String getTitleFromTransactionType(Context mContext, SmartPesaTransactionType transactionType) {
//
//        switch(transactionType){
//            case ACCOUNT_TRANSFER:
//                return mContext.getResources().getString(R.string.title_transfer_funds);
//            case SALE:
//                return mContext.getResources().getString(R.string.title_payment);
//            case BALANCE_INQUIRY:
//                return mContext.getResources().getString(R.string.title_inquiry);
//            case CASH_ADVANCE:
//                return mContext.getResources().getString(R.string.title_cash_advance);
//            case CASH_BACK:
//                return mContext.getResources().getString(R.string.title_cashback);
//            case CASH_DEPOSIT:
//                return mContext.getResources().getString(R.string.title_cash_in);
//            case WITHDRAWAL:
//                return mContext.getResources().getString(R.string.title_cashout);
//            case GENERAL_TRANSFER:
//                return mContext.getResources().getString(R.string.title_transfer_funds);
//            case PAYMENT:
//                return mContext.getResources().getString(R.string.title_services);
//            case REFUND:
//                return mContext.getString(R.string.title_refund);
//            case VOID:
//                return mContext.getString(R.string.title_void);
//            case REVERSAL:
//                return mContext.getString(R.string.title_reversal);
//            case LOYALTY:
//                return mContext.getString(R.string.title_loyalty_inquiry);
//            default:
//                return mContext.getResources().getString(R.string.title_payment);
//
//        }
//    }

    public static String combine(String string1, String string2, String delimiter) {
        return String.format("%s%s %s", string1, delimiter, string2);
    }

    public static String maskString(String target, String mask, int length) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < target.length(); i++) {
            if (i < length) {
                b.append(mask);
            } else {
                b.append(target.charAt(i));
            }
        }
        return b.toString();
    }

    public static String getLoyaltyTypeFromId(int type) {
        SmartPesa.RedeemableType redeemableType = SmartPesa.RedeemableType.fromEnumId(type);
        switch (redeemableType) {
            case UNKNOWN:
                return "UNKNOWN";
            case POOL:
                return "POOL";
            case E_COUPON:
                return "E-COUPON";
            case P_COUPON:
                return "P-COUPON";
            case OFFER:
                return "OFFER";
            default:
                return "UNKNOWN";

        }
    }

    public static String formatBigdecimal(BigDecimal bd) {
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd.toString();
    }

    public static String formatBalance(BigDecimal bd) {
        bd = bd.setScale(0, BigDecimal.ROUND_HALF_UP);
        return bd.toString();
    }
}
