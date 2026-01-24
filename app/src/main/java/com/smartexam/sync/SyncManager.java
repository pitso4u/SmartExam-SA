package com.smartexam.sync;

import com.google.gson.Gson;
import com.smartexam.database.AppDatabase;
import com.smartexam.models.Question;
import java.util.Arrays;
import java.util.List;

public class SyncManager {

    private final AppDatabase db;
    private final Gson gson = new Gson();

    public SyncManager(AppDatabase db) {
        this.db = db;
    }

    /**
     * Simulates downloading a pack and saving it to the database.
     */
    public void processPurchasedPack(String jsonPayload) {
        // Assuming jsonPayload is a JSON array of Question objects
        Question[] questionsArray = gson.fromJson(jsonPayload, Question[].class);
        if (questionsArray == null)
            return;

        List<Question> questions = Arrays.asList(questionsArray);

        new Thread(() -> {
            db.questionDao().insertAll(questions);
        }).start();
    }
}
