package edu.cnm.deepdive.nasaapod.model.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import java.util.Date;

@Entity(
    indices = @Index(value = "date", unique = true)
)
public class Apod {

  @ColumnInfo(name = "apod_id")
  @PrimaryKey(autoGenerate = true)
  private long id;

  @NonNull
  @Expose
  private Date date;

  @NonNull
  @ColumnInfo(index = true, collate = ColumnInfo.NOCASE)
  @Expose
  private String title;

  @NonNull
  @Expose
  @SerializedName("explanation")
  private String description;

  @Expose
  private String copyright;

  @NonNull
  @ColumnInfo(name = "media_type")
  @Expose
  @SerializedName("media_type")
  private MediaType mediaType;

  @Ignore
  @Expose
  @SerializedName("service_version")
  private String serviceVersion;

  @NonNull
  @Expose
  private String url;

  @ColumnInfo(name = "hd_url")
  @Expose
  @SerializedName("hdurl")
  private String hdUrl;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  @NonNull
  public Date getDate() {
    return date;
  }

  public void setDate(@NonNull Date date) {
    this.date = date;
  }

  @NonNull
  public String getTitle() {
    return title;
  }

  public void setTitle(@NonNull String title) {
    this.title = title;
  }

  @NonNull
  public String getDescription() {
    return description;
  }

  public void setDescription(@NonNull String description) {
    this.description = description;
  }

  public String getCopyright() {
    return copyright;
  }

  public void setCopyright(String copyright) {
    this.copyright = copyright;
  }

  @NonNull
  public MediaType getMediaType() {
    return mediaType;
  }

  public void setMediaType(@NonNull MediaType mediaType) {
    this.mediaType = mediaType;
  }

  public String getServiceVersion() {
    return serviceVersion;
  }

  public void setServiceVersion(String serviceVersion) {
    this.serviceVersion = serviceVersion;
  }

  @NonNull
  public String getUrl() {
    return url;
  }

  public void setUrl(@NonNull String url) {
    this.url = url;
  }

  public String getHdUrl() {
    return hdUrl;
  }

  public void setHdUrl(String hdUrl) {
    this.hdUrl = hdUrl;
  }

  public enum MediaType {
    @SerializedName("image")
    IMAGE,
    @SerializedName("video")
    VIDEO;

    @TypeConverter
    public static Integer toInteger(MediaType value) {
      return (value != null) ? value.ordinal() : null;
    }

    @TypeConverter
    public static MediaType toMediaType(Integer value) {
      return (value != null) ? MediaType.values()[value] : null;
    }

  }

}
