package com.vaultkey.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.*;
import com.vaultkey.data.model.PasswordEntry;
import java.util.List;

@Dao
public interface PasswordDao {

    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    LiveData<List<PasswordEntry>> getAll();

    @Query("SELECT * FROM passwords WHERE id = :id LIMIT 1")
    PasswordEntry getById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PasswordEntry entry);

    @Update
    void update(PasswordEntry entry);

    @Query("DELETE FROM passwords WHERE id = :id")
    void deleteById(long id);
}
