package com.smartexam.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(tableName = "purchased_packs", foreignKeys = @ForeignKey(entity = QuestionPack.class, parentColumns = "id", childColumns = "packId", onDelete = ForeignKey.CASCADE))
public class PurchasedPack {
    @PrimaryKey
    @NonNull
    private String packId;
    private long purchasedAt;
    private String transactionId;
    private boolean isSynced; // Whether it's been fully synced for offline use

    @NonNull
    public String getPackId() {
        return packId;
    }

    public void setPackId(@NonNull String packId) {
        this.packId = packId;
    }

    public long getPurchasedAt() {
        return purchasedAt;
    }

    public void setPurchasedAt(long purchasedAt) {
        this.purchasedAt = purchasedAt;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }
}
