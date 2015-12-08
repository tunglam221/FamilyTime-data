package com.example.admin.familytime2;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class DataPullingService extends IntentService {
    private static final String TAG = "UsingThingspeakAPI";
    private static final String THINGSPEAK_CHANNEL_ID = "70747";
    private static final String THINGSPEAK_API_KEY = "VR1IXUUDQ3IRCELV";
    private static final String THINGSPEAK_API_KEY_STRING = "api_key";

    /* Be sure to use the correct fields for your own app*/
    private static final String THINGSPEAK_CALORIE = "field1";
    private static final String THINGSPEAK_CALORIE_TODAY = "field2";
    private static final String THINGSPEAK_STEP_TODAY = "field3";

    private static final String THINGSPEAK_TIME = "created_at";
    private static final String THINGSPEAK_UPDATE_URL = "https://api.thingspeak.com/update?";
    private static final String THINGSPEAK_CHANNEL_URL = "https://api.thingspeak.com/channels/";
    private static final String THINGSPEAK_FEEDS_LAST = "/feeds/last?";

    private String timeStamp = "";



    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */


    public DataPullingService() {
        super("DataPullingService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        StringBuilder stringBuilder;
        while (true) {
            try {
                URL url = new URL(THINGSPEAK_CHANNEL_URL + THINGSPEAK_CHANNEL_ID +
                        THINGSPEAK_FEEDS_LAST + THINGSPEAK_API_KEY_STRING + "=" +
                        THINGSPEAK_API_KEY + "");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    stringBuilder = new StringBuilder();
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line).append("\n");
                    }
                    bufferedReader.close();
                    parseJSON(stringBuilder.toString());
                } finally {
                    urlConnection.disconnect();
                }
                Thread.sleep(3000);
            } catch (Exception e) {
                Log.e("ERROR", e.getMessage(), e);
            }
        }
    }

    protected void parseJSON(String str) {
        Log.d("JSON String", str);
        try {
            JSONObject channel = (JSONObject) new JSONTokener(str).nextValue();
            MainActivity.calorieCount = channel.getDouble(THINGSPEAK_CALORIE);
            MainActivity.calorieToday = channel.getDouble(THINGSPEAK_CALORIE_TODAY);
            MainActivity.stepToday = channel.getInt(THINGSPEAK_STEP_TODAY);
            String time = channel.getString(THINGSPEAK_TIME);

            Log.d("Time stamp", time + "/" + timeStamp);
            if (!time.equals(timeStamp)) {
                MainActivity.ready = true;
                timeStamp = time;
            }
        } catch (Exception e) {
            Log.e("Error", e.getMessage(), e);
        }

    }

}
