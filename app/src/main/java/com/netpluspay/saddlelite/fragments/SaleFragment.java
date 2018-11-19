package com.netpluspay.saddlelite.fragments;

import com.netpluspay.saddlelite.R;
import com.netpluspay.saddlelite.activities.PaymentProgressActivity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import butterknife.Bind;
import butterknife.ButterKnife;

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
    private static String merchantID = "";
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
        amount = getArguments().getString("amount");
        orderNo = getArguments().getString("orderNo");
        narrative = getArguments().getString("narrative");
        email = getArguments().getString("email");
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
            proceedToPayment(Double.valueOf(amount));
        }
    }

    private void proceedToPayment(double amount) {
        Bundle bundle = new Bundle();
        bundle.putDouble(PaymentProgressActivity.KEY_AMOUNT, amount);
        Intent i = new Intent(getActivity(), PaymentProgressActivity.class);
        i.putExtras(bundle);
        getActivity().startActivityForResult(i, REQUEST_CODE);

    }

    private void payCash() {
        new connectNetplusEndpointTask().execute(orderNo, amount, narrative, email, merchantID);
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
                        InputStreamReader responseBodyReader =
                                new InputStreamReader(responseBody, "UTF-8");
                        JsonReader jsonReader = new JsonReader(responseBodyReader);
                        jsonReader.beginObject(); // Start processing the JSON object

                        while (jsonReader.hasNext()) { // Loop through all keys
                            String key = jsonReader.nextName(); // Fetch the next key
                            if (key.equalsIgnoreCase("error")) { // Check if desired key
                                // Fetch the value as a String
                                //String value = jsonReader.nextString();
                                // Error handling code goes here
                                result = 1;
                                intent.putExtra("status", "Failed");
                                intent.putExtra("transactionId", order);
                                intent.putExtra("amount", amount);
                                //intent.putExtra("exception", value);
                                break; // Break out of the loop
                            }
                            if (key.equalsIgnoreCase("status")) { // Check if desired key
                                // Fetch the value as a String
                                String value = jsonReader.nextString();
                                if(value.equalsIgnoreCase("FAILED")){
                                    result = 1;
                                    intent.putExtra("status", "Failed");
                                    intent.putExtra("transactionId", order);
                                    intent.putExtra("amount", amount);
                                    break; // Break out of the loop
                                }
                                intent.putExtra("status", value);
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

                    Log.d("Error", e.getMessage());

                }
            }catch(Exception e){ // Catch the download exception
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
            if (result == -1)
                passData(intent, -1);
            else
                passData(intent, 1);
        }
    }
}
