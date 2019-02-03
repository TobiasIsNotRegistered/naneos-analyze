package naneos.analyze;

import android.text.format.DateFormat;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
    This class accepts Data received by the Partector,
    and adds it to an internal list so that an average can be computed
    when data should be uploaded to the cloud.
 **/

public class NaneosDataAverageObject implements Serializable {

    private int serial;

    List<Date> dateList = new ArrayList<>();
    List<Float> temperatureList = new ArrayList<>();
    List<Float> humidityList = new ArrayList<>();
    List<Float> batteryVoltageList = new ArrayList<>();
    List<Integer> errorList = new ArrayList<>();
    List<Float> diameterList = new ArrayList<>();
    List<Float> numberConcentrationList = new ArrayList<>();
    List<Float> LDSAList =  new ArrayList<>();
    List<Integer> RSSIList = new ArrayList<>();


    // constructor
    NaneosDataAverageObject(){
    }

    // reset function
    void reset() {
        // clear all data in the lists
        dateList.clear();
        temperatureList.clear();
        humidityList.clear();
        batteryVoltageList.clear();
        errorList.clear();
        diameterList.clear();
        numberConcentrationList.clear();
        LDSAList.clear();
        RSSIList.clear();
    }

    void setRSSI(int RSSI) {RSSIList.add(RSSI);}
    int getRSSI() {
        if(RSSIList.size() == 0)
            return -200;
        int RSSIsum = 0;
        for(int RSSI : RSSIList)
            RSSIsum += RSSI;
        return RSSIsum / RSSIList.size();
    }

    void setDate(Date date) {dateList.add(date); }
    Date getDate() {
        if(dateList.size() == 0)
            return null;
        long dateSum = 0;
        for(Date date : dateList)
            dateSum += date.getTime();
        dateSum = dateSum / dateList.size();
        return new Date(dateSum);
    }

    void setError(int error) {errorList.add(error);}
    int getError() {
        if(errorList.size() == 0)
            return 0;
        int errorOr = 0;
        for(int error : errorList)
            errorOr |= error;
        return errorOr;
    }

    void setTemperature(float temperature) {temperatureList.add(temperature);}
    float getTemperature() {
        if(temperatureList.size() == 0)
            return 0;
        float temperatureSum = 0;
        for(float temperature : temperatureList)
            temperatureSum += temperature;
        return temperatureSum/temperatureList.size();
    }

    void setHumidity(float humidity) {humidityList.add(humidity);}
    float getHumidity() {
        if(humidityList.size() == 0)
            return 0;
        float sum = 0;
        for(float h : humidityList)
            sum += h;
        return sum/humidityList.size();
    }

    void setDiameter(float diameter) {diameterList.add(diameter);}
    float getDiameter() {
        if(diameterList.size() == 0)
            return 0;
        float sum = 0;
        for(float h : diameterList)
            sum += h;
        return sum/diameterList.size();
    }

    void setLDSA(float LDSA) {LDSAList.add(LDSA);}
    float getLDSA() {
        if(LDSAList.size() == 0)
            return 0;
        float sum = 0;
        for(float h : LDSAList)
            sum += h;
        return sum/LDSAList.size();
    }

    void setNumberConcentration(float numberConcentration) {numberConcentrationList.add(numberConcentration);}
    float getNumberConcentration() {
        if(numberConcentrationList.size() == 0)
            return 0;
        float sum = 0;
        for(float h : numberConcentrationList)
            sum += h;
        return sum/numberConcentrationList.size();
    }

    void setBatteryVoltage(float b) {batteryVoltageList.add(b);}
    float getBatteryVoltage() {
        if(batteryVoltageList.size() == 0)
            return 0;
        float sum = 0;
        for(float h : batteryVoltageList)
            sum += h;
        return sum/batteryVoltageList.size();
    }

    int getSerial() {
        return serial;
    }

    void setSerial(int serial) {
        this.serial = serial;
    }
}
