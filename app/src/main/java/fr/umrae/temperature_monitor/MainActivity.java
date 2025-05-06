package fr.umrae.temperature_monitor;

import android.app.AlertDialog;

import androidx.annotation.NonNull;
import androidx.room.Room;

import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import fr.umrae.temperature_monitor.Fragment.MainFragment;
import fr.umrae.temperature_monitor.Fragment.GraphFragment;
import fr.umrae.temperature_monitor.Fragment.SettingsFragment;
import fr.umrae.temperature_monitor.dao.DataDao;
import fr.umrae.temperature_monitor.dao.DataObj;
import fr.umrae.temperature_monitor.serial.UsbSerial;
import fr.umrae.temperature_monitor.dao.AppDatabase;
import fr.umrae.temperature_monitor.util.http.Context;

import java.io.IOException;
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
    public String getCurrentDevice() {
        return currentDevice;
    }

    BottomNavigationView bottomNavigationView;

    //This is our viewPager
    private ViewPager viewPager;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    public void initLog(){
        scheduler.scheduleWithFixedDelay(this, 3, 5, SECONDS);
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

    public void onNewData(DataObj data){
        if(data!=null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        addLog(data.toString());
                        mainFragment.append(data);
                        graphFragment.append(data);
                        scheduler.execute(new InsertDataInDb(data, getDb().dataDao()));
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

    private Context httpContext=new Context("https://umrae.fr/api/v1/");

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
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setupViewPager(viewPager);
        init = true;
        initLog();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serial.close();
    }

    public void setCurrentDevice(String currentDevice) {
        if(!this.currentDevice.equals(currentDevice)){
            graphFragment.updateRange();
        }
        this.currentDevice = currentDevice;

    }

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
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();

    }

    public String getType() {
        return type;
    }

    private static class InsertDataInDb implements Runnable {
        DataObj dob;
        DataDao dataDao;

        public InsertDataInDb(DataObj dob, DataDao dataDao) {
            this.dob = dob;
            this.dataDao = dataDao;
        }

        @Override
        public void run() {
            dataDao.insertAll(dob);
        }
    }
}
