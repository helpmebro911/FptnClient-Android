package org.fptn.vpn.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import org.fptn.vpn.database.model.SniDto;

import java.util.List;

@Dao
public interface SniDao {

    @Query("SELECT * FROM sni_table")
    LiveData<List<SniDto>> getAllSni();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<SniDto> sniList);

    @Query("DELETE FROM sni_table")
    void deleteAll();
}
