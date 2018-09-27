package ph.edu.addu.richardleosala.piggyback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import ph.edu.addu.richardleosala.piggyback.Database.DatabaseHelper;
import ph.edu.addu.richardleosala.piggyback.Routing.CheckWifi;

public class MainActivity extends AppCompatActivity {
    Button btnOnOff, btnDiscover, btnSend;
    ListView listView, msgListView;
    TextView connectionStatus;
    TextView read_msg_box, availDev;
    EditText writeMsg, recipient;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    //WifiDirectAutoAccept wifiDirectAutoAccept;


    static final int MESSAGE_READ = 1;

    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;

    String trueDevName;

    DatabaseHelper myDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        checkDevName();
        exqListener();
    }

    private void checkDevName() {
        if(trueDevName == null){
            changeDevName();
        }
    }

    /**Method to Change the Device Name**/

    private void setDevName( final String devName) {
        try {
            Method m = mManager.getClass().getMethod(
                    "setDeviceName",
                    new Class[] { WifiP2pManager.Channel.class, String.class,
                            WifiP2pManager.ActionListener.class });

            m.invoke(mManager,mChannel, devName, new WifiP2pManager.ActionListener() {
                public void onSuccess() {
                    trueDevName = devName;
                    Toast.makeText(MainActivity.this, "Successfully Changed Device Name to: " + devName, Toast.LENGTH_SHORT).show();
                }

                public void onFailure(int reason) {
                    //Code to be done while name change Fails
                    Toast.makeText(MainActivity.this, "Something went wrong.", Toast.LENGTH_SHORT).show();
                    wifiManager.setWifiEnabled(true);
                    changeDevName();
                }
            });
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    /**Method to Register the Device Name**/

    public void changeDevName(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText edittext = new EditText(MainActivity.this);
        alert.setMessage("Enter Phone Number to Change the device name");
        alert.setTitle("Register Phone");
        edittext.setInputType(InputType.TYPE_CLASS_NUMBER);

        alert.setView(edittext);

        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //What ever you want to do with the value
                if(edittext.getText().toString().length() == 11){
                    setDevName(edittext.getText().toString());
                    btnDiscover.performClick();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            storedMsgsCheck();
                        }
                    },15000);
                }else {
                    Toast.makeText(MainActivity.this, "Must be 11 digits", Toast.LENGTH_LONG).show();
                    checkDevName();
                }
            }
        });

        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // what ever you want to do with No option.
                changeDevName();
            }
        });

        alert.show();
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case MESSAGE_READ:
                    byte[] readBbuff = (byte [])msg.obj;
                    String tempMsg = new String(readBbuff, 0, msg.arg1);
                    String msgSplit[] = tempMsg.split("#-#");
                    read_msg_box.setText(tempMsg);
                    if(msgSplit[1].contains(trueDevName)){
                        recipient.setText(msgSplit[2]);
                        read_msg_box.setText(msgSplit[0]+" From: "+msgSplit[2]);
                        myDb.addToTEXT(msgSplit[0], msgSplit[2]);
                        populate(read_msg_box.getText().toString());
                    }else{
                        read_msg_box.setText(tempMsg);
                        myDb.storeMsgs(tempMsg);
                    }
                    break;
            }
            return true;
        }
    });

    ArrayList<String> msgList = new ArrayList<String>();
    ArrayAdapter<String> msgAdapter;
    public void populate(String msg){
        msgList.add(msg);
        msgAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, msgList);
        msgListView.setAdapter(msgAdapter);
    }

    private void initialWork() {
        btnOnOff = findViewById(R.id.onOff);
        btnDiscover = findViewById(R.id.discover);
        btnSend = findViewById(R.id.sendButton);
        listView = findViewById(R.id.peerListView);
        read_msg_box = findViewById(R.id.readMsg);
        writeMsg = findViewById(R.id.writeMsg);
        connectionStatus = findViewById(R.id.connectionStatus);
        recipient = findViewById(R.id.recipient);
        availDev = findViewById(R.id.availDev);
        msgListView = findViewById(R.id.msgListView);
        myDb = new DatabaseHelper(this);

        btnDiscover.performClick();

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);

        mReceiver=new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }
    private void exqListener() {
        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(wifiManager.isWifiEnabled()){
                    wifiManager.setWifiEnabled(false);
                    btnOnOff.setText("ON");
                }else{
                    wifiManager.setWifiEnabled(true);
                    btnOnOff.setText("OFF");
                }
            }
        });
        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText("Discovery Started");
                    }

                    @Override
                    public void onFailure(int reason) {
                        connectionStatus.setText("Discovery Starting Failed");
                        Toast.makeText(MainActivity.this, "Be Sure to turn ON WIFI before starting Discovery", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
                final WifiP2pDevice device = deviceArray[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;
                //wifiDirectAutoAccept = new WifiDirectAutoAccept(this, mManager, mChannel);
                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "Connected to " + device.deviceName, Toast.LENGTH_SHORT ).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(getApplicationContext(), "Not Connected" , Toast.LENGTH_SHORT ).show();
                    }
                });
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(connectionStatus.getText().toString() == "Host" || connectionStatus.getText().toString() == "Client"){
                    /*String msg = writeMsg.getText().toString() + "#-#" + recipient.getText().toString() +"#-#" + trueDevName;
                    sendReceive.write(msg.getBytes());
                    populate(writeMsg.getText().toString());*/
                    //writeMsg.setText("");
                }else{
                    int lapse = 0; //should contain any an incremented number if the target device is found
                    for(int i = 0; i < deviceNameArray.length; i++){
                        if(deviceNameArray[i].contains(recipient.getText().toString())){
                            //Toast.makeText(MainActivity.this, "Has Recipient", Toast.LENGTH_SHORT).show();
                            mConnect(i);// connect to specific node, using i as the placement of deviceNameArray(specific)
                            final Handler handler = new Handler();
                            lapse++;
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    final String msg = writeMsg.getText().toString() + "#-#" + recipient.getText().toString() +"#-#" + trueDevName;
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            String split[];
                                            split = msg.split("#-#");
                                            populate(split[0]);
                                            sendReceive.write(msg.getBytes());
                                            writeMsg.setText("");
                                            Handler handler1 = new Handler();
                                            handler1.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    disconnect();
                                                }
                                            }, 500);
                                        }
                                    }, 2000);

                                    //sendReceive.write(msg.getBytes());
                                    //Toast.makeText(MainActivity.this, msg.getBytes().toString(), Toast.LENGTH_SHORT).show();
                                }
                            },3000);
                            break;
                        }
                    }
                    if(lapse == 0){
                        /**Broadcasting Code doing it manually**/
                        if(deviceNameArray.length > 0){
                            String tempmsg = writeMsg.getText().toString()+"#-#"+recipient.getText().toString()+"#-#"+trueDevName+"#-#";
                            for(int i = 0; i <deviceNameArray.length; i++){ tempmsg += deviceNameArray[i]; }
                            //for(int i = 0; i <deviceNameArray.length; i++){ mConnect(i); }
                            final String finalMsg = tempmsg;
                            mConnect(0);
                            final String msg = tempmsg;
                            Handler handler1 = new Handler();
                            handler1.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    sendReceive.write(msg.getBytes());
                                    disconnect();
                                }
                            }, 10000);
                            for (int i = 1; i < deviceNameArray.length; i++){
                                Handler handler = new Handler();
                                final int j = i;
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mConnect(j);
                                        final String msg = finalMsg;
                                        Handler handler1 = new Handler();
                                        handler1.postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                sendReceive.write(msg.getBytes());
                                                Handler handler2 = new Handler();
                                                writeMsg.setText("");
                                                handler2.postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        disconnect();
                                                    }
                                                }, 500);
                                            }
                                        },3000);

                                    }
                                }, 5000);
                            }
                        }
                    }
                }
            }
        });
    }

    private void disconnect() {
        mManager.removeGroup(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d("Success", "Disconnected");
            }

            @Override
            public void onFailure(int reason) {
                Log.d("Failed", "Som Ting Wong!");
            }
        });
    }

    //connect to a device after recipient is found
    private void mConnect(int i){
        final WifiP2pDevice device = deviceArray[i];
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        //wifiDirectAutoAccept = new WifiDirectAutoAccept(this, mManager, mChannel);
        mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(getApplicationContext(), "Connected to " + device.deviceName, Toast.LENGTH_SHORT ).show();
            }

            @Override
            public void onFailure(int reason) {
                Toast.makeText(getApplicationContext(), "Not Connected" , Toast.LENGTH_SHORT ).show();
            }
        });
    }
    //Broadcast Connection
    private void bConnect(){
        for (int i = 0; i<deviceNameArray.length; i++){
            final WifiP2pDevice device = deviceArray[i];
            WifiP2pConfig config = new WifiP2pConfig();
            config.deviceAddress = device.deviceAddress;
            //wifiDirectAutoAccept = new WifiDirectAutoAccept(this, mManager, mChannel);
            mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getApplicationContext(), "Connected to " + device.deviceName, Toast.LENGTH_SHORT ).show();
                }

                @Override
                public void onFailure(int reason) {
                    Toast.makeText(getApplicationContext(), "Not Connected" , Toast.LENGTH_SHORT ).show();
                }
            });
        }
    }

    //On Click in btnDiscover
    WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            if (!peerList.getDeviceList().equals(peers)) {
                peers.clear();
                peers.addAll(peerList.getDeviceList());
                deviceNameArray = new String[peerList.getDeviceList().size()];
                deviceArray = new WifiP2pDevice[peerList.getDeviceList().size()];
                int index = 0;
                for (WifiP2pDevice device : peerList.getDeviceList()) {
                    deviceNameArray[index] = device.deviceName;
                    deviceArray[index] = device;
                    index++;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNameArray);
                listView.setAdapter(adapter);
                availDev.setText("Available Nearby Devices: " + deviceNameArray.length);
            }
            /**Check Stored Messages if the there are available peers nearby that matches recipient*/

            if (peers.size() == 0) {
                Toast.makeText(getApplicationContext(), "No Device Found", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public void storedMsgsCheck(){
        Cursor data = myDb.getStoreMsgs();
        String[] storedMsgs = new String[500];
        if(data.getCount() == 0){
            Toast.makeText(MainActivity.this, "No Stored Messages", Toast.LENGTH_SHORT).show();
        }else{
            int c = 0;
            String msg;
            String [] devName = deviceNameArray;
            while (data.moveToNext()){
                //boolean add = listNum.add(data.getString(1));
                msg = data.getString(1);
                String []splitMsg = msg.split("#-#");
                if (devName != null) {
                    for(int i = 0; i < deviceNameArray.length;i++) {
                        if (deviceNameArray[i].contains(splitMsg[1])) {
                            Log.d("Device if found", "Found Target Device");
                            mConnect(i);
                            sendMessage(msg);
                        }else{
                            Log.d("Device if found", "Target Device not Found");
                        }
                    }
                }
                else{ Log.d("devName", "NULL");}
                /*
                }*/
                c++;
            }
        }
    }

    public void sendMessage(String msg){
        final String fMsg = msg;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendReceive.write(fMsg.getBytes());
                Handler handler1 = new Handler();
                handler1.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        disconnect();
                    }
                },500);
            }
        },10000);
    }

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){
                connectionStatus.setText("Host");
                serverClass = new ServerClass();
                serverClass.start();
            }else if(wifiP2pInfo.groupFormed){
                connectionStatus.setText("Client");
                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();
            }
        }
    };

    WifiP2pManager.GroupInfoListener groupInfoListener = new WifiP2pManager.GroupInfoListener() {
        @Override
        public void onGroupInfoAvailable(WifiP2pGroup group) {
            Collection<WifiP2pDevice> peerList = group.getClientList();
            ArrayList<WifiP2pDevice> list = new ArrayList<WifiP2pDevice>(peerList);
            String host;
            for (int i = 1; i < list.size(); i++) {
                host = list.get(i).deviceAddress;
                /** transferFile here **/
            }
        }
    };

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        //mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);
        registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }

    public class ServerClass extends Thread{
        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendReceive extends Thread{
        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        public SendReceive(Socket skt){
            socket = skt;
            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            while (socket!=null){
                try {
                    bytes= inputStream.read(buffer);
                    if (bytes >0){
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ClientClass extends Thread{
        Socket socket;
        String hostAdd;

        public ClientClass(InetAddress hostAddress){
            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();
        }

        @Override
        public void run() {
            try {
                socket.connect(new InetSocketAddress(hostAdd, 8888), 500);
                sendReceive = new SendReceive(socket);
                sendReceive.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
