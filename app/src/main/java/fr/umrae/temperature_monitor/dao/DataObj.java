package fr.umrae.temperature_monitor.dao;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(indices = {@Index("deviceId")})
public class DataObj {

    private String deviceId;
    @PrimaryKey
    private long dateTime;
    @ColumnInfo(name = "temperature")
    private float temperature;

    public DataObj() {}

    @Ignore
    public DataObj(long dateTime) {
        this.dateTime = dateTime;
    }

    public DataObj(String deviceId, long dateTime, float temperature) {
        this.deviceId = deviceId;
        this.dateTime = dateTime;
        this.temperature = temperature;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }
}
