package com.example.admin.familytime2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class MainActivity extends FragmentActivity {

    public static double calorieCount;
    public static double calorieToday;
    public static int stepToday;
    public static boolean ready;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new ReportFragment())
                    .commit();
        }

        Intent serviceIntent = new Intent(this, DataPullingService.class);
        startService(serviceIntent);
    }
}
