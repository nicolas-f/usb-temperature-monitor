package fr.umrae.temperature_monitor.serial;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import fr.umrae.temperature_monitor.MainActivity;
import fr.umrae.temperature_monitor.dao.DataObj;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.List;

public class UsbSerial implements PropertyChangeListener {
    public static final String TAG = "Usb";

    private static final int ROOT_USB_HUB = 0x1d6b;

    public UsbSerial(MainActivity handler) {
        this.handler = handler;
    }

    private UsbSerialPort mDriver;
    private static PendingIntent mPermissionIntent = null;
    protected UsbManager manager;
    protected UsbDevice device;
    protected UsbDeviceConnection connection;
    protected MainActivity handler;

    public Protocol protocol;

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals("data")) {
            handler.onNewData((DataObj) evt.getNewValue());
        }
    }

    public static void getPermission(final UsbManager manager, Context context, UsbDevice device) {
        if (mPermissionIntent == null) {
            mPermissionIntent = PendingIntent.getBroadcast(context, 0,
                    new Intent("USB_PERMISSION"), PendingIntent.FLAG_IMMUTABLE);
        }
        if (manager == null)
            return;
        if (device != null && mPermissionIntent != null) {
            if (!manager.hasPermission(device)) {
                Log.d(TAG, "Request permission : " + device.toString());
                manager.requestPermission(device, mPermissionIntent);
            }
        }
    }

    public boolean findFirstDevice() {
        if (manager == null) {
            manager = (UsbManager) handler.getSystemService(Context.USB_SERVICE);
        }
        for (final UsbDevice usbDevice : manager.getDeviceList().values()) {
            getPermission(manager, handler, usbDevice);
            if (!manager.hasPermission(usbDevice)) {
                Log.d(TAG, "Doesn't have permission device: " + device.toString());
            } else {
                device = usbDevice;
                int deviceVID = device.getVendorId();
                if (deviceVID != ROOT_USB_HUB) {
                    // There is a device connected to our Android device. Try to
                    // open it as a Serial Port.
                    android.util.Log.i(TAG, "DEVICE OK" + device);
                    connection = manager.openDevice(device);
                    return true;
                } else {
                    connection = null;
                    device = null;
                }
            }
        }
        return false;
    }

    public void connect() throws IOException {
        Log.e(TAG, "CONNECT!!!!!!!");
        findFirstDevice();
        open();
    }

    public boolean open() throws IOException {
        Log.i(TAG, "open");
        // Find all available drivers from attached devices.
        if (manager == null) {
            manager = (UsbManager) handler.getSystemService(Context.USB_SERVICE);
        }
        List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        Log.i(TAG, "drivers:" + drivers.size());
        if (drivers.isEmpty()) {
            return false;
        }
        List<UsbSerialPort> ports = drivers.get(0).getPorts();
        Log.i(TAG, "ports:" + ports.size());
        if (ports.isEmpty()) {
            return false;
        }

        // Open a connection to the first available driver.

        mDriver = ports.get(0);
        int deviceVID = mDriver.getDriver().getDevice().getVendorId();
        int devicePID = mDriver.getDriver().getDevice().getProductId();
        String deviceName = mDriver.getDriver().getDevice().getDeviceName();
        Log.e(TAG, "deviceVID:" + deviceVID + " devicePID:" + devicePID + " deviceName:" + deviceName);
        try {
            if (mDriver == null) {
                return false;
            }
            protocol = new Protocol(mDriver);
            protocol.addPropertyChangeListener(this);
            protocol.connect();
            android.util.Log.i(TAG, "conn on : " + 115200);
        } catch (IOException e) {
            android.util.Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
            try {
                mDriver.close();
            } catch (IOException e2) {
                // Ignore.
            }
            mDriver = null;
            return false;
        }
        return true;
    }

    public void close() {
        if (protocol != null) {
            protocol.unplug();
        }
        stop();
    }

    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }

    private State mState = State.STOPPED;

    public synchronized void stop() {
        if (getState() == State.RUNNING) {
            android.util.Log.i(TAG, "Stop requested");
            mState = State.STOPPING;
        }
    }

    private synchronized State getState() {
        return mState;
    }

}
