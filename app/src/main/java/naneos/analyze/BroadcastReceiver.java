package naneos.analyze;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;

//This class is to listen to statechanges on the BluetoothAdapter and to enable the app to wait on Bluetooth
//https://stackoverflow.com/questions/36735067/how-to-wait-till-bluetooth-turn-on
class BroadcastReceiver extends android.content.BroadcastReceiver {

    Context context;

    public BroadcastReceiver(Context context){
        this.context = context;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                    BluetoothAdapter.ERROR);
            switch (bluetoothState) {
                case BluetoothAdapter.STATE_ON:
                    //Bluethooth is on, now you can perform your tasks
                    Intent startReceivingData = new Intent(context, ReceiveDataFromBleActivity.class);
                    context.startActivity(startReceivingData);

                    break;
            }
        }
    }


}