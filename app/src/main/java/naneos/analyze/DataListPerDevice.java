package naneos.analyze;

import android.util.Log;

import java.util.ArrayList;

//this class holds a list called 'store' which stores the dataObjects according to their serial/macAddress
//this enables receiving data from multiple devices and storing them in seperate lists
//for every device which sends data another instance of this object should be created. Thus, as many instances of this class exist, from as many devices we received data
//TODO: after X amount of dataObjects per serial, a syncOperation with the server can be issued
public class DataListPerDevice {

    private int serial;
    private String macAddress;

    private NaneosDataObject averagedData;
    private NaneosDataObject aggregatedPreviousData;

    ArrayList<NaneosDataObject> store;
    int amountOfSyncedObjects;


    //macAddress is always available, serial not!
    DataListPerDevice() {
        store = new ArrayList<>();
        amountOfSyncedObjects = 0;
    }
    

    @Override
    public String toString() {
        return "SN: " + getSerial()
                + " / " + getMacAddress() + " RSSI: " + aggregatedPreviousData.getRSSI()
                + "\n" + "received: " + store.size()
                + "\tsynced: " + amountOfSyncedObjects
                + "\n"
                + "\n" + "ldsa: " + aggregatedPreviousData.getLDSA()
                + "\t" + "diam: " + aggregatedPreviousData.getDiameter()
                + "\t" + "N: " + (int)aggregatedPreviousData.getNumberC()
                + "\n" + "RH: " + aggregatedPreviousData.getHumidity()
                + "\t" + "voltage: " + aggregatedPreviousData.getBatteryVoltage()
                + "\t" + "status: " + (int)aggregatedPreviousData.getError();
        //aggregatedPreviousData.toString()
    }
    /*
    public String toString() {
        return "Serial: " + getSerial() +  "\t  RSSI: " + aggregatedPreviousData.getRSSI()
                + "\n" + "Mac: " + getMacAddress()
                + "\n" + "received Data: " + store.size()
                + "\n" + "syncedData: " + amountOfSyncedObjects
                + "\n"
                + "\n" + "ldsa: " + aggregatedPreviousData.getLDSA()
                + "\n" + "humidity: " + aggregatedPreviousData.getHumidity()
                + "\n" + "diameter: " + aggregatedPreviousData.getDiameter()
                + "\n" + "voltage: " + aggregatedPreviousData.getBatteryVoltage()
                + "\n" + "numberC: " + aggregatedPreviousData.getNumberC();
    }*/

    public void add(NaneosDataObject objectToAdd) {
        // case0: empty list, first item is being added
        if (store.size() == 0) {
            this.macAddress = objectToAdd.getMacAddress();
            if (objectToAdd.getSerial() != 0) {
                setSerial(objectToAdd.getSerial());
            }
            store.add(objectToAdd);
            averagedData = objectToAdd;
            aggregatedPreviousData = objectToAdd;


            // case1: macAddress is set, size > 0
            //check precondition, dataObject's macAddress must be the same as this lists macAddress
        } else if (!objectToAdd.getMacAddress().equals(macAddress)) {
            throw new IllegalArgumentException("DataListPerDevice: you tried to add a dataObject with macAddress: " + objectToAdd.getMacAddress() + " to this list, which is reserved for dataObjects with macAddress: " + macAddress);
            //list is not empty, macAddress of dataObject equals this lists' macAddress
        } else {
            if (objectToAdd.getSerial() != 0) {
                setSerial(objectToAdd.getSerial());
            }

            copyMissingValuesFromPreviousDataObjects(objectToAdd);

            store.add(objectToAdd);
        }

    }


    /**
     * set previous data if new data is available
     **/
    private void copyMissingValuesFromPreviousDataObjects(NaneosDataObject objectToAdd) {

        aggregatedPreviousData.setLDSA(objectToAdd.getLDSA());
        aggregatedPreviousData.setRSSI(objectToAdd.getRSSI());

        if(aggregatedPreviousData.getSerial() != 0){
            objectToAdd.setSerial(aggregatedPreviousData.getSerial());
        }

        //humidity
        if (objectToAdd.getHumidity() != 0) {
            aggregatedPreviousData.setHumidity(objectToAdd.getHumidity());
        } else {
            objectToAdd.setHumidity(aggregatedPreviousData.getHumidity());
        }

        //voltage
        if (objectToAdd.getBatteryVoltage() != 0) {
            aggregatedPreviousData.setBatteryVoltage(objectToAdd.getBatteryVoltage());
        } else {
            objectToAdd.setBatteryVoltage(aggregatedPreviousData.getBatteryVoltage());
        }

        //diameter
        if (objectToAdd.getDiameter() != 0) {
            aggregatedPreviousData.setDiameter(objectToAdd.getDiameter());
        } else {
            objectToAdd.setDiameter(aggregatedPreviousData.getDiameter());
        }

        //numberC
        if (objectToAdd.getNumberC() != 0) {
            aggregatedPreviousData.setNumberC(objectToAdd.getNumberC());
        } else {
            objectToAdd.setNumberC(aggregatedPreviousData.getNumberC());
        }

        //temp
        if (objectToAdd.getTemp() != 0) {
            aggregatedPreviousData.setTemp(objectToAdd.getTemp());
        } else {
            objectToAdd.setTemp(aggregatedPreviousData.getTemp());
        }
    }


    public NaneosDataObject getAverageObject(int amountOfItemsToAverage) {
        return averagedData;
    }

    int getSerial() {
        return serial;
    }

    private void setSerial(int serial) {
        this.serial = serial;
        for (int i = 0; i < store.size(); i++) {
            if (store.get(i).getSerial() == 0 && serial != 0) {
                store.get(i).setSerial(serial);
            }
        }
    }

    String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
