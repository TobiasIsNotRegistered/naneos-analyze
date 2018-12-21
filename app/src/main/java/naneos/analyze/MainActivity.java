package naneos.analyze;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.format.DateFormat;
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

import com.google.android.gms.common.util.SharedPreferencesUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.Permissions;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    //TODO: Check Location Permission Requests, doesn't request atm if its off
    //TODO: Check if network / wlan is enabled, needed for Firebase Sync!

    //layout
    protected ListView listView;
    protected Button btn_syncOnce;
    protected Button btn_save;
    protected Switch switch_keepSynced;
    protected DrawerLayout mDrawerLayout;
    protected TextView amountOfLocalData;
    protected TextView syncFrequency;
    protected TextView drawer_textfield_email;

    //data
    private List<NaneosDataObject> rawDataList = new ArrayList<NaneosDataObject>();
    private List<DataListPerDevice> listPerDeviceMetaList = new ArrayList<DataListPerDevice>();
    private ArrayAdapter adapter;
    public static NaneosBleDataBroadcastReceiver bleDataReceiver;

    //db
    private FirebaseFirestore db;
    private Timer timer;
    private TimerTask timerTask;

    //authentication
    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    //Auxilliary
    protected Context mainContext = this;
    private NaneosScanCallback mScanCallback;
    private PermissionManager permissionManager;

    //LE-Scanner
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothLeScanner mLEScanner;
    protected ScanSettings settings;
    protected List<ScanFilter> filters;


    /********* LIFECYCLE ************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* ******************** AUTH ********************** */
        mAuth = FirebaseAuth.getInstance();

        /* ******************* Layout ********************* */
        listView = findViewById(R.id.lv_main);
        btn_syncOnce = findViewById(R.id.btn_syncOnce);
        btn_save = findViewById(R.id.btn_save);
        switch_keepSynced = findViewById(R.id.switch_keepSynced);
        amountOfLocalData = findViewById(R.id.textView_amountOfDataObjects);

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
        drawer_textfield_email = (TextView) navigationView.getHeaderView(0).findViewById(R.id.drawer_textfield_email);

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        // set item as selected to persist highlight
                        menuItem.setChecked(true);
                        // close drawer when item is tapped
                        mDrawerLayout.closeDrawers();

                        switch (menuItem.getTitle().toString()) {
                            case "Logout":
                                mAuth.signOut();
                                Toast.makeText(mainContext, "Signed out!", Toast.LENGTH_SHORT).show();
                                Intent startLoginActivity = new Intent(mainContext, LoginActivity.class);
                                mainContext.startActivity(startLoginActivity);
                                break;
                            default:
                                Toast.makeText(mainContext, menuItem.getTitle().toString() + ": not implemented yet", Toast.LENGTH_SHORT).show();
                        }


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


        btn_syncOnce.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, "Not implemented yet!", Toast.LENGTH_SHORT).show();
            }
        });

        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {


                Toast.makeText(MainActivity.this, "Not implemented yet!", Toast.LENGTH_SHORT).
                        show();
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


        Log.d("MainActivity", "onCreate finnished!");
    }


    @Override
    protected void onResume() {
        super.onResume();

        PermissionManager pm = new PermissionManager(mainContext);
        pm.requestLocation();
        pm.checkNetworkAvailability();

        if (!pm.isBluetoothEnabled()) {
            pm.requestBluetooth();
        }


        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Intent startLoginActivity = new Intent(mainContext, LoginActivity.class);
            mainContext.startActivity(startLoginActivity);
        } else {
            currentUser = mAuth.getCurrentUser();
            Long tsLastLogin = currentUser.getMetadata().getLastSignInTimestamp();
            checkIfUserWantsToProceedWithCurrentAccount(tsLastLogin);

            if(pm.isBluetoothEnabled()) {
                initateBLE();
            }
        }
    }

    public void checkIfUserWantsToProceedWithCurrentAccount(Long tsLastLogin) {
        Long tsLong = System.currentTimeMillis();
        String tsNow = tsLong.toString();

        Long deltaT = (Long.valueOf(tsNow) - tsLastLogin);
        Long deltaTinSeconds = deltaT / 1000;

        Toast.makeText(mainContext, "Time since last login: " + deltaTinSeconds + "s", Toast.LENGTH_SHORT).show();

        //900s = 15 min --> only asks in onResume, not while App is active
        if (deltaTinSeconds > 3600) {

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Proceed with current account?");
            builder.setMessage("You are logged in as " + currentUser.getEmail() + ". Do you want to proceed with this account?");
            builder.setCancelable(false);
            builder.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                }
            });
            builder.setNegativeButton("Logout", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    mAuth.signOut();
                    Intent startLoginActivity = new Intent(mainContext, LoginActivity.class);
                    mainContext.startActivity(startLoginActivity);
                }
            });
            builder.show();
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
                if (mAuth != null && mAuth.getCurrentUser() != null) {
                    drawer_textfield_email.setText(mAuth.getCurrentUser().getEmail());
                } else {
                    drawer_textfield_email.setText("No User signed in!");
                }

                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bleDataReceiver);
        mLEScanner.stopScan(mScanCallback);
    }


    private void SyncDataToRealtimeDB() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myDbRef = database.getReference();

        if (currentUser == null) {
            Toast.makeText(this, "Error: no available user found", Toast.LENGTH_SHORT).show();
            return;
        } else {
            if (listPerDeviceMetaList.size() > 0) {
                /** sync data for each list of devices available **/
                for (int i = 0; i < listPerDeviceMetaList.size(); i++) {
                    //retrieve lastObject
                    //TODO: retrieve averaged Data instead of lastObject!
                    final NaneosDataObject dataToSync = listPerDeviceMetaList.get(i).store.get(listPerDeviceMetaList.get(i).store.size() - 1);

                    if (dataToSync.getSerial() != 0) {
                        /** We need to replace points with commas to store it in firebase, see here: https://stackoverflow.com/questions/31904123/good-way-to-replace-invalid-characters-in-firebase-keys **/
                        DatabaseReference dataRef = myDbRef.child(currentUser.getEmail().replaceAll("\\.", ",")).child(String.valueOf(dataToSync.getSerial())).child(dataToSync.getDateAsFirestoreKey()).push();
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
    }


    //Timed Task for uploading data
    private void startSyncingWithTimer() {
        if (timer != null) {
            return;
        }
        timer = new Timer();
        int freq = 5000;

        timer.scheduleAtFixedRate(timerTask, 0, freq); //60000 = 1 min
    }

    private void stopSyncingWithTimer() {
        timer.cancel();
        timer = null;
    }


    //receives data from class "NaneosScanCallback"
    //checks if data was received from the same device and stores it sequentially in the device's list
    public class NaneosBleDataBroadcastReceiver extends BroadcastReceiver {
        public static final String SEND_BLE_DATA = "com.naneos.SEND_BLE_DATA";

        @Override
        public void onReceive(Context context, Intent intent) {
            //get the dataObject
            NaneosDataObject currentData = (NaneosDataObject) intent.getSerializableExtra("newDataObject");

            /** case1: metaList is empty  **/
            if (listPerDeviceMetaList.size() == 0) {
                DataListPerDevice newList = new DataListPerDevice();
                newList.add(currentData);
                listPerDeviceMetaList.add(newList);
            } else {
                /** case2: metaList has at least one entry **/
                boolean deviceKnown = false;
                //check if there is a 'DataListPerDevice' for this device
                for (int i = 0; i < listPerDeviceMetaList.size(); i++) {
                    DataListPerDevice currentList = listPerDeviceMetaList.get(i);
                    //check metaList if we already have a dataList for this macAddress
                    if (currentList.getMacAddress().equals(currentData.getMacAddress())) {
                        currentList.add(currentData);
                        deviceKnown = true;
                    }
                }
                //if there's no 'DataListPerDevice' for this macAddress, create a new one and add it to the metaList
                if (!deviceKnown) {
                    DataListPerDevice newList = new DataListPerDevice();
                    newList.add(currentData);
                    listPerDeviceMetaList.add(newList);
                }

            }

            adapter.notifyDataSetChanged();
            //listView.smoothScrollToPosition(adapter.getCount() - 1);

            String formattedText = getString(R.string.amountOfDataObjects, listPerDeviceMetaList.size());
            amountOfLocalData.setText(formattedText);
        }
    }

    // called in onCreate to setup bluetooth
    public boolean initateBLE() {

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

        mLEScanner.startScan(mScanCallback);
        return (mBluetoothAdapter != null);
    }

    private String getDate(long time) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(time);
        String date = DateFormat.format("dd-MM-yyyy hh:mm:ss", cal).toString();
        return date;
    }

}




