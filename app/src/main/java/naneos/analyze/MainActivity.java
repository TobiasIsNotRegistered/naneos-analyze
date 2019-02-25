package naneos.analyze;

import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

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

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    //TODO: Check Location Permission Requests, doesn't request atm if its off
    //TODO: Status: Connection to DB established?
    //TODO: Status: Bluettoth, Location, Wlan on? (not important!)

    private int SYNC_FREQ = 60000;

    //layout
    protected ListView lv_main;
    protected Button btn_syncOnce;
    protected Button btn_save;
    protected Switch switch_keepSynced;
    protected DrawerLayout mDrawerLayout;
    protected TextView tv_amountOfLocalData;
    protected TextView drawer_textfield_email;
    protected TextView tv_main_status;
    protected TextView tv_main_status_scanning;
    protected TextView tv_main_status_syncing;
    protected TextView tv_main_status_timeSinceLastReceived;
    protected TextView tv_main_status_timeSinceLastSynced;
    protected TextView tv_main_status_sync_freq;

    //data
    private List<NaneosDataObject> rawDataList = new ArrayList<NaneosDataObject>();
    private List<DataListPerDevice> listPerDeviceMetaList = new ArrayList<DataListPerDevice>();
    private ArrayAdapter adapter;
    public static NaneosBleDataBroadcastReceiver bleDataReceiver;
    private ArrayList listOfDevicesForUserFromDB;
    public HashMap<String, ArrayList<String>> jsonSyncMap;
    public ArrayList<String> jsonSyncList_days;

    //db
    private FirebaseFirestore db;
    private Timer timerDBSync;
    private TimerTask timerDBSyncTask;

    //authentication
    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    //Auxilliary
    protected Context mainContext = this;
    private NaneosScanCallback mScanCallback;
    private NaneosDataObject lastSyncedDataObject;
    private int secondsSinceLastObjectReceived;
    private int secondsSinceLastObjectSynced;
    public Handler updateAssertScanStatusHandler;
    public Handler updateAssertSyncStatusHandler;
    protected Handler updateTimeSinceLastSyncHandler;
    protected Handler updateTimeSinceLastReceivedHandler;
    public long timeSinceLastProceedWithCurrentAccount;

    //LE-Scanner
    protected BluetoothAdapter mBluetoothAdapter;
    protected BluetoothLeScanner mLEScanner;
    protected ScanSettings settings;
    protected List<ScanFilter> filters;
    private Timer timerAssertScanStatus;
    private TimerTask timerAssertScanStatusTask;

    // added MF:
    private NaneosDataAverageObject average = new NaneosDataAverageObject();


    /********* LIFECYCLE ************/
    // onCreate()
    // onStart()   <----------onRestart()
    // onResume() <----------      |
    // activity running      |     |
    // onPause()  -----------      |
    // onStop()  ------------------
    // onDestroy()
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* ******************** AUTH ********************** */
        FirebaseApp.initializeApp(this);
        mAuth = FirebaseAuth.getInstance();     // MF: does this work without internet permission?

        /* ******************* Layout ********************* */
        lv_main = findViewById(R.id.lv_main);
        tv_amountOfLocalData = findViewById(R.id.textView_amountOfDataObjects);
        tv_main_status = findViewById(R.id.tv_main_status);
        tv_main_status_syncing = findViewById(R.id.tv_main_status_syncing);
        tv_main_status_timeSinceLastReceived = findViewById(R.id.tv_main_status_timeSyncLastReceived);
        tv_main_status_timeSinceLastSynced = findViewById(R.id.tv_main_status_timeSinceLastSync);
        tv_main_status_sync_freq = findViewById(R.id.tv_main_status_sync_freq);

        adapter = new ArrayAdapter<>(mainContext, android.R.layout.simple_list_item_1, listPerDeviceMetaList);
        lv_main.setAdapter(adapter);


        /* ******************* DB ********************* */
        db = FirebaseFirestore.getInstance();       // does this work without permission?
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
                                Toast.makeText(mainContext, menuItem.getTitle().toString() + ": to be implemented", Toast.LENGTH_SHORT).show();
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

        /* ******* TIMERS FOR SCHEDULED STUFF (SYNCING TO DB, ASSERTING SCANNING STATUS) ************* */
        updateAssertScanStatusHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    tv_main_status_scanning.setText(msg.obj.toString());
                }
            }
        };

        updateAssertSyncStatusHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 0) {
                    tv_main_status_syncing.setText(msg.obj.toString());
                }
            }
        };

        updateTimeSinceLastSyncHandler = new Handler() {
            public void handleMessage(Message msg){
                if(msg.what == 0){
                    tv_main_status_timeSinceLastSynced.setText(msg.obj.toString());
                }
            }
        };

        updateTimeSinceLastReceivedHandler = new Handler(){
            public void handleMessage(Message msg){
                if(msg.what == 0){
                    tv_main_status_timeSinceLastReceived.setText(msg.obj.toString());
                }
            }
        };


        timerAssertScanStatusTask = new TimerTask() {
            @Override
            public void run() {
                secondsSinceLastObjectReceived++;
                secondsSinceLastObjectSynced++;

                //time since last received object / sync is > than 5 minutes --> display an Error to the user
                if(secondsSinceLastObjectReceived > 300 || secondsSinceLastObjectSynced > 300){
                    tv_main_status.setText("Status: Error");
                }

                updateTimeSinceLastSyncHandler.obtainMessage(0, "Syncing: in Progress - Time since last sync: " + secondsSinceLastObjectSynced).sendToTarget();
                updateTimeSinceLastReceivedHandler.obtainMessage(0, "Scanning: in Progress - Time since last object: " + secondsSinceLastObjectReceived).sendToTarget();
            }
        };

        startAssertScanningStatusWithTimer();

        /* ************** BLE *************** */
        bleDataReceiver = new NaneosBleDataBroadcastReceiver();
        this.registerReceiver(bleDataReceiver, new IntentFilter(NaneosBleDataBroadcastReceiver.SEND_BLE_DATA));
        mScanCallback = new NaneosScanCallback(this);


        /* ************************************* */
        // todo: this function would tell if something is wrong, auto-retry?
        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                boolean connected = snapshot.getValue(Boolean.class);
                if (connected) {
                    Log.d("NaneosOnCreate", "Connected to Firebase!");
                    updateAssertSyncStatusHandler.obtainMessage(0, "DB: connected").sendToTarget();
                } else {
                    Log.d("NaneosOnCreate", "Not connected to Firebase!");
                    updateAssertSyncStatusHandler.obtainMessage(0, "DB: not connected").sendToTarget();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                updateAssertSyncStatusHandler.obtainMessage(0, "DB: Upload canceled").sendToTarget();
            }
        });

        // start BLEtimer task here
        startBLETimer();
        //start DB-Sync here
        startSyncingWithTimer();

        // MF added: start channel for notifications
        createNotificationChannel();

        //init textViews to fill placeholder-texts
        tv_amountOfLocalData.setText("Devices: 0");
        tv_main_status_sync_freq.setText("Sync frequency: " + SYNC_FREQ/1000 + "s");

        tv_main_status.setText("Status: running...");

        Log.d("NaneosMainActivity", "onCreate finished!");
    }


    @Override
    protected void onResume() {
        super.onResume();

        // TODO: I think this is the wrong place for looking for permissions. We should
        // get permissions at onCreate!?

        PermissionManager pm = new PermissionManager(mainContext);
        pm.requestLocation();
        pm.checkNetworkAvailability();

        if (!pm.isBluetoothEnabled()) {
            pm.requestBluetooth();
        }

        // TODO: don't you need to wait until premissions are granted somehow?
        //TODO: Answer from Tobi: Permissions werden in der SplashScreenActivity abgefragt. Die SplashScreenActivity startet die LoginActivity, falls Permission-Antworten kommen (momentan auch bei negativer Antwort).
        //TODO: Die Loginactivty startet dann wiederum die MainActivity, falls das Login gültig war. Das bedeutet, zu diesem Zeitpunkt haben wir bereits alle Permissions & Daten

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            Intent startLoginActivity = new Intent(mainContext, LoginActivity.class);
            mainContext.startActivity(startLoginActivity);
        } else {
            currentUser = mAuth.getCurrentUser();
            Long tsLastLogin = currentUser.getMetadata().getLastSignInTimestamp();
            checkIfUserWantsToProceedWithCurrentAccount(tsLastLogin);
            getListOfDevicesFromDB();
            attemptLoadingAppStatus();

            if (pm.isBluetoothEnabled()) {
                initateBLE();
            }
        }

        // MF added a notification here
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this, getString(R.string.channel_ID))
                .setSmallIcon(R.drawable.common_google_signin_btn_icon_light_normal)
                .setContentTitle("naneos")
                .setContentText("naneos web upload running")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

// notificationId is a unique int for each notification that you must define
        notificationManager.notify(1, mBuilder.build());

    }

    @Override
    protected void onStop() {
        super.onStop();
        attemptSavingAppStatus();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.cancelAll();
    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_ID);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_MAX;
            NotificationChannel channel = new NotificationChannel(getString(R.string.channel_ID), name, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public void attemptSavingAppStatus() {
        try {
            FileOutputStream fos = mainContext.openFileOutput("NaneosAppStatus.txt", Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(timeSinceLastProceedWithCurrentAccount);
            os.close();
            fos.close();
        } catch (IOException e) {
            Log.d("Naneos", "Unable to write to the TestFile.txt");
        }
        Log.d("Naneose", "Write to TestFile.txt file.");
    }

    public void attemptLoadingAppStatus() {
        try {
            FileInputStream fis = mainContext.openFileInput("NaneosAppStatus.txt");
            ObjectInputStream is = new ObjectInputStream(fis);
            timeSinceLastProceedWithCurrentAccount = is.read();
            is.close();
            fis.close();
        } catch (IOException e) {
            Log.e("Naneos", "Unable to read to the TestFile.txt file.");
        }
        Log.d("Naneos", "Read to TestFile.txt file.");
    }


    public void checkIfUserWantsToProceedWithCurrentAccount(Long tsLastLogin) {
        Long tsLong = System.currentTimeMillis();
        String tsNow = tsLong.toString();
        Long timeSinceLastProceed;

        Long deltaT = (tsLong - tsLastLogin);

        if (timeSinceLastProceedWithCurrentAccount > 0) {
            timeSinceLastProceed = tsLong - timeSinceLastProceedWithCurrentAccount;
            Log.d("Naneos", "Time since last Proceed (s): " + timeSinceLastProceed / 1000);
        } else {
            timeSinceLastProceed = 0L;
        }


        Long deltaTinSeconds = deltaT / 1000;

        Toast.makeText(mainContext, "Welcome, " + currentUser.getEmail() + "! \n" + " Time since last login: " + deltaTinSeconds + "s", Toast.LENGTH_SHORT).show();

        //900s = 15 min --> only asks in onResume, not while App is active
        if (deltaTinSeconds > 3600 && timeSinceLastProceed > 3600) {

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Proceed with current account?");
            builder.setMessage("You are logged in as " + currentUser.getEmail() + ". Do you want to proceed with this account?");
            builder.setCancelable(false);
            builder.setPositiveButton("Proceed", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    timeSinceLastProceedWithCurrentAccount = System.currentTimeMillis();
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
        stopAssertScanningStatusWithTimer();
        stopSyncingWithTimer();
    }


    //get List of devices for the connected user from DB
    //this is used in order to update the Meta-List "Devices" in the DB, if a new device is connected
    //the Meta_List "Devices" will be used by the web-application in order to be able to download individual data instead of all data at once
    private void getListOfDevicesFromDB() {
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myDbRef = database.getReference();

        DatabaseReference dataRef = myDbRef.child(currentUser.getEmail().replaceAll("\\.", ",")).child("devices");
        dataRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("Naneos", "getListOfDevicesFromDB: dataSnapshot: " + dataSnapshot.getValue());
                listOfDevicesForUserFromDB = new ArrayList<JSONObject>();
                ArrayList<Long> data = (ArrayList) dataSnapshot.getValue();
                /*
                if (data != null) {
                    for (int i = 0; i < data.size(); i++) {
                        listOfDevicesForUserFromDB.add(data.get(i) != null ? data.get(i).intValue() : null);
                    }
                }
                */
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    //precondition: listPerDeviceMetaList.size() > 0 && listOfDevicesForUserFromDB.size() > 0
    public void checkIfUpdateOfDeviceMetaListIsNecessary(NaneosDataObject dataSyncObject) {

        //case 0: no metaList exists on rtDB for this user
        if ((listPerDeviceMetaList != null && listPerDeviceMetaList.size() > 0) && (listOfDevicesForUserFromDB == null || listOfDevicesForUserFromDB.size() < 1)) {

            for (int i = 0; i < listPerDeviceMetaList.size(); i++) {
                jsonSyncMap = new HashMap<String, ArrayList<String>>();
                jsonSyncList_days = new ArrayList<String>();

                jsonSyncList_days.add(dataSyncObject.getDateAsFirestoreKey());
                jsonSyncMap.put(String.valueOf(dataSyncObject.getSerial()), jsonSyncList_days);

            }
            updateDeviceMetaListOnRTDB(jsonSyncMap);
            return;
        }

        /*
        //TODO: Add logic to add a specific serial to the metalist if the already exists on the server and is not empty
        boolean found_serial;
        boolean found_day;
        //case 1: metaLists exists but doesn't contain all devices which we are receiving from --> update list
        if (listOfDevicesForUserFromDB != null && listOfDevicesForUserFromDB.size() > 0 && listPerDeviceMetaList != null && listPerDeviceMetaList.size() > 0) {
            //loop through local data
            for (int i = 0; i < listPerDeviceMetaList.size(); i++) {
                found_serial = false;
                found_day = false;
                //loop through dbData and see if we have local data which doesn't exist online
                for (int j = 0; j < listOfDevicesForUserFromDB.size(); j++) {
                    try {
                        JSONObject _dbMetaList = (JSONObject) listOfDevicesForUserFromDB.get(j);
                        if (listPerDeviceMetaList.get(i).getSerial() == (Integer.valueOf(_dbMetaList.getString("serial")))){
                            ArrayList<String> _dbDays = (ArrayList<String>) _dbMetaList.get("jsonSyncList_days");
                            found_serial = true;

                            //list of jsonSyncList_days exists and is not empty
                            if (_dbDays != null && _dbDays.size() > 0) {
                                if (_dbDays.contains(dataSyncObject.getDateAsFirestoreKey())) {
                                    found_day = true;
                                } else {
                                    //day doesn't exist on DB - add it to the list
                                    _dbDays.add(dataSyncObject.getDateAsFirestoreKey());
                                }
                            }


                        }

                    } catch (JSONException e) {
                        Log.d("Naneos", "checkIfUpdateOfDeviceMetaListIsNecessary(): reading from JSON failed");
                    }
                }

                //Serial doesn't exist on DB - add it to the list
                if (!found_serial) {
                    try{
                        JSONObject _dbMetaList = new JSONObject();
                        ArrayList<String> _dbDays = new ArrayList<String>();
                        _dbDays.add(dataSyncObject.getDateAsFirestoreKey());
                        _dbMetaList.put("serial", dataSyncObject.getSerial());
                        _dbMetaList.put("jsonSyncList_days", _dbDays);
                    }catch  (JSONException e){
                        Log.d("Naneos", "checkIfUpdateOfDeviceMetaListIsNecessary(): writing to JSON failed");
                    }
                }

                if(!found_serial || !found_day){
                    updateDeviceMetaListOnRTDB(_dbMetaList);
                }
            }
        }
        */

    }

    public void updateDeviceMetaListOnRTDB(HashMap<String, ArrayList<String>> _newMetaList) {
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myDbRef = database.getReference();

        DatabaseReference dataRef = myDbRef.child(currentUser.getEmail().replaceAll("\\.", ",")).child("devices");

        dataRef.setValue(_newMetaList).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("Naneos", "updateDeviceMetaList - success!");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("Naneos", "updateDeviceMetaList - failure! " + e.getMessage());
            }
        });
    }


    private void SyncDataToRealtimeDB() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myDbRef = database.getReference();

        if (currentUser == null) {
            //todo: next line makes an error if java.lang.RuntimeException: Can't toast on a thread that has not called Looper.prepare()
            //Toast.makeText(this, "Error: no available user found", Toast.LENGTH_SHORT).show();
            return;
        } else {
            if (listPerDeviceMetaList.size() > 0) {
                // sync data for each list of devices available
                for (int i = 0; i < listPerDeviceMetaList.size(); i++) {
                    //retrieve lastObject
                    //TODO: retrieve averaged Data instead of lastObject!
                    final NaneosDataObject dataToSync = listPerDeviceMetaList.get(i).store.get(listPerDeviceMetaList.get(i).store.size() - 1);
                    final DataListPerDevice currentDataList = listPerDeviceMetaList.get(i);

                    //only upload if new Data is apparent
                    if (dataToSync != null && dataToSync != lastSyncedDataObject) {

                        if (dataToSync.getSerial() != 0) {
                            // We need to replace points with commas to store it in firebase, see here: https://stackoverflow.com/questions/31904123/good-way-to-replace-invalid-characters-in-firebase-keys
                            DatabaseReference dataRef = myDbRef.child(currentUser.getEmail().replaceAll("\\.", ",")).child(String.valueOf(dataToSync.getSerial())).child(dataToSync.getDateAsFirestoreKey()).push();

                            // replace dataToSync with a NaneosSyncObject which has less data inside!
                            NaneosDataSyncObject naneosDataSyncObject = new NaneosDataSyncObject(dataToSync);
                            dataRef.setValue(naneosDataSyncObject).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    secondsSinceLastObjectSynced = 0;
                                    currentDataList.amountOfSyncedObjects++;
                                    dataToSync.setStoredInDB(true);
                                    adapter.notifyDataSetChanged();
                                    lastSyncedDataObject = dataToSync;
                                    checkIfUpdateOfDeviceMetaListIsNecessary(dataToSync);
                                    tv_main_status.setText("Status: running...");
                                    Log.d("Firestore", "success");
                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    dataToSync.setStoredInDB(false);
                                    updateAssertSyncStatusHandler.obtainMessage(0, "Syncing: Error - Could not send Data to DB!").sendToTarget();
                                    tv_main_status.setText("Status: last sync failed");
                                    Log.d("Firestore", "fail");
                                }
                            });
                        }
                    } else {
                        updateAssertSyncStatusHandler.obtainMessage(0, "Syncing: Error - no new Data received!").sendToTarget();
                        return;
                    }

                }
            } else {
                updateAssertSyncStatusHandler.obtainMessage(0, "Syncing: No Data available").sendToTarget();
            }
        }
    }

    private void startAssertScanningStatusWithTimer() {
        if (timerAssertScanStatus != null) {
            return;
        }
        timerAssertScanStatus = new Timer();
        int freq = 1000;

        timerAssertScanStatus.scheduleAtFixedRate(timerAssertScanStatusTask, 0, freq);
    }

    private void stopAssertScanningStatusWithTimer() {
        if (timerAssertScanStatus != null) {
            timerAssertScanStatus.cancel();
            timerAssertScanStatus = null;
        }
    }


    //Timed Task for uploading data
    private void startSyncingWithTimer() {
        if (timerDBSync != null) {
            return;
        }

        timerDBSync = new Timer();
        int freq = SYNC_FREQ; // 5000; // 60000; //60000 = 1 min

        timerDBSyncTask = new TimerTask() {
            @Override
            public void run() {
                //SyncDataToFirestore();
                SyncDataToRealtimeDB();
                Log.d("Naneos", "Invoked 'SyncDataToRealtimeDB' ....");
            }
        };

        timerDBSync.scheduleAtFixedRate(timerDBSyncTask, 0, freq);
    }

    private void stopSyncingWithTimer() {
        if (timerDBSync != null) {
            timerDBSync.cancel();
            timerDBSync = null;
        }
    }


    //receives data from class "NaneosScanCallback"
    //checks if data was received from the same device and stores it sequentially in the device's list
    public class NaneosBleDataBroadcastReceiver extends BroadcastReceiver {
        public static final String SEND_BLE_DATA = "com.naneos.SEND_BLE_DATA";

        @Override
        public void onReceive(Context context, Intent intent) {

            updateAssertScanStatusHandler.obtainMessage(0, "scanning!");
            //get the dataObject
            NaneosDataObject currentData = (NaneosDataObject) intent.getSerializableExtra("newDataObject");

            //if new data is received, reset the timer
            secondsSinceLastObjectReceived = 0;

            // case1: metaList is empty
            if (listPerDeviceMetaList.size() == 0) {
                DataListPerDevice newList = new DataListPerDevice();
                newList.add(currentData);
                listPerDeviceMetaList.add(newList);
            } else {
                // case2: metaList has at least one entry
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
            //lv_main.smoothScrollToPosition(adapter.getCount() - 1);

            String formattedText = getString(R.string.amountOfDataObjects, listPerDeviceMetaList.size());
            tv_amountOfLocalData.setText(formattedText);
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

        //mLEScanner.startScan(mScanCallback);


        ScanFilter scanFilter = new ScanFilter.Builder().setDeviceName("Partector").build();
        final ScanSettings.Builder builderScanSettings = new ScanSettings.Builder();
        builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        builderScanSettings.setReportDelay(0);
        filters = new ArrayList<>();
        filters.add(scanFilter);
        filters.add(new ScanFilter.Builder().setDeviceName("P2").build());
        mLEScanner.startScan(filters, builderScanSettings.build(), mScanCallback);
        updateAssertScanStatusHandler.obtainMessage(0, "scanning...");


        return (mBluetoothAdapter != null);
    }

    void startBLETimer() {
        // create a timer which will fire once every 10 minutes and which will turn the scan off and on again
        // I do this because android will terminate the BLE scanning by itself after 30 minutes, so I need to
        // prevent it from doing this...
        ScanFilter scanFilter = new ScanFilter.Builder().setDeviceName("Partector").build();
        filters = new ArrayList<>();
        filters.add(scanFilter);
        filters.add(new ScanFilter.Builder().setDeviceName("P2").build());
        final ScanSettings.Builder builderScanSettings = new ScanSettings.Builder();
        builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        builderScanSettings.setReportDelay(0);


        System.out.println("creating a new timer");
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
                                  @Override
                                  public void run() {  // called repetitively, scans for devices
                                      Log.d("Naneos: BLETimer", "Restart scanning");
                                      mLEScanner.stopScan(mScanCallback);
                                      mLEScanner.startScan(filters, builderScanSettings.build(), mScanCallback);

                                  }
                              },
                // set how long to wait before starting the Timer Task
                300000,
                // set how often to call (in ms)
                300000);  // do this all 10 minutes
    }

    private String getDate(long time) {
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(time);
        String date = DateFormat.format("dd-MM-yyyy hh:mm:ss", cal).toString();
        return date;
    }


}




