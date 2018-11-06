package naneos.analyze;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReceiveDataFromBleActivity extends AppCompatActivity {

    //layout
    private ListView listView;
    private Button btn_syncOnce;
    private Button btn_flush;
    private Button btn_scan;
    private Switch switch_keepSynced;
    private DrawerLayout mDrawerLayout;

    //data
    private List<DataObject> data;
    private ArrayAdapter<DataObject> adapter;
    private Date currentTime;

    //db
    private FirebaseFirestore db;



    private String lastreceived = "";    // lastreceived will hold the last valid advertising data received
    private String buffer = "";          // concatenate all valid stuff in here for parsing

    //android
    private Context mainContext = this;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* ******************* Layout ********************* */
        listView            = findViewById(R.id.lv_main);
        btn_syncOnce        = findViewById(R.id.btn_syncOnce);
        btn_flush           = findViewById(R.id.btn_flush);
        btn_scan            = findViewById(R.id.btn_scan);
        switch_keepSynced   = findViewById(R.id.switch_keepSynced);

        /* ******************* DB ********************* */
        db = FirebaseFirestore.getInstance();

        /* ******************* Data ********************* */
        data = new ArrayList<>();
        adapter = new ArrayAdapter<>(mainContext, android.R.layout.simple_list_item_1, data);
        listView.setAdapter(adapter);

        /* ******************* Handlers & Listeners ********************* */
        btn_syncOnce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setAllDataInFirestoreOnce();
            }
        });
        btn_flush.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                data.clear();
                adapter.notifyDataSetChanged();
            }
        });

        btn_scan.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){


               // mLEScanner.startScan(mScanCallback);
                Log.d("btn_scan", "Scanner started");

            }
        });

        /* ******************* DRAWER ********************* */
        mDrawerLayout = findViewById(R.id.drawer_layout);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        // Add code here to update the UI based on the item selected
                        // For example, swap UI fragments here

                        return true;
                    }
                });

        /* ******************* TOOLBAR ********************* */
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);



        Log.d("onCreate", "onCreate finished succesfully");
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onResume() {


        super.onResume();
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    private void setAllDataInFirestoreOnce() {
        for (int i = 0; i < data.size(); i++) {

            db.collection("DummyData").document(data.get(i).getDate().toString()).set(data.get(i))
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Firestore", "DocumentSnapshot written with ID: " + "?");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("Firestore", "Error adding document", e);
                        }
                    });
        }
    }


    private void setNewDataInFirestoreContinuously(DataObject obj) {
        db.collection("DummyData").document(obj.getDate().toString()).set(obj)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d("Firestore", "DocumentSnapshot written with ID: " + "?");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("Firestore", "Error adding document", e);
                    }
                });
    }

    // this version uses the new non-deprecated API
    private ScanCallback mScanCallback = new ScanCallback() {
        float LDSA = 0;
        float RH = 0;
        float Temperature = 0;
        float BatteryVoltage = 0;
        int errorcode = 0;
        int number = 0;
        int diameter = 0;

        @Override
        // callback when a BLE advertisement has been found
        public void onScanResult(int callbackType, final ScanResult result) {
            //Log.i("callbackType", String.valueOf(callbackType));
            //Log.i("result", result.toString());
            final BluetoothDevice device = result.getDevice();
            final android.bluetooth.le.ScanRecord scanRecord = result.getScanRecord();

            ((Activity) mainContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // use lastreceived and buffer to parse data properly
                    if (device == null /*|| device.getName() == null*/) {
                        Log.d("onScanResult", "device was null");
                        return;
                    }

                    /*
                    System.out.println("device:" + device.toString());
                    System.out.println("name_1:" + device.getName());
                    System.out.println("name_2:" + result.getScanRecord().getDeviceName());
                    System.out.println("address:" + device.getAddress());
                    System.out.println("bluetoothClass:" + device.getBluetoothClass());
                    System.out.println("______________");
                    */

                    //if(device.getAddress().contains("00:07:80")) {

                    // name appears to be
                    // Log.e("partector name", "deviceName: " + device.getName());
                    if (device.getName() != null && (device.getName().contains("Partector") || device.getName().contains("P2"))) {
                        //if(device.toString().equalsIgnoreCase("2C:AA:D3:53:27:6B")){
                        System.out.println("Partector Access Successful");
                        System.out.println("____________________________");
                        //System.out.println("device name is" + device.getName());  // this is "P2" which we should search instead of address!
                        //System.out.println(device.toString());  // this is mac address which we could store to connect to multiple P2s

                        // if(device.getName().contains("Partector")) {     // this would be much nicer, but doesn't work because it first needs a scan result = a connection to the P2
                        //System.out.println("found a P2....");

                        String msg = "ascii: ";

                        for (byte b : scanRecord.getBytes())
                            msg += (char) (b & 0xFF);

                        msg = msg.substring(16, 36);
                        System.out.println(msg);
                        if (!msg.equals(lastreceived)) {
                            // new data
                            //System.out.println("new data:" + msg);

                            // 1. remember it
                            lastreceived = msg;
                            //System.out.println("last received is" + lastreceived);
                            // 2. add it to input buffer
                            buffer = buffer + msg;
                            // 3. parse
                            String[] parts = buffer.split("S");
                            if (parts.length > 0) {
                                try {
                                    //System.out.println("buffer contains:" + buffer);
                                    for (int i = 0; i < parts.length - 1; i++) {
                                        //System.out.println("part " +i + ":" + parts[i]);
                                        //if(containsOnlyNumbers(parts[i]) == false)
                                        if (parts[i].length() == 0)
                                            continue;
                                        //mDeviceFragment.displayData(parts[i]);
                                        Log.d("DisplayData: ", parts[i]);
                                        switch (parts[i].charAt(0)) {
                                            case 'L':
                                                //System.out.println("String:" + parts[i]);
                                                //System.out.println("Parsing:" + parts[i].substring(1));
                                                if (parts[i].endsWith("l")) {

                                                    LDSA = Float.parseFloat(parts[i].substring(1, parts[i].length() - 1));
                                                    System.out.println("LDSA " + LDSA);
                                                    // old code for uploading data
                                                    //&name means variable name and $random means variable value
                                                    /*String url = "https://server-application.eu-gb.mybluemix.net/save?token=123456&name="+Temperature+"&ldsa="+LDSA;
                                                    System.out.println(url);
                                                    StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
                                                        @Override
                                                        public void onResponse(String response) {
                                                            System.out.println("response is " + response);
                                                        }
                                                    }, new Response.ErrorListener() {
                                                        @Override
                                                        public void onErrorResponse(VolleyError error) {
                                                            System.out.println("That didn't work");
                                                            System.out.println(error.toString());
                                                        }
                                                    });

                                                    queue.add(stringRequest);*/
                                                }
                                                break;
                                            case 'T':
                                                if (parts[i].endsWith("t")) {
                                                    Temperature = Float.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                    System.out.println("T " + Temperature);
                                                }
                                                break;
                                            case 'H':
                                                if (parts[i].endsWith("h")) {
                                                    RH = Float.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                    System.out.println("Humidity " + RH);
                                                }
                                                break;
                                            case 'B':
                                                if (parts[i].endsWith("b")) {
                                                    BatteryVoltage = Float.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                    System.out.println("Batt " + BatteryVoltage);
                                                }
                                                break;
                                            case 'E':
                                                errorcode = Integer.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                System.out.println("err " + errorcode);
                                                break;
                                            case 'D':
                                                diameter = Integer.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                System.out.println("diam " + diameter);
                                                break;
                                            case 'C':
                                                number = Integer.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                System.out.println("number " + number);
                                                break;
                                        }
                                        //System.out.println(parts[i]);
                                    }

                                } catch (NumberFormatException e) {
                                    System.out.println("number format exception caught" + buffer);
                                }
                            }
                            // 4. keep only last unparsed stuff
                            if (parts.length > 0)
                                buffer = parts[parts.length - 1];
                            //System.out.println("new buffer is " + buffer);
                            // if there was a trailing S, restore it (it was stripped by string.split
                            if (msg.charAt(msg.length() - 1) == 'S')
                                buffer = buffer + 'S';

                        } else {
                            //System.out.println("no new data:" + msg);
                        }

                    }
                }
            });
        }

    };




}
