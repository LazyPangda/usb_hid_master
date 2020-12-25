package com.example.usb_hid;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import java.nio.ByteBuffer;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private UsbManager myUsbManager;
    private UsbDevice myUsbDevice;
    private UsbInterface myInterface;
    private UsbDeviceConnection myDeviceConnection;

    private final int VendorID = 1504;
    private final int ProductID = 4608;

    private int mVendorID;
    private int mProductID;

    private TextView info;
    private TextView tv_recive;

    private UsbEndpoint epOut;
    private UsbEndpoint epIn;
    private String TAG_UsbPermission = "TAG_UsbPermission";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        info = (TextView) findViewById(R.id.tv_info);
        tv_recive = (TextView) findViewById(R.id.tv_recive);

        myUsbManager = (UsbManager) getSystemService(USB_SERVICE);

        enumerateDevice();

        findInterface();

        openDevice();

        assignEndpoint();

        usb_receiveData();


    }

    /**
     * 分配端点，IN | OUT，即输入输出
     */
    private void assignEndpoint() {
        for (int i = 0; i < myInterface.getEndpointCount(); i++) {
            UsbEndpoint ep = myInterface.getEndpoint(i);
            // look for bulk endpoint
            if (ep.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.getDirection() == UsbConstants.USB_DIR_OUT) {
                    epOut = ep;
                } else {
                    epIn = ep;

                }
            }
        }
    }

    /**
     * 打开设备
     */
    private void openDevice() {
        if (myInterface != null) {
            UsbDeviceConnection conn = null;
            // 在open前判断是否有连接权限；对于连接权限可以静态分配，也可以动态分配权限，可以查阅相关资料
            if (myUsbManager.hasPermission(myUsbDevice)) {
                conn = myUsbManager.openDevice(myUsbDevice);
            } else {
                // 注册广播，接收用户权限选择
                PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(TAG_UsbPermission), 0);
                registerReceiver(new MyPermissionReceiver(), new IntentFilter(TAG_UsbPermission));
                // 弹出对话框，申请权限
                myUsbManager.requestPermission(myUsbDevice, pi);
            }

            if (conn == null) {
                return;
            }

            if (conn.claimInterface(myInterface, true)) {
                myDeviceConnection = conn; // 到此你的android设备已经连上HID设备
                Toast.makeText(this, "打开设备成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "打开设备失败", Toast.LENGTH_SHORT).show();
                conn.close();
            }
        } else {
        }
    }

    /**
     * 找设备接口
     */
    private void findInterface() {
        if (myUsbDevice != null) {
            //寻找接口，一般是第一个。
            myInterface = myUsbDevice.getInterface(0);


        }
    }

    /**
     * 枚举设备
     */
    private void enumerateDevice() {
        if (myUsbManager == null)
            return;

        HashMap<String, UsbDevice> deviceList = myUsbManager.getDeviceList();
        if (!deviceList.isEmpty()) {
            // deviceList不为空
            StringBuffer sb = new StringBuffer();
            for (UsbDevice device : deviceList.values()) {
                sb.append(device.toString());
                sb.append("\n");
                sb.append("\n");
                info.setText(sb);
                // 输出设备信息
                Log.e(TAG, sb.toString());
                // 枚举到设备
                if (device.getVendorId() == VendorID && device.getProductId() == ProductID) {
                    myUsbDevice = device;
                }

            }
        }
    }








    public void usb_receiveData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //判断条件可自己定义
                while (true) {
                    if (myDeviceConnection != null && myInterface != null) {

                        try {

                            byte[] inByte = new byte[epIn.getMaxPacketSize()];
                            int inLength = myDeviceConnection.bulkTransfer(epIn, inByte, epIn.getMaxPacketSize(), 100);
                            if (inLength > 0) {
                                // TODO:
                            }
                        } catch (Exception e) {

                        }




                        //method2
                        ByteBuffer byteBuffer = ByteBuffer.allocate(epIn.getMaxPacketSize());
                        UsbRequest usbRequest = new UsbRequest();
                        usbRequest.initialize(myDeviceConnection, epIn);
                        usbRequest.queue(byteBuffer, epIn.getMaxPacketSize());
                        if (myDeviceConnection.requestWait() == usbRequest) {
                            byte[] retData = byteBuffer.array();
                            // TODO:
                        }


                    }
                }
            }
        }).start();
    }










    // 定义的广播接收器
    private class MyPermissionReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TAG_UsbPermission)) {
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                if (granted) {
                    // Todo：已经获取权限，可以执行其他操作
                    openDevice();
                } else {
                    // Todo：未获取权限。
                }
            }
        }
    }



}