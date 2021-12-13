package com.example.blueclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.ref.WeakReference;
import java.util.UUID;

public class Client extends AppCompatActivity {
    private static final int CONN_SUCCESS = 0x1;
    private static final int CONN_FAIL = 0x2;
    private static final int RECEIVER_INFO = 0x3;
    private static final int SET_EDITTEXT_NULL = 0x4;

    private static Button send;
    private static TextView client_state;
    private static EditText client_send;

    BluetoothAdapter bluetooth = null;//本地蓝牙设备
    BluetoothDevice device = null;//远程蓝牙设备
    BluetoothSocket socket = null;//蓝牙设备Socket客户端

    //输入输出流
    PrintStream out;
    BufferedReader in;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        setTitle("蓝牙客户端");



        client_state = findViewById(R.id.client_state);
        client_send =  findViewById(R.id.client_send);
        send = findViewById(R.id.send);
        init();
    }

    //创建蓝牙客户端端的Socket
    private void init() {
        client_state.setText("客户端已启动，正在等待连接...\n");
        new Thread(new Runnable() {
            @Override
            public void run() {
                //1.得到本地蓝牙设备的默认适配器
                bluetooth = BluetoothAdapter.getDefaultAdapter();
                //2.通过本地蓝牙设备得到远程蓝牙设备，把“输入服务器端的设备地址”换成另一台手机的mac地址
                device = bluetooth.getRemoteDevice("02:00:00:00:00:00");
                //3.根据UUID创建并返回一个BoluetoothSocket
                try {
                    socket = device.createRfcommSocketToServiceRecord(UUID.fromString("6ee26c8e-a2ab-4291-adf3-8a679123616b"));
                    if (socket != null) {
                        // 连接
                        socket.connect();
                        //处理客户端输出流
                        out = new PrintStream(socket.getOutputStream());
                        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                    }
                    handler.sendEmptyMessage(CONN_SUCCESS);
                } catch (IOException e) {
                    e.printStackTrace();
                    Message msg = handler.obtainMessage(CONN_FAIL, e.getLocalizedMessage());
                    handler.sendMessage(msg);

                }

            }
        }).start();
    }

    //防止内存泄漏 正确的使用方法
    private final MyHandler handler = new MyHandler(this);

    public class MyHandler extends Handler {
        //软引用
        WeakReference<Client> weakReference;

        public MyHandler(Client activity) {
            weakReference = new WeakReference<Client>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Client activity = weakReference.get();
            if (activity != null) {
                switch (msg.what) {
                    case RECEIVER_INFO:
                        setInfo(msg.obj.toString() + "\n");
                        break;
                    case SET_EDITTEXT_NULL:
                        client_send.setText("");
                        break;
                    case CONN_SUCCESS:
                        setInfo("连接成功！\n");
                        send.setEnabled(true);
                        System.out.println("name" + device.getName());
                        System.out.println("Uuids" + device.getUuids());
                        System.out.println("Address" + device.getAddress());
                        new Thread(new ReceiverInfoThread()).start();
                        break;
                    case CONN_FAIL:
                        setInfo("连接失败！\n");
                        setInfo(msg.obj.toString() + "\n");
                        break;
                    default:
                        break;
                }
            }
        }
    }


    private boolean isReceiver = true;

    //接收信息的线程
    class ReceiverInfoThread implements Runnable {
        @Override
        public void run() {
            String info = null;
            while (isReceiver) {
                try {
                    System.out.println("--ReceiverInfoThread start --");
                    info = in.readLine();
                    System.out.println("--ReceiverInfoThread read --");
                    Message msg = handler.obtainMessage(RECEIVER_INFO, info);
                    handler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public void send(View v) {
        final String content = client_send.getText().toString();
        if (TextUtils.isEmpty(content)) {
            Toast.makeText(this, "不能发送空消息", Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                out.println(content);
                out.flush();
                handler.sendEmptyMessage(SET_EDITTEXT_NULL);
            }
        }).start();
    }

    private void setInfo(String info) {
        StringBuffer sb = new StringBuffer();
        sb.append(client_state.getText());
        sb.append(info);
        client_state.setText(sb);
    }
}