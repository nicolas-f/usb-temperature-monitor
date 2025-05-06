package fr.umrae.temperature_monitor.Fragment;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import fr.umrae.temperature_monitor.MainActivity;
import fr.umrae.temperature_monitor.R;

import java.util.ArrayList;
import java.util.List;


public class SettingsFragment extends Fragment implements View.OnClickListener {

    private MainActivity main;

    public void setMain(MainActivity main) {
        this.main = main;
    }

    @Override
    public void onClick(View v) {

    }

    public SettingsFragment() {
        // Required empty public constructor
    }

    private TextView cmdLog;

    boolean init = false;

    public TextView getCmdLog() {
        return cmdLog;
    }
    CheckBox showLogCB;
    private List<String> logs = new ArrayList<>(10);
    public void initLog() {

        for (int i = 0; i < 5; i++) {
            logs.add("");
        }
        setLog(false);
    }
    public void setLog(boolean enable) {
        if(enable){
            getCmdLog().setVisibility(View.VISIBLE);
        }else{
            getCmdLog().setVisibility(View.GONE);
        }
    }


    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        cmdLog =  getActivity().findViewById(R.id.cmdLog);
        showLogCB = getActivity().findViewById( R.id.showLogCB );
        showLogCB.setChecked(false);
        showLogCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked){
                setLog(isChecked);
            }
        });
        init = true;
        initLog();
        getCmdLog().setText("\n\n\n\n\n");

    }
    public void addLog(String log){
        if(!init){
            return;
        }

        if(logs.size()>5) {
            logs.remove(0);
        }
        logs.add(log);
        final StringBuilder sb = new StringBuilder("");
        for(int i=0;i<logs.size();i++){
            sb.append(logs.get(i)).append("\n");
        }
        getCmdLog().setText(sb.toString());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_2, container, false);
    }

}
