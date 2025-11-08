package org.fptn.vpn.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.database.model.FptnServerDto;

import java.util.List;

@Dao
public interface FptnServerDAO {

    @Insert
    void insert(FptnServerDto server);

    @Insert
    void insertAll(List<FptnServerDto> fptnServerDtoList);

    @Query("SELECT * FROM server_table")
    LiveData<List<FptnServerDto>> getAllServersLiveData();

    @Query("SELECT * FROM server_table")
    ListenableFuture<List<FptnServerDto>> getAllServersListFuture();

    @Query("SELECT * FROM server_table WHERE censured = :censured")
    List<FptnServerDto> getServersList(boolean censured);

    @Query("UPDATE server_table SET isSelected = CASE WHEN id = :id THEN 1 ELSE 0 END")
    void setIsSelected(int id);

    @Query("SELECT * FROM server_table WHERE id = :id")
    FptnServerDto getById(int id);

    @Query("SELECT * FROM server_table WHERE isSelected = 1")
    FptnServerDto getSelected();

    @Query("UPDATE server_table SET isSelected = 0 WHERE 1")
    void resetSelected();

    @Query("DELETE FROM server_table")
    void deleteAll();
}
