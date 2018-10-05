package com.stephanleuch.partector_v6;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used for generating and managing kml files
 * not really necessary for an app that would only
 * send data into the cloud, but I would like this functionality for the app!
 * (Martin Fierz, 3.10.2018)
 */
public class GoogleEarthFile extends File {

    public GoogleEarthFile(String path){
        super(path);
    }

    boolean write(ArrayList<GPSrecord> gpslist) throws Exception{
        if(gpslist.isEmpty()) {
            System.out.println("there is no valid gps data, returning");
            return false;
        }
        try {
            FileOutputStream fos = new FileOutputStream(this);

            WriteHeader(fos, gpslist);
            System.out.println("header written");
            WriteChart(fos, gpslist);
            System.out.println("chart written");
            WriteChartGrid(fos, gpslist, 25);
            System.out.println("grid 25 written");
            WriteChartGrid(fos, gpslist, 50);
            System.out.println("grid 50 written");
            WriteChartGrid(fos, gpslist, 75);
            System.out.println("grid 75 written");
            WriteBody(fos, gpslist);
            System.out.println("body written");
            WriteFooter(fos);
            System.out.println("footer written");

            fos.flush();
            fos.close();

            return true;
        } catch (Exception e){
            throw e;
        }
    }

    // TODO check for gpsArrayList.isempty()!
    static void WriteHeader(FileOutputStream w, ArrayList<GPSrecord> gpsArrayList){
        try {
            w.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes());
            w.write("<kml xmlns=\"http://earth.google.com/kml/2.0\"> <Folder> <name>Google Earth - GPS</name> <open>1</open> <LookAt>".getBytes());
            w.write(("<longitude>" + gpsArrayList.get(0).longitude + "</longitude>  <latitude>" + gpsArrayList.get(0).latitude + "</latitude>  <range>1023.916841486158</range> <tilt>30</tilt>").getBytes());
            w.write("<heading>0.002571939014361242</heading> </LookAt>".getBytes());

            // write some styles
            w.write("<Style id=\"lStyle1\"> <LineStyle><color>88ffffff</color><width>1</width></LineStyle>  <PolyStyle><color>66FFFFFF</color></PolyStyle> </Style>".getBytes());
            w.write("<Style id=\"lStyle2\"> <LineStyle><color>ff0000ff</color><width>5</width></LineStyle>  <PolyStyle><color>660000FF</color></PolyStyle> </Style>".getBytes());

            w.write("\n".getBytes());
            w.write("\n".getBytes());
            w.write("\n".getBytes());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    void WriteChart(FileOutputStream w, ArrayList<GPSrecord> gpsArrayList){

        String separator = System.getProperty("line.separator");
        try {
            // writes a semitransparent chart with height 100m
            String output;
            //long starttime = gpsArrayList.get(0).time_ms;
            w.write("<Placemark id=\"chart\">  <visibility>1</visibility>".getBytes());
            w.write("<styleUrl>#lStyle1</styleUrl>".getBytes());
            w.write("<LineString id=\"linestring1\">  <extrude>1</extrude> <tessellate>1</tessellate> <altitudeMode>relativeToGround</altitudeMode> <coordinates>".getBytes());
            for (int i = 0; i < gpsArrayList.size(); i++) {
                output = gpsArrayList.get(i).longitude + "," + gpsArrayList.get(i).latitude + "," + 100;
                w.write(output.getBytes());
                w.write(" ".getBytes());
                w.write(separator.getBytes());
            }
            w.write("</coordinates> </LineString> </Placemark>".getBytes());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    void WriteChartGrid(FileOutputStream w, ArrayList<GPSrecord> gpsArrayList, int height){

        String separator = System.getProperty("line.separator");
        try {
            // writes a semitransparent chart with height 100m
            String output;
            //long starttime = gpsArrayList.get(0).time_ms;
            w.write("<Placemark id=\"chartgrid\">  <visibility>1</visibility>".getBytes());
            w.write("<styleUrl>#lStyle1</styleUrl>".getBytes());
            w.write("<LineString id=\"linestring1\">  <extrude>0</extrude> <tessellate>1</tessellate> <altitudeMode>relativeToGround</altitudeMode> <coordinates>".getBytes());
            for (int i = 0; i < gpsArrayList.size(); i++) {
                output = gpsArrayList.get(i).longitude + "," + gpsArrayList.get(i).latitude + "," + height;
                w.write(output.getBytes());
                w.write(" ".getBytes());
                w.write(separator.getBytes());
            }
            w.write("</coordinates> </LineString> </Placemark>".getBytes());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    void WriteBody(FileOutputStream w, ArrayList<GPSrecord> gpsArrayList){
        String separator = System.getProperty("line.separator");
        try {
            // writes a polystring
            String output;
            //long starttime = gpsArrayList.get(0).time_ms;

            w.write("<Placemark id=\"Partector+GPS\">  <visibility>1</visibility>".getBytes());
            w.write("<styleUrl>#lStyle2</styleUrl>".getBytes());
            w.write("<LineString id=\"linestring1\">  <extrude>0</extrude> <tessellate>1</tessellate> <altitudeMode>relativeToGround</altitudeMode> <coordinates>".getBytes());

            for (int i = 0; i < gpsArrayList.size(); i++) {
                output = gpsArrayList.get(i).longitude + ","
                        + gpsArrayList.get(i).latitude + ","
                        + gpsArrayList.get(i).measurement;
                w.write(output.getBytes());
                w.write(" ".getBytes());
                w.write(separator.getBytes());
            }

            w.write("</coordinates> </LineString> </Placemark>".getBytes());
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    static void WriteFooter(FileOutputStream w){
        try {
            w.write("\n".getBytes());
            w.write(" </Folder> </kml>".getBytes());
            w.flush();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void open(Context context) {
        Intent viewDoc = new Intent(Intent.ACTION_VIEW);
        viewDoc.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        viewDoc.setDataAndType(Uri.fromFile(this),"application/vnd.google-earth.kml+xml"); // text/plain

        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> apps = pm.queryIntentActivities(viewDoc, PackageManager.MATCH_DEFAULT_ONLY);

        if (apps.size() > 0)
            context.startActivity(viewDoc);
    }
}
