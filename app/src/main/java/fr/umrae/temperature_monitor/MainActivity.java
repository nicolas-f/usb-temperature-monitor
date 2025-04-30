package fr.umrae.temperature_monitor;

import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.room.Room;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.reflect.TypeToken;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import fr.umrae.temperature_monitor.Fragment.MainFragment;
import fr.umrae.temperature_monitor.Fragment.GraphFragment;
import fr.umrae.temperature_monitor.Fragment.SettingsFragment;
import fr.umrae.temperature_monitor.dao.BootConfDAO;
import fr.umrae.temperature_monitor.dao.DataObj;
import fr.umrae.temperature_monitor.dao.DataSourceDTO;
import fr.umrae.temperature_monitor.serial.UsbSerial;
import fr.umrae.temperature_monitor.dao.AppDatabase;
import fr.umrae.temperature_monitor.util.MqttHelper;
import fr.umrae.temperature_monitor.util.http.Context;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MainActivity extends AppCompatActivity implements Runnable {

    public static final String TYPE_USB = "USB";
    public static final String TYPE_LWA = "LWA";
    public static final String USB_DEV = "USB";
    private String currentDevice = USB_DEV;
    public static final String  DEV_TYPE_LW = "soilwcs3";
    public static final String  DEV_TYPE_USB = "SWCS3USB";
    public static final String serverUri = "tcp://zerver.io:1883";
    MqttAndroidClient mqttAndroidClient;
    public String getCurrentDevice() {
        return currentDevice;
    }

    BottomNavigationView bottomNavigationView;

    //This is our viewPager
    private ViewPager viewPager;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    public void initLog(){
        scheduler.scheduleAtFixedRate(this, 3, 5, SECONDS);
    }

    @Override
    public void run() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(!connected.get() && currentDevice.equals(USB_DEV)){
                        try {
                            getSerial().connect();
                        } catch (IOException e) {
                            Log.e("main","connButton",e);
                        }

                    }
                }catch (Exception ex){
                    Log.e("main","addLine",ex);
                }
            }
        });
    }

    private String type=null;

    public void addLine(final String log){
        if(log!=null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //Log.i("main", "in:::" + log );
                        addLog(log);
                        String line;
                        if (log.startsWith(">")) {
                            line = log.substring(1,log.length());
                        }else{
                            line = log;
                        }
                        if (!connected.get() && line.length()>3) {
                            Log.i("main", "line:" + line );
                            type = line.substring(0, 3);;
                            if(type.equalsIgnoreCase(TYPE_USB)){
                                settingsFragment.setUsbEnabled(true);
                                settingsFragment.setLwEnabled(false);
                            }else if(type.equalsIgnoreCase(TYPE_LWA)){
                                settingsFragment.setUsbEnabled(false);
                                settingsFragment.setLwEnabled(true);
                            }else{
                                type = null;
                            }
                            if(type!=null) {
                                //android.util.Log.e("main", "DETECTED : " + type);
                                connected.compareAndSet(false,true);
                                mainFragment.setConnected(connected.get());
                            }
                        } else {
                            String[] split = line.split(",");
                            if (split.length > 2) {
                                mainFragment.append(split);
                                graphFragment.append(split);
                                settingsFragment.append(split);
                            }
                        }
                    }catch (Exception ex){
                        Log.e("main","addLine",ex);
                    }
                }
            });
        }
    }
    boolean init = false;
    public void addLog(String log){
        settingsFragment.addLog(log);
    }
    //Fragments

    private GraphFragment graphFragment;
    private MainFragment mainFragment;
    private SettingsFragment settingsFragment;
    private MenuItem prevMenuItem;

    private Context httpContext=new Context("https://zerver.io/api/v1/");

    public Context getHttpContext() {
        return httpContext;
    }

    private UsbSerial serial = new UsbSerial(this);

    public UsbSerial getSerial() {
        return serial;
    }

    public AppDatabase getDb() {
        return db;
    }

    AppDatabase db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "soilSensDb").build();
        //Initializing viewPager
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        //Initializing the bottomNavigationView
        bottomNavigationView = (BottomNavigationView)findViewById(R.id.bottom_navigation);

        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_call:
                                viewPager.setCurrentItem(0);
                                break;
                            case R.id.action_chat:
                                viewPager.setCurrentItem(1);
                                break;
                            case R.id.action_contact:
                                viewPager.setCurrentItem(2);
                                break;
                        }
                        return false;
                    }
                });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (prevMenuItem != null) {
                    prevMenuItem.setChecked(false);
                }
                else
                {
                    bottomNavigationView.getMenu().getItem(0).setChecked(false);
                }
               // Log.d("page", "onPageSelected: "+position);
                bottomNavigationView.getMenu().getItem(position).setChecked(true);
                prevMenuItem = bottomNavigationView.getMenu().getItem(position);

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        if (android.os.Build.VERSION.SDK_INT > 9)
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        setupViewPager(viewPager);
        init = true;
        initLog();
        startMqtt();
    }

    public void setCurrentDevice(String currentDevice) {
        if(!this.currentDevice.equals(currentDevice)){
            graphFragment.updateRange();
        }
        this.currentDevice = currentDevice;

    }
    BootConfDAO conf = null;

    public void reloadDev() {
        Type listType = new TypeToken<ArrayList<DataSourceDTO>>(){}.getType();
        ArrayList<DataSourceDTO> devs = httpContext.doGetRequest("datasources",listType);
        mainFragment.setDss(devs);
    }

    private GoogleSignInClient signInClient;

    public GoogleSignInClient getSignInClient() {
        return signInClient;
    }
    int RC_SIGN_IN = 12345;
    public void signIn() {
        Intent signInIntent = signInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    String TAG = "main";

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
        mainFragment =new MainFragment();
        mainFragment.setMain(this);
        graphFragment =new GraphFragment();
        graphFragment.setMain(this);
        settingsFragment =new SettingsFragment();
        settingsFragment.setMain(this);
        adapter.addFragment(mainFragment);
        adapter.addFragment(graphFragment);
        adapter.addFragment(settingsFragment);
        viewPager.setAdapter(adapter);
    }
    AtomicBoolean connected = new AtomicBoolean(false);

    public boolean isConnected() {
        return connected.get();
    }

    public void alertNotConn(){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage("Device not connected");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();

    }
    public void alertInvalidName(){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage("Device name not valid");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();

    }
    public void alertNotLogin(){
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage("Not logged in");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();

    }
    public String getType() {
        return type;
    }


    public void disconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("main","!!!!!!!DISCONNECTED!!!!!!!!!!!!");
                connected.compareAndSet(true,false);
                mainFragment.setConnected(connected.get());
                type = null;

            }
        });
    }
    MqttHelper mqttHelper;
    private void startMqtt(){
        mqttHelper = new MqttHelper(getApplicationContext());
        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("Mqtt", "connectComplete ");

            }

            @Override
            public void connectionLost(Throwable throwable) {
                Log.e("Mqtt", "conn lost ",throwable);

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
//                Log.i("Mqtt",topic + "Incoming message1: " + new String(message.getPayload()));
                String[] tok = topic.split("/");
                if(tok[1].equals(DEV_TYPE_LW)){
                    byte cmd = Byte.valueOf(tok[4]);
                    if(cmd==7){
                        ByteBuffer buf=ByteBuffer.wrap(message.getPayload());
                        try{//DataObj(String devId, long dateTime, float dp, float ec, float temp, float vwc, int rssi)
                            DataObj dop = new DataObj(tok[2], System.currentTimeMillis(), buf.getShort()/100, buf.getShort()/100, buf.getShort()/100, buf.getShort(), buf.get(),buf.getShort());
                            graphFragment.updatez(dop);
                            if(currentDevice!=null && tok[2].equals(currentDevice)) {
                                mainFragment.append(dop);
                            }
                        } catch (Exception exe){
                            Log.e(TAG,"Message Parse failed: "  +toStr(message.getPayload()),exe);

                        }

                    }
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }
    static String separator = ",";
    public String  toStr(byte[] l) {
        StringBuilder sb = new StringBuilder("(");
        String sep = "";
        for (byte object : l) {
            sb.append(sep).append(object & 0xFF);
            sep = separator;
        }
        return sb.append(")").toString();
    }
}
