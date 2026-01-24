package com.smartexam.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.smartexam.models.QuestionPack;
import java.util.List;

@Dao
public interface QuestionPackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(QuestionPack pack);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<QuestionPack> packs);

    @Query("SELECT * FROM question_packs WHERE subject = :subject AND grade = :grade")
    List<QuestionPack> getPacks(String subject, int grade);

    @Query("SELECT * FROM question_packs WHERE id = :id LIMIT 1")
    QuestionPack getPackById(String id);

    @Query("DELETE FROM question_packs")
    void deleteAll();
}
