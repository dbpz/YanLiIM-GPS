package li.yan.im.yanliim_gps;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Parcel;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

public class TrackingActivity extends AppCompatActivity {
    private static final int COMMAND_SCAN_PMID = 0x1010;
    private static final String IM_BASIC_PREFERENCE_FILE = "YanLiIM.BasicPref";

    public static String myActivity = "running";
    public static String myRoom = null;
    public static String myPmid = null;
    public static long myReportInterval = 15000;
    public static Context thisContext = null;

    private Button trackingBtn = null;
    private Button trackingScan = null;
    private TextView textStatus = null;
    protected IBinder GPSBinder = null;
    protected boolean isTrackingRunning = false;

    ServiceConnection GPSBindConnector = new ServiceConnection(){
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GPSBinder = service;
            getGPSStatus();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            GPSBinder = null;
        }
    };


    private void getGPSStatus(){
        if(GPSBinder != null){
            try {
                Parcel req = Parcel.obtain();
                Parcel res = Parcel.obtain();

                req.writeInt(GPSservice.REQUEST_GET_SERVICE_STARUS);
                GPSBinder.transact(0, req, res, 0);
                int status_gps = res.readInt();
                req.recycle();
                res.recycle();

                isTrackingRunning = status_gps>0;
                trackingBtn.setText(isTrackingRunning ?"STOP TRACKING":"START TRACKING");
            }catch(Exception ee){
                android.util.Log.e("YanLiIM.Main", "Unable to send request to service", ee);
            }
        }
    }

    private void doGPSrunning(boolean set){
        if(GPSBinder != null){
            try {
                Parcel req = Parcel.obtain();
                Parcel res = Parcel.obtain();

                req.writeInt(set?GPSservice.REQUEST_START_TRACKING:GPSservice.REQUEST_STOP_TRACKING);
                GPSBinder.transact(0, req, res, 0);
                req.recycle();
                res.recycle();
            }catch(Exception ee){
                android.util.Log.e("YanLiIM.Main", "Unable to send request to service", ee);
            }
        }
    }


    View.OnClickListener scanBtnListener = new View.OnClickListener(){
        public void onClick(View v) {
            android.util.Log.i("YanLiIM.Main","Scanning QR Code : "+v);
            try {
                Intent intent = new Intent("com.google.zxing.client.android.SCAN");
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                startActivityForResult(intent, COMMAND_SCAN_PMID);
            } catch(Exception e) {
                android.util.Log.e("YanLiIM.Main","Unable to scan QR",e);
            }
        }
    };

    View.OnClickListener trackingBtnListener = new View.OnClickListener(){
        public void onClick(View v) {
            isTrackingRunning = !isTrackingRunning;
            doGPSrunning(isTrackingRunning);
            trackingBtn.setText(isTrackingRunning ?"STOP TRACKING":"START TRACKING");
        }
    };


    public void onRadioIconClicked(View view){
        if(view.getId() != R.id.radioButton_running)((RadioButton)findViewById(R.id.radioButton_running)).setChecked(false);
        if(view.getId() != R.id.radioButton_cycling)((RadioButton)findViewById(R.id.radioButton_cycling)).setChecked(false);
        if(view.getId() != R.id.radioButton_driving)((RadioButton)findViewById(R.id.radioButton_driving)).setChecked(false);
        if(view.getId() != R.id.radioButton_pt)((RadioButton)findViewById(R.id.radioButton_pt)).setChecked(false);
        switch(view.getId()) {
            case R.id.radioButton_running:
                myActivity="running";
                break;
            case R.id.radioButton_cycling:
                myActivity="cycling";
                break;
            case R.id.radioButton_pt:
                myActivity="train";
                break;
            case R.id.radioButton_driving:
                myActivity="driving";
                break;

        }
    }


    public void onRadioButtonClicked(View view) {

        if(view.getId() != R.id.radioButton_120s)((RadioButton)findViewById(R.id.radioButton_120s)).setChecked(false);
        if(view.getId() != R.id.radioButton_40s)((RadioButton)findViewById(R.id.radioButton_40s)).setChecked(false);
        if(view.getId() != R.id.radioButton_15s)((RadioButton)findViewById(R.id.radioButton_15s)).setChecked(false);
        if(view.getId() != R.id.radioButton_3s)((RadioButton)findViewById(R.id.radioButton_3s)).setChecked(false);

        switch(view.getId()) {
            case R.id.radioButton_3s:
                myReportInterval = 3000;
                break;
            case R.id.radioButton_15s:
                myReportInterval = 15000;
                break;
            case R.id.radioButton_40s:
                myReportInterval = 40000;
                break;
            case R.id.radioButton_120s:
                myReportInterval = 120000;
                break;
        }
        myReportInterval = Math.max(3000,myReportInterval);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent){
        if (requestCode == COMMAND_SCAN_PMID && resultCode == RESULT_OK) {
            //Toast.makeText(thisContext, "SCAN : " + intent.getStringExtra("SCAN_RESULT"), Toast.LENGTH_LONG).show();
            String new_url = intent.getStringExtra("SCAN_RESULT");
            if(new_url.startsWith("YanLiIM/")){
                myPmid = new_url.split("/")[2];
                myRoom = new_url.split("/")[1];
                SharedPreferences settings = getSharedPreferences(IM_BASIC_PREFERENCE_FILE, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("User.PMID", myPmid);
                editor.putString("User.Room", myRoom);
                editor.commit();
                textStatus.setText(myPmid+"/"+myRoom);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tracking);
        thisContext = getApplicationContext();

        trackingBtn = (Button)findViewById(R.id.button_start);
        trackingBtn.setOnClickListener(trackingBtnListener);
        trackingScan = (Button)findViewById(R.id.button_scan_code);
        trackingScan.setOnClickListener(scanBtnListener);
        SharedPreferences settings = getSharedPreferences(IM_BASIC_PREFERENCE_FILE, 0);
        myRoom =settings.getString("User.Room", "");
        myPmid =settings.getString("User.PMID", "PLEASE SCAN FOR THE PMID");

        textStatus = (TextView) findViewById(R.id.textView_status);
        textStatus.setText(myPmid+"/"+myRoom);

        Intent intent = new Intent(thisContext,GPSservice.class);
        bindService(intent, GPSBindConnector, Context.BIND_AUTO_CREATE );

    }
}
