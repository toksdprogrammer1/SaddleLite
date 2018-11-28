package com.netpluspay.saddlelite.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.netpluspay.saddlelite.R;
import com.netpluspay.saddlelite.activities.CashActivity;
import com.netpluspay.saddlelite.database.DatabaseHelper;
import com.netpluspay.saddlelite.database.model.Cash;
import com.netpluspay.saddlelite.utils.LogTransactionOnServer;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class CallEndpointService extends JobService {

    private DatabaseHelper db;
    // Notification channel ID.
    private static final String PRIMARY_CHANNEL_ID =
            "primary_notification_channel";
    // Notification manager.
    NotificationManager mNotifyManager;
    public String status = "FAILED";
    LogTransactionOnServer logTransactionOnServer;
    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        // Create the notification channel.
        createNotificationChannel();

        db = new DatabaseHelper(this);
        Log.d("Calling Db Size", db.getPendingCash().size()+"");

        for (Cash cash : db.getPendingCash()){
            Log.d("about to send", cash.getId() + " " + cash.getAmount());
            new connectNetplusEndpointTask(this, cash).execute(cash.getOrderNo(), cash.getAmount(),
                    cash.getNarrative(), cash.getEmail(), cash.getMerchantId());
        }

       // if (allSuccessfull) {

            SharedPreferences preferences = PreferenceManager.
                    getDefaultSharedPreferences(this);

            //update shared preference
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean("firstRunComplete", false);
            editor.commit();
            jobFinished(jobParameters, false);
            return false;
       // }
       // else
       //     jobFinished(jobParameters, true);
       //     return true;
    }

    /**
     * Creates a Notification channel, for OREO and higher.
     */
    public void createNotificationChannel() {

        // Create a notification manager object.
        mNotifyManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Notification channels are only available in OREO and higher.
        // So, add a check on SDK version.
        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O) {

            // Create the NotificationChannel with all the parameters.
            NotificationChannel notificationChannel = new NotificationChannel
                    (PRIMARY_CHANNEL_ID,
                            getString(R.string.job_service_notification),
                            NotificationManager.IMPORTANCE_HIGH);

            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription
                    (getString(R.string.notification_channel_description));

            mNotifyManager.createNotificationChannel(notificationChannel);
        }
    }

    private String getEmail(){

        SharedPreferences preferences = PreferenceManager.
                getDefaultSharedPreferences(this);

       return preferences.getString("email", "");

    }

    private String getPassword(){

        SharedPreferences preferences = PreferenceManager.
                getDefaultSharedPreferences(this);

        return preferences.getString("password", "");

    }

    private class connectNetplusEndpointTask extends AsyncTask<String,Void,String> {

        Context context;
        Cash cash;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        public connectNetplusEndpointTask(Context context, Cash cash){
            this.context= context;
            this.cash = cash;
        }

        /*
            doInBackground(Params... params)
                Override this method to perform a computation on a background thread.
         */

        protected String doInBackground(String...param) {
            String order = param[0];
            String amount = param[1];
            String narrative = param[2];
            String email = param[3];
            String merchantID = param[4];

                Log.d("Sendingg", order + " " + amount);
                try {
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
                                status = "FAILED";

                                break; // Break out of the loop
                            }
                            if (key.equalsIgnoreCase("status")) { // Check if desired key
                                // Fetch the value as a String
                                String value = jsonReader.nextString();
                                if (value.equalsIgnoreCase("FAILED")) {
                                    status = "FAILED";

                                    break; // Break out of the loop
                                } else
                                    status = "SUCCESS";
                            } else {
                                jsonReader.skipValue(); // Skip values of other keys
                                status = "FAILED";
                            }
                        }
                        jsonReader.close();
                        myConnection.disconnect();


                    } catch (Exception e) {

                        Log.d("Error", e.getMessage());

                    }
                } catch (Exception e) { // Catch the download exception
                    e.printStackTrace();
                }
                return status;
            }


        /*
            onPostExecute(Result result)
                Runs on the UI thread after doInBackground(Params...).
         */
        @Override
        protected void onPostExecute(String status2){

            status = status2;
            if (status.equalsIgnoreCase("SUCCESS")){
                cash.setStatus("SUCCESS");
                db.updateCash(cash);
                logTransactionOnServer = new LogTransactionOnServer(getEmail(),getPassword(), cash.getOrderNo(), cash.getAmount(), "SUCCESS", 0);
                logTransactionOnServer.logCashTransaction();
            }
            else {
                logTransactionOnServer = new LogTransactionOnServer(getEmail(),getPassword(), cash.getOrderNo(), cash.getAmount(), "FAILED", 0);
                logTransactionOnServer.logCashTransaction();
            }
            //Set up the notification content intent to launch the app when clicked
            PendingIntent contentPendingIntent = PendingIntent.getActivity
                    (context, 0, new Intent(context, CashActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder
                    (context, PRIMARY_CHANNEL_ID)
                    .setContentTitle(getString(R.string.job_service_notification))
                    .setContentText("Order No: " + cash.getOrderNo() + " Amount: " + cash.getAmount() + "\n" + "Successfully Synchronised" )
                    .setContentIntent(contentPendingIntent)
                    .setSmallIcon(R.drawable.notificationicon)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true);

            mNotifyManager.notify(0, builder.build());
            Log.d("Completed", status);

        }
    }


    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }
}
