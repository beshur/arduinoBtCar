package net.buznik.arduinobtcar2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.SharedPreferences;
import static android.content.Context.MODE_PRIVATE;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

class TouchBinder {
    String command;
    private OutputStream outputStream;

    public void setOutputStream(OutputStream outputStreamArg) {
        outputStream = outputStreamArg;
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
                e.printStackTrace();
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
                e.printStackTrace();
            }

        }

        return false;
    }

}

public class MainActivity extends AppCompatActivity {
    private final String DEVICE_ADDRESS = "98:D3:11:FD:22:12"; //MAC Address of Bluetooth Module
    private final String ADDRESSS_KEY = "BT_MAC";
    private static final String TAG = "MainActivity";

    private String device_address;
    private final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private BluetoothDevice device;
    private BluetoothSocket socket;
    private OutputStream outputStream;
    private TouchBinder touchBinderInstance = new TouchBinder();

    Button forward_btn, forward_left_btn, forward_right_btn, reverse_btn, reverse_left_btn, reverse_right_btn, bluetooth_connect_btn;
    EditText edit_mac_input;

    String command; //string variable that will store value to be transmitted to the bluetooth module
    SharedPreferences sharedPreferences;

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
        edit_mac_input = (EditText) findViewById(R.id.editMAC);

        sharedPreferences = getPreferences(MODE_PRIVATE);

        device_address = sharedPreferences.getString(ADDRESSS_KEY, DEVICE_ADDRESS);

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


        edit_mac_input.setText(device_address);
        edit_mac_input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {
            }

            @Override
            public void afterTextChanged(final Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String address = "" + edit_mac_input.getText();
                Log.d(TAG, "Address " + address);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(ADDRESSS_KEY, address);
                editor.commit();

                device_address = address;
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

        if(connected)
        {
            try
            {
                outputStream = socket.getOutputStream(); //gets the output stream of the socket
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
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
