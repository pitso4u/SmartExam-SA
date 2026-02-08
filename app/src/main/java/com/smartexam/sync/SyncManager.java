package com.smartexam.sync;

import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.smartexam.database.AppDatabase;
import com.smartexam.models.Question;
import com.smartexam.models.PurchasedPack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final long SYNC_INTERVAL_MINUTES = 30; // Sync every 30 minutes max
    private static final long CACHE_VALIDITY_MINUTES = 60; // Cache valid for 1 hour
    
    private final AppDatabase db;
    private final FirebaseFirestore firestore;
    private final FirebaseAuth mAuth;
    
    // Local cache to prevent repeated Firebase reads
    private final Map<String, Long> lastSyncTimestamps = new HashMap<>();
    private final Map<String, List<PurchasedPack>> purchasedPacksCache = new HashMap<>();
    private final Map<String, List<Question>> questionsCache = new HashMap<>();

    public SyncManager(AppDatabase db) {
        this.db = db;
        this.firestore = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
    }

    /**
     * Optimized sync with local caching and rate limiting
     */
    public void syncPurchasedPacks(SyncCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User not authenticated");
            callback.onSyncFailed("User not authenticated");
            return;
        }

        String userId = user.getUid();
        long currentTime = System.currentTimeMillis();
        
        // Check if we recently synced (rate limiting)
        Long lastSync = lastSyncTimestamps.get(userId);
        if (lastSync != null && (currentTime - lastSync) < TimeUnit.MINUTES.toMillis(SYNC_INTERVAL_MINUTES)) {
            Log.d(TAG, "Using cached purchased packs data (rate limited)");
            List<PurchasedPack> cachedPacks = purchasedPacksCache.get(userId);
            if (cachedPacks != null) {
                callback.onSyncSuccess(cachedPacks.size());
                return;
            }
        }

        // Check local database first (avoid Firebase read)
        List<PurchasedPack> localPacks = db.purchasedPackDao().getAllPurchasedPacks();
        if (!localPacks.isEmpty()) {
            Log.d(TAG, "Using local purchased packs data: " + localPacks.size() + " packs");
            purchasedPacksCache.put(userId, localPacks);
            lastSyncTimestamps.put(userId, currentTime);
            callback.onSyncSuccess(localPacks.size());
            return;
        }

        // Only fetch from Firebase if local data is empty
        Log.d(TAG, "Fetching purchased packs from Firebase for user: " + userId);
        fetchPurchasedPacksFromFirebase(userId, callback);
    }

    /**
     * Fetch from Firebase only when absolutely necessary
     */
    private void fetchPurchasedPacksFromFirebase(String userId, SyncCallback callback) {
        firestore.collection("users")
            .document(userId)
            .collection("purchased_packs")
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<PurchasedPack> purchasedPacks = new ArrayList<>();
                    List<String> packIds = new ArrayList<>();
                    
                    // Process documents efficiently
                    for (DocumentSnapshot document : task.getResult()) {
                        try {
                            PurchasedPack pack = document.toObject(PurchasedPack.class);
                            if (pack != null) {
                                pack.setPackId(document.getId());
                                purchasedPacks.add(pack);
                                packIds.add(pack.getPackId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing pack document: " + document.getId(), e);
                        }
                    }
                    
                    // Cache the results
                    purchasedPacksCache.put(userId, purchasedPacks);
                    lastSyncTimestamps.put(userId, System.currentTimeMillis());
                    
                    // Save locally for future use
                    savePurchasedPacksLocally(purchasedPacks);
                    
                    // Batch fetch questions for all packs (avoid multiple reads)
                    if (!packIds.isEmpty()) {
                        fetchQuestionsForPacksBatch(packIds, callback);
                    } else {
                        callback.onSyncSuccess(purchasedPacks.size());
                    }
                    
                } else {
                    Log.e(TAG, "Error fetching purchased packs", task.getException());
                    callback.onSyncFailed("Error fetching purchased packs: " + task.getException().getMessage());
                }
            });
    }

    /**
     * Batch fetch questions for multiple packs in one operation
     */
    private void fetchQuestionsForPacksBatch(List<String> packIds, SyncCallback callback) {
        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
        
        // Create tasks for all question collections
        for (String packId : packIds) {
            // Check cache first
            if (questionsCache.containsKey(packId)) {
                continue; // Skip if already cached
            }
            
            Task<QuerySnapshot> task = firestore.collection("question_packs")
                .document(packId)
                .collection("questions")
                .get();
            tasks.add(task);
        }
        
        // Execute all tasks in parallel (more efficient than sequential)
        Tasks.whenAllComplete(tasks)
            .addOnCompleteListener(results -> {
                List<Question> allQuestions = new ArrayList<>();
                int successCount = 0;
                
                for (int i = 0; i < tasks.size(); i++) {
                    Task<QuerySnapshot> task = tasks.get(i);
                    String packId = packIds.get(i);
                    
                    if (task.isSuccessful()) {
                        List<Question> packQuestions = new ArrayList<>();
                        for (DocumentSnapshot document : task.getResult()) {
                            try {
                                Question question = document.toObject(Question.class);
                                if (question != null) {
                                    question.setId(document.getId());
                                    packQuestions.add(question);
                                    allQuestions.add(question);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error parsing question document: " + document.getId(), e);
                            }
                        }
                        
                        // Cache questions for this pack
                        questionsCache.put(packId, packQuestions);
                        successCount++;
                    } else {
                        Log.e(TAG, "Error fetching questions for pack: " + packId, task.getException());
                    }
                }
                
                Log.d(TAG, "Batch fetch completed: " + successCount + "/" + packIds.size() + " packs successful");
                
                // Save all questions locally
                if (!allQuestions.isEmpty()) {
                    saveQuestionsLocally(allQuestions);
                }
                
                callback.onSyncSuccess(allQuestions.size());
            });
    }

    /**
     * Get questions from local cache or database (avoid Firebase reads)
     */
    public List<Question> getQuestionsForPack(String packId) {
        // Check memory cache first
        List<Question> cachedQuestions = questionsCache.get(packId);
        if (cachedQuestions != null) {
            Log.d(TAG, "Using cached questions for pack: " + packId);
            return cachedQuestions;
        }
        
        // Check local database
        List<Question> localQuestions = db.questionDao().getQuestionsByPackId(packId);
        if (!localQuestions.isEmpty()) {
            Log.d(TAG, "Using local questions for pack: " + packId + " (" + localQuestions.size() + " questions)");
            // Cache for future use
            questionsCache.put(packId, localQuestions);
            return localQuestions;
        }
        
        Log.d(TAG, "No questions found locally for pack: " + packId);
        return new ArrayList<>();
    }

    /**
     * Check if pack is purchased locally (avoid Firebase read)
     */
    public boolean isPackPurchased(String packId) {
        return db.purchasedPackDao().isPackPurchased(packId);
    }

    /**
     * Save purchased packs to local Room database
     */
    private void savePurchasedPacksLocally(List<PurchasedPack> packs) {
        new Thread(() -> {
            try {
                db.purchasedPackDao().insertAll(packs);
                Log.d(TAG, "Saved " + packs.size() + " purchased packs to local database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving purchased packs locally", e);
            }
        }).start();
    }

    /**
     * Save questions to local Room database
     */
    private void saveQuestionsLocally(List<Question> questions) {
        new Thread(() -> {
            try {
                db.questionDao().insertAll(questions);
                Log.d(TAG, "Saved " + questions.size() + " questions to local database");
            } catch (Exception e) {
                Log.e(TAG, "Error saving questions locally", e);
            }
        }).start();
    }

    /**
     * Clear cache (useful for logout or refresh)
     */
    public void clearCache() {
        lastSyncTimestamps.clear();
        purchasedPacksCache.clear();
        questionsCache.clear();
        Log.d(TAG, "Cache cleared");
    }

    /**
     * Force refresh from Firebase (bypass cache)
     */
    public void forceRefresh(SyncCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            callback.onSyncFailed("User not authenticated");
            return;
        }
        
        // Clear cache for this user
        String userId = user.getUid();
        lastSyncTimestamps.remove(userId);
        purchasedPacksCache.remove(userId);
        
        // Force sync
        syncPurchasedPacks(callback);
    }

    /**
     * Test Firebase connection with minimal data usage
     */
    public void testFirebaseConnection(FirebaseTestCallback callback) {
        Map<String, Object> testData = Map.of(
            "timestamp", System.currentTimeMillis(),
            "testType", "smartexam_sync_test",
            "userId", mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonymous"
        );

        firestore.collection("sync_tests")
            .add(testData)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Firebase connection test successful, doc ID: " + documentReference.getId());
                callback.onTestSuccess("Connection successful! Test data written to Firestore.");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Firebase connection test failed", e);
                callback.onTestFailed("Connection failed: " + e.getMessage());
            });
    }

    /**
     * Legacy method for backward compatibility - processes mock data
     */
    public void processPurchasedPack(String jsonPayload) {
        Log.d(TAG, "Processing mock pack payload (legacy method)");
        com.google.gson.Gson gson = new com.google.gson.Gson();
        com.smartexam.models.Question[] questionsArray = gson.fromJson(jsonPayload, com.smartexam.models.Question[].class);
        if (questionsArray != null) {
            new Thread(() -> {
                db.questionDao().insertAll(java.util.Arrays.asList(questionsArray));
                Log.d(TAG, "Processed mock pack with " + questionsArray.length + " questions");
            }).start();
        }
    }

    public interface SyncCallback {
        void onSyncSuccess(int itemCount);
        void onSyncFailed(String errorMessage);
    }

    public interface FirebaseTestCallback {
        void onTestSuccess(String message);
        void onTestFailed(String errorMessage);
    }
}
