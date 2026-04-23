package com.vaultkey.data.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "passwords")
public class PasswordEntry {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title = "";
    public String username = "";
    public String password = "";
    public String url = "";
    public String notes = "";
    public String category = "Other";

    @ColumnInfo(defaultValue = "0")  public int strengthScore = 0;
    @ColumnInfo(defaultValue = "0")  public long createdAt = 0;
    @ColumnInfo(defaultValue = "0")  public long updatedAt = 0;
    @NonNull @ColumnInfo(defaultValue = "") public String avatarPath = "";
}
