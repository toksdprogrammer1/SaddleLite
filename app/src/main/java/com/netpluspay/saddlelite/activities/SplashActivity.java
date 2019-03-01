package com.netpluspay.saddlelite.activities;

import com.netpluspay.saddlelite.R;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.Bind;
import butterknife.ButterKnife;
import okhttp3.HttpUrl;
import smartpesa.sdk.ServiceManager;
import smartpesa.sdk.ServiceManagerConfig;
import smartpesa.sdk.core.error.SpException;
import smartpesa.sdk.error.SpSessionException;
import smartpesa.sdk.log.SpLogLevel;
import smartpesa.sdk.models.merchant.VerifiedMerchantInfo;
import smartpesa.sdk.models.merchant.VerifyMerchantCallback;
import smartpesa.sdk.models.version.GetVersionCallback;
import smartpesa.sdk.models.version.Version;
import smartpesa.sdk.network.NetworkSettings;

public class SplashActivity extends AppCompatActivity {

    @Bind(R.id.progress_tv) TextView progressTv;
    @Bind(R.id.splash_pb) ProgressBar mProgressBar;
    ServiceManager mServiceManager;
    private static final int REQUEST_PERMISSION = 232;
    private static String amount = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);

        Intent myIntent = getIntent();
        amount = myIntent.getStringExtra("amount");

        //initialise SmartPesa ServiceManager
        ServiceManagerConfig config = new ServiceManagerConfig.Builder(getApplicationContext())
                .logLevel(SpLogLevel.DEBUG)
                .networkSettings(new NetworkSettings.Builder()
                        .url(new HttpUrl.Builder()
                                .host("netplus.prod.smartpesa.com")
                                .scheme("http")
                                .build())
                        .build())
                .build();
        ServiceManager.init(config);

        /*ServiceManagerConfig config = ServiceManagerConfig.newBuilder(getApplicationContext())
                .endPoint("secure.netpluspay.com")
                .withSsl()
                .build();

        ServiceManager.init(config);*/

        //use this to get serviceManager instance anywhere
        mServiceManager = ServiceManager.get(SplashActivity.this);

        //perform getVersion
        checkIsMerchantIsAvailable();
    }

    private void getVersion() {
        mProgressBar.setVisibility(View.VISIBLE);
        progressTv.setText("Perform getVersion");

        mServiceManager.getVersion(new GetVersionCallback() {

            public void onSuccess(Version version) {
                //mProgressBar.setVisibility(View.GONE);
                String major = String.valueOf(version.getMajor());
                String minor = String.valueOf(version.getMinor());
                String build = String.valueOf(version.getBuild());
                String a = "GetVersion success! Version: " + major + "." + minor + "." + build;
                progressTv.setText(a);
                //dialogBox("Merchant not logged in, proceed to LoginActivity...");
                checkAndroidPermissions();
            }

            @Override
            public void onError(SpException exception) {
                mProgressBar.setVisibility(View.GONE);
                progressTv.setText(exception.getMessage());
            }
        });
    }

    //to check if the merchant is already logged in
    private void checkIsMerchantIsAvailable() {

        //if the merchant is null, take him to login screen
        if(mServiceManager.getCachedMerchant() == null){
            getVersion();
        } else{
            //take the user into the Main Activity
            //dialogBox("Merchant is already logged in, proceed to MainActivity...");
            showMainActivity();
        }
    }

    public void dialogBox(final String message) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("Ok",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (message.equals("Merchant not logged in, proceed to LoginActivity...")) {
                            checkAndroidPermissions();
                        } else {
                            showMainActivity();
                        }
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    //check for permissions in android Marshmallow
    private void checkAndroidPermissions() {
        String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH};
        ActivityCompat.requestPermissions(SplashActivity.this,
                permissions,
                REQUEST_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showLoginActivity();
                        }
                    }, 300);
                } else {
                    Toast.makeText(this, R.string.bt_permission_denied, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void showMainActivity() {
        startActivity(new Intent(SplashActivity.this, MainActivity.class));
        finish();
    }

    private void showLoginActivity() {
        //startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        //finish();
        performVerifyMerchant("SEVENUP", "100", "1000");
    }

    private void performVerifyMerchant(String merchantCode, String operatorCode, String operatorPin) {
        //final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this);
       // progressDialog.setMessage("Performing verifyMerchant, please wait..");
        //progressDialog.setTitle("SmartPesa Login");
       // progressDialog.setCancelable(false);
       // progressDialog.show();

        mServiceManager.verifyMerchant(merchantCode, operatorCode, operatorPin, new VerifyMerchantCallback() {
            @Override
            public void onSuccess(VerifiedMerchantInfo verifiedMerchantInfo) {
                //progressDialog.dismiss();
                //showToast("Login Success");
                showMainActivity();
            }

            @Override
            public void onError(SpException exception) {
                if(exception instanceof SpSessionException) {
                    //progressDialog.dismiss();
                    //showToast("Session Expired, proceeding to getVersion i.e. SplashActivity");
                    //showSplashActivity();
                } else {
                    //progressDialog.dismiss();
                    //showToast(exception.getMessage());
                }
            }
        });
    }

}
