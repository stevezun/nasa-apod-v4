package edu.cnm.deepdive.nasaapod.service;

import android.app.Application;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import edu.cnm.deepdive.nasaapod.model.dao.AccessDao;
import edu.cnm.deepdive.nasaapod.model.dao.ApodDao;
import edu.cnm.deepdive.nasaapod.model.entity.Access;
import edu.cnm.deepdive.nasaapod.model.entity.Apod;
import edu.cnm.deepdive.nasaapod.service.ApodDatabase.Converters;
import java.util.Date;

@Database(
    entities = {Apod.class, Access.class},
    version = 1,
    exportSchema = true
)
@TypeConverters({Converters.class, Apod.MediaType.class})
public abstract class ApodDatabase extends RoomDatabase {

  private static final String DB_NAME = "apod_db";

  private static Application context;

  public static void setContext(Application context) {
    ApodDatabase.context = context;
  }

  public static ApodDatabase getInstance() {
    return InstanceHolder.INSTANCE;
  }

  public abstract ApodDao getApodDao();

  public abstract AccessDao getAccessDao();

  private static class InstanceHolder {

    private static final ApodDatabase INSTANCE =
        Room.databaseBuilder(context, ApodDatabase.class, DB_NAME)
            .build();

  }

  public static class Converters {

    @TypeConverter
    public static Long fromDate(Date date) {
      return (date != null) ? date.getTime() : null;
    }

    @TypeConverter
    public static Date fromLong(Long value) {
      return (value != null) ? new Date(value) : null;
    }

  }

}

