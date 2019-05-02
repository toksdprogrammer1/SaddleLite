package com.netpluspay.saddlelite.utils;


import android.content.Context;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import com.netpluspay.saddlelite.R;
import com.netpluspay.saddlelite.database.model.Cash;
import com.netpluspay.saddlelite.database.DatabaseHelper;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class LogTransactionOnServer {

    public String login;
    public String pasword;
    public String orderNo;
    public String amount;
    public String status;
    public String token;
    public int type;
    private DatabaseHelper db;

    public LogTransactionOnServer(String login, String password, String orderNo, String amount, String status, int type, Context context) {
        this.login = login;
        this.pasword = password;
        this.orderNo = orderNo;
        this.amount = amount;
        this.status = status;
        this.token = "";
        this.type = type;
        db = new DatabaseHelper(context);
    }

    public void logCashTransaction(){
        //Log.d("Email", login);
        //Log.d("Password", pasword);
        new CallToGetTokenTask(type).execute(login, pasword);

    }



    public static SSLContext sc () throws Exception{

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return null;
            }
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());

        return sc;
    }

    public static HostnameVerifier allHostsValid (){

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        return allHostsValid;
    }

    private class CallToGetTokenTask extends AsyncTask<String,Void,String> {

        int type;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        public CallToGetTokenTask(int type){
            this.type = type;
        }

        /*
            doInBackground(Params... params)
                Override this method to perform a computation on a background thread.
         */

        protected String doInBackground(String...param) {

            String login = param[0];
            String pasword = param[1];
            Log.d("Calling", "To get token");
            Log.d("Email", login);
            Log.d("Password", pasword);
            try {
                // Create URL
                URL endpoint = new URL("https://saddleng.com/api/auth");
                HttpsURLConnection.setDefaultSSLSocketFactory(sc().getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid());
                HttpsURLConnection myConnection =
                        (HttpsURLConnection) endpoint.openConnection();
                myConnection.setRequestMethod("POST");
                myConnection.setUseCaches(false);
                myConnection.setDoOutput(true);
                myConnection.setConnectTimeout(10000);
                myConnection.setReadTimeout(10000);
                //myConnection.setRequestProperty("Accept",
                //        "application/json");
                myConnection.setRequestProperty("Content-Type",
                        "application/json");
                //myConnection.connect();
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("login", login);
                jsonParam.put("password", pasword);

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
                    //Log.d("BodyReader", jsonReader.toString());
                    jsonReader.beginObject(); // Start processing the JSON object
                    int count =0;
                    while (jsonReader.hasNext()) { // Loop through all keys
                        Log.d("Key", jsonReader.nextName());
                        token = jsonReader.nextString();
                        if (count == 3){
                            break;
                        }
                        //Log.d("Key", jsonReader.nextName());
                        //Log.d("Value", jsonReader.nextString());
                        //String key = jsonReader.nextName(); // Fetch the next key

                        //if (key.equalsIgnoreCase("token")) { // Check if desired key
                        //    token = jsonReader.nextString();
                        //    break; // Break out of the loop
                       // }

                    }
                    jsonReader.close();
                    myConnection.disconnect();


                } catch (Exception e) {
                    //e.printStackTrace();
                    Log.d("Error", e.getMessage());

                }
            } catch (Exception e) { // Catch the download exception
                e.printStackTrace();
            }
            return token;
        }


        /*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
        @Override
        protected void onPostExecute(String token){

            Log.d("Token", token);
            if (type == 1){
                new LogCardTransaction().execute(token);
            }
            else
                new LogCashTransaction().execute(token);
            //
        }
    }

    private class LogCardTransaction extends AsyncTask<String,Void,String> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        public LogCardTransaction(){

        }

        /*
            doInBackground(Params... params)
                Override this method to perform a computation on a background thread.
         */

        protected String doInBackground(String...param) {

            String response ="";
            String token = param[0];
            Log.d("Connecting", "LogCardTransaction");
            try {
                // Create URL
                URL endpoint = new URL("https://saddleng.com/api/v2/logSaddleLiteCardTransaction");
                HttpsURLConnection.setDefaultSSLSocketFactory(sc().getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid());
                HttpsURLConnection myConnection =
                        (HttpsURLConnection) endpoint.openConnection();
                myConnection.setRequestMethod("POST");
                myConnection.setUseCaches(false);
                myConnection.setDoOutput(true);
                myConnection.setConnectTimeout(10000);
                myConnection.setReadTimeout(10000);
                myConnection.setRequestProperty("Authorization",
                        "Bearer " + token);
                myConnection.setRequestProperty("Content-Type",
                        "application/json");
                //myConnection.connect();
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("transaction_id", orderNo);
                jsonParam.put("amount", amount);
                jsonParam.put("status", status);

                DataOutputStream os = new DataOutputStream(myConnection.getOutputStream());
                //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                os.writeBytes(jsonParam.toString());
                os.flush();
                os.close();
                try {
                    response = String.valueOf(myConnection.getResponseCode());
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
                        Log.d("Name", jsonReader.nextName());
                        Log.d("msg", jsonReader.nextString());
                        /*String key = jsonReader.nextName(); // Fetch the next key
                        if (key.equalsIgnoreCase("status")) { // Check if desired key
                            Log.i("status", jsonReader.nextString());
                            break; // Break out of the loop
                        }*/

                    }
                    jsonReader.close();
                    myConnection.disconnect();


                } catch (Exception e) {

                    Log.d("Error", e.getMessage());

                }
            } catch (Exception e) { // Catch the download exception
                e.printStackTrace();
            }
            return response;
        }


        /*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
        @Override
        protected void onPostExecute(String response){

            if (!response.equalsIgnoreCase("200") || !response.equalsIgnoreCase("201")){
                db.insertCash(orderNo, amount, "", "", "", "PENDING");
                Log.d("FALED", "Unable to Logged to the server");
            }
            else
                Log.d("Completed", "Logged to the server");

        }
    }


    private class LogCashTransaction extends AsyncTask<String,Void,String> {


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        public LogCashTransaction(){

        }

        /*
            doInBackground(Params... params)
                Override this method to perform a computation on a background thread.
         */

        protected String doInBackground(String...param) {

            String response = "";
            String token = param[0];
            Log.d("Token2", "Bearer " + token);
            Log.d("OrderNo", orderNo);
            Log.d("Amount", amount);
            Log.d("status", status);
            Log.d("Connecting", "LogCashTransaction");
            try {
                // Create URL
                URL endpoint = new URL("https://saddleng.com/api/v2/logSaddleLiteCashTransaction");
                HttpsURLConnection.setDefaultSSLSocketFactory(sc().getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid());
                HttpsURLConnection myConnection =
                        (HttpsURLConnection) endpoint.openConnection();
                myConnection.setRequestMethod("POST");
                myConnection.setUseCaches(false);
                myConnection.setDoInput(true);
                myConnection.setDoOutput(true);
                //myConnection.setConnectTimeout(10000);
                //myConnection.setReadTimeout(10000);
                myConnection.setRequestProperty("Authorization",
                        "Bearer " + token);
                myConnection.setRequestProperty("Content-Type",
                        "application/json");
                myConnection.setRequestProperty("Accept",
                       "application/json");
                //myConnection.connect();
                JSONObject jsonParam = new JSONObject();
                jsonParam.put("transaction_id", orderNo);
                jsonParam.put("amount", amount);
                jsonParam.put("status", status);

                DataOutputStream os = new DataOutputStream(myConnection.getOutputStream());
                //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                os.writeBytes(jsonParam.toString());
                os.flush();
                os.close();
                try {
                    response = String.valueOf(myConnection.getResponseCode());
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
                        Log.d("Name", jsonReader.nextName());
                        Log.d("msg", jsonReader.nextString());
                        /*String key = jsonReader.nextName(); // Fetch the next key
                        if (key.equalsIgnoreCase("status")) { // Check if desired key
                            Log.i("status", jsonReader.nextString());
                            break; // Break out of the loop
                        }*/

                    }
                    jsonReader.close();
                    myConnection.disconnect();


                } catch (Exception e) {
                    //e.printStackTrace();
                    Log.d("Error", e.getMessage());

                }
            } catch (Exception e) { // Catch the download exception
                e.printStackTrace();
            }
            return response;
        }


        /*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
        @Override
        protected void onPostExecute(String response){

            if (!response.equalsIgnoreCase("200") || !response.equalsIgnoreCase("201")){
                db.insertCash(orderNo, amount, "", "", "", "PENDING");
                Log.d("FALED", "Unable to Logged to the server");
            }
            else
                Log.d("Completed", "Logged to the server");

        }
    }
}
