package fr.umrae.temperature_monitor.Fragment;


import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import fr.umrae.temperature_monitor.MainActivity;
import fr.umrae.temperature_monitor.R;
import fr.umrae.temperature_monitor.dao.DataObj;
import fr.umrae.temperature_monitor.dao.DataSourceDTO;
import fr.umrae.temperature_monitor.dao.DeviceObj;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;


public class MainFragment extends Fragment implements View.OnClickListener{

    private MainActivity main;

    public void setMain(MainActivity main) {
        this.main = main;

    }
    public MainFragment() {
        // Required empty public constructor
    }
    public void setConnected(boolean connected){
        if(connText==null){
            return;
        }
       if(connected){
            readButton.setVisibility(View.VISIBLE);
            connText.setText("USB Connected "+main.getType());
        }else{
            connText.setText("USB Disconnected ");
            readButton.setVisibility(View.GONE);
        }

    }

    DecimalFormat df = new DecimalFormat("###.##");
    public void append(DataObj dob){
        if(!init){
            return;
        }
        mTemperatureView.setText(getString(R.string.temp_label, df.format (dob.getTemperature())));
    }

    private TextView mTemperatureView;

    private TextView connText;
//    private Button connButton;
    private Button readButton;
    boolean init = false;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_0, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        weatherConditionsIcons();
        initializeTextView();
        //updateCurrentWeather();
        setConnected(main.isConnected());
    }

    private void updateCurrentWeather() {
        mTemperatureView.setText(getString(R.string.temp_label, "0.00"));

     }


    @Override
    public void onClick(View view)
    {
        //Log.d("page", "onClick: "+view.getId());
        switch (view.getId()) {
//            case R.id.connButton:
//                try {
//                    main.run();
//                } catch (Exception e) {
//                    Log.e("main","connButton",e);
//                }
//                break;
            case R.id.readButton:
                if(!main.isConnected()){
                    main.alertNotConn();
                    return;
                }
                break;
        }
    }
    private void initializeTextView() {
        Typeface weatherFontIcon = Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/weathericons-regular-webfont.ttf");
        Typeface robotoLight = Typeface.createFromAsset(getActivity().getAssets(),
                "fonts/Roboto-Light.ttf");

        readButton =  getActivity().findViewById(R.id.readButton);
        readButton.setOnClickListener(this);
        connText =  getActivity().findViewById(R.id.connText);
        mTemperatureView =  getActivity().findViewById(R.id.main_temp);
        connText.setTypeface(robotoLight);
        mTemperatureView.setTypeface(robotoLight);
        updateCurrentWeather();
        radiogroup = (RadioGroup) getActivity().findViewById(R.id.radiogroup);
        radiogroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                Log.i("MF","Checked change:"+checkedId);
                // This will get the radiobutton that has changed in its check state
                DeviceObj dev = devices.get(checkedId);
                // This puts the value (true/false) into the variable
                Log.i("MF","Checked change1:"+dev);
                if(dev!=null) {
                    main.setCurrentDevice(dev.getDeviceId());
                }
            }
        });
        initDeviceList(new DeviceObj(main.USB_DEV,"USB Device"));
        init=true;
    }
    RadioGroup radiogroup;
    private List<DeviceObj> devices = new ArrayList<>();

    public void setDss(ArrayList<DataSourceDTO> dss) {
        Log.i("MF","setDss:"+dss.size());
        if(dss.size()==0){
            return;
        }
        devices.clear();
        devices.add(new DeviceObj(main.USB_DEV,"USB Device"));
        for(DataSourceDTO ds:dss){
            if(ds.getId()!=null && ds.getType().equals(main.DEV_TYPE_LW)) {
                devices.add(new DeviceObj(ds.getId(), ds.getName()));
            }
        }
        populateDevList();
    }

    private void initDeviceList(DeviceObj dev){
        devices.clear();
        devices.add(dev);
        populateDevList();
    }

    private void populateDevList() {
        if(radiogroup==null){
            return;
        }
        radiogroup.removeAllViews();
        LinearLayout.LayoutParams layoutParams = new RadioGroup.LayoutParams(
                RadioGroup.LayoutParams.WRAP_CONTENT,
                RadioGroup.LayoutParams.WRAP_CONTENT);
        for (int i=0;i<devices.size();i++){
            DeviceObj dev = devices.get(i);
            RadioButton newRadioButton = new RadioButton(this.getContext());
            newRadioButton.setText(dev.getName());
            newRadioButton.setTag(dev.getDeviceId());
            newRadioButton.setId(i);
            if(main!=null && main.getCurrentDevice() !=null && dev.getDeviceId().equals(main.getCurrentDevice())){
                newRadioButton.setChecked(true);
            }
            radiogroup.addView(newRadioButton, layoutParams);
        }

    }

    private String mIconTemp;

    private void weatherConditionsIcons() {
        mIconTemp = getString(R.string.icon_temp);
    }
}
