package net.buznik.arduinobtcar2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.SharedPreferences;

import org.w3c.dom.Node;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


interface OnBrokenPipeListener {
    void onBrokenPipe();
}

class TouchBinder {
    String command;
    private OutputStream outputStream;
    private OnBrokenPipeListener listener;

    public void setOnBrokenPipeListener(OnBrokenPipeListener listener) {
        this.listener = listener;
    }

    public void setOutputStream(OutputStream outputStreamArg) {
        outputStream = outputStreamArg;
    }

    private void onException(IOException e) {
        Log.d("TouchBinder", "exception: " + e.getMessage());
        if(e.getMessage().contains("Broken pipe")){
            if (listener != null) listener.onBrokenPipe();
        }
        e.printStackTrace();
    }


    public boolean handler(View v, MotionEvent event, String boundCommand) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) //MotionEvent.ACTION_DOWN is when you hold a button down
        {
            command = boundCommand;

            try
            {
                outputStream.write(command.getBytes()); //transmits the value of command to the bluetooth module
            }
            catch (IOException e)
            {
                onException(e);
            }
        }
        else if(event.getAction() == MotionEvent.ACTION_UP) //MotionEvent.ACTION_UP is when you release a button
        {
            command = "S";
            try
            {
                outputStream.write(command.getBytes());
            }
            catch(IOException e)
            {
                onException(e);
            }

        }
        return false;
    }
}

class BT_Devices_List {
    ArrayList<String> names;
    ArrayList<String> macs;

    BT_Devices_List() {
        names  = new ArrayList<String>();
        macs  = new ArrayList<String>();
    }

    public void addDevices(Set<BluetoothDevice> bondedDevices) {
        names.clear();
        macs.clear();
        for(BluetoothDevice iterator : bondedDevices) {
            names.add(iterator.getName());
            macs.add(iterator.getAddress());
        }
    }

    public String getMacByIndex(int index) {
        return macs.get(index);
    }

    public ArrayList<String> getNames() {
        return names;
    }

    public int getSelectedIndex(String device_address) {
        return macs.indexOf(device_address);
    }
}

public class MainActivity extends AppCompatActivity implements OnBrokenPipeListener {
    private final String DEVICE_ADDRESS = "98:D3:11:FD:22:12"; //MAC Address of Bluetooth Module
    private final String ADDRESSS_KEY = "BT_MAC";
    private static final String TAG = "MainActivity";

    private String device_address;
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private TouchBinder touchBinderInstance;

    Button forward_btn, forward_left_btn, forward_right_btn, reverse_btn, reverse_left_btn, reverse_right_btn, bluetooth_connect_btn;
    Spinner bt_devices_select;
    ImageView bt_connected_icon;

    BT_Devices_List bt_devices_list;
    String command; //string variable that will store value to be transmitted to the bluetooth module
    SharedPreferences sharedPreferences;

    @Override
    public void onBrokenPipe() {
        device = null;
        Toast.makeText(getApplicationContext(), "Device disconnected", Toast.LENGTH_SHORT).show();
        toggleConnectedIcon(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //declaration of button variables
        forward_btn = (Button) findViewById(R.id.forward_btn);
        forward_left_btn = (Button) findViewById(R.id.forward_left_btn);
        forward_right_btn = (Button) findViewById(R.id.forward_right_btn);
        reverse_btn = (Button) findViewById(R.id.reverse_btn);
        bluetooth_connect_btn = (Button) findViewById(R.id.bluetooth_connect_btn);
        bt_connected_icon = (ImageView)findViewById(R.id.bt_connected_view);

        sharedPreferences = getPreferences(MODE_PRIVATE);
        device_address = sharedPreferences.getString(ADDRESSS_KEY, DEVICE_ADDRESS);

        bt_devices_list = new BT_Devices_List();

        touchBinderInstance = new TouchBinder();
        touchBinderInstance.setOnBrokenPipeListener(this);

        //OnTouchListener code for the forward button (button long press)
        forward_btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return touchBinderInstance.handler(v, event,"U");
            }

        });

        //OnTouchListener code for the reverse button (button long press)
        reverse_btn.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                return touchBinderInstance.handler(v, event,"D");
            }
        });

        //OnTouchListener code for the forward left button (button long press)
        forward_left_btn.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return touchBinderInstance.handler(v, event, "L");
            }
        });

        //OnTouchListener code for the forward right button (button long press)
        forward_right_btn.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return touchBinderInstance.handler(v, event, "R");
            }
        });

        bt_devices_select = (Spinner) findViewById(R.id.bt_devices_select);

        bt_devices_select.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                String mac = bt_devices_list.getMacByIndex(position);
                Log.d(TAG, "onItemSelected mac" + mac);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(ADDRESSS_KEY, mac);
                editor.commit();

                device_address = mac;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // your code here
            }
        });


        //Button that connects the device to the bluetooth module when pressed
        bluetooth_connect_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(BTinit())
                {
                    BTconnect();
                }

            }
        });

        if(BTinit())
        {
            BTconnect();
        }
    }

    private void toggleConnectedIcon(boolean show) {
        int state = show ? View.VISIBLE : View.INVISIBLE;
        bt_connected_icon.setVisibility(state);
    }

    //Initializes bluetooth module
    public boolean BTinit()
    {
        boolean found = false;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(bluetoothAdapter == null) //Checks if the device supports bluetooth
        {
            Toast.makeText(getApplicationContext(), "Device doesn't support bluetooth", Toast.LENGTH_SHORT).show();
        }

        if(!bluetoothAdapter.isEnabled()) //Checks if bluetooth is enabled. If not, the program will ask permission from the user to enable it
        {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter,0);

            try
            {
                Thread.sleep(1000);
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        if(bondedDevices.isEmpty()) //Checks for paired bluetooth devices
        {
            Toast.makeText(getApplicationContext(), "Please pair the device first", Toast.LENGTH_SHORT).show();
        }
        else
        {
            for(BluetoothDevice iterator : bondedDevices)
            {
                if(iterator.getAddress().equals(device_address))
                {
                    device = iterator;
                    found = true;
                    break;
                }
            }

            bt_devices_list.addDevices(bondedDevices);
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, bt_devices_list.getNames());
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            bt_devices_select.setAdapter(adapter);
            bt_devices_select.setSelection(bt_devices_list.getSelectedIndex(device_address));

        }

        return found;
    }

    public boolean BTconnect()
    {
        boolean connected = true;

        try
        {
            socket = device.createRfcommSocketToServiceRecord(PORT_UUID); //Creates a socket to handle the outgoing connection
            socket.connect();

            Toast.makeText(getApplicationContext(),
                    "Connection to bluetooth device successful", Toast.LENGTH_LONG).show();
        }
        catch(IOException e)
        {
            e.printStackTrace();
            connected = false;
        }

        toggleConnectedIcon(false);

        if(connected)
        {
            try
            {
                outputStream = socket.getOutputStream(); //gets the output stream of the socket
                toggleConnectedIcon(true);
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        } else {
        }

        touchBinderInstance.setOutputStream(outputStream);
        return connected;
    }

    @Override
    protected void onStart()
    {
        super.onStart();
    }

}
