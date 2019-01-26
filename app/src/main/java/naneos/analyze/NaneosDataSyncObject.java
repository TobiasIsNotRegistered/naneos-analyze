package naneos.analyze;

import java.io.Serializable;
import java.util.Date;
import android.text.format.DateFormat;
import android.util.Log;

/**
 This class represents the Data received by the Partector.
 For every result which comes from the Partector, the class "NaneosScanCallback"
 instantiates a new NaneosDataObject.
 **/
public class NaneosDataSyncObject implements Serializable {

    //data
    private Date date;
    private float temp;
    private float humidity;
    private float error;
    private float diameter;
    private float numberC;
    private float LDSA;
    private float batteryVoltage;

    // stuff that is in NaneosDataObject but not in NaneosDataSyncObject:
    //private int RSSI;
    //private int ID;


    //meta
    //private boolean isStoredInDB;
    //private int serial;
    //private String macAddress;

    //constructor


    //necessary for firebase!
    NaneosDataSyncObject() {

    }

    /*
    NaneosDataSyncObject(NaneosDataObject naneosDataObject){
        date = naneosDataObject.getDate();
        temp = naneosDataObject.getTemp();
        humidity = naneosDataObject.getHumidity();
        error = naneosDataObject.getError();
        diameter = naneosDataObject.getDiameter();
        numberC = naneosDataObject.getNumberC();
        LDSA = naneosDataObject.getLDSA();
        batteryVoltage = naneosDataObject.getBatteryVoltage();
        Log.d("NaneosSync", this.toString());
    }*/

    @Override
    public String toString() {
        return "LDSA: " + getLDSA() + "T: " + getTemp() + "RH: " + getHumidity();
    }

    /** GETTER & SETTER **/

    public Date getDate() {
        return date;
    }

    String getDateAsFirestoreKey(){
        String dayOfTheWeek = (String) DateFormat.format("EEEE", date); // Thursday
        String day          = (String) DateFormat.format("dd",   date); // 20
        String monthString  = (String) DateFormat.format("MMM",  date); // Jun
        String monthNumber  = (String) DateFormat.format("MM",   date); // 06
        String year         = (String) DateFormat.format("yyyy", date); // 2013

        return day + "-" +  monthNumber + "-" + year;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    float getTemp() {
        return temp;
    }

    void setTemp(float temp) {
        this.temp = temp;
    }

    float getHumidity() {
        return humidity;
    }

    void setHumidity(float humidity) {
        this.humidity = humidity;
    }


    public float getError() {
        return error;
    }

    void setError(float error) {
        this.error = error;
    }

    float getDiameter() {
        return diameter;
    }

    void setDiameter(float diameter) {
        this.diameter = diameter;
    }

    float getNumberC() {
        return numberC;
    }

    void setNumberC(float numberC) {
        this.numberC = numberC;
    }

    float getLDSA() {
        return LDSA;
    }

    void setLDSA(float LDSA) {
        this.LDSA = LDSA;
    }
}
