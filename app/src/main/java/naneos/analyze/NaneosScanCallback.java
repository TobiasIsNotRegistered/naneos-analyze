package naneos.analyze;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class NaneosScanCallback extends ScanCallback {
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

    Context mainContext;
    List<DataObject> dataList;
    TextView amountOfLocalData;

    //data
    private ListView listView;
    private ArrayAdapter<DataObject> adapter;

    public NaneosScanCallback(Context context, List<DataObject> dataList, ArrayAdapter adapter, ListView listView){
        this.mainContext = context;
        this.dataList = dataList;
        this.adapter = adapter;
        this.listView = listView;
//        amountOfLocalData = (TextView)((Activity)context).findViewById(R.id.textView_amountOfDataObjects);

    }


}
