package com.netpluspay.saddlelite.activities;

import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.netpluspay.saddlelite.R;
import com.netpluspay.saddlelite.fragments.SaleFragment;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;
import android.util.Log;

import butterknife.Bind;
import butterknife.ButterKnife;
import smartpesa.sdk.ServiceManager;
import smartpesa.sdk.ServiceManagerConfig;
import smartpesa.sdk.error.SpException;
import smartpesa.sdk.error.SpSessionException;
import smartpesa.sdk.models.merchant.VerifiedMerchantInfo;
import smartpesa.sdk.models.merchant.VerifyMerchantCallback;
import smartpesa.sdk.models.operator.LogoutCallback;
import smartpesa.sdk.models.version.GetVersionCallback;
import smartpesa.sdk.models.version.Version;

public class MainActivity extends AppCompatActivity implements SaleFragment.OnDataPass {

    public static final String LOG_TAG = "MainActivity";
    private static final int REQUEST_CODE = 100;
    private static final long MENU_LOGOUT = 2;
    private static final long MENU_SALE = 1;
    private static final int REQUEST_PERMISSION = 232;
    private static String amount = "";
    private static String orderNo = "";
    private static String narrative = "";
    private static String email = "";
    private static String merchantID = "";
    @Bind(R.id.toolbar) Toolbar mToolbar;

    Drawer drawer;
    ServiceManager mServiceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);


        Intent myIntent = getIntent();
        amount = myIntent.getStringExtra("amount");
        orderNo = myIntent.getStringExtra("orderNo");
        narrative = myIntent.getStringExtra("narrative");
        email = myIntent.getStringExtra("email");
        merchantID = myIntent.getStringExtra("merchantID");

        //initialise SmartPesa ServiceManager
        ServiceManagerConfig config = ServiceManagerConfig.newBuilder(getApplicationContext())
                .endPoint("netplus.prod.smartpesa.com")
                .withoutSsl()
                .build();
        ServiceManager.init(config);

        //use this to get serviceManager instance anywhere
        mServiceManager = ServiceManager.get(MainActivity.this);



        //get serviceManager instance
        //mServiceManager = ServiceManager.get(MainActivity.this);

        //setup toolbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        //setUpDrawer();

        if (savedInstanceState == null) {
            // on first time display view for first nav item
           displayFragment(MENU_SALE);
        }
        //perform getVersion
        checkIsMerchantIsAvailable();
    }

    private void setUpDrawer() {
        //create the navigation drawer
        drawer = new DrawerBuilder()
                .withActivity(this)
                .withToolbar(mToolbar)
                .withActionBarDrawerToggleAnimated(true)
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        displayFragment(drawerItem.getIdentifier());
                        return false;
                    }
                })
                .build();

        PrimaryDrawerItem Sale = new PrimaryDrawerItem().withName("Sale").withIdentifier(MENU_SALE);
        PrimaryDrawerItem logout = new PrimaryDrawerItem().withName("Logout").withIdentifier(MENU_LOGOUT);
        drawer.addItem(Sale);
        drawer.addItem(logout);
    }

    private void displayFragment(long menuId) {
        Fragment fragment = null;
        switch ((int) menuId){
            case (int) MENU_SALE:
                fragment = SaleFragment.newInstance();
                break;
            case (int) MENU_LOGOUT:
                logoutUser();
                break;
        }

        if (fragment != null) {
            Bundle bundle = new Bundle();
            bundle.putString("amount", amount);
            bundle.putString("orderNo", orderNo);
            bundle.putString("narrative", narrative);
            bundle.putString("email", email);
            bundle.putString("merchantID", merchantID);
            FragmentManager fragmentManager = getSupportFragmentManager();
            fragment.setArguments(bundle);
            fragmentManager.beginTransaction().replace(R.id.container_body, fragment).commitAllowingStateLoss();
        }
    }

    private void logoutUser() {
        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Logging out user, please wait");
        progressDialog.setCancelable(false);
        progressDialog.show();
        mServiceManager.logout(new LogoutCallback() {
            @Override
            public void onSuccess(Boolean aBoolean) {
                progressDialog.dismiss();
                //on successful login take user to the splash screen
                finish();
                startActivity(new Intent(MainActivity.this, SplashActivity.class));
            }

            @Override
            public void onError(SpException exception) {
                //implement error while logout here

            }

        });
    }

    private void getVersion() {
        //mProgressBar.setVisibility(View.VISIBLE);
        //progressTv.setText("Perform getVersion");

        mServiceManager.getVersion(new GetVersionCallback() {

            public void onSuccess(Version version) {
                //mProgressBar.setVisibility(View.GONE);
                String major = String.valueOf(version.getMajor());
                String minor = String.valueOf(version.getMinor());
                String build = String.valueOf(version.getBuild());
                String a = "GetVersion success! Version: " + major + "." + minor + "." + build;
                //progressTv.setText(a);
                //dialogBox("Merchant not logged in, proceed to LoginActivity...");
                checkAndroidPermissions();
            }

            @Override
            public void onError(SpException exception) {
                //mProgressBar.setVisibility(View.GONE);
                //progressTv.setText(exception.getMessage());
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
            //showMainActivity();
        }
    }



    //check for permissions in android Marshmallow
    private void checkAndroidPermissions() {
        String[] permissions = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH};
        ActivityCompat.requestPermissions(MainActivity.this,
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
        //startActivity(new Intent(SplashActivity.this, MainActivity.class));
        //finish();
    }

    private void showLoginActivity() {
        //startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        //finish();
        performVerifyMerchant("webmall", "100", "1234");
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
                //showMainActivity();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //if (requestCode == TRANSACTION_REQUEST_CODE ) {
        if (requestCode == REQUEST_CODE ) {
            if (data == null) {

                return;
            } else if (resultCode == RESULT_OK) {
                // Parse successful transaction data.
                //TransactionResult result = SpConnect.parseSuccessTransaction(data);
                //onSuccess(result);
                Log.d(LOG_TAG, "I got here for success");
                onDataPass(data, -1);
            } else {
                Log.d(LOG_TAG, "I got here for payment error");
                onDataPass(data, 1);
                // Parse failed transaction data.
                //TransactionError error = SpConnect.parseErrorTransaction(data);
                //onFail(error);
            }
        }
    }



    @Override
    public void onDataPass(Intent data, int result) {
        ///Log.d("LOG","hello " + data);
        //Intent intent = new Intent();
        //intent.putExtra("EditText_Value", editText.getText().toString().trim());
        if (result == 1) {
            data.putExtra("amount", amount);
            setResult(RESULT_CANCELED, data);
        }
        else {
            data.putExtra("amount", amount);
            setResult(RESULT_OK, data);
        }
        finish();
    }

}
