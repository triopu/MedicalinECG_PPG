package medicalin.ekg;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import medicalin.ekg.Photoplethysmogram.PeakPhotoplethysmogram;

public class Main extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, NameDialog.NameDialogListener {
    private final static String TAG = Main.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private String deviceAddress;
    private String deviceName;

    FileWriter fw;
    FileWriter fw2;
    String printFormat;

    GraphView graphView;
    private LineGraphSeries<DataPoint> ecgGraph, ppgGraph;
    private double graph2LastXValue = 5d;
    private double graph2LastXValue1 = 5d;
    private String lastPPG = "000";
    private String lastECG = "000";

    private boolean autoScrollX = false;
    private boolean lock = true;
    private int xView = 500;
    private double minX, maxX, minY, maxY;

    private boolean connected = false;
    private boolean record = false;

    private String fileName = "EKG";

    private BluetoothLeService bluetoothLeService;

    private ArrayList<ArrayList<BluetoothGattCharacteristic>> gattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

    private BluetoothGattCharacteristic notifyCharacteristic;

    private static QRSPeak qrsPeak;
    private static QRSOnset qrsOnset;
    private static TEnd tEnd;
    private static CalculateQT calQT;

    private static PeakPhotoplethysmogram peakPPG;
    private double tECG = 0.000;
    private double tPPG = 0.000;

    boolean ppgMode = true;
    boolean ecgMode = true;

    //Part of Signal Processing
    //private SampleData sampleData;
    //private int[] sampleECGData = SampleData.generateData(4);

    //Make a data processing container
    private ArrayList<Integer> processedECGData = new ArrayList<Integer>();
    private ArrayList<Integer> processedPPGData = new ArrayList<Integer>();
    private ArrayList<Double> ecgTime = new ArrayList<Double>();
    private ArrayList<Double> ppgTime = new ArrayList<Double>();

    private List<Integer> annPPG = new ArrayList<Integer>();

    ArrayList<Integer> data1, data2;
    ArrayList<Double> time1, time2;

    double rrECG, hrECG, qtECG;
    double rrPPG, hrPPG;

    int second;
    private long startTime = 0;

    //It's a goAsync part
    BroadcastReceiver.PendingResult result;

    //Boolean to Process the Data.
    boolean process = false;
    boolean unprocess = true;

    boolean ecg = true;
    boolean ppg = false;

    //Initialize TextView
    private TextView HR;
    private TextView RR;
    private TextView QT;

    boolean download = false;
    boolean upload = true;

    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    int countProcess = 0;

    double max_val = 1023.000;
    double min_val = 0.000;

    private void DownloadData() {
        if (download) {
            mDatabase.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    countProcess++;
                    if (countProcess >= 1000) {
                        new SignalProcessing().execute(processedECGData, processedPPGData, ecgTime, ppgTime);
                        countProcess = 0;
                    }
                    String dataDownload = dataSnapshot.child("data").getValue(String.class);
                    if (ecgMode) dataProcess(dataDownload, 1);
                    if (ppgMode) dataProcess(dataDownload, 2);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }

            });
        }
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!bluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            bluetoothLeService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
        }
    };

    @SuppressLint("StaticFieldLeak")
    private class SignalProcessing extends AsyncTask<Object, int[], ArrayList<Integer>> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @SafeVarargs
        @Override
        protected final ArrayList<Integer> doInBackground(Object... integers) {

            //Parse the input of AsyncTask, ECG data in 0 and Time in 1

            data1 = (ArrayList<Integer>) integers[0];
            data2 = (ArrayList<Integer>) integers[1];
            time1 = (ArrayList<Double>) integers[2];
            time2 = (ArrayList<Double>) integers[3];

            Log.d("Save status: ", String.valueOf(record) + " " + String.valueOf(data1.size()) + " | " + String.valueOf(data2.size()));

            if (ppgMode) {

                ArrayList<Integer> dataPPGProcessed = new ArrayList<Integer>();
                for (int l = 0; l < data2.size(); l++){
                    dataPPGProcessed.add((int)(map((double)(data2.get(l)),min_val,max_val,100.00,500.00)));
                }

                peakPPG = new PeakPhotoplethysmogram(data2, time2);
                annPPG = peakPPG.getAnnotation();
                rrPPG = peakPPG.getRrAvr();
                hrPPG = peakPPG.getHr();

                Log.d("HeartRatePPG ",String.valueOf(hrPPG));

                if (record){
                    Log.d("Saving PPG", String.valueOf(time2.size()) + " | " + String.valueOf(data2.size()) + " | " + String.valueOf(annPPG.size()));
                    for (int i = 0; i < annPPG.size(); i++) {
                        try {
                            printFormat = String.format("%.4f\t%d\t%d", time2.get(i), data2.get(i), annPPG.get(i));
                            printFormat = printFormat.replace(',', '.');
                            fw2.append(printFormat).append("\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                processedPPGData = new ArrayList<>();
                ppgTime = new ArrayList<>();
                annPPG = new ArrayList<>();
            }

            if (ecgMode) {
                //If data size is less than 10,cancel AsynTask by return the data
                if (data1.size() < 10) return data1;
                int dataCount = data1.size() - 1;

                qrsPeak = new QRSPeak(time1, data1, dataCount);

                List<Integer> listOfPeak = new ArrayList<Integer>();
                List<Integer> hpVal = new ArrayList<Integer>();
                List<Integer> leftBound = new ArrayList<Integer>();
                List<Integer> rightBound = new ArrayList<Integer>();
                List<Integer> listOfQRSOnset = new ArrayList<Integer>();
                List<Integer> listOfTEnd = new ArrayList<Integer>();

                listOfPeak = qrsPeak.getPeak();
                hpVal = qrsPeak.getHP();
                leftBound = qrsPeak.getLeftBound();
                rightBound = qrsPeak.getRightBound();

                qrsOnset = new QRSOnset(hpVal, leftBound, listOfPeak);
                listOfQRSOnset = qrsOnset.getQRSOnset();

                Log.d("Length of Data T-End:", String.valueOf(data1.size()));
                tEnd = new TEnd(data1, time1, listOfPeak, rightBound);
                listOfTEnd = tEnd.getTEnd();

                calQT = new CalculateQT(data1, time1, listOfPeak, listOfQRSOnset, listOfTEnd);

                //Data Left
                processedECGData = calQT.getUnusedData();
                ecgTime = calQT.getUnusedTime();

                ArrayList<Integer> processedData = calQT.getUsedData();
                ArrayList<Double> processedTime = calQT.getUsedTime();
                List<Integer> annECG = calQT.getAnnECG();

                hrECG = calQT.getHr();
                rrECG = calQT.getRrAvr();
                qtECG = calQT.getQtAvr();

                if (record) {
                    Log.d("Saving ECG", String.valueOf(processedTime.size()) + " | " + String.valueOf(processedData.size()) + " | " + String.valueOf(annECG.size()));
                    for (int i = 0; i < processedData.size(); i++) {
                        try {
                            printFormat = String.format("%.4f\t%d\t%d", processedTime.get(i), processedData.get(i), annECG.get(i));
                            printFormat = printFormat.replace(',', '.');
                            fw.append(printFormat).append("\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    double hr = 0;
                    double qt = 0;
                    double rr = 0;
                    if(ecg || (ecg&&ppg)) {
                        hr = hrECG;
                        rr = rrECG;
                        qt = qtECG;
                    }

                    if (ppg&&!ecg){
                        hr = hrPPG;
                        rr = rrPPG;
                    }
                    if(hr != 0) {
                        QT.setText(" QT: " + String.format("%.3f", qt));
                        RR.setText(" RR: " + String.format("%.3f", rr));
                        HR.setText(" HR: " + String.format("%.0f", hr));
                        if (hr > 100 || hr < 60) {
                            HR.setTextColor(Color.RED);
                        } else {
                            HR.setTextColor(Color.BLACK);
                        }
                    }
                }
            });

            if (!download) result.finish();
            return data1;

        }

        @Override
        protected void onProgressUpdate(int[]... values) {
            //super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(ArrayList integers) {
            //super.onPostExecute(integers);
        }
    }

    // Handles various events fired by the Service.
    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                //ACTION_GATT_CONNECTED: connected to a GATT server.
                connected = true;
                Toast.makeText(getApplicationContext(), "Connected!", Toast.LENGTH_LONG).show();
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                //ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
                connected = false;
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                //ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
                connectGattServices(bluetoothLeService.getSupportedServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read or notification operations.
                String incomeData = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                String[] items = incomeData.split("\\*");
                Log.d("BluetoothData ", Arrays.toString(items));
                if (items.length != 4){
                    Log.d("Items length ",String.valueOf(items.length));
                    return;
                }
                for (int i = 0; i < items.length / 2; i++) {
                    if (!items[i].equals("")) {
                        int async = dataProcess(items[i], 1);
                        if (async == 1) {
                            result = goAsync();
                            new SignalProcessing().execute(processedECGData, processedPPGData, ecgTime, ppgTime);
                        }
                        if (upload) {
                            if (ecg || (ecg && ppg)) mDatabase.child("data").setValue(items[i]);
                        }
                    }
                }

                for (int i = items.length / 2; i < items.length; i++) {
                    if (!items[i].equals("")) {
                        dataProcess(items[i], 2);
                        if (ppg && !(ecg && ppg)) mDatabase.child("data").setValue(items[i]);
                    }
                }
            }
        }
    };

    int dataProcess(String data, int code) {
        int async = 0;                      //Tell the doInBackground async or not
        if (code == 1) {
            if (ecg) graphECG(data);
            if (process) {
                second = getTime(startTime);
                if (second < 5) {
                    //Get the time of ECG data
                    tECG = tECG + 0.005;
                    //Collect data
                    processedECGData.add(Integer.parseInt(data));
                    ecgTime.add(tECG);
                } else {
                    startTime = System.currentTimeMillis();
                    if (processedECGData.size() > 0) {
                        async = 1;              //goAsync if doInBackground ready
                    }
                }
            }
        } else {
            if (ppg) graphPPG(data);
            if (process) {
                tPPG = tPPG + 0.005;
                processedPPGData.add(Integer.parseInt(data));
                ppgTime.add(tPPG);
            }
        }

        return async;
    }

    // Connect the GATT Servive
    private void connectGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        gattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> thegattCharacteristics = gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas = new ArrayList<BluetoothGattCharacteristic>();

            for (BluetoothGattCharacteristic gattCharacteristic : thegattCharacteristics) {
                charas.add(gattCharacteristic);
                uuid = gattCharacteristic.getUuid().toString();

                //My BLE RX_TX is 0000ffe1-0000-1000-8000-00805f9b34fb
                //0000dfb1-0000-1000-8000-00805f9b34fb
                if (uuid.equals("0000ffe1-0000-1000-8000-00805f9b34fb")) {
                    notifyCharacteristic = gattCharacteristic;
                    bluetoothLeService.setCharacteristicNotification(notifyCharacteristic, true);
                }
            }
            gattCharacteristics.add(charas);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private int getTime(long startTime) {
        long millis = System.currentTimeMillis() - startTime;
        int seconds = (int) (millis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return seconds;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main_qt);

        DrawerLayout drawerLayout = findViewById(R.id.main_qt_layout);

        graphInit();

        final Intent intent = getIntent();
        deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

        NavigationView navigationView = findViewById(R.id.navi_main);
        navigationView.setNavigationItemSelectedListener(this);

        HR = (TextView) findViewById(R.id.heart_rate);
        HR.setMovementMethod(new ScrollingMovementMethod());
        RR = (TextView) findViewById(R.id.rr_interval);
        RR.setMovementMethod(new ScrollingMovementMethod());
        QT = (TextView) findViewById(R.id.qt_interval);
        QT.setMovementMethod(new ScrollingMovementMethod());
        HR.setText(" HR: ");
        RR.setText(" RR: ");
        QT.setText(" QT: ");
        HR.setVisibility(View.INVISIBLE);
        RR.setVisibility(View.INVISIBLE);
        QT.setVisibility(View.INVISIBLE);

        DownloadData();

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        if (bluetoothLeService != null) {
            final boolean result = bluetoothLeService.connect(deviceAddress);
            Log.d(TAG, "Connect request results = " + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        bluetoothLeService = null;

        //If crash and recording, try to close the file writer
        if (fw != null) {
            try {
                fw.flush();
                fw.close();
                fw2.flush();
                fw2.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.bt_connect) {
            if (!connected) {
                download = false;
                upload = true;
                bluetoothLeService.connect(deviceAddress);
                ppgMode = true;
                ecgMode = true;
                Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();
            } else {
                bluetoothLeService.disconnect();
                download = true;
                upload = false;
                ppgMode = false;
                ecgMode = true;
                DownloadData();
                Toast.makeText(getApplicationContext(), "Disconnected!", Toast.LENGTH_SHORT).show();
                Toast.makeText(getApplicationContext(), "Download Mode", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.record) {
            if (!record) {
                if (unprocess) {
                    Toast.makeText(getApplicationContext(), "Must be processing!", Toast.LENGTH_SHORT).show();
                } else {
                    //theTime = System.currentTimeMillis() / 1000.00000;
                    record = true;
                    //Try make a new file in the Internal
                    File sdCard = Environment.getExternalStorageDirectory();
                    File dir = new File(sdCard.getAbsolutePath());
                    File file = new File(dir, "/" + fileName + "_ECG.txt");
                    File file2 = new File(dir, "/" + fileName + "_PPG.txt");

                    Log.d("File is", String.valueOf(file));

                    try {
                        fw = new FileWriter(file, true);
                        fw2 = new FileWriter(file2, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    Toast.makeText(getApplicationContext(), "Recording: " + fileName, Toast.LENGTH_SHORT).show();
                }
            } else {
                record = false;
                startTime = 0;
                if (fw != null) {
                    try {
                        fw.flush();
                        fw.close();
                        fw2.flush();
                        fw2.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(), "Stopped", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Can't be stopped", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (id == R.id.name_edit) {
            openDialog();
        } else if (id == R.id.process) {
            if (!process) {
                process = true;
                unprocess = false;
                HR.setVisibility(View.VISIBLE);
                RR.setVisibility(View.VISIBLE);
                QT.setVisibility(View.VISIBLE);
                double theTime = System.currentTimeMillis() / 1000.00000;
                Toast.makeText(getApplicationContext(), "Processing data...", Toast.LENGTH_SHORT).show();
            } else if (!unprocess) {
                process = false;
                unprocess = true;
                HR.setVisibility(View.INVISIBLE);
                RR.setVisibility(View.INVISIBLE);
                QT.setVisibility(View.INVISIBLE);
                Toast.makeText(getApplicationContext(), "Stop processing data...", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.ecg_switch) {
            if (!ecg) {
                ecg = true;
                graph2LastXValue = 0;
                Toast.makeText(getApplicationContext(), "ECG activated...", Toast.LENGTH_SHORT).show();
            } else {
                ecg = false;
                ecgGraph.resetData(new DataPoint[]{new DataPoint(0, Double.parseDouble(lastECG))});
                Toast.makeText(getApplicationContext(), "ECG deactivated...", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.ppg_switch) {
            if (!ppg) {
                ppg = true;
                graph2LastXValue1 = 0;
                Toast.makeText(getApplicationContext(), "PPG activated...", Toast.LENGTH_SHORT).show();
            } else {
                ppg = false;
                ppgGraph.resetData(new DataPoint[]{new DataPoint(0, Double.parseDouble(lastPPG))});
                Toast.makeText(getApplicationContext(), "PPG deactivated...", Toast.LENGTH_SHORT).show();
            }
        } else if (id == R.id.download) {
            if (ppgMode) {
                ecgMode = true;
                ppgMode = false;
                ecg = true;
                ppg = false;
                download = true;
                upload = false;
                ppgGraph.resetData(new DataPoint[]{new DataPoint(0, Double.parseDouble(lastPPG))});
                ecgGraph.resetData(new DataPoint[]{new DataPoint(0, Double.parseDouble(lastECG))});
                DownloadData();
                Toast.makeText(getApplicationContext(), "Download ECG...", Toast.LENGTH_SHORT).show();
            } else {
                ecgMode = false;
                ppgMode = true;
                ecg = false;
                ppg = true;
                download = true;
                upload = false;
                ppgGraph.resetData(new DataPoint[]{new DataPoint(0, Double.parseDouble(lastPPG))});
                ecgGraph.resetData(new DataPoint[]{new DataPoint(0, Double.parseDouble(lastECG))});
                DownloadData();
                Toast.makeText(getApplicationContext(), "Download PPG...", Toast.LENGTH_SHORT).show();
            }
        }

        DrawerLayout drawer = findViewById(R.id.main_qt_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void openDialog() {
        NameDialog nameDialog = new NameDialog();
        nameDialog.show(getSupportFragmentManager(), "Name Dialog");
    }

    @Override
    public void applyText(String namefile) {
        fileName = namefile;
        Toast.makeText(getApplicationContext(), "Your file name is " + fileName, Toast.LENGTH_SHORT).show();
    }

    private void graphECG(String item) {

        if (graph2LastXValue >= xView) {
            graph2LastXValue = 0;
            ecgGraph.resetData(new DataPoint[]{new DataPoint(graph2LastXValue, Double.parseDouble(item))});
        } else {
            graph2LastXValue += 1d;
        }
        lastECG = item;
        ecgGraph.appendData(new DataPoint(graph2LastXValue, Double.parseDouble(item)), autoScrollX, 500);
    }

    private void graphPPG(String item) {

        if (graph2LastXValue1 >= xView) {
            graph2LastXValue1 = 0;
            ppgGraph.resetData(new DataPoint[]{new DataPoint(graph2LastXValue1, Double.parseDouble(item))});
            if(processedPPGData.size() > 1) {
                min_val = Collections.min(processedPPGData);
                max_val = Collections.max(processedPPGData);
                Log.d("Min-Max ",String.valueOf(min_val)+" | "+String.valueOf(max_val));
            }
        } else {
            graph2LastXValue1 += 1d;
        }
        lastPPG = item;
        double dataplot = map(Double.parseDouble(item),min_val,max_val,100,400);
        ppgGraph.appendData(new DataPoint(graph2LastXValue1, dataplot), autoScrollX, 500);
    }

    double map(double x, double in_min, double in_max, double out_min, double out_max) {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

    private void graphInit() {
        graphView = findViewById(R.id.graph);
        ecgGraph = new LineGraphSeries<>();
        ppgGraph = new LineGraphSeries<>();
        graphView.addSeries(ecgGraph);
        graphView.addSeries(ppgGraph);

        ecgGraph.setThickness(1);
        ecgGraph.setColor(Color.GREEN);

        ppgGraph.setThickness(1);
        ppgGraph.setColor(Color.YELLOW);

        graphView.getViewport().setScrollable(true);

        graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graphView.getGridLabelRenderer().setVerticalLabelsVisible(false);

        graphView.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.BOTH);
        graphView.getViewport().setDrawBorder(false);

        graphView.getGridLabelRenderer().setGridColor(Color.WHITE);


        minX = 0;
        maxX = 500;
        minY = 0;
        maxY = 500;

        makeBorder(graphView, minX, maxX, minY, maxY);

        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMinY(minY);
        graphView.getViewport().setMaxY(maxY);

        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(minX);
        graphView.getViewport().setMaxX(maxX);

        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graphView);
        staticLabelsFormatter.setVerticalLabels(new String[]{"", "", "", "", "", ""});
        staticLabelsFormatter.setHorizontalLabels(new String[]{"", "", "", "", "", ""});
        graphView.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);
        graphView.getGridLabelRenderer().setVerticalLabelsVisible(false);
        graphView.getGridLabelRenderer().setHorizontalLabelsVisible(false);

        double[] centerX = {100, 200, 300, 400};
        double tickWidthY = 20;
        double tickLengthY = 2;

        makeSecondTickY(centerX, minY, maxY, tickWidthY, tickLengthY);

        double[] centerY = {100, 200, 300, 400};
        double tickWidthX = 20;
        double tickLengthX = 2;

        makeSecondTickX(centerY, minX, maxX, tickWidthX, tickLengthX);
    }

    private void makeSecondTickY(double[] center, double minY, double maxY, double tickWidth, double tickLength) {
        for (int i = 0; i < center.length; i++) {
            double y = minY;
            while (y < maxY) {
                y = y + tickWidth;
                int thickness = 1;
                LineGraphSeries<DataPoint> tick = new LineGraphSeries<>(new DataPoint[]{
                        new DataPoint(center[i] - tickLength, y),
                        new DataPoint(center[i] + tickLength, y)
                });
                graphView.addSeries(tick);
                tick.setThickness(thickness);
                tick.setColor(Color.WHITE);
            }
        }
    }

    private void makeSecondTickX(double[] center, double minX, double maxX, double tickWidth, double tickLength) {
        for (int i = 0; i < center.length; i++) {
            double x = minX;
            while (x < maxX) {
                x = x + tickWidth;
                int thickness = 1;
                LineGraphSeries<DataPoint> tick = new LineGraphSeries<>(new DataPoint[]{
                        new DataPoint(x, center[i] - tickLength),
                        new DataPoint(x, center[i] + tickLength)
                });
                graphView.addSeries(tick);
                tick.setThickness(thickness);
                tick.setColor(Color.WHITE);
            }
        }
    }

    private void makeBorder(GraphView graphView, double startX, double endX, double startY, double endY) {
        int thickness = 4;
        LineGraphSeries<DataPoint> series1 = new LineGraphSeries<>(new DataPoint[]{
                new DataPoint(startX, startY),
                new DataPoint(endX, startY)
        });
        graphView.addSeries(series1);
        series1.setThickness(thickness);
        series1.setColor(Color.WHITE);

        LineGraphSeries<DataPoint> series2 = new LineGraphSeries<>(new DataPoint[]{
                new DataPoint(endX, startY),
                new DataPoint(endX, endY)
        });
        graphView.addSeries(series2);
        series2.setThickness(thickness);
        series2.setColor(Color.WHITE);

        LineGraphSeries<DataPoint> series3 = new LineGraphSeries<>(new DataPoint[]{
                new DataPoint(startX, endY),
                new DataPoint(endX, endY)
        });
        graphView.addSeries(series3);
        series3.setThickness(thickness);
        series3.setColor(Color.WHITE);


        LineGraphSeries<DataPoint> series4 = new LineGraphSeries<>(new DataPoint[]{
                new DataPoint(startX, startY),
                new DataPoint(startX, endY)
        });
        graphView.addSeries(series4);
        series4.setThickness(3);
        series4.setColor(Color.WHITE);
    }
}
