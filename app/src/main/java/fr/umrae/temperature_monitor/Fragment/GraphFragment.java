package fr.umrae.temperature_monitor.Fragment;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.components.Description;
import com.google.gson.Gson;
import fr.umrae.temperature_monitor.MainActivity;

import android.graphics.Color;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IDataSet;

import fr.umrae.temperature_monitor.R;
import fr.umrae.temperature_monitor.util.CustomValueFormatter;
import fr.umrae.temperature_monitor.dao.DataObj;
import fr.umrae.temperature_monitor.util.XAxisValueFormatter;
import fr.umrae.temperature_monitor.util.YAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.Context.CLIPBOARD_SERVICE;

import androidx.fragment.app.Fragment;

public class GraphFragment extends Fragment implements View.OnClickListener,AdapterView.OnItemSelectedListener {

    ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private MainActivity main;
    boolean init=false;
    Gson gson = new Gson();
    public void setMain(MainActivity main) {
        this.main = main;
    }

    private String asCSV(){
        StringBuilder sb = new StringBuilder("");
        sb.append("\"TIME\",\"TEMPERATURE\"\n\r");
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault());
        for(DataObj d:dataList){
            sb.append(format.format(new Date(d.getDateTime()))).append(",").append(d.getTemperature()).append("\n\r");
        }
        return sb.toString();
    }

    private Button genButton;
    private Button genPrev;
    private Button genNext;
    private Random rnd= new Random();
    boolean last = true;
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.genButton:
                final android.content.ClipboardManager clipboardManager = (ClipboardManager)getActivity().getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("JSON", asCSV());
                clipboardManager.setPrimaryClip(clipData);
                break;
            case R.id.genPrev:
                end = start;
                start = start - range;
                last= false;
                updateRange();
                break;
            case R.id.genNext:
                start = end;
                end = end+range;
                if(end>System.currentTimeMillis()){
                    end = System.currentTimeMillis();
                    start = end - range;
                    last = true;
                }
                updateRange();
                break;
        }
    }
    public void updateRange(){
        mExecutor.execute(new UpdateRangeTask());
    }
    public void append(DataObj data){
        if(!init){
            return;
        }
        updatez(data);
    }

    public void updatez(final DataObj dob) {
        if(last) {
            dataList.add(dob);
            if (dataList.size() > 200) {
                dataList.remove(0);
            }
            updateUI();
        }
    }

    long min = 1000*60;
    long range = 5*min;;
    long end=System.currentTimeMillis();
    long start=end-range;
    public void onItemSelected(AdapterView<?> parent, View view,
                               int pos, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        switch (pos){
            case 0://5m
                range = 5*min;
                break;
            case 1://15m
                range = 15*min;
                break;
            case 2://1h
                range = 60*min;
                break;
            case 3://6h
                range = 6*60*min;
                break;
            case 4://12h
                range = 12*60*min;
                break;
            case 5://24h
                range = 24*60*min;
                break;
            case 6://2d
                range = 2*24*60*min;
                break;
            case 7://7d
                start = 7*24*60*min;
                break;
            case 8://30d
                range = 30*24*60*min;
                break;
        }
        end=System.currentTimeMillis();
        start = end - range;
        updateRange();
    }

    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    public GraphFragment() {
        // Required empty public constructor
    }
    private LineChart mTempChart;
    private String[] mDatesArray;

    private CustomValueFormatter mValueFormatter;
    private YAxisValueFormatter mYAxisFormatter;
    public List<DataObj> dataList=new ArrayList<>();
    Description ee = new Description();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_1, container, false);
    }


    @Override
    public void onViewCreated(View vv, Bundle savedInstanceState){
        ee.setText("");
        genButton = getActivity().findViewById(R.id.genButton);
        genButton.setOnClickListener(this);
        genPrev = getActivity().findViewById(R.id.genPrev);
        genPrev.setOnClickListener(this);
        genNext = getActivity().findViewById(R.id.genNext);
        genNext.setOnClickListener(this);


        Spinner spinner = getActivity().findViewById(R.id.time_range_spinner);
// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.time_range_array, R.layout.spinner_item);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(R.layout.spinner_item);
// Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

        dataList.add(new DataObj(System.currentTimeMillis()));
        mValueFormatter = new CustomValueFormatter();
        mYAxisFormatter = new YAxisValueFormatter();
        mTempChart = (LineChart) getActivity().findViewById(R.id.temp_chart);
        TextView temperatureLabel = (TextView) getActivity().findViewById(R.id.graphs_temp_label);
        temperatureLabel.setText(R.string.temperature);
        updateRange();
        init=true;
        // Inflate the layout for this fragment
    }
    private void setTempChart() {
        if(mTempChart==null){
            return;
        }
        mTempChart.setDescription(ee);
        mTempChart.setDrawGridBackground(false);
        mTempChart.setTouchEnabled(true);
        mTempChart.setDragEnabled(true);
        mTempChart.setMaxHighlightDistance(300);
        mTempChart.setPinchZoom(true);
        mTempChart.getLegend().setEnabled(false);

        XAxis x = mTempChart.getXAxis();
        x.setEnabled(true);
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setValueFormatter(new XAxisValueFormatter(mDatesArray));

        YAxis yLeft = mTempChart.getAxisLeft();
        yLeft.setEnabled(true);
        yLeft.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
        yLeft.setDrawAxisLine(false);
        yLeft.setDrawGridLines(true);
        yLeft.enableGridDashedLine(5f, 10f, 0f);
        yLeft.setGridColor(Color.parseColor("#333333"));
        yLeft.setXOffset(15);
        yLeft.setValueFormatter(mYAxisFormatter);

        mTempChart.getAxisRight().setEnabled(false);

        List<Entry> entries = new ArrayList<>();
        float svg = 0;
        int cnt=0;
        int tot=0;
        for (int i = 0; i < dataList.size(); i++) {
            svg=svg+ dataList.get(i).getTemperature();
            cnt++;
            if(cnt==avgCnt) {
                entries.add(new Entry(tot, svg/cnt));
                tot++;
                svg=0;
                cnt=0;
            }
        }
        if(cnt>0){
            entries.add(new Entry(tot, svg/cnt));
        }
        //Log.i("GF","getTemp entries.size:"+entries.size()+" tot:"+tot+" dataList.size():"+dataList.size());

        LineDataSet set;
        if (mTempChart.getData() != null) {
            mTempChart.getData().removeDataSet(0);
            set = new LineDataSet(entries, "Temp");
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set.setCubicIntensity(0.2f);
            set.setDrawCircles(false);
            set.setLineWidth(2f);
            set.setDrawValues(false);
            set.setValueTextSize(12f);
            set.setColor(Color.parseColor("#E84E40"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);

            LineData data = new LineData(set);
            mTempChart.setData(data);
        } else {
            set = new LineDataSet(entries, "Temp");
            set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            set.setCubicIntensity(0.2f);
            set.setDrawCircles(false);
            set.setLineWidth(2f);
            set.setValueTextSize(12f);
            set.setDrawValues(false);
            set.setColor(Color.parseColor("#E84E40"));
            set.setHighlightEnabled(false);
            set.setValueFormatter(mValueFormatter);

            LineData data = new LineData(set);
            mTempChart.setData(data);
        }
        mTempChart.notifyDataSetChanged();
        mTempChart.invalidate();
    }

    int SIZE_MAX=200;
    int avgCnt = 1;
    private void formatDate() {
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        if (dataList != null) {
            avgCnt = dataList.size() / SIZE_MAX;
            if(avgCnt==0){
                avgCnt = 1;
            }
            int mSize = dataList.size()/avgCnt;
            mDatesArray = new String[mSize];
            int a=0;
            int cnt = 0;
            for (int i = 0; i < dataList.size(); i++) {
                cnt++;
                if(cnt==avgCnt) {
                    cnt=0;
                    Date date = new Date(dataList.get(i).getDateTime());
                    String day = format.format(date);
                    mDatesArray[a] = day;
                    a++;
                }
            }
            //Log.i("GF","date.size:"+mDatesArray.length +" avgCnt:"+avgCnt);
        }
    }



    private void toggleValues() {
        for (IDataSet set : mTempChart.getData().getDataSets()) {
            set.setDrawValues(!set.isDrawValuesEnabled());
        }
        mTempChart.invalidate();
    }

    private void toggleYAxis() {
        mTempChart.getAxisLeft().setEnabled(!mTempChart.getAxisLeft().isEnabled());
        mTempChart.invalidate();
    }


    private void updateUI() {
        formatDate();
        setTempChart();
    }

    private class UpdateRangeTask implements Runnable {
        @Override
        public void run() {
            dataList = main.getDb().dataDao().getRangeDevice(start, end, main.getCurrentDevice());
            if(dataList.isEmpty()){
                dataList.add(new DataObj(System.currentTimeMillis()));
            }
            main.runOnUiThread(GraphFragment.this::updateUI);
        }
    }

}
