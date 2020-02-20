package edu.cnm.deepdive.nasaapod.model.repository;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import edu.cnm.deepdive.nasaapod.BuildConfig;
import edu.cnm.deepdive.nasaapod.model.dao.AccessDao;
import edu.cnm.deepdive.nasaapod.model.dao.ApodDao;
import edu.cnm.deepdive.nasaapod.model.entity.Access;
import edu.cnm.deepdive.nasaapod.model.entity.Apod;
import edu.cnm.deepdive.nasaapod.model.pojo.ApodWithStats;
import edu.cnm.deepdive.nasaapod.service.ApodDatabase;
import edu.cnm.deepdive.nasaapod.service.ApodService;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.schedulers.Schedulers;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class ApodRepository {

  private static final int NETWORK_THREAD_COUNT = 10;

  private final ApodDatabase database;
  private final ApodService nasa;
  private final Executor networkPool;

  private static Application context;

  private ApodRepository() {
    if (context == null) {
      throw new IllegalStateException();
    }
    database = ApodDatabase.getInstance();
    nasa = ApodService.getInstance();
    networkPool = Executors.newFixedThreadPool(NETWORK_THREAD_COUNT);
  }

  public static void setContext(Application context) {
    ApodRepository.context = context;
  }

  public static ApodRepository getInstance() {
    return InstanceHolder.INSTANCE;
  }

  public Single<Apod> get(Date date) {
    ApodDao dao = database.getApodDao();
    return dao.select(date)
        .subscribeOn(Schedulers.io())
        .switchIfEmpty((SingleSource<? extends Apod>) (observer) ->
            nasa.get(BuildConfig.API_KEY, ApodService.DATE_FORMATTER.format(date))
                .subscribeOn(Schedulers.from(networkPool))
                .flatMap((apod) ->
                    dao.insert(apod)
                        .map((id) -> {
                          apod.setId(id);
                          return apod;
                        })
                )
                .subscribe(observer)
        )
        .doAfterSuccess(this::insertAccess);
  }

  public LiveData<List<ApodWithStats>> get() {
    return database.getApodDao().selectWithStats();
  }

  public Single<String> getImage(@NonNull Apod apod) {
    // TODO Add local file download & reference.
    return Single.fromCallable(apod::getUrl);
  }

  private void insertAccess(Apod apod) {
    AccessDao accessDao = database.getAccessDao();
    Access access = new Access();
    access.setApodId(apod.getId());
    accessDao.insert(access)
        .subscribeOn(Schedulers.io())
        .subscribe(/* TODO Handle error result */);
  }

  private static class InstanceHolder {

    private static final ApodRepository INSTANCE = new ApodRepository();

  }

}
