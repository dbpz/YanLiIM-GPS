package li.yan.im.yanliim_gps;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.support.annotation.Nullable;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by COM on 2017/3/30.
 */

public class GPSservice extends Service implements LocationListener {


    protected static final int REQUEST_GET_LAST_LOCATION = 0 ;
    protected static final int REQUEST_GET_SERVICE_STARUS = 10 ;
    protected static final int REQUEST_START_TRACKING = 30 ;
    protected static final int REQUEST_STOP_TRACKING = 31 ;
    boolean isGPSrunning = false;

    double lat=-1, lng=-1, kph=-1, heading=-1;
    long lastPostTime = -1;
    int markerTimes = 0;

    class YanLiPoster extends AsyncTask {
        @Override
        protected Object doInBackground(Object[] params) {
            try {
                URL url = new URL("https://im.yan.li/YanLiIM/Post");
                String datapost = "{\"method\":\"PushMsg\",\"lat\":\"" + lat + "\",\"lng\":\"" + lng + "\",\"msg\":\"\",\"MYPMID\":\"" + TrackingActivity.myPmid +
                        "\",\"room\":\"" + TrackingActivity.myRoom + "\",\"act\":\""+TrackingActivity.myActivity+"\",\"single\":\"no\",\"kph\":\"" + kph + "\",\"heading\":\"" + heading + "\"}";
                byte [] dataBytes = datapost.getBytes();

                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Length", ""+(dataBytes.length));
                urlConnection.connect();
                OutputStream postos = urlConnection.getOutputStream();
                postos.write(dataBytes);
                postos.close();
                int postcode = urlConnection.getResponseCode();
                if(postcode == 200){
                    markerTimes++;
                }
                // android.util.Log.i("YanLiIM.GPS","POST CODE "+postcode+" : "+datapost);
                urlConnection.disconnect();
            }catch(Exception ee){
                android.util.Log.e("YanLiIM.GPS","Unable to post to network", ee);
            }
            return null;
        }
    };

    @Override
    @TargetApi(23)
    public void onLocationChanged(Location location) {
        lat=location.getLatitude();
        lng=location.getLongitude();
        kph=location.getSpeed()*3.6;
        heading=location.getBearing();

        if(System.currentTimeMillis() - lastPostTime > TrackingActivity.myReportInterval && TrackingActivity.myPmid != null && !TrackingActivity.myPmid.contains("PLEASE SCAN FOR THE PMID") && TrackingActivity.myRoom.length()>2){
            new YanLiPoster().execute();
            lastPostTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onDestroy(){
        isGPSrunning = false;
        locManager.removeUpdates(GPSservice.this);
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

    class MyGPScommunication extends Binder {
        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags){
            int method = data.readInt();

            android.util.Log.e("YanLiIM.GPS", "Request:"+method+" by "+GPSservice.this);
            if(method == REQUEST_GET_LAST_LOCATION){
                double [] das = new double[]{lat,lng,kph,heading};
                reply.writeDoubleArray(das);
                reply.writeInt(markerTimes);
            }else if(method == REQUEST_START_TRACKING){
                try{
                    locManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000,3, GPSservice.this);
                }catch(SecurityException se){
                    android.util.Log.e("YanLiIM.GPS", "No permission", se);
                    locManager = null;
                }
                isGPSrunning = true;
            }else if(method == REQUEST_STOP_TRACKING){
                isGPSrunning = false;
                locManager.removeUpdates(GPSservice.this);
            }else if(method == REQUEST_GET_SERVICE_STARUS){
                reply.writeInt(isGPSrunning?1:0);
            }else throw new RuntimeException("Unknown Cmd : "+method);
            return true;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {

        if(null == locManager)locManager = (LocationManager) TrackingActivity.thisContext.getSystemService(TrackingActivity.thisContext.LOCATION_SERVICE);
        android.util.Log.i("YanLiIM.GPS","New Bind Coming: "+intent);

        return new MyGPScommunication();
    }

    @Override
    public boolean onUnbind(Intent intent){
        android.util.Log.i("YanLiIM.GPS","Bind Lost: "+intent);

        return true;
    }

    LocationManager locManager = null;

}
