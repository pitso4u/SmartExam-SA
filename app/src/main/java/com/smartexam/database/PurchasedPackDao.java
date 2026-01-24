package com.smartexam.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import com.smartexam.models.PurchasedPack;
import java.util.List;

@Dao
public interface PurchasedPackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PurchasedPack purchasedPack);

    @Query("SELECT * FROM purchased_packs")
    List<PurchasedPack> getAllPurchasedPacks();

    @Query("SELECT EXISTS(SELECT 1 FROM purchased_packs WHERE packId = :packId)")
    boolean isPackPurchased(String packId);

    @Query("UPDATE purchased_packs SET isSynced = :synced WHERE packId = :packId")
    void updateSyncStatus(String packId, boolean synced);
}
