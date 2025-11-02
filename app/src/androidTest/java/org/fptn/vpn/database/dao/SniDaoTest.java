package org.fptn.vpn.database.dao;

import static org.assertj.core.api.Assertions.assertThat;

import android.content.Context;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.fptn.vpn.database.FptnDatabase;
import org.fptn.vpn.database.model.SniDto;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class SniDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private FptnDatabase db;
    private SniDao sniDao;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, FptnDatabase.class).build();
        sniDao = db.sniDao();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void insertAndGetAllSni() throws InterruptedException {
        SniDto sni1 = new SniDto("sni1");
        SniDto sni2 = new SniDto("sni2");
        List<SniDto> snis = Arrays.asList(sni1, sni2);

        sniDao.insertAll(snis);

        LiveData<List<SniDto>> allSni = sniDao.getAllSni();
        List<SniDto> observedSnis = getObservedValue(allSni);

        assertThat(observedSnis).hasSize(2);
        assertThat(observedSnis.get(0).sni).isEqualTo("sni1");
        assertThat(observedSnis.get(1).sni).isEqualTo("sni2");
    }

    @Test
    public void insertDuplicateSni() throws InterruptedException {
        SniDto sni1 = new SniDto("sni1");
        SniDto sni2 = new SniDto("sni1"); // Duplicate
        List<SniDto> snis = Arrays.asList(sni1, sni2);

        sniDao.insertAll(snis);

        LiveData<List<SniDto>> allSni = sniDao.getAllSni();
        List<SniDto> observedSnis = getObservedValue(allSni);

        assertThat(observedSnis).hasSize(1);
        assertThat(observedSnis.get(0).sni).isEqualTo("sni1");
    }

    @Test
    public void deleteAll() throws InterruptedException {
        SniDto sni1 = new SniDto("sni1");
        List<SniDto> snis = Arrays.asList(sni1);

        sniDao.insertAll(snis);
        sniDao.deleteAll();

        LiveData<List<SniDto>> allSni = sniDao.getAllSni();
        List<SniDto> observedSnis = getObservedValue(allSni);

        assertThat(observedSnis).isEmpty();
    }

    private <T> T getObservedValue(final LiveData<T> liveData) throws InterruptedException {
        final Object[] data = new Object[1];
        final CountDownLatch latch = new CountDownLatch(1);
        Observer<T> observer = new Observer<T>() {
            @Override
            public void onChanged(T o) {
                data[0] = o;
                latch.countDown();
                liveData.removeObserver(this);
            }
        };
        liveData.observeForever(observer);
        latch.await(2, TimeUnit.SECONDS);
        //noinspection unchecked
        return (T) data[0];
    }
}
