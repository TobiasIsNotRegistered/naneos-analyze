package naneos.analyze;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    //TODO: Check Location Permission Requests, doesn't request atm if its off
    //TODO: Check if network / wlan is enabled, needed for Firebase Sync!

    //layout
    protected ListView listView;
    protected Button btn_syncOnce;
    protected Button btn_flush;
    protected Button btn_scan;
    protected Switch switch_keepSynced;
    protected DrawerLayout mDrawerLayout;
    protected TextView amountOfLocalData;
    protected TextView syncFrequency;

    //data
    private List<NaneosDataObject> rawDataList = new ArrayList<NaneosDataObject>();
    private List<DataListPerDevice> listPerDeviceMetaList = new ArrayList<DataListPerDevice>();
    private ArrayAdapter adapter;
    public static NaneosBleDataBroadcastReceiver bleDataReceiver;

    //db
    private FirebaseFirestore db;
    private Timer timer;
    private TimerTask timerTask;

    //bluetooth
    private static final int REQUEST_ENABLE_BT = 101; // request code to enable bluetooth
    private static final int REQUEST_ENABLE_FINE_LOCATION = 201;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    //Auxilliary
    protected Context mainContext = this;
    private NaneosScanCallback mScanCallback;
    boolean isScanning;


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
        syncFrequency = findViewById(R.id.editText);

        adapter = new ArrayAdapter<>(mainContext, android.R.layout.simple_list_item_1, listPerDeviceMetaList);
        listView.setAdapter(adapter);

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
                toggleScan();
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
                //SyncDataToFirestore();
                SyncDataToRealtimeDB();
            }
        };

        /* ************** BLE *************** */
        bleDataReceiver = new NaneosBleDataBroadcastReceiver();
        this.registerReceiver(bleDataReceiver, new IntentFilter(NaneosBleDataBroadcastReceiver.SEND_BLE_DATA));
        mScanCallback = new NaneosScanCallback(this);




        PermissionManager permissionManager = new PermissionManager(mainContext);
        permissionManager.checkLocationPermission();
        permissionManager.checkNetworkAvailability();

        /*  INIT */
        initateBLE();

        Log.d("MainActivity", "onCreate finnished!");
    }

    public void toggleScan() {

        if (!isScanning) {
            mLEScanner.startScan(mScanCallback);
            isScanning = true;
            btn_scan.setText("STOP");
            Log.d("btn_scan", "Scanner started");

        } else if (isScanning) {
            mLEScanner.stopScan(mScanCallback);
            //mLEScanner = null;
            isScanning = false;
            btn_scan.setText("SCAN");

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUEST_ENABLE_BT) {
            Toast.makeText(getApplicationContext(), "Bluetooth turned on", Toast.LENGTH_SHORT);


            //toggleScan();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //TODO: implemented logic when back button is pressed on mainActivity screen
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


    //TODO: Cleanup after onDestroy!
    @Override
    public void onDestroy() {
        super.onDestroy();

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
    private boolean initateBLE() {

        Log.d("Bluetooth Initiate", "Initiate started!");
        // Use this check to determine whether BLE is supported on the device.
        // Then you can selectively disable BLE-related features.
        if (!mainContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.d("initateBLE", "Bluetooth LE seemingly not supported");
            return false;
        } else {
            Log.d("initateBLE", "Bluetooth Le seems to be supported.");
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


    private void SyncDataToRealtimeDB(){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myDbRef = database.getReference();

        if(listPerDeviceMetaList.size() > 0){
            /** sync data for each list of devices available **/
            for(int i = 0; i<listPerDeviceMetaList.size(); i++){
                //retrieve lastObject
                final NaneosDataObject dataToSync =  listPerDeviceMetaList.get(i).store.get(listPerDeviceMetaList.get(i).store.size() - 1);

                if(dataToSync.getSerial() != 0){
                    DatabaseReference dataRef =  myDbRef.child("naneos").child(String.valueOf(dataToSync.getSerial())).child(dataToSync.getDateAsFirestoreKey()).push();
                    dataRef.setValue(dataToSync).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            dataToSync.setStoredInDB(true);
                            adapter.notifyDataSetChanged();
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            dataToSync.setStoredInDB(false);
                            Toast.makeText(mainContext, "Couldn't store dataToSync in DB!", Toast.LENGTH_SHORT);
                        }
                    });
                }
            }
        }
    }


    //Timed Task for uploading data
    private void startSyncingWithTimer() {
        if (timer != null) {
            return;
        }
        timer = new Timer();
        int freq = 60000;
        if(syncFrequency.getText() != null){
            freq = Integer.parseInt(syncFrequency.getText().toString()) * 1000;
        }else{
            Log.d("Naneos Analyze", "Couldn't get int from TextField!");
        }

        timer.scheduleAtFixedRate(timerTask, 0, freq); //60000 = 1 min
    }

    private void stopSyncingWithTimer() {
        timer.cancel();
        timer = null;
    }


    //receives data from class "NaneosScanCallback" and stores it in "rawDataList"
    public class NaneosBleDataBroadcastReceiver extends BroadcastReceiver {
        public static final String SEND_BLE_DATA = "com.naneos.SEND_BLE_DATA";

        @Override
        public void onReceive(Context context, Intent intent) {
            //get the dataObject
            NaneosDataObject currentData = (NaneosDataObject) intent.getSerializableExtra("newDataObject");
            Log.d("NaneosAnalyze", "received dataObject!");

            /** case1: metaList is empty  **/
            if(listPerDeviceMetaList.size() == 0){
                DataListPerDevice newList = new DataListPerDevice();
                newList.add(currentData);
                listPerDeviceMetaList.add(newList);
            }else{
            /** case2: metaList has at least one entry **/
            boolean deviceKnown = false;
                //check if there is a 'DataListPerDevice' for this device
                for(int i = 0; i<listPerDeviceMetaList.size(); i++){
                    DataListPerDevice currentList = listPerDeviceMetaList.get(i);
                    //check metaList if we already have a dataList for this macAddress
                    if(currentList.getMacAddress().equals(currentData.getMacAddress())) {
                        currentList.add(currentData);
                        deviceKnown = true;
                    }
                }
                //if there's no 'DataListPerDevice' for this macAddress, create a new one and add it to the metaList
                if(!deviceKnown){
                    DataListPerDevice newList = new DataListPerDevice();
                    newList.add(currentData);
                    listPerDeviceMetaList.add(newList);
                }

            }



            rawDataList.add(currentData);
            adapter.notifyDataSetChanged();
            listView.smoothScrollToPosition(adapter.getCount() - 1);

            String formattedText = getString(R.string.amountOfDataObjects, listPerDeviceMetaList.size());
            amountOfLocalData.setText(formattedText);
        }
    }


    private void SyncDataToFirestore() {
        //do not attempt to sync if no data is available
        if (rawDataList.size() > 0) {
            //get last object
            //TODO: get an average dataObject with as much data as possible
            final NaneosDataObject dataToSync = rawDataList.get(rawDataList.size() - 1);

            /*
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
             */
        }
    }


    private void setAllDataInFirestoreOnce() {
        for (int i = 0; i < rawDataList.size(); i++) {

            db.collection("DummyData").document(rawDataList.get(i).getDate().toString()).set(rawDataList.get(i))
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

}




