package com.smartexam.marketplace.services;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.Data;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.smartexam.database.AppDatabase;
import com.smartexam.models.Question;
import com.smartexam.models.QuestionPack;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";
    public static final String EXTRA_PACK_ID = "extra_pack_id";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String packId = getInputData().getString(EXTRA_PACK_ID);
        if (packId != null) {
            syncPack(packId);
            return Result.success();
        }
        return Result.failure();
    }

    private void syncPack(String packId) {
        Log.d(TAG, "Starting sync for pack: " + packId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AppDatabase localDb = AppDatabase.getInstance(getApplicationContext());

        db.collection("question_packs").document(packId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    QuestionPack pack = documentSnapshot.toObject(QuestionPack.class);
                    if (pack != null && pack.getQuestionIds() != null) {
                        downloadQuestions(pack.getQuestionIds(), packId);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Failed to fetch pack details", e));
    }

    private void downloadQuestions(List<String> questionIds, String packId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        AppDatabase localDb = AppDatabase.getInstance(getApplicationContext());
        AtomicInteger count = new AtomicInteger(0);

        for (String qId : questionIds) {
            db.collection("questions").document(qId).get()
                    .addOnSuccessListener(doc -> {
                        Question question = doc.toObject(Question.class);
                        if (question != null) {
                            new Thread(() -> {
                                localDb.questionDao().insert(question);
                                if (question.getImagePath() != null) {
                                    downloadImage(question.getImagePath());
                                }
                                if (count.incrementAndGet() == questionIds.size()) {
                                    finalizeSync(packId);
                                }
                            }).start();
                        }
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to download question: " + qId, e));
        }
    }

    private void downloadImage(String remotePath) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference ref = storage.getReference(remotePath);

        File localFile = new File(getApplicationContext().getExternalFilesDir(null), "assets/" + remotePath);
        if (!localFile.getParentFile().exists()) {
            localFile.getParentFile().mkdirs();
        }

        ref.getFile(localFile)
                .addOnSuccessListener(taskSnapshot -> Log.d(TAG, "Image downloaded: " + remotePath))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to download image: " + remotePath, e));
    }

    private void finalizeSync(String packId) {
        AppDatabase localDb = AppDatabase.getInstance(getApplicationContext());
        new Thread(() -> {
            localDb.purchasedPackDao().updateSyncStatus(packId, true);
            Log.d(TAG, "Sync complete for pack: " + packId);
        }).start();
    }
}
