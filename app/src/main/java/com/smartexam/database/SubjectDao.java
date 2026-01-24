package com.smartexam.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import com.smartexam.models.Subject;
import java.util.List;

@Dao
public interface SubjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Subject subject);

    @Update
    void update(Subject subject);

    @Delete
    void delete(Subject subject);

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    List<Subject> getAllSubjects();

    @Query("SELECT * FROM subjects WHERE id = :id")
    Subject getSubjectById(int id);

    @Query("SELECT * FROM subjects WHERE name = :name LIMIT 1")
    Subject getSubjectByName(String name);

    @Query("SELECT COUNT(*) FROM subjects")
    int getSubjectCount();
}
