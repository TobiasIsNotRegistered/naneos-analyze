package naneos.analyze;

import java.io.Serializable;
import java.util.Date;
import android.text.format.DateFormat;

/**
    This class represents the Data received by the Partector.
    For every result which comes from the Partector, the class "NaneosScanCallback"
    instantiates a new NaneosDataObject.
 **/
public class NaneosDataObject implements Serializable {

    //data
    private int ID;
    private Date date;
    private float temp;
    private float humidity;
    private float batteryVoltage;
    private float error;
    private float diameter;
    private float numberC;
    private float LDSA;
    private int RSSI;

    //meta
    private boolean isStoredInDB;
    private int serial;
    private String macAddress;

    //empty constructor
    //necessary for firebase!
    NaneosDataObject(){
    }



    /** GETTER & SETTER **/
    private int getID() {
        return ID;
    }

    void setID(int ID) {
        this.ID = ID;
    }

    int getRSSI() {return RSSI; }
    void setRSSI(int RSSI) {this.RSSI = RSSI;}

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

    float getBatteryVoltage() {
        return batteryVoltage;
    }

    void setBatteryVoltage(float batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
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

    public boolean isStoredInDB() {
        return isStoredInDB;
    }

    void setStoredInDB(boolean storedInDB) {
        isStoredInDB = storedInDB;
    }

    int getSerial() {
        return serial;
    }

    void setSerial(int serial) {
        this.serial = serial;
    }

    String getMacAddress() {
        return macAddress;
    }

    void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
}
