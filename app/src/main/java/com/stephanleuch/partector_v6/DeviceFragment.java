package com.stephanleuch.partector_v6;
// this is the "main" program, i.e. the main screen view

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Main fragment of User Interface which also does most of the action
 * warning: uses or overrides a deprecated API
 */
public class DeviceFragment extends Fragment {
    private final static String TAG = "myDeviceFragment";

    public final int COLOR_GREEN   = 0;
    public final int COLOR_YELLOW  = 1;
    public final int COLOR_RED     = 2;

    private View view;
    public TextView ldsaValue;
    public TextView errorValue;
    public ImageView earthImage;
    public int currentError;
    public boolean mActive = true;

    public ArrayList<GPSrecord> gpslist = new ArrayList<>();        // holds GPS records to match
    public ArrayList<TextView> ciphers = new ArrayList<>();

    public LocationManager locationManager;
    public Location currentLocation;

    // Required empty public constructor
    public DeviceFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
        currentLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if(currentLocation == null){
            currentLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }
    }

    /*@Override
    public void onResume()
    {
        super.onResume();
    }*/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_device, container, false);

        errorValue = (TextView) view.findViewById(R.id.data_value_error);
        errorValue.setOnClickListener(new TextView.OnClickListener() {
            @Override
            public  void onClick(View view){
                String errorList = "";
                boolean[] errors = int2booleanArray(currentError);
                for (int i = 0;i < errors.length-1;i++){
                    if(errors[i]) errorList =  errorList + "- " + getErrorMessage(i) + "\n";
                }

                new AlertDialog.Builder(getActivity())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("Errors")
                        .setMessage(errorList)
                        .setNegativeButton("back", null)
                        .show();
            }
        });

        ldsaValue = (TextView) view.findViewById(R.id.ldsa_value);
        earthImage = (ImageView) view.findViewById(R.id.earth_image);
        earthImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportToGoogleEarth();
            }
        });
        return view;
    }

    private void exportToGoogleEarth() {
        int n = 0;

        if(AppIsInstalled("com.google.earth")){
            //String filename = myFilename+"."+mySuffix;
            String path;
            String filename = "partector2.kml";
            String newfilename;

            // TODO: find out if this is the right place to store to?!

            if(isExternalStorageWritable()) {
                System.out.println("external storage available");
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).toString();


            } else {  // use internal storage instead
                System.out.println("using internal storage");
                path = "/storage/emulated/0/KML-files";
            }

           //String path ="/storage/documents/KML-files";
            //String path = "/internal storage/KML-files";

            File myDirectory = new File(path);

            if (!myDirectory.exists()){
                if(!myDirectory.mkdir()){
                    Log.w(TAG, "unable to create GoogleEarthFile Directory");
                }
            }
            System.out.println("path is " + myDirectory.toString());


            // TODO: try to create a file with a unique filename, e.g. by finding out if the file we create already exists
            // File file = new File(fileDirectory, "file.txt");
           // if (file.exits()) {

           // } else...

            File f = new File(path, filename);
            newfilename = filename;
            System.out.println("trying " + filename);

            while (f.exists()) {
                newfilename = "partector_" + n + ".kml";
                System.out.println("trying" + newfilename);
                f = new File(path, newfilename);
                n++;
            }
            filename = newfilename;
            System.out.println("new file name is " + newfilename);

            GoogleEarthFile myFile = new GoogleEarthFile(path+"/"+filename);
            System.out.println("file is " + myFile.toString());

            try{
                if(!myFile.createNewFile()){
                    Log.w(TAG, "unable to create GoogleEarthFile");
                }

                if(!myFile.write(gpslist)){
                    Log.w(TAG, "unable to write to GoogleEarthFile");
                }

                Toast.makeText(getActivity().getBaseContext(), "File saved successfully!", Toast.LENGTH_SHORT).show();
                myFile.open(getActivity().getBaseContext());

            } catch (Exception e){
                e.printStackTrace();
            }
        }

        else{
            Toast.makeText(getActivity().getBaseContext(),"You have to install Google Earth",Toast.LENGTH_LONG).show();
        }
    }

    public File getKMLStorageDir(String filename) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), filename);
        if (!file.mkdirs()) {
            //Log.e(LOG_TAG, "Directory not created");
            System.out.println("directory not created");
        }
        return file;
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    public boolean AppIsInstalled(String appName){
        PackageManager pm = getActivity().getBaseContext().getPackageManager();
        boolean installed;
        try {
            pm.getPackageInfo(appName, PackageManager.GET_ACTIVITIES);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    public void animatedMarginChange(View v, int l, int t, int r, int b)
    {
        if (v.getLayoutParams() instanceof ViewGroup.MarginLayoutParams)
        {
            final int newLeftMargin = l;
            final int newTopMargin = t;
            final int newRightMargin = r;
            final int newBottomMargin = b;
            final View thisView = v;

            final int oldTopMargin = ((ViewGroup.MarginLayoutParams) v.getLayoutParams()).topMargin;

            Animation slide = new Animation() {

                @Override
                protected void applyTransformation(float interpolatedTime, Transformation t) {

                    ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) thisView.getLayoutParams();
                    p.setMargins(newLeftMargin , (int) ((newTopMargin  - oldTopMargin) * interpolatedTime + oldTopMargin), newRightMargin, newBottomMargin);
                    thisView.setLayoutParams(p);
                }
            };
            slide.setDuration(500); // in ms
            v.startAnimation(slide);
        }
    }


    @Override
    public void onDetach()
    {
        ciphers.clear();
        super.onDetach();
    }

    public void displayData(String data) {
        // data is being sent here from NaneosActivity.java, here we just do the display
        // NaneosActivity has already checked that String data only contains digits
        FrameLayout timeDisplay = (FrameLayout) view.findViewById(R.id.time_display);
        setTime(timeDisplay);
        String str;

        try {

            if (data != null && data.length() > 0) {
                System.out.println("display data: " + data);

                // get enclosed number between the identifiers, e.g. "L" and "l"
                str = data.substring(1,data.length()-1);
                switch (data.charAt(0)) {
                    // todo: Add D d (diameter) + C c (number)
                    case 'L':
                        //System.out.println("String:" + parts[i]);
                        //System.out.println("Parsing:" + parts[i].substring(1));
                        //LDSA = Float.parseFloat(data.substring(1));
                        //System.out.println("LDSA " + LDSA);
                           if(data.endsWith("l") && containsOnlyNumbers(str)) {
                                displayLDSA(data.substring(1, data.length()-1));
                                // do color coding of display
                                float value = Float.valueOf(str);
                                if(mActive) {
                                    if (value < 50)
                                        ldsaValue.setBackground(getResources().getDrawable(R.drawable.data_value_background_green));
                                    else if (value < 250)
                                        ldsaValue.setBackground(getResources().getDrawable(R.drawable.data_value_background_yellow));
                                    else
                                        ldsaValue.setBackground(getResources().getDrawable(R.drawable.data_value_background_red));
                                }

                               try {
                                   if(currentLocation != null)
                                       gpslist.add(new GPSrecord((long) 0, currentLocation.getLongitude(), currentLocation.getLatitude(), value));
                               }
                               catch(Exception e) {
                                   System.out.println("exception " + e.toString());
                                   if(currentLocation == null)
                                       System.out.println("current location is null");
                               }
                           }
                        else {
                               System.out.println("not only numbers in " + str);
                           }
                        break;
                    case 'T':
                        if(data.endsWith("t") && containsOnlyNumbers(str))
                            displayT(str);
                        break;
                    case 'H':
                        if(data.endsWith("h") && containsOnlyNumbers(str))
                            displayRH(str);
                        break;
                    case 'B':
                        if(data.endsWith("b") && containsOnlyNumbers(str))
                            displayBattery(str);
                        break;
                    case 'E':
                        if(data.endsWith("e") && containsOnlyNumbers(str))
                            displayError(str);
                        break;
                    case 'C':
                        if(data.endsWith("c") && containsOnlyNumbers(str))
                            displayNumber(str);
                        break;
                    case 'N':
                        if(data.endsWith("n") && containsOnlyNumbers(str))
                            displaySerialNumber(str);
                        break;
                    case 'D':
                        if(data.endsWith("d") && containsOnlyNumbers(str))
                            displayDiameter(str);
                        break;
                }
            }
        }
        catch (Exception e) {
            System.out.println("Displaydata: exception caught " + e.toString());
        }
    }

    public void displayRH(String str) {
        if(!mActive)
            return;
        TextView tv = (TextView) view.findViewById(R.id.rh_value);
        tv.setText(str);
    }

    public void displayT(String str) {
        if(!mActive)
            return;
        TextView tv = (TextView) view.findViewById(R.id.temperature_value);
        tv.setText(str);
    }

    public void displayError(String str) {
        if(!mActive)
            return;
        int err = Integer.parseInt(str);
        if(err != 0) {
            errorValue.setVisibility(View.VISIBLE);
            errorValue.setText("ERROR\n"+err);
            currentError = err;
        } else {
            errorValue.setVisibility(View.INVISIBLE);
        }
    }

    public void displayBattery(String str) {
        if(!mActive)
            return;
        TextView tv = (TextView) view.findViewById(R.id.ubatt_value);
        tv.setText(str);
    }
    public void displayNumber(String str) {
        if(!mActive)
            return;
        TextView tv = (TextView) view.findViewById(R.id.number);
        tv.setText(str);
    }
    public void displaySerialNumber(String str) {
        if(!mActive)
            return;
        TextView tv = (TextView) view.findViewById(R.id.serialnumber);
        tv.setText(str);
    }
    public void displayDiameter(String str) {
        if(!mActive)
            return;
        TextView tv = (TextView) view.findViewById(R.id.diameter);
        tv.setText(str);
    }

    public void displayLDSA(String str) {
        if(!mActive)
            return;
        ldsaValue.setText(str);
    }


    public static boolean[] int2booleanArray(int x) {
        byte bytes;
        bytes = (byte) (x & 0xFF);
        boolean[] bits = new boolean[8];
        for (int i = 0; i < 8; i++) {
            if ((bytes & (1 << (7 - (i % 8)))) > 0)
                bits[i] = true;
        }
        return bits;
    }

    public String getErrorMessage(int error){
        switch (error){
            case 0:
                return "pulse is low";
            case 1:
                return "pulse is high";
            case 2:
                return "humidity is high";
            case 3:
                return "offset is high";
            case 4:
                return "flow is low";
            case 5:
                return "buffer overflow";
            case 6:
                return "SD-Card is missing";
            default:
                return "unknown error";
        }
    }

    /***
     * LocationListener to register location changes
     */
    private LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            currentLocation = location;
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    public void setTime(FrameLayout layout)
    {
        if(!mActive)
            return;
        Calendar c = Calendar.getInstance();
        String hours = String.valueOf(c.get(Calendar.HOUR_OF_DAY));
        while (hours.length() < 2) hours = "0"+hours;
        String minutes = String.valueOf(c.get(Calendar.MINUTE));
        while (minutes.length() < 2) minutes = "0"+minutes;
        String seconds = String.valueOf(c.get(Calendar.SECOND));
        while (seconds.length() < 2) seconds = "0"+seconds;

        CharSequence text = hours+minutes+seconds;
        FrameLayout.LayoutParams params;
        for(int i = 0;i < text.length();i++){
            if(i >= ciphers.size()){
                params = new FrameLayout.LayoutParams(20, 40);
                params.gravity = Gravity.START;
                params.leftMargin = 23 * i + (3*(i - i%2));

                ciphers.add(new TextView(getActivity().getBaseContext()));   // or crash here after an onPause
                ciphers.get(i).setLayoutParams(params);     // this line can make a null pointer exception in particular when google earth is started
                ciphers.get(i).setGravity(Gravity.CENTER);
                ciphers.get(i).setTextColor(Color.WHITE);
                ciphers.get(i).setBackground(getResources().getDrawable(R.drawable.timecipher));
                ciphers.get(i).animate();
                char l = text.charAt(i);
                ciphers.get(i).setText(String.valueOf(l));
                layout.addView(ciphers.get(i));
            } else {
                char l = text.charAt(i);
                ciphers.get(i).setText(String.valueOf(l));
            }
        }
    }

    public boolean containsOnlyNumbers(String str) {
        //It can't contain only numbers if it's null or empty...
        int periods = 0;

        if (str == null || str.length() == 0)
            return false;

        for (int i = 0; i < str.length(); i++) {
            if(str.charAt(i) == '.') {
                periods++;
                continue;
            }
            //If we find a non-digit character we return false.
            if (Character.isDigit(str.charAt(i)))
                continue;
            return false;
        }
        return periods <= 1;
    }
}
