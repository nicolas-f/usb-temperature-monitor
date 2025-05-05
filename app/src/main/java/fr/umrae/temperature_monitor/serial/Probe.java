package eu.perpro.android.utmp_basic;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.Date;

class ProbeMemory {

    @SuppressWarnings("unused")
    private static final String mTAG = ProbeMemory.class.getSimpleName();

    public ProbeMemory() { }

    public ProbeMemory(@NonNull ProbeMemory pm) {
        mResolution = pm.mResolution;
        mTH = pm.mTH;
        mTL = pm.mTL;
    }

    public void setMemory(@NonNull byte[] payload) {
        mResolution = 9 + (payload[SP_CNF] >> 5);
        mTH = payload[SP_TH];
        mTL = payload[SP_TL];
        // Log.d(mTAG, String.format("(setMemory) res: %d TH: %d TL: %d", mResolution, mTH, mTL));
    }

    public byte[] getMemory() {
        byte[] sp = new byte[3];
        sp[0] = mTH;
        sp[1] = mTL;
        sp[2] = (byte)(((byte)(mResolution - 9) << 5) | 0x1f);
        return sp;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ProbeMemory)) {
            return false;
        }
        ProbeMemory other = (ProbeMemory)o;
        return mResolution == other.mResolution && mTH == other.mTH && mTL == other.mTL;
    }

    public void setResolution(int newResolution) {
        if (newResolution < 9 || newResolution > 12) {
            throw new IllegalArgumentException();
        }
        mResolution = newResolution;
    }

    public int getResolution() {
        return mResolution;
    }

    private static final int SP_TH = 2;
    private static final int SP_TL = 3;
    private static final int SP_CNF = 4;

    protected int mResolution;
    protected byte mTH;
    protected byte mTL;
}

public class Probe extends ProbeMemory {

    @SuppressWarnings("unused")
    private static final String mTAG = Probe.class.getSimpleName();

    public Probe() {
        Reset();
    }

    private void Reset() {
        setValid(false);
        mPendingProbeMemory = null;
    }

    public void MeasurementTaken() {
        mTaken = new Date();
    }

    public void parseROM(byte[] payload) {
        mROM = eu.perpro.android.util.HexDump.toHexString(payload);
        // Log.i(mTAG, String.format("device rom: %s", mROM));
    }

    public boolean parseScratchpad(byte[] payload) {
        if (!checkCRC(payload)) {
            return false;
        }
        short s = ByteBuffer.wrap(new byte[] {payload[1], payload[0]}).getShort();
        mTemperature = (float)s / 16;
        setMemory(payload);
        updatePendingMemoryStatus();
        // Log.i(mTAG, String.format("temperature %.03f, resolution %d", mTemperature, mResolution));
        return true;
    }

    public void setValid(boolean valid) {
        if (!valid) {
            mROM = null;
            mTaken = null;
        }
    }

    private boolean checkCRC(@NonNull byte[] payload) {
        byte crc = 0;
        for (byte b : payload) {
            crc ^= b;
            for (int j = 8; j > 0; j--) {
                crc = (byte)(((crc & 0x01) == 0x01) ? ((crc >>> 1) & 0x7f) ^ 0x8c : (crc >>> 1) & 0x7f);
            }
        }
        return crc == 0;
    }

    private void updatePendingMemoryStatus() {
        if (mPendingProbeMemory != null && mPendingProbeMemory.equals(this)) {
            mPendingProbeMemory = null;
        }
    }

    public boolean isPendingScratchpadWrite() {
        return mPendingProbeMemory != null;
    }

    @Override
    public void setResolution(int newResolution) {
        if (newResolution < 9 || newResolution > 12) {
            return;
        }
        if (mPendingProbeMemory == null) {
            mPendingProbeMemory = new ProbeMemory(this);
        }
        mPendingProbeMemory.setResolution(newResolution);
        updatePendingMemoryStatus();
    }

    @Override
    public byte[] getMemory() {
        if (mPendingProbeMemory != null) {
            return mPendingProbeMemory.getMemory();
        }
        return super.getMemory();
    }

    /* probe values */
    public boolean isDataValid() {
        return mROM != null & mTaken != null;
    }

    public String getROM() {
        return mROM;
    }

    public Date getTaken() {
        return mTaken;
    }

    public float getTemperature() {
        return mTemperature;
    }

    /* private space */
    private String mROM;
    private Date mTaken;
    private float mTemperature;

    private ProbeMemory mPendingProbeMemory;
}