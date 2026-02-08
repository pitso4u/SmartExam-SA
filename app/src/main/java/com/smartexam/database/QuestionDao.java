package com.smartexam.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.smartexam.models.Question;
import java.util.List;

@Dao
public interface QuestionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Question question);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Question> questions);

    @Query("SELECT * FROM questions ORDER BY createdAt DESC")
    List<Question> getAllQuestions();

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    Question getQuestionById(String id);

    @Query("SELECT * FROM questions WHERE subject = :subject AND grade = :grade")
    List<Question> getQuestionsForSubject(String subject, int grade);

    @Query("SELECT * FROM questions WHERE packId = :packId")
    List<Question> getQuestionsByPackId(String packId);

    @Query("SELECT COUNT(*) FROM questions")
    int getQuestionCount();

    @Query("SELECT COUNT(*) FROM questions WHERE subject = :subject")
    int getQuestionCountForSubject(String subject);

    @Query("DELETE FROM questions")
    void deleteAll();

    @Query("DELETE FROM questions WHERE id = :id")
    void deleteById(String id);
}
