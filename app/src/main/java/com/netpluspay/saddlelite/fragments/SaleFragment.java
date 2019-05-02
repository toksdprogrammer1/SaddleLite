package com.netpluspay.saddlelite.fragments;

import com.netpluspay.saddlelite.R;
import com.netpluspay.saddlelite.activities.PaymentProgressActivity;

import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import butterknife.Bind;
import butterknife.ButterKnife;
import com.netpluspay.saddlelite.database.DatabaseHelper;
import com.netpluspay.saddlelite.database.model.Cash;
import com.netpluspay.saddlelite.service.CallEndpointService;
import com.netpluspay.saddlelite.service.CallEndpointServiceForFailed;
import com.netpluspay.saddlelite.utils.LogTransactionOnServer;

import static android.content.Context.JOB_SCHEDULER_SERVICE;

public class SaleFragment extends Fragment {

    OnDataPass dataPasser;
    private static final int REQUEST_CODE = 100;
    @Bind(R.id.sales_continue_btn) Button payBtn;
    //@Bind(R.id.sales_back_btn) Button backBtn;
    @Bind(R.id.sales_cash_btn) Button cashBtn;
    //@Bind(R.id.amount_et) EditText amountEt;
    @Bind(R.id.amount_et) TextView amountEt;
    @Bind(R.id.vsHeader) ViewStub stub;

    private static String amount = "";
    private static String orderNo = "";
    private static String narrative = "";
    private static String email = "";
    private static String password = "";
    private static String merchantID = "";
    private DatabaseHelper db;
    private LogTransactionOnServer logTransactionOnServer;
    public static SaleFragment newInstance(){
        return new SaleFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        db = new DatabaseHelper(getActivity());
        amount = getArguments().getString("amount");
        orderNo = getArguments().getString("orderNo");
        narrative = getArguments().getString("narrative");
        email = getArguments().getString("email");
        password = getArguments().getString("password");
        merchantID = getArguments().getString("merchantID");

        View view = inflater.inflate(R.layout.fragment_sales, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (amount != null)
            amountEt.setText("₦" + amount);
        else
            amountEt.setText(amount);

        payBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payAmount();
            }
        });
        cashBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                payCash();
            }
        });

    }

    private void payAmount() {
        //String amount = amountEt.getText().toString();

        if(TextUtils.isEmpty(amount)){
            Toast.makeText(getActivity(), "Please enter amount", Toast.LENGTH_LONG).show();
        }else{
            AlertDialog.Builder alertDialogbuilder = new AlertDialog.Builder(getContext());
            alertDialogbuilder.setTitle("Card transaction");
            alertDialogbuilder
                    .setMessage("Do you want to complete card transaction for: " + amount)
                    .setCancelable(true)
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            proceedToPayment(Double.valueOf(amount));

                        }
                    });
            AlertDialog alertDialog = alertDialogbuilder.create();
            alertDialog.show();
            //proceedToPayment(Double.valueOf(amount));
        }
    }

    private void proceedToPayment(double amount) {
        Bundle bundle = new Bundle();
        bundle.putDouble(PaymentProgressActivity.KEY_AMOUNT, amount);
        bundle.putString(PaymentProgressActivity.KEY_EMAIL, email);
        bundle.putString(PaymentProgressActivity.KEY_PASSWORD, password);
        bundle.putString(PaymentProgressActivity.KEY_ORDER_NO, orderNo);
        Intent i = new Intent(getActivity(), PaymentProgressActivity.class);
        i.putExtras(bundle);
        getActivity().startActivityForResult(i, REQUEST_CODE);

    }

    private void payCash() {

        AlertDialog.Builder alertDialogbuilder = new AlertDialog.Builder(getContext());
        alertDialogbuilder.setTitle("Cash transaction");
        alertDialogbuilder
                .setMessage("Do you want to complete cash transaction for: " + amount)
                .setCancelable(true)
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if (isNetworkConnected()) {
                            new LogTransactionOnServer(email,password, orderNo, amount, "PENDING", 0, getActivity()).logCashTransaction();

                            //new connectNetplusEndpointTask().execute(orderNo, amount, narrative, email, merchantID);
                        }
                        else {
                            db.insertCash(orderNo, amount, narrative, email, merchantID, "PENDING");
                            Log.d("Db Size", db.getPendingCash().size() + "");

                            SharedPreferences preferences = PreferenceManager.
                                    getDefaultSharedPreferences(getActivity());

                            if (!preferences.getBoolean("firstRunComplete", false)) {
                                //schedule the job only once.
                                scheduleJobCallEndpoint();

                                //update shared preference
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putBoolean("firstRunComplete", true);
                                editor.commit();
                            }

                            if (!preferences.getBoolean("secondRunComplete", false)) {
                                //schedule the job only once.
                                schedulePendingJobCallEndpoint();

                                //update shared preference
                                SharedPreferences.Editor editor2 = preferences.edit();
                                editor2.putBoolean("secondRunComplete", true);
                                editor2.commit();
                            }


                        }
                        Intent intent = new Intent();
                        intent.putExtra("status", "SUCCESS");
                        intent.putExtra("transactionId", orderNo);
                        intent.putExtra("amount", amount);
                        passData(intent, -1);

                    }
                });
        AlertDialog alertDialog = alertDialogbuilder.create();
        alertDialog.show();


    }

    private void scheduleJobCallEndpoint(){
        JobScheduler jobScheduler = (JobScheduler)getActivity().getSystemService(JOB_SCHEDULER_SERVICE);

        ComponentName componentName = new ComponentName(getActivity().getPackageName(),
                CallEndpointService.class.getName());

        JobInfo jobInfo = new JobInfo.Builder(1, componentName)
                .setRequiredNetworkType(
                        JobInfo.NETWORK_TYPE_ANY)
                //.setBackoffCriteria(3000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .setPersisted(true).build();
        jobScheduler.schedule(jobInfo);
    }

    private void schedulePendingJobCallEndpoint(){
        JobScheduler jobScheduler = (JobScheduler)getActivity().getSystemService(JOB_SCHEDULER_SERVICE);

        ComponentName componentName = new ComponentName(getActivity().getPackageName(),
                CallEndpointServiceForFailed.class.getName());

        JobInfo jobInfo = new JobInfo.Builder(2, componentName)
                .setRequiredNetworkType(
                        JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(900000)
                .setPersisted(true).build();
        jobScheduler.schedule(jobInfo);
    }

    public boolean isNetworkConnected() {

        ConnectivityManager cm = (ConnectivityManager)getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isInternetAvailable() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com");
            //You can replace it with your name
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        android.support.v7.app.ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.title_sales));
        }
    }

    public interface OnDataPass {
        public void onDataPass(Intent data, int result);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        dataPasser = (OnDataPass) context;
    }

    public void passData(Intent data, int result) {
        dataPasser.onDataPass(data, result);
    }

    private class connectNetplusEndpointTask extends AsyncTask<String,Void,Intent> {

        int result;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (stub == null) {
                //stub = ((ViewStub) findViewById(R.id.vsHeader)).inflate();
                ((ProgressBar) stub.findViewById(R.id.progressBar1)).setIndeterminate(true);
            } else {
                stub.setVisibility(View.VISIBLE);
                cashBtn.setVisibility(View.INVISIBLE);
                payBtn.setVisibility(View.INVISIBLE);
            }
        }

        public connectNetplusEndpointTask(){
            this.result = 0;
        }

        /*
            doInBackground(Params... params)
                Override this method to perform a computation on a background thread.
         */

        protected Intent doInBackground(String...param){
            String order = param[0];
            String amount = param[1];
            String narrative = param[2];
            String email = param[3];
            String merchantID = param[4];
            Intent intent = new Intent();
            try{
                // Create URL
                URL endpoint = new URL("https://netpluspay.com/api/v1/recurrent/");
                HttpsURLConnection myConnection =
                        (HttpsURLConnection) endpoint.openConnection();
                myConnection.setRequestMethod("POST");
                myConnection.setUseCaches(false);
                myConnection.setDoOutput(true);
                myConnection.setConnectTimeout(10000);
                myConnection.setReadTimeout(10000);
                myConnection.setRequestProperty("Accept",
                        "application/json");
                myConnection.setRequestProperty("Content-Type",
                        "application/json");
                //myConnection.connect();
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("full_name", "");
                jsonParam.put("amount", Double.parseDouble(amount));
                jsonParam.put("currency", "NGN");
                jsonParam.put("narration", narrative);
                jsonParam.put("email", email);
                jsonParam.put("merchantid", merchantID);
                jsonParam.put("orderid", order);
                DataOutputStream os = new DataOutputStream(myConnection.getOutputStream());
                //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                os.writeBytes(jsonParam.toString());
                os.flush();
                os.close();
                try {
                    Log.d("STATUS", String.valueOf(myConnection.getResponseCode()));
                    Log.d("MSG", myConnection.getResponseMessage());
                } catch (Exception e) {
                    Log.d("Endpoint Error", e.getMessage());
                }
                try {

                        InputStream responseBody = myConnection.getInputStream();
                        //Log.d("Response body", responseBody.toString());
                        InputStreamReader responseBodyReader =
                                new InputStreamReader(responseBody, "UTF-8");
                        JsonReader jsonReader = new JsonReader(responseBodyReader);
                        jsonReader.beginObject(); // Start processing the JSON object
                        Log.d("Json", jsonReader.toString());

                        while (jsonReader.hasNext()) { // Loop through all keys
                            String key = jsonReader.nextName(); // Fetch the next key
                            if (key.equalsIgnoreCase("status_code")) { // Check if desired key
                                // Fetch the value as a String
                                //String value = jsonReader.nextString();
                                // Error handling code goes here
                                Log.d("resp", jsonReader.nextString());
                                result = 2;
                                intent.putExtra("status", "FAILED");
                                intent.putExtra("transactionId", order);
                                intent.putExtra("amount", amount);
                                //intent.putExtra("exception", value);
                                //db.insertCash(order, amount, narrative, email, merchantID, "PENDING");
                                break; // Break out of the loop
                            }
                            if (key.equalsIgnoreCase("error")) { // Check if desired key
                                // Fetch the value as a String
                                //String value = jsonReader.nextString();
                                // Error handling code goes here
                                result = 1;
                                intent.putExtra("status", "FAILED");
                                intent.putExtra("transactionId", order);
                                intent.putExtra("amount", amount);
                                //intent.putExtra("exception", value);
                                //db.insertCash(order, amount, narrative, email, merchantID, "PENDING");
                                break; // Break out of the loop
                            }
                            if (key.equalsIgnoreCase("status")) { // Check if desired key
                                // Fetch the value as a String
                                String value = jsonReader.nextString();
                                if(value.equalsIgnoreCase("FAILED")){
                                    result = 1;
                                    intent.putExtra("status", "FAILED");
                                    intent.putExtra("transactionId", order);
                                    intent.putExtra("amount", amount);
                                   // db.insertCash(order, amount, narrative, email, merchantID, "PENDING");
                                    break; // Break out of the loop
                                }
                                intent.putExtra("status", "SUCCESS");
                                continue; // Break out of the loop
                            }
                            else if (key.equalsIgnoreCase("orderid")) { // Check if desired key
                                // Fetch the value as a String
                                String value = jsonReader.nextString();
                                intent.putExtra("transactionId", value);
                                continue; // Break out of the loop
                            }
                            else if (key.equalsIgnoreCase("amount")) { // Check if desired key
                                // Fetch the value as a String
                                String value = jsonReader.nextString();
                                intent.putExtra("amount", value);
                                continue; // Break out of the loop
                            }
                            else {
                                jsonReader.skipValue(); // Skip values of other keys
                                result = -1;
                            }
                        }
                        jsonReader.close();
                        myConnection.disconnect();



                }
                catch (Exception e){
                    db.insertCash(order, amount, narrative, email, merchantID, "PENDING");
                    Log.d("Error", e.getMessage());

                }
            }catch(Exception e){ // Catch the download exception
                db.insertCash(order, amount, narrative, email, merchantID, "PENDING");
                e.printStackTrace();
            }
            return intent;
        }

        /*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
        @Override
        protected void onPostExecute(Intent intent){

            //imageView.setImageBitmap(result);
            stub.setVisibility(View.GONE);
            cashBtn.setVisibility(View.VISIBLE);
            payBtn.setVisibility(View.VISIBLE);
            if (result == -1) {

                db.insertCash(orderNo, amount, narrative, email, merchantID, "SUCCESS");
                logTransactionOnServer = new LogTransactionOnServer(email,password, orderNo, amount, "SUCCESS", 0, getActivity());
                logTransactionOnServer.logCashTransaction();
                passData(intent, -1);
            }
            else if (result == 2){
                db.insertCash(orderNo, amount, narrative, email, merchantID, "FAILED");
                logTransactionOnServer = new LogTransactionOnServer(email,password, orderNo, amount, "FAILED", 0, getActivity());
                logTransactionOnServer.logCashTransaction();
            }
            else
                db.insertCash(orderNo, amount, narrative, email, merchantID, "PENDING");
                passData(intent, 1);
        }
    }
}
