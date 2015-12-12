package com.example.admin.familytime2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity {

    public static double calorieCount;
    public static double calorieToday;
    public static int stepToday;
    public static boolean ready;
    public static int visits;

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

    public void checkinBtnClicked (View v) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter();

        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);

        registerReceiver(mReceiver, filter);
        adapter.startDiscovery();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                Toast.makeText(getApplicationContext(), "Device found" + device.getName(), Toast.LENGTH_SHORT).show(); //Device found
                if (device.getName().equals("SM-T325")) {
                    visits++;
                    ((TextView) findViewById(R.id.textView)).setText(
                            "Number of visits this month: " + MainActivity.visits + " times");
                }
            }
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Action connected", Toast.LENGTH_SHORT).show(); //Device is now connected
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Discovery finished", Toast.LENGTH_SHORT).show(); //Done searching
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Disconnect Requested", Toast.LENGTH_SHORT).show(); //Device is about to disconnect
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show(); //Device has disconnected
            }

        }
    };

}
