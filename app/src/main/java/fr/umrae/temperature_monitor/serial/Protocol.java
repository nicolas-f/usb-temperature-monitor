package fr.umrae.temperature_monitor.serial;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import eu.perpro.android.utmp_basic.Probe;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;

public class Protocol implements SerialInputOutputManager.Listener {

    @SuppressWarnings("unused")
    private static final String mTAG = Protocol.class.getSimpleName();

    public enum DeviceStatus { None, Idle, Ground, Missing, Present }

    public DeviceStatus deviceStatus;

    Protocol() {
        mProbe = new Probe();
        Looper looper = Looper.myLooper();
        mainLooper = new Handler(looper);
        checkTimeoutRunnable = this::checkTimeout;
        sendResetRunnable = this::sendReset;

        mProbeNewResolution = 0;
        updateData(DeviceStatus.None);
    }

    Protocol(UsbSerialPort usbSerialPort) {
        this();
        mUsbSerialPort = usbSerialPort;
    }

    public boolean isConnected() {
        return deviceStatus != DeviceStatus.None && deviceStatus != DeviceStatus.Idle;
    }

    public boolean isDefined() {
        return deviceStatus != DeviceStatus.None;
    }

    public void unplug() {
        disconnect(DeviceStatus.None);
    }

    public void pause() {
        if (isDefined()) {
            disconnect(DeviceStatus.Idle);
        }
    }

    public int getProbeResolution() {
        return mProbe.getResolution();
    }

    public void setProbeResolution(int resolution) {
        mProbeNewResolution = resolution;
    }

    public void connect() throws IOException {
        if (mUsbSerialPort == null) {
            return;
        }
        try {
            mUsbSerialPort.setParameters(9600, 8, 1, UsbSerialPort.PARITY_NONE);
            mUsbIoManager = new SerialInputOutputManager(mUsbSerialPort, this);
            mUsbIoManager.start();
            communicationActivityStart();
        } catch (IOException ex) {
            unplug();
            throw ex;
        }
    }

    /*
     * SerialInputOutputManager.Listener
     */
    @Override
    public void onNewData(byte[] data) {
        mainLooper.post(() -> receive(data));
    }

    @Override
    public void onRunError(Exception e) {
        mainLooper.post(this::unplug);
    }

    private void stopUsbIoManager() {
        if (mUsbIoManager != null) {
            mUsbIoManager.stop();
            mUsbIoManager.setListener(null);
        }
        mUsbIoManager = null;
    }

    private void closeSerialPort() {
        if (mUsbSerialPort == null) {
            return;
        }
        try {
            mUsbSerialPort.close();
        } catch (IOException ignored) { }
        mUsbSerialPort = null;
    }

    private void sendDataUpstream() {
    //mListener.onData(mProbe);
    }

    private void sendDataDownstream(boolean valid) {
        mProbe.setValid(valid);
        if (valid) {
            mProbe.setResolution(mProbeNewResolution);
            if (mProbe.isPendingScratchpadWrite()) {
                mPendingDeviceActivity = DeviceActivity.WriteScratchpad;
            }
        }
    }

    private void updateData() {
        boolean valid = deviceStatus == DeviceStatus.Present;
        sendDataDownstream(valid);
        sendDataUpstream();
    }

    public void updateData(DeviceStatus newDeviceStatus) {
        deviceStatus = newDeviceStatus;
        updateData();
    }

    private void disconnect(DeviceStatus newDeviceStatus) {
        communicationActivityStop();
        stopUsbIoManager();
        closeSerialPort();
        updateData(newDeviceStatus);
    }

    private void receive(byte[] data) {
        feed();
        doRespond(data);
    }

    private void sendReset() {
        mRemainingBits = 1;
        byte[] data = { (byte)0xf0 };
        try {
            mUsbSerialPort.setParameters(9600, 8, 1, UsbSerialPort.PARITY_NONE);
            mUsbSerialPort.write(data, WRITE_WAIT_MILLIS);
            mCommunicationStatus = CommunicationStatus.ResetSent;
        }
        catch (IOException ignored) {
            updateData(DeviceStatus.None);
        }
        catch (NullPointerException ignored) {
            disconnect(DeviceStatus.None);
        }
        kick();
    }

    private void sendByte(byte data) {
        byte[] buf = new byte[8];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (data & (1 << i)) > 0 ? (byte)0xff : 0;
        }
        try {
            mUsbSerialPort.write(buf, WRITE_WAIT_MILLIS);
            mRemainingBits += buf.length;
        }
        catch (IOException ignored) {
            deviceStatus = DeviceStatus.None;
        }
        catch (NullPointerException ignored) {
            disconnect(DeviceStatus.None);
        }
        kick();
    }

    private void doRespond(@NonNull byte[] data) {

        int len = data.length;
        mRemainingBits -= len;

        /* RECEIVE */
        switch (mCommunicationStatus) {
            case ResetSent:
                if (len != 1) {
                    deviceStatus = DeviceStatus.None;
                    break;
                }
                mCommunicationStatus = CommunicationStatus.ResetReceived;
                switch (data[0]) {
                    case 0:
                        deviceStatus = DeviceStatus.Ground;
                        break;
                    case (byte) 0xf0:
                        deviceStatus = DeviceStatus.Missing;
                        break;
                    default:
                        deviceStatus = DeviceStatus.Present;
                }
                if (deviceStatus != DeviceStatus.Present) {
                    updateData();
                    return;
                }
                try {
                    mUsbSerialPort.setParameters(115200, 6, 1, UsbSerialPort.PARITY_NONE);
                    switch (mDeviceActivity) {
                        case ReadROM:
                            sendByte((byte) 0x33); /* READ ROM */
                            mCommunicationStatus = CommunicationStatus.CommandSent;
                            break;
                        case MeasureTemperature:
                        case ReadTemperature:
                        case WriteScratchpad:
                            sendByte((byte) 0xcc); /* SKIP ROM */
                            mCommunicationStatus = CommunicationStatus.AddressSent;
                            break;
                    }
                }
                catch (IOException | NullPointerException ignored) {
                    deviceStatus = DeviceStatus.None;
                }
                break;

            case AddressSent:
                if (mRemainingBits != 0 || deviceStatus != DeviceStatus.Present) {
                    break;
                }
                mCommunicationStatus = CommunicationStatus.AddressReceived;
                switch (mDeviceActivity) {
                    case MeasureTemperature:
                        sendByte((byte) 0x44);
                        mCommunicationStatus = CommunicationStatus.CommandSent;
                        break;
                    case ReadTemperature:
                        sendByte((byte) 0xbe);
                        mCommunicationStatus = CommunicationStatus.CommandSent;
                        break;
                    case WriteScratchpad:
                        sendByte((byte) 0x4e);
                        mCommunicationStatus = CommunicationStatus.CommandSent;
                        break;
                }
                break;

            case CommandSent:
                if (mRemainingBits != 0 || deviceStatus != DeviceStatus.Present) {
                    break;
                }
                mCommunicationStatus = CommunicationStatus.CommandReceived;
                switch (mDeviceActivity) {
                    case MeasureTemperature:
                        mDeviceActivity = DeviceActivity.WaitingForConversion;
                        mProbe.MeasurementTaken();
                        break;
                    case ReadROM:
                        mRemainingBytes = 8;
                        mPayload = new byte[8];
                        sendByte((byte) 0xff);
                        mCommunicationStatus = CommunicationStatus.DataReceiving;
                        break;
                    case ReadTemperature:
                        mRemainingBytes = 9;
                        mPayload = new byte[9];
                        sendByte((byte) 0xff);
                        mCommunicationStatus = CommunicationStatus.DataReceiving;
                        break;
                    case WriteScratchpad:
                        mPayload = mProbe.getMemory();
                        mRemainingBytes = mPayload.length;
                        sendByte(mPayload[0]);
                        mRemainingBytes--;
                        mCommunicationStatus = CommunicationStatus.DataSending;
                        break;
                }
                break;

            case DataReceiving:
                for (int i = 0; i < len; i++) {
                    if (data[i] % 2 == 1) {
                        mPayload[mPayload.length - mRemainingBytes] |= (byte) (0x80 >> (len + mRemainingBits - i - 1));
                    }
                }
                if (mRemainingBits == 0) {
                    mRemainingBytes--;
                    if (mRemainingBytes > 0) {
                        sendByte((byte) 0xff);
                    }
                    else {
                        mCommunicationStatus = CommunicationStatus.DataReceived;
                        switch (mDeviceActivity) {
                            case ReadROM:
                                mProbe.parseROM(mPayload);
                                mPayload = null;
                                mDeviceActivity = DeviceActivity.Idle;
                                break;
                            case ReadTemperature:
                                if (mProbe.parseScratchpad(mPayload)) {
                                    updateData();
                                }
                                mPayload = null;
                                mDeviceActivity = DeviceActivity.Idle;
                                break;
                        }
                    }
                }
                break;

            case DataSending:
                if (mRemainingBits == 0) {
                    if (mRemainingBytes > 0) {
                        sendByte(mPayload[mPayload.length - mRemainingBytes]);
                        mRemainingBytes--;
                    }
                    else {
                        mDeviceActivity = DeviceActivity.Idle;
                    }
                }
                break;
        }

        if (!isCommunicationInProgress()) {
            changeActivity();
        }
    }

    private boolean isCommunicationInProgress() {
        return mRemainingBits > 0;
    }

    private void changeActivity() {
        /* ACTIVITY CHANGE */
        switch (mDeviceActivity) {
            case WaitingForConversion:
                mDeviceActivity = DeviceActivity.ReadTemperature;
                delayedReset();
                break;
            case Idle:
                if (mPendingDeviceActivity != DeviceActivity.Idle) {
                    mDeviceActivity = mPendingDeviceActivity;
                    mPendingDeviceActivity = DeviceActivity.Idle;
                }
                else {
                    mDeviceActivity = DeviceActivity.MeasureTemperature;
                }
                sendReset();
                break;
        }
    }

    /* main activity loop */
    private void communicationActivityStart() {
        mDeviceActivity = DeviceActivity.ReadROM;
        mPendingDeviceActivity = DeviceActivity.MeasureTemperature;
        sendReset();
    }

    private void communicationActivityStop() {
        mainLooper.removeCallbacks(sendResetRunnable);
        feed();
    }

    private void delayedReset() {
        mainLooper.postDelayed(sendResetRunnable, refreshInterval);
    }

    /* Transfer Watchdog */
    private void kick() {
        mainLooper.postDelayed(checkTimeoutRunnable, checkTimeoutInterval);
    }

    private void feed() {
        mainLooper.removeCallbacks(checkTimeoutRunnable);
    }

    private void checkTimeout() {
        if (mCommunicationStatus == CommunicationStatus.ResetSent) {
            deviceStatus = DeviceStatus.Missing;
        }
        mDeviceActivity = DeviceActivity.Idle;
        updateData();

        if (deviceStatus == DeviceStatus.Ground) {
            return;
        }
        changeActivity();
    }

    private static final int refreshInterval = 800; // milliseconds
    private static final int checkTimeoutInterval = 300;
    private static final int WRITE_WAIT_MILLIS = 500;

    private enum CommunicationStatus { ResetSent, ResetReceived, AddressSent, AddressReceived, CommandSent, CommandReceived, DataReceiving, DataReceived, DataSending }
    private enum DeviceActivity { Idle, ReadROM, MeasureTemperature, WaitingForConversion, ReadTemperature, WriteScratchpad }

    private CommunicationStatus mCommunicationStatus;
    private DeviceActivity mDeviceActivity;
    private DeviceActivity mPendingDeviceActivity;
    private SerialInputOutputManager mUsbIoManager;
    private UsbSerialPort mUsbSerialPort;

    private byte[] mPayload;
    private int mRemainingBits;
    private int mRemainingBytes;
    private int mProbeNewResolution;

    private final Probe mProbe;
    private final Runnable sendResetRunnable;
    private final Runnable checkTimeoutRunnable;
    private final Handler mainLooper;
}