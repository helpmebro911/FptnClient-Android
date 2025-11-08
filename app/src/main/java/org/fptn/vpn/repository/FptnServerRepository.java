package org.fptn.vpn.repository;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.google.common.util.concurrent.ListenableFuture;

import org.fptn.vpn.database.FptnDatabase;
import org.fptn.vpn.database.dao.FptnServerDAO;
import org.fptn.vpn.database.model.FptnServerDto;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FptnServerRepository {
    private final FptnServerDAO fptnServerDAO;
    private final ExecutorService executorService;

    public FptnServerRepository(Context context) {
        this.fptnServerDAO = FptnDatabase.getInstance(context.getApplicationContext()).fptnServerDAO();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    public ListenableFuture<List<FptnServerDto>> getAllServersListFuture() {
        return fptnServerDAO.getAllServersListFuture();
    }

    public List<FptnServerDto> getServersList(boolean censured) {
        return fptnServerDAO.getServersList(censured);
    }

    public FptnServerDto getById(int id) {
        return fptnServerDAO.getById(id);
    }

    public LiveData<List<FptnServerDto>> getAllServersLiveData() {
        return fptnServerDAO.getAllServersLiveData();
    }

    public void deleteAllServers() {
        executorService.execute(fptnServerDAO::deleteAll);
    }

    public void insertAll(List<FptnServerDto> serverDtoList) {
        executorService.execute(() -> fptnServerDAO.insertAll(serverDtoList));
    }

    public void resetSelected() {
        fptnServerDAO.resetSelected();
    }

    public void setIsSelected(int id) {
        fptnServerDAO.setIsSelected(id);
    }

    public FptnServerDto getSelected() {
        return fptnServerDAO.getSelected();
    }
}
