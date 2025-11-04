package org.fptn.vpn.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import org.fptn.vpn.database.FptnDatabase;
import org.fptn.vpn.database.dao.SniDao;
import org.fptn.vpn.database.model.SniDto;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SniRepository {
    private final SniDao sniDao;
    private final ExecutorService executorService;

    public SniRepository(Context context) {
        this.sniDao = FptnDatabase.getInstance(context.getApplicationContext()).sniDao();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<SniDto>> getAllSni() {
        return sniDao.getAllSni();
    }

    public LiveData<Integer> getSniCountLiveData() {
        return sniDao.getSniCountLiveDate();
    }

    public void insertAll(final List<SniDto> sniList) {
        executorService.execute(() -> sniDao.insertAll(sniList));
    }

    public void deleteAll() {
        executorService.execute(sniDao::deleteAll);
    }

}
