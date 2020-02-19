package edu.cnm.deepdive.nasaapod.model.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import edu.cnm.deepdive.nasaapod.model.entity.Apod;
import edu.cnm.deepdive.nasaapod.model.pojo.ApodWithStats;
import io.reactivex.Maybe;
import io.reactivex.Single;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Dao
public interface ApodDao {

  String APOD_STATS_QUERY = "SELECT \n"
      + "    a.*, \n"
      + "    acc.lastAccess, \n"
      + "    acc.accessCount \n"
      + "FROM \n"
      + "    Apod AS a \n"
      + "    LEFT JOIN (\n"
      + "        SELECT \n"
      + "            apod_id, \n"
      + "            MAX(timestamp) AS lastAccess, \n"
      + "            COUNT(*) AS accessCount \n"
      + "        FROM \n"
      + "            Access \n"
      + "        GROUP BY \n"
      + "            apod_id \n"
      + "    ) AS acc \n"
      + "    ON acc.apod_id = a.apod_id \n"
      + "ORDER BY \n"
      + "    a.date DESC;";

  @Insert
  Single<Long> insert(Apod apod);

  @Insert
  Single<List<Long>> insert(Collection<Apod> apods);

  @Insert
  Single<List<Long>> insert(Apod... apods);

  @Delete
  Single<Integer> delete(Apod apod);

  @Delete
  Single<Integer> delete(Collection<Apod> apods);

  @Delete
  Single<Integer> delete(Apod... apods);

  @Query("SELECT * FROM Apod ORDER BY date DESC")
  LiveData<List<Apod>> select();

  @Query(APOD_STATS_QUERY)
  LiveData<List<ApodWithStats>> selectWithStats();

  @Query("SELECT * FROM Apod WHERE date = :date")
  Maybe<Apod> select(Date date);

  @Query("SELECT * FROM Apod WHERE apod_id = :id")
  Single<Apod> select(long id);

}
