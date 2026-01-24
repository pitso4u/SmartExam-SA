package com.smartexam.marketplace.data;

import android.content.Context;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.smartexam.database.AppDatabase;
import com.smartexam.models.QuestionPack;
import com.smartexam.models.PurchasedPack;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for handling Marketplace data with production Firestore
 * integration.
 */
public class MarketplaceRepository {

    private static MarketplaceRepository instance;
    private final FirebaseFirestore db;
    private final AppDatabase localDb;
    private final Context context;

    private MarketplaceRepository(Context context, AppDatabase localDb) {
        this.db = FirebaseFirestore.getInstance();
        this.localDb = localDb;
        this.context = context.getApplicationContext();
    }

    public static synchronized MarketplaceRepository getInstance(Context context, AppDatabase localDb) {
        if (instance == null) {
            instance = new MarketplaceRepository(context, localDb);
        }
        return instance;
    }

    public interface DataCallback<T> {
        void onSuccess(T data);

        void onError(String error);
    }

    /**
     * Fetches available packs from Firestore.
     */
    public void getAvailablePacks(DataCallback<List<QuestionPack>> callback) {
        db.collection("question_packs")
                .whereEqualTo("isPublished", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<QuestionPack> packs = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        QuestionPack pack = document.toObject(QuestionPack.class);
                        packs.add(pack);
                    }
                    callback.onSuccess(packs);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Verifies a purchase and initiates local sync.
     */
    public void recordPurchase(String packId, String transactionId, DataCallback<Boolean> callback) {
        // Logic to record purchase in Firestore and local DB
        PurchasedPack purchasedPack = new PurchasedPack();
        purchasedPack.setPackId(packId);
        purchasedPack.setTransactionId(transactionId);
        purchasedPack.setPurchasedAt(System.currentTimeMillis());
        purchasedPack.setSynced(false);

        new Thread(() -> {
            try {
                localDb.purchasedPackDao().insert(purchasedPack);
                // Trigger background sync
                com.smartexam.marketplace.services.SyncService.enqueueWork(context, packId);
                callback.onSuccess(true);
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    /**
     * Checks if a pack is purchased locally.
     */
    public void isPackPurchased(String packId, DataCallback<Boolean> callback) {
        new Thread(() -> {
            boolean purchased = localDb.purchasedPackDao().isPackPurchased(packId);
            callback.onSuccess(purchased);
        }).start();
    }
}
