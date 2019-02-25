package naneos.analyze;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

/**
 * This class extends ScanCallback and overrides its "onScanResult".
 * A new Thread is used to deserialize the result from receiving the ble-advertisement
 * At the end, the new data is being sent to the mainActivity via a BroadcastReceiver
 * Most of the code here is from the old Naneos-App
 */
// TODO: Use AsyncTask instead of Thread, do not restart on receive! (Right now, a new Thread is used to deserialize the data --> inefficient
// TODO MF: this class should own a list of devices

public class NaneosScanCallback extends ScanCallback {
    //Data
    /*private float LDSA;
    private float RH;
    private float Temperature;
    private float BatteryVoltage;
    private int errorcode;
    private int number;
    private int diameter;
    private int serial;*/
   // private int currentDataIndex;


    private String lastreceived = "";    // lastreceived will hold the last valid advertising data received
    //private String buffer = "";

    //Meta
    //private Runnable deserialization;
    private Thread receiveAndDeserializeBleData;
    private Activity mainActivity;

    NaneosScanCallback(Activity activity) {
        this.mainActivity = activity;
    }

    @Override
    // callback when scan fails
    public void onScanFailed(int errorcode) {
        Log.d("onScanFailed", "Scan failed with error code " + errorcode);
        // error codes below for reference
        //SCAN_FAILED_ALREADY_STARTED = 1;
        //SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 2;
        //SCAN_FAILED_INTERNAL_ERROR = 3;
        //SCAN_FAILED_FEATURE_UNSUPPORTED = 4;
    }

    @Override
    // callback when a BLE advertisement has been found
    public void onScanResult(int callbackType, final ScanResult result) {

        // TODO: the two lines below appear dangerous!  they are not in the same place as receiveAndDeserializeBleData
        // TODO: what happens if advertisements are mixed?? (I see strange things in the app...)
        final BluetoothDevice device = result.getDevice();
        final android.bluetooth.le.ScanRecord scanRecord = result.getScanRecord();


        receiveAndDeserializeBleData = new Thread(new Runnable() {
            @Override
            public void run() {
                int RSSI;
                float LDSA;
                float RH;
                float Temperature;
                float BatteryVoltage;
                int errorcode;
                int number;
                int diameter;
                int serial;

                byte [] scanrecord_bytes = new byte[62];
                String deviceToString = new String();
                String deviceName = new String();
                //TODO data is in the wrong place!?

                // use lastreceived and buffer to parse data properly
                if (device == null /*|| device.getName() == null*/) {
                    Log.d("onScanResult", "device was null");
                    return;
                }

                if (device.getName() != null  && device.getName().contains("P2")) {
                    String msg = "";
                    RSSI = result.getRssi();
                    Log.d("onScanResult", "RSSI is " + RSSI);

                   /* for (byte b : scanRecord.getBytes())
                        msg += (char) (b & 0xFF);
                    msg = msg.substring(9, 29);*/

                    scanrecord_bytes = scanRecord.getBytes();
                    System.out.println("-->found " + scanrecord_bytes.length + " bytes in scanrecord");

                    // 1. retrieve manufacturer specific data from advertisement
                    for (byte b : scanrecord_bytes)
                        msg += (char) (b & 0xFF);
                    System.out.println("--> scanrecord" + msg);

// now, we go looking for the manufacturer-specific data
                    int start = 0; // precondition: we are at the start of a packet
                    int length = 0;
                    while(start < 31 && scanrecord_bytes[start+1] != (byte)0xFF) {
                        System.out.println(start + ": length is " + scanrecord_bytes[start]);
                        System.out.println("packet data type is: " + scanrecord_bytes[start+1]);
                        length = scanrecord_bytes[start];
                        start += (length + 1);
                    }

                    // now that we end up here, scanrecord_bytes [start+1] should be 0xFF
                    if(start < 31 && scanrecord_bytes[start + 1] == (byte)0xFF) {
                        // we found manufacturer-specific data
                        length = scanrecord_bytes[start];
                        msg = msg.substring(start+2, start+2+length-1);
                    }
                    else {
                        System.out.println("ill-formated packet found");
                        return;
                    }

                    System.out.println("--> payload found at " + (start+2) + " with length " + length + ":" + msg + " " + msg.length());

                    if (!msg.equals(lastreceived)) {
                        // new data
                        NaneosDataObject newData = new NaneosDataObject();
                        newData.setDate(Calendar.getInstance().getTime());
                        //newData.setID(currentDataIndex++);
                        newData.setMacAddress(device.getAddress());
                        newData.setRSSI(RSSI);

                        // TODO: actually we should first parse the message and only create a naneos data object if it makes sense

                        // 1. remember it
                        lastreceived = msg;

                        // 2. parse
                        String[] parts = msg.split("S");
                        System.out.println("msg is:"+msg);
                        if (parts.length > 0) {
                            try {

                                for (int i = 0; i < parts.length; i++) {
                                    parts[i] = parts[i].trim();
                                    if (parts[i].length() == 0)
                                        continue;

                                    switch (parts[i].charAt(0)) {
                                        case 'L':
                                            if (parts[i].endsWith("l")) {
                                                LDSA = Float.parseFloat(parts[i].substring(1, parts[i].length() - 1));
                                                newData.setLDSA(LDSA);
                                            }
                                            break;
                                        case 'T':
                                            if (parts[i].endsWith("t")) {
                                                Temperature = Float.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                newData.setTemp(Temperature);
                                            }
                                            break;
                                        case 'H':
                                            if (parts[i].endsWith("h")) {
                                                RH = Float.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                newData.setHumidity(RH);
                                            }
                                            break;
                                        case 'B':
                                            if (parts[i].endsWith("b")) {
                                                BatteryVoltage = Float.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                newData.setBatteryVoltage(BatteryVoltage);
                                            }
                                            break;
                                        case 'E':
                                            errorcode = Integer.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                            newData.setError(errorcode);
                                            break;
                                        case 'N':
                                            serial = Integer.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                            newData.setSerial(serial);
                                            break;
                                        case 'D':
                                            System.out.println("length is " + parts[i].length());
                                            if(parts[i].length() > 1 ) {
                                                diameter = Integer.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                                //todo: string index out of bounds exception length = 1 regionstart = 1, regionlength = 1
                                                newData.setDiameter(diameter);
                                            }
                                            break;
                                        case 'C':
                                            number = Integer.valueOf(parts[i].substring(1, parts[i].length() - 1));
                                            newData.setNumberC(number);
                                            break;
                                    }
                                }
                            } catch (NumberFormatException e) {
                                System.out.println("number format exception caught" + msg);
                            }
                        }


                        //Send data to mainAcitivity by making NaneosDataObject Serializable
                        // TODO: is this intent stuff really necessary?? no other way to call back main Activity?
                        // TODO: how did I do this in my old app?
                        Intent intent = new Intent();
                        // the line below gives a lint warning "static member accessed via instance reference"
                        intent.setAction(MainActivity.NaneosBleDataBroadcastReceiver.SEND_BLE_DATA);
                        intent.putExtra("newDataObject", newData);
                        mainActivity.sendBroadcast(intent);
                    }
                }
            }
        });
        receiveAndDeserializeBleData.start();
    }
}