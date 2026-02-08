package com.smartexam.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import com.smartexam.models.AssessmentPaper;
import com.smartexam.models.PaperQuestion;
import com.smartexam.models.PurchasedPack;
import com.smartexam.models.Question;
import com.smartexam.models.QuestionPack;
import com.smartexam.models.Subject;

@Database(entities = { Question.class, Subject.class, AssessmentPaper.class,
        PaperQuestion.class, QuestionPack.class, PurchasedPack.class }, version = 5, exportSchema = false)
@TypeConverters({ Converters.class })
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract QuestionDao questionDao();

    public abstract SubjectDao subjectDao();

    public abstract PaperDao paperDao();

    public abstract QuestionPackDao questionPackDao();

    public abstract PurchasedPackDao purchasedPackDao();

    public static AppDatabase getInstance(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "smart_exam_database")
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
