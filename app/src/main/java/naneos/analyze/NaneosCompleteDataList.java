package naneos.analyze;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NaneosCompleteDataList{

    //indexes all received serials
    //size of this list equals the amount of different devices
    List serials = new ArrayList();

    //this list holds other lists corresponding to their serial
    //for each new serial, a new list is generated to hold the respective data
    List<ArrayList<NaneosDataObject>> rawDataMetaList = new ArrayList<ArrayList<NaneosDataObject>>();

    List<ArrayList<NaneosDataObject>> completeDataMetaList = new ArrayList<ArrayList<NaneosDataObject>>();

    public NaneosCompleteDataList(){

    }

    public void push(NaneosDataObject obj){
        //serial not existing --> new Device
        if(!serials.contains(obj.getSerial())){
            serials.add(obj.getSerial());
            rawDataMetaList.add(new ArrayList<NaneosDataObject>());
            rawDataMetaList.get(rawDataMetaList.size()-1).add(obj);


        //Serial existing --> already got data from this device
        //List ist already existing for this device
        }else if(serials.contains(obj.getSerial())){
            int dataIndex = serials.indexOf(obj.getSerial());
            ArrayList<NaneosDataObject> currentData =  rawDataMetaList.get(dataIndex);
            currentData.add(obj);

            if(currentData.size()>9 && currentData.size() % 10 == 0){
                conglomerateData(currentData);
            }
        }
    }



    public void get(){

    }

}
