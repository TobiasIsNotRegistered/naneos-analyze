package naneos.analyze;

import java.text.DateFormat;
import java.util.Date;

public class DataObject {
    public int ID;
    public Date date;
    public int content;

    public DataObject(){

    }

    public DataObject(int ID, Date date, int content){
        this.ID = ID;
        this.date = date;
        this.content = content;
    }

    @Override
    public String toString(){
        return ID + ": "   + content + "\n" + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(date);
    }
}
