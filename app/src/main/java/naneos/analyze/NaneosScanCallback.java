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
//TODO: Use AsyncTask instead of Thread, do not restart on receive! (Right now, a new Thread is used to deserialize the data --> inefficient
public class NaneosScanCallback extends ScanCallback {

    //Data
    private float LDSA;
    private float RH;
    private float Temperature;
    private float BatteryVoltage;
    private int errorcode;
    private int number;
    private int diameter;
    private int currentDataIndex;
    private int serial;

    private String lastreceived = "";    // lastreceived will hold the last valid advertising data received
    private String buffer = "";

    //Meta
    private Runnable deserialization;
    private Thread receiveAndDeserializeBleData;
    private Activity mainActivity;


    public NaneosScanCallback(Activity activity) {
        this.mainActivity = activity;
    }

    @Override
    // callback when a BLE advertisement has been found
    public void onScanResult(int callbackType, final ScanResult result) {

        final BluetoothDevice device = result.getDevice();
        final android.bluetooth.le.ScanRecord scanRecord = result.getScanRecord();

        //Log.d("NaneosScanCallback", "onResult invoked!");

        receiveAndDeserializeBleData = new Thread(new Runnable() {
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
                        NaneosDataObject newData = new NaneosDataObject();
                        newData.setDate(Calendar.getInstance().getTime());
                        newData.setID(currentDataIndex++);
                        newData.setMacAddress(device.getAddress());

                        // 1. remember it
                        lastreceived = msg;
                        // 2. add it to input buffer
                        buffer = buffer + msg;
                        // 3. parse
                        String[] parts = buffer.split("S");
                        if (parts.length > 0) {
                            try {

                                for (int i = 0; i < parts.length - 1; i++) {

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
                                            diameter = Integer.valueOf(parts[i].substring(1, parts[i].length() - 1));

                                            newData.setDiameter(diameter);
                                            break;
                                        case 'C':
                                            number = Integer.valueOf(parts[i].substring(1, parts[i].length() - 1));

                                            newData.setNumberC(number);
                                            break;
                                    }

                                }

                            } catch (NumberFormatException e) {
                                System.out.println("number format exception caught" + buffer);
                            }
                        }
                        // 4. keep only last unparsed stuff
                        if (parts.length > 0)
                            buffer = parts[parts.length - 1];
                        // if there was a trailing S, restore it (it was stripped by string.split
                        if (msg.charAt(msg.length() - 1) == 'S')
                            buffer = buffer + 'S';




                        //Send data to mainAcitivity by making NaneosDataObject Serializable
                        Intent intent = new Intent();
                        intent.setAction(MainActivity.bleDataReceiver.SEND_BLE_DATA);
                        intent.putExtra("newDataObject", newData);
                        mainActivity.sendBroadcast(intent);
                        Log.d("NaneosScanCallback", "Broadcast sent to mainActivity!");
                    }
                }
            }
        });

        receiveAndDeserializeBleData.start();
    }
}
