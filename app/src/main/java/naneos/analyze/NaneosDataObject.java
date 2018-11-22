package naneos.analyze;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

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

    //meta
    private boolean isStoredInDB;
    private int serial;

    //empty constructor
    public NaneosDataObject() {
        isStoredInDB = false;
    }

     @Override
    public String toString() {
        return "ID: " + getID() + " /LDSA: " + getLDSA() + "\t" + "Serial: " + getSerial() + "/ is Synced: " + isStoredInDB;
    }

    /** GETTER & SETTER **/
    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public float getTemp() {
        return temp;
    }

    public void setTemp(float temp) {
        this.temp = temp;
    }

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public float getBatteryVoltage() {
        return batteryVoltage;
    }

    public void setBatteryVoltage(float batteryVoltage) {
        this.batteryVoltage = batteryVoltage;
    }

    public float getError() {
        return error;
    }

    public void setError(float error) {
        this.error = error;
    }

    public float getDiameter() {
        return diameter;
    }

    public void setDiameter(float diameter) {
        this.diameter = diameter;
    }

    public float getNumberC() {
        return numberC;
    }

    public void setNumberC(float numberC) {
        this.numberC = numberC;
    }

    public float getLDSA() {
        return LDSA;
    }

    public void setLDSA(float LDSA) {
        this.LDSA = LDSA;
    }

    public boolean isStoredInDB() {
        return isStoredInDB;
    }

    public void setStoredInDB(boolean storedInDB) {
        isStoredInDB = storedInDB;
    }

    public int getSerial() {
        return serial;
    }

    public void setSerial(int serial) {
        this.serial = serial;
    }
}
