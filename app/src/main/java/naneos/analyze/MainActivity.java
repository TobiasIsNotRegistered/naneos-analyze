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
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    //TODO: Check Location Permission Requests, doesn't request atm if its off
    //TODO: Check if network / wlan is enabled, needed for Firebase Sync!

    //layout
    private ListView listView;
    private Button btn_syncOnce;
    private Button btn_flush;
    private Button btn_scan;
    private Switch switch_keepSynced;
    private DrawerLayout mDrawerLayout;
    private TextView amountOfLocalData;

    //data
    private List<DataObject> dataList = new ArrayList<DataObject>();
    private ArrayAdapter<DataObject> adapter;

    //db
    private FirebaseFirestore db;
    private Timer timer;
    private TimerTask timerTask;


    //Broadcastreceiver used to listen on changes in Bluetooth & Location permissions
    //TODO: Implement this!
    final BroadcastReceiver mReceiver = new BroadcastReceiver(this);


    //bluetooth
    private static final int REQUEST_ENABLE_BT = 101; // request code to enable bluetooth
    private static final int REQUEST_ENABLE_FINE_LOCATION = 201;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    boolean isScanning;

    private Context mainContext = this;


    /********* LIFECYCLE ************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /* ******************* Layout ********************* */
        listView = findViewById(R.id.lv_main);
        btn_syncOnce = findViewById(R.id.btn_syncOnce);
        btn_flush = findViewById(R.id.btn_flush);
        btn_scan = findViewById(R.id.btn_scan);
        switch_keepSynced = findViewById(R.id.switch_keepSynced);
        amountOfLocalData = findViewById(R.id.textView_amountOfDataObjects);

        adapter = new ArrayAdapter<>(mainContext, android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);

        String formattedText = getString(R.string.amountOfDataObjects, 0);
        amountOfLocalData.setText(formattedText);

        /* ******************* DB ********************* */
        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        db.setFirestoreSettings(settings);


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


        /* ************* INTERACTIONS ************************ */

        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    ((Activity) mainContext).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    toggleScan();
                }
            }
        });

        btn_syncOnce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

            }
        });

        switch_keepSynced.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    startSyncingWithTimer();
                } else {
                    stopSyncingWithTimer();
                }
            }
        });

        /* ******* TIMER FOR DATA-SYNC ************* */
        timerTask = new TimerTask() {
            @Override
            public void run() {
                SyncData();
            }
        };


        /*  INIT */

        checkPermissions();
    }

    public void toggleScan() {
        //user is not scanning and mleScanner exists
        if (!isScanning) {
            mLEScanner.startScan(mScanCallback);
            isScanning = true;
            btn_scan.setText("STOP");
            Log.d("btn_scan", "Scanner started");
            //user is scanning and wants to stop and scanner exists
        } else if (isScanning) {
            mLEScanner.stopScan(mScanCallback);
            mLEScanner = null;
            isScanning = false;
            btn_scan.setText("SCAN");
            //user wants to scan but mleScanner was disabled (user disabled bluetooth)
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT) {
            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_SHORT);


            toggleScan();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //TODO: implemented logic when back button is pressed on main screen
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
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private boolean getBluetoothScanner() {
        final BluetoothManager bluetoothManager = (BluetoothManager) mainContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        filters = new ArrayList<>();

        if (!mBluetoothAdapter.isEnabled()) {

            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity) mainContext).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        return mBluetoothAdapter.isEnabled();
    }


    // called in onCreate to setup bluetooth
    private boolean checkPermissions() {

        Log.d("Bluetooth Initiate", "Initiate started!");
        // Use this check to determine whether BLE is supported on the device.
        // Then you can selectively disable BLE-related features.
        if (!mainContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d("checkPermissions", "Bluetooth LE seemingly not supported");
            return false;
        } else {
            Log.d("checkPermissions", "Bluetooth Le seems to be supported.");
        }


        if (!hasLocationPermissions()) {
            Log.d("checkPermissions", "Location requested");
            requestLocationPermission();
        } else {
            Log.d("checkPermissions", "Location permission successful");
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(mainContext, "Please enable a network connection to proceed - syncing data is disabled", Toast.LENGTH_LONG).show();
        } else {
            Log.d("checkPermissions", "Network is available");
        }


        final BluetoothManager bluetoothManager = (BluetoothManager) mainContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        filters = new ArrayList<>();


        if (!mBluetoothAdapter.isEnabled()) {

            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity) mainContext).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        // Checks if Bluetooth is supported on the device.
        return (mBluetoothAdapter != null);
    }


    private boolean hasLocationPermissions() {
        int locationMode = 0;
        String locationProviders;

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
        Log.d("LocationPermission:", "API-Level too low!");
        return false;
    }

    private void requestLocationPermission() {

        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_ENABLE_FINE_LOCATION);

    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
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
        int currentDataIndex = 0;

        private String lastreceived = "";    // lastreceived will hold the last valid advertising data received
        private String buffer = "";

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

                    if (device.getName() != null && (device.getName().contains("Partector") || device.getName().contains("P2"))) {
                        String msg = "ascii: ";

                        for (byte b : scanRecord.getBytes())
                            msg += (char) (b & 0xFF);

                        msg = msg.substring(16, 36);
                        if (!msg.equals(lastreceived)) {
                            // new data
                            DataObject newData = new DataObject();
                            newData.setDate(Calendar.getInstance().getTime());
                            newData.setID(currentDataIndex++);
                            String formattedText = mainContext.getString(R.string.amountOfDataObjects, currentDataIndex);
                            amountOfLocalData.setText(formattedText);
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
                                                    newData.setLDSA(LDSA);
                                                }
                                                break;
                                            case 'T':
                                                if (parts[i].endsWith("t")) {
                                                    Temperature = Float.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                    System.out.println("T " + Temperature);
                                                    newData.setTemp(Temperature);
                                                }
                                                break;
                                            case 'H':
                                                if (parts[i].endsWith("h")) {
                                                    RH = Float.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                    System.out.println("Humidity " + RH);
                                                    newData.setHumidity(RH);
                                                }
                                                break;
                                            case 'B':
                                                if (parts[i].endsWith("b")) {
                                                    BatteryVoltage = Float.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                    System.out.println("Batt " + BatteryVoltage);
                                                    newData.setBatteryVoltage(BatteryVoltage);
                                                }
                                                break;
                                            case 'E':
                                                errorcode = Integer.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                System.out.println("err " + errorcode);
                                                newData.setError(errorcode);
                                                break;
                                            case 'D':
                                                diameter = Integer.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                System.out.println("diam " + diameter);
                                                newData.setDiameter(diameter);
                                                break;
                                            case 'C':
                                                number = Integer.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                System.out.println("number " + number);
                                                newData.setNumberC(number);
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


                            dataList.add(newData);
                            adapter.notifyDataSetChanged();
                            listView.smoothScrollToPosition(adapter.getCount() - 1);

                        } else {
                            //System.out.println("no new data:" + msg);
                        }

                    }
                }
            });

        }
    };

    private void conglomerateData() {
        List<DataObject> alreadySyncedData;
        DataObject conglomerateData;

        for (DataObject obj : dataList) {
        }
    }


    private void setAllDataInFirestoreOnce() {
        for (int i = 0; i < dataList.size(); i++) {

            db.collection("DummyData").document(dataList.get(i).getDate().toString()).set(dataList.get(i))
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


    DataObject oldData = null;

    private void SyncData() {
        //do not attempt to sync if no data is available
        if (dataList.size() > 0) {
            //get last object
            //TODO: get an average dataObject with as much data as possible
            final DataObject dataToSync = dataList.get(dataList.size() - 1);


            Log.d("TimedSync", "Started syncing with dataObj: " + dataToSync);


            db.collection("Customers")
                    .document("Naneos")
                    .collection("TestProjektTobi")
                    .document(dataToSync.getDate().toString())
                    .set(dataToSync)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(mainContext, "Document successfully sent to Firestore! ID: " + dataToSync.getDate().toString(), Toast.LENGTH_SHORT).show();

                            if (oldData != null) {
                                System.out.println("oldData: " + oldData.getLDSA() + " | dataToSync: " + dataToSync.getLDSA());
                            }
                            oldData = dataToSync;

                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {

                }
            });

        }
    }

    private void startSyncingWithTimer() {
        if (timer != null) {
            return;
        }
        timer = new Timer();
        timer.scheduleAtFixedRate(timerTask, 0, 5000);
    }

    private void stopSyncingWithTimer() {
        timer.cancel();
        timer = null;
    }


}




