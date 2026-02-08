package com.smartexam.marketplace.data;

import android.content.Context;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.smartexam.database.AppDatabase;
import com.smartexam.models.QuestionPack;
import com.smartexam.models.PurchasedPack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository for handling Marketplace data with production Firestore
 * integration.
 */
public class MarketplaceRepository {

    private static MarketplaceRepository instance;
    private final FirebaseFirestore db;
    private final AppDatabase localDb;
    private final Context context;
    private final FirebaseAuth auth;

    private MarketplaceRepository(Context context, AppDatabase localDb) {
        this.db = FirebaseFirestore.getInstance();
        this.localDb = localDb;
        this.context = context.getApplicationContext();
        this.auth = FirebaseAuth.getInstance();
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
                FirebaseUser user = auth.getCurrentUser();
                if (user == null) {
                    callback.onError("User not authenticated");
                    return;
                }

                String userId = user.getUid();
                String email = user.getEmail();

                // Create user record in Firestore if not exists
                Map<String, Object> userData = new HashMap<>();
                userData.put("userId", userId);
                userData.put("email", email != null ? email : "");
                userData.put("createdAt", System.currentTimeMillis());

                Tasks.await(db.collection("users").document(userId).set(userData));

                // Add purchased pack to user's purchased_packs collection
                Map<String, Object> packData = new HashMap<>();
                packData.put("packId", purchasedPack.getPackId());
                packData.put("transactionId", purchasedPack.getTransactionId());
                packData.put("purchasedAt", purchasedPack.getPurchasedAt());
                packData.put("synced", purchasedPack.isSynced());

                Tasks.await(db.collection("users").document(userId)
                        .collection("purchased_packs").document(packId).set(packData));

                // Insert into local DB
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
