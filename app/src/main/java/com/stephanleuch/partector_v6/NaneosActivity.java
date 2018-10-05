package com.stephanleuch.partector_v6;

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
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.volley.RequestQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class NaneosActivity extends ActionBarActivity{

    private Context mainContext = this;
    private static final int REQUEST_ENABLE_BT = 1;

    // new BLE interface
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;

    private DeviceFragment mDeviceFragment;

    private String lastreceived = "";    // lastreceived will hold the last valid advertising data received
    private String buffer = "";          // concatenate all valid stuff in here for parsing

    RequestQueue queue;

    // first, the 6 core callbacks of the activity lifecycle
    // onCreate(), onStart(), onResume(), onPause(), onStop() and onDestroy()

    /**
     * First called Method when User opens Application.
     * @param savedInstanceState ... whatever
     // onCreate(), onStart(), onResume(), onPause(), onStop() and onDestroy()
     perform basic application startup logic that should only happen once
     *For example, your implementation of onCreate() might bind data to lists, initialize background threads, and instantiate some class-scope variables. This method receives the parameter savedInstanceState, which is a Bundle object containing the activity's previously saved state. If the activity has never existed before, the value of the Bundle object is null.

    The following example of the onCreate() method shows fundamental setup for the activity, such as declaring the user interface (defined in an XML layout file), defining member variables, and configuring some of the UI. In this example, the XML layout file is specified by passing files resource ID R.layout.main_activity to setContentView().

     */
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        System.out.println("********************************");
        System.out.println("   onCreate    ");
        System.out.println("********************************");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_naneos);

        mDeviceFragment = new DeviceFragment();
        mDeviceFragment.mActive = true;

        if (!initiate()) {
            Toast.makeText(mainContext, "Bluetooth not supported.",
                    Toast.LENGTH_LONG).show();
            ((Activity) mainContext).finish();
        }

        // start a timer which will do a scan at fixed intervals to look for advertising devices
        // TODO: this timer should be created in onResume, and destroyed in onPause!?!?
        // this timer should not run as daemon

        // create a timer which will fire once every 10 minutes and which will turn the scan off and on again
        // I do this because android will terminate the BLE scanning by itself after 30 minutes, so I need to
        // prevent it from doing this...
        ScanFilter scanFilter = new ScanFilter.Builder().setDeviceName("Partector").build();
        filters = new ArrayList<>();
        filters.add(scanFilter);
        final ScanSettings.Builder builderScanSettings = new ScanSettings.Builder();
        builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        builderScanSettings.setReportDelay(0);


        System.out.println("creating a new timer");
        Timer t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {
                                  @Override
                                  public void run() {  // called repetitively, scans for devices
                                      Log.e("Restart", "Restart scanning");
                                      mLEScanner.stopScan(mScanCallback);
                                      //mLEScanner.startScan(mScanCallback);
                                      mLEScanner.startScan(filters, builderScanSettings.build(), mScanCallback);
                                  }
                              },
                // set how long to wait before starting the Timer Task
                600000,
                // set how often to call (in ms)
                600000);  // do this all 10 minutes

        //  to detect API version programmatically:
        //  int i = Build.VERSION.SDK_INT;
        // System.out.println("API version is " + i);
    }

    @Override
    protected void onStart() {
        // onCreate(), onStart(), onResume(), onPause(), onStop() and onDestroy()
        // here, onStart() is "missing" - it could/should initialize GUI
        super.onStart();
        System.out.println("********************************");
        System.out.println("   onStart    ");
        System.out.println("********************************");

        // add fragment to container
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mDeviceFragment)
                .commit();
    }

    /**
     * onResume:
     * Last Method to be called as Application is opened.
     * This Method is also executed if Application is opened from paused state
     * // onCreate(), onStart(), onResume(), onPause(), onStop() and onDestroy()
     when app comes in foreground, onResume is called.
     onResume should activate stuff that we are disabling during onPause()
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        System.out.println("********************************");
        System.out.println("   onResume    ");
        System.out.println("********************************");

        mDeviceFragment.mActive = true;

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ((Activity) mainContext).startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
        //settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        ScanFilter scanFilter = new ScanFilter.Builder().setDeviceName("Partector").build();
        final ScanSettings.Builder builderScanSettings = new ScanSettings.Builder();
        builderScanSettings.setScanMode(ScanSettings.SCAN_MODE_BALANCED);
        builderScanSettings.setReportDelay(0);
        //filters = new ArrayList<>();
        //filters.add(scanFilter);

        System.out.println("----------------------------->naneosActivity startscan");
        if(mLEScanner != null)
            //mLEScanner.startScan(mScanCallback);
            mLEScanner.startScan(filters, builderScanSettings.build(), mScanCallback);
    }

    /**
     * This method is called before Application goes into paused state
     Use the onPause() method to pause operations such animations and music playback that should not continue while the Activity is in the Paused state, and that you expect to resume shortly.
     For example, if your application uses the Camera, the onPause() method is a good place to release it
     onPause() execution is very brief, and does not necessarily afford enough time to perform save operations. For this reason, you should not use onPause() to save application or user data, make network calls, or execute database transactions; such work may not complete before the method completes. Instead, you should perform heavy-load shutdown operations during onStop().
     */
    @Override
    protected void onPause()
    {
        System.out.println("********************************");
        System.out.println("   onPause    ");
        System.out.println("********************************");

        mDeviceFragment.mActive = false;

        super.onPause();
        // as far as I can see, the application is always immediately stopped and destroyed once it is paused!
        // no, one exception, when I start google earth, then I get pause + stop, but not destroy!
    }

    // When your activity is no longer visible to the user, it has entered the Stopped state, and the system invokes the onStop() callback. This may occur, for example, when a newly launched activity covers the entire screen. The system may also call onStop() when the activity has finished running, and is about to be terminated.
    // In the onStop() method, the app should release almost all resources that aren't needed while the user is not using it. For example, if you registered a BroadcastReceiver in onStart() to listen for changes that might affect your UI, you can unregister the broadcast receiver in onStop(), as the user can no longer see the UI. It is also important that you use onStop() to release resources that might leak memory, because it is possible for the system to kill the process hosting your activity without calling the activity's final onDestroy() callback.

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("********************************");
        System.out.println("   onStop    ");
        System.out.println("********************************");
    }

    /**
     * This is the last method to be called before Activity is destroyed
     */
    @Override
    protected void onDestroy()
    {
        mLEScanner.stopScan(mScanCallback);
        System.out.println("********************************");
        System.out.println("   onDestroy    ");
        System.out.println("********************************");
        super.onDestroy();
    }

    /**
     * Called after onCreate (only for ActionBarActivities)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_naneos, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Defines what happens if an Item of the ActionBar is Selected
     * @param item Item which was Selected
     * @return returns the result its super-method
     */
    @Override
    // todo: the options menu is doing nothing currently!
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        // not deleted although unused, because we might use the action bar later again
        int id = item.getItemId();      // get selected menu item

        // all of this below is unused!!
/*
        if (id == R.id.menu_stop) {
            invalidateOptionsMenu();
            return true;

        } else if (id == R.id.menu_scan) {
            // todo: this is probably unused

            /*
             * mScanDeviceDialog is opened
             *   -> displays all discovered devices
             */
            //mScanDeviceDialog.show();
        /*
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, mDeviceFragment)
                .commit();
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }

    // this version uses the new non-deprecated API
    private ScanCallback mScanCallback = new ScanCallback() {
        float LDSA = 0;
        float RH = 0;
        float Temperature = 0;
        float BatteryVoltage = 0;
        int   errorcode = 0;
        int number = 0;
        int diameter = 0;

        @Override
        // callback when a BLE advertisement has been found
        public void onScanResult(int callbackType, ScanResult result) {
            Log.i("callbackType", String.valueOf(callbackType));
            Log.i("result", result.toString());
            final BluetoothDevice device = result.getDevice();
            final android.bluetooth.le.ScanRecord scanRecord = result.getScanRecord();

            ((Activity) mainContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // use lastreceived and buffer to parse data properly
                    if(device == null /*|| device.getName() == null*/) {
                        return;
                    }
                    System.out.println("device:" + device.toString());
                    System.out.println("name:" + device.getName());
                    //if(device.getAddress().contains("00:07:80")) {

                    // name appears to be
                    if(device.getName() != null && (device.getName().contains("Partector") || device.getName().contains("P2"))) {
                        //System.out.println("device name is" + device.getName());  // this is "P2" which we should search instead of address!
                        //System.out.println(device.toString());  // this is mac address which we could store to connect to multiple P2s

                        // if(device.getName().contains("Partector")) {     // this would be much nicer, but doesn't work because it first needs a scan result = a connection to the P2
                        //System.out.println("found a P2....");
                        Log.e("partector name", device.getName());

                        String msg = "ascii: ";

                        for (byte b : scanRecord.getBytes())
                            msg += (char) (b & 0xFF);

                        msg = msg.substring(16,36);
                        //System.out.println(msg);
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
                            if(parts.length > 0) {
                                try {
                                    //System.out.println("buffer contains:" + buffer);
                                    for(int i = 0; i<parts.length-1; i++) {
                                        //System.out.println("part " +i + ":" + parts[i]);
                                        //if(containsOnlyNumbers(parts[i]) == false)
                                        if (parts[i].length() == 0)
                                            continue;
                                        mDeviceFragment.displayData(parts[i]);
                                        switch (parts[i].charAt(0)) {
                                            case 'L':
                                                //System.out.println("String:" + parts[i]);
                                                //System.out.println("Parsing:" + parts[i].substring(1));
                                                if(parts[i].endsWith("l")) {

                                                    LDSA = Float.parseFloat(parts[i].substring(1,parts[i].length()-1));
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
                                                if(parts[i].endsWith("t")) {
                                                    Temperature = Float.valueOf(parts[i].substring(1,parts[i].length()-1));
                                                    System.out.println("T " + Temperature);
                                                }
                                                break;
                                            case 'H':
                                                if(parts[i].endsWith("h")) {
                                                    RH = Float.valueOf(parts[i].substring(1,parts[i].length()-1));
                                                    System.out.println("Humidity " + RH);
                                                }
                                                break;
                                            case 'B':
                                                if(parts[i].endsWith("b")) {
                                                    BatteryVoltage = Float.valueOf(parts[i].substring(1,parts[i].length()-1));
                                                    System.out.println("Batt " + BatteryVoltage);
                                                }
                                                break;
                                            case 'E':
                                                errorcode = Integer.valueOf(parts[i].substring(1,parts[i].length()-1));
                                                System.out.println("err " + errorcode);
                                                break;
                                            case 'D':
                                                diameter = Integer.valueOf(parts[i].substring(1,parts[i].length() - 1));
                                                System.out.println("diam " + diameter);
                                                break;
                                            case 'C':
                                                number = Integer.valueOf(parts[i].substring(1,parts[i].length() - 1));
                                                System.out.println("number " + number);
                                                break;
                                        }
                                        //System.out.println(parts[i]);
                                    }

                                }
                                catch(NumberFormatException e) {
                                    System.out.println("number format exception caught" + buffer);
                                }
                            }
                            // 4. keep only last unparsed stuff
                            if(parts.length > 0)
                                buffer = parts[parts.length - 1];
                            //System.out.println("new buffer is " + buffer);
                            // if there was a trailing S, restore it (it was stripped by string.split
                            if(msg.charAt(msg.length() - 1) == 'S')
                                buffer = buffer + 'S';

                        } else {
                             //System.out.println("no new data:" + msg);
                        }

                    }
                }
            });
        }

        @Override
        // todo: is this ever used?
        public void onBatchScanResults(List<ScanResult> results) {
            System.out.println("batch scan results");
            for (ScanResult sr : results) {
                Log.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        // todo: is this ever used?
        // callback for when scan could not be started
        public void onScanFailed(int errorCode) {
            System.out.println("scan failed");
            Log.e("Scan Failed", "Error Code: " + errorCode);
        }
    };


    public boolean containsOnlyNumbers(String str) {
        //It can't contain only numbers if it's null or empty...
        if (str == null || str.length() == 0)
            return false;

        for (int i = 0; i < str.length(); i++) {
            //If we find a non-digit character we return false.
            if (!Character.isDigit(str.charAt(i)))
                return false;
        }
        return true;
    }


    /**
     * This method is called if Activity gets a result from an earlier request
     * @param requestCode request from Activity
     * @param resultCode response
     * @param data data which was part of request
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT
                && resultCode == Activity.RESULT_CANCELED) {
            ((Activity) mainContext).finish();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * This method is called if user presses the BackButton
     */
    /*@Override
    public void onBackPressed()
    {
        super.onBackPressed();
    }*/


    // called in onCreate to setup bluetooth
    private boolean initiate()
    {
        // Use this check to determine whether BLE is supported on the device.
        // Then you can
        // selectively disable BLE-related features.
        if (!mainContext.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            return false;
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) mainContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        return (mBluetoothAdapter != null);
    }

/*    private boolean hasLocationPermissions() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
    }*/
}

