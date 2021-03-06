package com.example.mybtcar;
// Manifest
// android:theme="@style/Theme.MyApplication"
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButtonToggleGroup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity{

    // GUI Components
    private TextView mBluetoothStatus;
    private TextView SeekerValue;
    private TextView mReadBuffer;
    private Button mScanBtn;
    private Button mOffBtn;
    private Button mListPairedDevicesBtn;
    private ImageButton take_picture;
    private Button mDiscoverBtn;
    private BluetoothAdapter mBTAdapter;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrayAdapter;
    private ListView mDevicesListView;
    private Switch on_off;
    private Switch mode;

    private Button forward;
    int mode_num = 0;
    private Button back;
    private Button left;
    private Button right;
    //private Button stop;
    private Button stop_2;
    private SeekBar motorSpeed;
    //public static Handler andler;
    private Handler mHandler;
    // Our main handler that will receive callback notifications
    private ConnectedThread mConnectedThread;
    // bluetooth background worker thread to send and receive data
    private BluetoothSocket mBTSocket = null;
    // bi-directional client-to-client data path
    private static final UUID BTMODULEUUID = UUID.fromString
            ("00001101-0000-1000-8000-00805f9b34fb"); // "random" unique identifier
    // video
    private WebView webView;
    private WebSettings webSettings;
    //private VideoView streamView;
    private MediaController mediaController;
    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1;
    // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2;
    // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3;
    // used in bluetooth handler to identify message status
    private  String _recieveData = "";
    private SensorManager mSensorManager;
    private Sensor mGyroscope;
    private Sensor mRotate;
    public MainActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
        on_off = (Switch)findViewById(R.id.on_off);
        mode = (Switch) findViewById(R.id.switch2);

        //???????????????
        //videoView = (VideoView) findViewById(R.id.videoView);
        mBluetoothStatus = (TextView)findViewById(R.id.bluetoothStatus);
        SeekerValue = (TextView)findViewById(R.id.seekerValue);
        //mReadBuffer = (TextView) findViewById(R.id.readBuffer);
        //mScanBtn = (Button)findViewById(R.id.scan);
        take_picture = (ImageButton)findViewById(R.id.imageButton);
        mOffBtn = (Button)findViewById(R.id.off);
        mDiscoverBtn = (Button)findViewById(R.id.discover);
        forward = (Button)findViewById(R.id.forward);
        back = (Button)findViewById(R.id.back);
        //stop = (Button)findViewById(R.id.stop);
        stop_2 = (Button)findViewById(R.id.stop_2);
        left = (Button)findViewById(R.id.left);
        right = (Button)findViewById(R.id.right);
        // ??????

        //streamView = (VideoView) findViewById(R.id.videoview);
        webView = (WebView)findViewById(R.id.webview);
        // Device array
        mBTArrayAdapter = new ArrayAdapter<String>
                (this,android.R.layout.simple_list_item_1);
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        // get a handle on the bluetooth radio
        mDevicesListView = (ListView)findViewById(R.id.devicesListView);
        mDevicesListView.setAdapter(mBTArrayAdapter); // assign model to view
        mDevicesListView.setOnItemClickListener(mDeviceClickListener);
        // ?????????
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        //????????????
        mRotate = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        //SeekerBar
        motorSpeed = (SeekBar)findViewById(R.id.seekBar);
        // ????????????????????????
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);

        //??????????????? ??????????????????????????????????????????
        mHandler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){ //??????MESSAGE_READ ??????????????????
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                        readMessage =  readMessage.substring(0,1);
                        //?????????????????????????????????????????????????????????
                        _recieveData += readMessage; //????????????????????????????????????
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    mReadBuffer.setText(_recieveData); //????????????????????????????????????

                }

                if(msg.what == CONNECTING_STATUS){
                    //??????CONNECTING_STATUS ??????????????????
                    if(msg.arg1 == 1)
                        mBluetoothStatus.setText("Connected to Device: " + (String)(msg.obj));
                    else
                        mBluetoothStatus.setText("Connection Failed");
                }
            }
        };


        if (mBTArrayAdapter == null) {
            // Device does not support Bluetooth
            mBluetoothStatus.setText("Status: Bluetooth not found");
            Toast.makeText(getApplicationContext(),"Bluetooth device not found!",Toast.LENGTH_SHORT).show();
        }
        else {

            // Discover button
            mDiscoverBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    discover(v);
                }
            });
            //??????
            take_picture.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    _recieveData = ""; //???????????????????????????
                    if(mConnectedThread != null) //First check to make sure thread created
                        mConnectedThread.write("1");//??????
                }
            });
            //???????????? (webview)
            on_off.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked){
                        //streamView.setVisibility(CompoundButton.VISIBLE);
                        //streamView.setVideoURI(Uri.parse("rtsp://192.168.0.110:8000/"));
                        //streamView.setVideoURI(Uri.parse("rtsp://192.168.43.246:8000/"));
                        //streamView.setVideoURI(Uri.parse("http://192.168.0.110:8081"));
                        //mediaController = new MediaController(MainActivity.this);
                        //streamView.setMediaController(mediaController);
                        //streamView.requestFocus();
                        //streamView.start();
                        take_picture.setEnabled(true);
                        take_picture.setVisibility(CompoundButton.VISIBLE);
                        webView.setVisibility(CompoundButton.VISIBLE);
                        webSettings = webView.getSettings();
                        webSettings.setJavaScriptEnabled(true);
                        webSettings.setLoadWithOverviewMode(true);
                        webSettings.setUseWideViewPort(true); //????????????????????????webview?????????
                        webSettings.setLoadWithOverviewMode(true); // ????????????????????????
                        webView.setWebViewClient(new WebViewClient()); //????????????????????????
                        webView.loadUrl("http://192.168.0.110:8000");
                    }
                    else{
                        take_picture.setEnabled(false);
                        take_picture.setVisibility(CompoundButton.INVISIBLE);
                        webView.setVisibility(CompoundButton.INVISIBLE);
                        //streamView.setVisibility(CompoundButton.INVISIBLE);
                    }
                }
            });
            //??????mode switch
            mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) { // mode 2
                        forward.setEnabled(false);
                        forward.setVisibility(CompoundButton.INVISIBLE);
                        right.setEnabled(false);
                        right.setVisibility(CompoundButton.INVISIBLE);
                        left.setEnabled(false);
                        left.setVisibility(CompoundButton.INVISIBLE);
                        back.setEnabled(false);
                        back.setVisibility(CompoundButton.INVISIBLE);
                        //stop.setEnabled(false);
                        //stop.setVisibility(CompoundButton.INVISIBLE);
                        //stop_2.setEnabled(true);
                        //stop_2.setVisibility(CompoundButton.VISIBLE);
                        mode_num = 1;
                    }
                    else{ // mode 1
                        forward.setEnabled(true);
                        forward.setVisibility(CompoundButton.VISIBLE);
                        right.setEnabled(true);
                        right.setVisibility(CompoundButton.VISIBLE);
                        left.setEnabled(true);
                        left.setVisibility(CompoundButton.VISIBLE);
                        back.setEnabled(true);
                        back.setVisibility(CompoundButton.VISIBLE);
                        //stop.setEnabled(true);
                        //stop.setVisibility(CompoundButton.VISIBLE);
                        //stop_2.setEnabled(false);
                        //stop_2.setVisibility(CompoundButton.INVISIBLE);
                        mode_num = 0;
                        _recieveData = "";
                        if(mConnectedThread != null) //First check to make sure thread created
                            mConnectedThread.write("s");
                    }
                }
            });
            SensorEventListener gyroscopeSensorListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    if (event.values[2] > 0.6f && mode_num == 1) { // anticlockwise
                        _recieveData = ""; //???????????????????????????
                        if(mConnectedThread != null) //First check to make sure thread created
                            mConnectedThread.write("d");//??????
                    } else if (event.values[2] < -0.6f && mode_num == 1) { // clockwise
                        _recieveData = ""; //???????????????????????????
                        if(mConnectedThread != null) //First check to make sure thread created
                            mConnectedThread.write("a");//??????
                    }else if (event.values[1] > 0.6f && mode_num == 1){
                        _recieveData = ""; //???????????????????????????
                        if(mConnectedThread != null) //First check to make sure thread created
                            mConnectedThread.write("w");//??????
                    }else if (event.values[1] < -0.6f && mode_num == 1){
                        _recieveData = ""; //???????????????????????????
                        if(mConnectedThread != null) //First check to make sure thread created
                            mConnectedThread.write("x");//??????
                    }
                }
                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) { }

            };mSensorManager.registerListener(gyroscopeSensorListener, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
            //??????????????????
            right.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    _recieveData = "";
                    if(mConnectedThread != null) //First check to make sure thread created
                        mConnectedThread.write("a"); }
            });
            //??????????????????
            left.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    _recieveData = "";
                    if(mConnectedThread != null) //First check to make sure thread created
                        mConnectedThread.write("d"); }
            });
            //??????????????????
            forward.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    _recieveData = "";
                    if(mConnectedThread != null) //First check to make sure thread created
                        mConnectedThread.write("w"); }
            });

            //??????????????????
            back.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    _recieveData = "";
                    if(mConnectedThread != null) //First check to make sure thread created
                        mConnectedThread.write("x"); }
            });
            stop_2.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    _recieveData = "";
                    if(mConnectedThread != null) //First check to make sure thread created
                        mConnectedThread.write("s");
                }
            });

            //??????????????????
            /*
            stop.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    _recieveData = "";
                    if(mConnectedThread != null) //First check to make sure thread created
                        mConnectedThread.write("s");
                }
            });
            */
            //SeekerBar????????????
            motorSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    seekBar.setMax(100);
                    int speed = seekBar.getProgress();
                    if (speed >=0 && speed <= 20){// motorSpeed +10
                        _recieveData = "";
                        if(mConnectedThread != null) //First check to make sure thread created
                            mConnectedThread.write("A");
                    }else if (speed >20 && speed <= 40){
                        _recieveData = "";
                        if(mConnectedThread != null) //First check to make sure thread created
                            mConnectedThread.write("B");
                    }else if (speed >40 && speed <= 60) {
                        _recieveData = "";
                        if (mConnectedThread != null) //First check to make sure thread created
                            mConnectedThread.write("C");
                    }else if (speed >60 && speed <= 80) {
                        _recieveData = "";
                        if (mConnectedThread != null) //First check to make sure thread created
                            mConnectedThread.write("D");
                    }
                    else if (speed >80 && speed <= 100) {
                        _recieveData = "";
                        if (mConnectedThread != null) //First check to make sure thread created
                            mConnectedThread.write("E");
                    }
                    SeekerValue.setText(String.valueOf(seekBar.getProgress()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }   // ???????????????????????????

    }



    private void bluetoothOn(View view){
        if (!mBTAdapter.isEnabled()) {//?????????????????????
            Intent enableBtIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);//????????????
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            //????????????????????????
            mBluetoothStatus.setText("Bluetooth enabled");
            Toast.makeText(getApplicationContext(),"Bluetooth turned on",Toast.LENGTH_SHORT).show();
        }
        else{
            Toast.makeText(getApplicationContext(),"Bluetooth is already on",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // Enter here after user selects "yes" or "no" to enabling radio
    //???????????????????????????????????????????????????????????????
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent Data) {
        // Check which request we're responding to
        super.onActivityResult(requestCode, resultCode, Data);
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                mBluetoothStatus.setText("Enabled");
            } else
                mBluetoothStatus.setText("Disabled");
        }
    }

    private void bluetoothOff(View view){
        mBTAdapter.disable(); // turn off bluetooth
        mBluetoothStatus.setText("Bluetooth disabled");
        Toast.makeText(getApplicationContext(),"Bluetooth turned Off",
                Toast.LENGTH_SHORT).show();
    }

    private void discover(View view){
        // Check if the device is already discovering
        if(mBTAdapter.isDiscovering()){ //????????????????????????
            mBTAdapter.cancelDiscovery(); //????????????
            Toast.makeText(getApplicationContext(),"Discovery stopped",Toast.LENGTH_SHORT).show();
        }
        else{
            if(mBTAdapter.isEnabled()) { //???????????????????????????????????????
                mBTArrayAdapter.clear(); // clear items
                mBTAdapter.startDiscovery(); //????????????
                Toast.makeText(getApplicationContext(), "Discovery started",
                        Toast.LENGTH_SHORT).show();
                registerReceiver(blReceiver, new
                        IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                Toast.makeText(getApplicationContext(), "Bluetooth not on",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // add the name to the list
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                mBTArrayAdapter.notifyDataSetChanged();
            }
        }
    };

    private void listPairedDevices(View view){
        mPairedDevices = mBTAdapter.getBondedDevices();
        if(mBTAdapter.isEnabled()) {
            // put it's one to the adapter
            for (BluetoothDevice device : mPairedDevices)
                mBTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

            Toast.makeText(getApplicationContext(), "Show Paired Devices",
                    Toast.LENGTH_SHORT).show();
        }
        else
            Toast.makeText(getApplicationContext(), "Bluetooth not on",
                    Toast.LENGTH_SHORT).show();
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new
            AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {

                    if(!mBTAdapter.isEnabled()) {
                        Toast.makeText(getBaseContext(), "Bluetooth not on",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    mBluetoothStatus.setText("Connecting...");
                    // Get the device MAC address, which is the last 17 chars in the View
                    String info = ((TextView) v).getText().toString();
                    final String address = info.substring(info.length() - 17);
                    final String name = info.substring(0,info.length() - 17);

                    // Spawn a new thread to avoid blocking the GUI one
                    new Thread()
                    {
                        public void run() {
                            boolean fail = false;
                            //????????????MAC???????????????????????????
                            BluetoothDevice device = mBTAdapter.getRemoteDevice(address);

                            try {
                                mBTSocket = createBluetoothSocket(device);
                                //????????????socket
                            } catch (IOException e) {
                                fail = true;
                                Toast.makeText(getBaseContext(), "Socket creation failed",
                                        Toast.LENGTH_SHORT).show();
                            }
                            // Establish the Bluetooth socket connection.
                            try {
                                mBTSocket.connect(); //??????????????????
                            } catch (IOException e) {
                                try {
                                    fail = true;
                                    mBTSocket.close(); //??????socket
                                    //??????????????? ????????????
                                    mHandler.obtainMessage(CONNECTING_STATUS, -1, -1)
                                            .sendToTarget();
                                } catch (IOException e2) {
                                    //insert code to deal with this
                                    Toast.makeText(getBaseContext(), "Socket creation failed",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                            if(!fail) {
                                //??????????????????????????????????????????
                                mConnectedThread = new ConnectedThread(mBTSocket);
                                mConnectedThread.start();
                                //??????????????????????????????????????????
                                mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                                        .sendToTarget();
                            }
                        }
                    }.start();
                }
            };
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws
            IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.available();
                    if(bytes != 0) {
                        SystemClock.sleep(100);
                        //pause and wait for rest of data
                        bytes = mmInStream.available();
                        // how many bytes are ready to be read?
                        bytes = mmInStream.read(buffer, 0, bytes);
                        // record how many bytes we actually read
                        mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                .sendToTarget(); // Send the obtained bytes to the UI activity
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
        /* Call this from the main activity to send data to the remote device */
        public void write(String input) {
            byte[] bytes = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }
        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }
}
