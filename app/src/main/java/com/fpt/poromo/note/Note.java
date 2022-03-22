package com.fpt.poromo.note;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

@Entity(tableName = "notes")
public class Note implements Serializable {

    @PrimaryKey(autoGenerate = true)
    @SerializedName("localId")
    public int id;

    @ColumnInfo(name = "is_sync")
    public Integer isSync = 0;

    @ColumnInfo(name = "title")
    @SerializedName("title")
    public String title;

    @ColumnInfo(name = "date_time")
    @SerializedName("date_time")
    public String dateTime;

    @ColumnInfo(name = "subtitle")
    @SerializedName("subtitle")
    public String subtitle;

    @ColumnInfo(name = "note_text")
    @SerializedName("note_text")
    public String noteText;

    @ColumnInfo(name = "image_path")
    @SerializedName("image_path")
    public String imagePath;

    @ColumnInfo(name = "color")
    @SerializedName("color")
    public String color;

    @ColumnInfo(name = "web_link")
    @SerializedName("web_link")
    public String webLink;

    @SerializedName("created_by")
    public Integer createdBy;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getWebLink() {
        return webLink;
    }

    public void setWebLink(String webLink) {
        this.webLink = webLink;
    }

    public Integer getIsSync() {
        return isSync;
    }

    public void setIsSync(Integer isSync) {
        this.isSync = isSync;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String toString() {
        return "Note{" +
                "id=" + id +
                ", isSync='" + isSync + '\'' +
                ", title='" + title + '\'' +
                ", dateTime='" + dateTime + '\'' +
                ", subtitle='" + subtitle + '\'' +
                ", noteText='" + noteText + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", color='" + color + '\'' +
                ", webLink='" + webLink + '\'' +
                '}';
    }
}
