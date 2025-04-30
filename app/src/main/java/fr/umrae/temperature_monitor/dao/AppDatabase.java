package fr.umrae.temperature_monitor.dao;


import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {DataObj.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DataDao dataDao();
}