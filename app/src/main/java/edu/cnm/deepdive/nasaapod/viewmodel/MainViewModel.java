package edu.cnm.deepdive.nasaapod.viewmodel;

import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.Lifecycle.Event;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;
import edu.cnm.deepdive.nasaapod.BuildConfig;
import edu.cnm.deepdive.nasaapod.model.dao.AccessDao;
import edu.cnm.deepdive.nasaapod.model.dao.ApodDao;
import edu.cnm.deepdive.nasaapod.model.entity.Access;
import edu.cnm.deepdive.nasaapod.model.entity.Apod;
import edu.cnm.deepdive.nasaapod.model.entity.Apod.MediaType;
import edu.cnm.deepdive.nasaapod.model.pojo.ApodWithStats;
import edu.cnm.deepdive.nasaapod.service.ApodDatabase;
import edu.cnm.deepdive.nasaapod.service.ApodService;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.ResponseBody;

public class MainViewModel extends AndroidViewModel implements LifecycleObserver {

  private static final int NETWORK_THREAD_COUNT = 10;
  private static final String LOCAL_FILENAME_FORMAT = "%1$tY%1$tm%1$td-%2$s";
  private static final Pattern URL_FILENAME_PATTERN =
      Pattern.compile("^.*/([^/#?]*)(?:\\?.*)?(?:#.*)?$");

  private final MutableLiveData<Apod> apod;
  private final MutableLiveData<Throwable> throwable;
  private final ApodDatabase database;
  private final ApodService nasa;
  private final CompositeDisposable pending;
  private final Executor networkPool;

  public MainViewModel(@NonNull Application application) {
    super(application);
    database = ApodDatabase.getInstance();
    nasa = ApodService.getInstance();
    apod = new MutableLiveData<>();
    throwable = new MutableLiveData<>();
    pending = new CompositeDisposable();
    networkPool = Executors.newFixedThreadPool(NETWORK_THREAD_COUNT);
    Date today = new Date();
    String formattedDate = ApodService.DATE_FORMATTER.format(today);
    try {
      setApodDate(ApodService.DATE_FORMATTER
          .parse(formattedDate)); // TODO Investigate adjustment for NASA APOD-relevant time zone.
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public LiveData<List<ApodWithStats>> getAllApodSummaries() {
    return database.getApodDao().selectWithStats();
  }

  public LiveData<Apod> getApod() {
    return apod;
  }

  public LiveData<Throwable> getThrowable() {
    return throwable;
  }

  public void setApodDate(Date date) {
    throwable.setValue(null);
    ApodDao dao = database.getApodDao();
    pending.add(
        dao.select(date)
            .subscribeOn(Schedulers.io())
            .switchIfEmpty((SingleSource<? extends Apod>) (observer) ->
                nasa.get(BuildConfig.API_KEY, ApodService.DATE_FORMATTER.format(date))
                    .subscribeOn(Schedulers.from(networkPool))
                    .doOnSuccess((apod) ->
                        pending.add(
                            dao.insert(apod)
                                .doOnSuccess(apod::setId)
                                .doOnError(throwable::postValue)
                                .subscribe()
                        )
                    )
                    .subscribe(observer)
            )
            .doOnSuccess(apod::postValue)
            .doOnError(throwable::postValue)
            .doAfterSuccess(this::insertAccess)
            .subscribe()
    );
  }

  public void getImage(@NonNull Apod apod, @NonNull Consumer<String> pathConsumer) {
    pending.add(getImage(apod).subscribe(pathConsumer, throwable::setValue));
  }

  private Single<String> getImage(@NonNull Apod apod) {
    throwable.setValue(null);
    boolean canBeLocal = (apod.getMediaType() == MediaType.IMAGE);
    File file = canBeLocal ? getFilename(apod) : null;
    return Maybe.fromCallable(() ->
        canBeLocal
            ? (file.exists() ? file.toURI().toString() : null)
            : apod.getUrl())
        .subscribeOn(Schedulers.io())
        .switchIfEmpty((SingleSource<String>) (observer) ->
            nasa.getFile(apod.getUrl())
                .map((body) -> {
                  try {
                    return download(body, file);
                  } catch (IOException ex) {
                    return apod.getUrl();
                  }
                })
                .subscribeOn(Schedulers.from(networkPool))
                .subscribe(observer)
        )
        .observeOn(AndroidSchedulers.mainThread());
  }

  private String download(ResponseBody body, File file) throws IOException {
    try (
        InputStream input = body.byteStream();
        OutputStream output = new FileOutputStream(file)
    ) {
      byte[] buffer = new byte[4096];
      for (int bytesRead = input.read(buffer); bytesRead > -1;
          bytesRead = input.read(buffer)) {
        output.write(buffer, 0, bytesRead);
      }
      output.flush();
      return file.toURI().toString();
    }
  }

  private void insertAccess(Apod apod) {
    AccessDao accessDao = database.getAccessDao();
    Access access = new Access();
    access.setApodId(apod.getId());
    accessDao.insert(access)
        .subscribeOn(Schedulers.io())
        .subscribe(/* TODO Handle error result */);
  }

  private File getFilename(Apod apod) {
    File file = null;
    Matcher matcher = URL_FILENAME_PATTERN.matcher(apod.getUrl());
    if (matcher.matches()) {
      @SuppressLint("DefaultLocale")
      String filename = String.format(LOCAL_FILENAME_FORMAT, apod.getDate(), matcher.group(1));
      File directory = getApplication().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
      file = new File(directory, filename);
    }
    return file;
  }

  @SuppressWarnings("unused")
  @OnLifecycleEvent(Event.ON_STOP)
  private void disposePending() {
    pending.clear();
  }

}
