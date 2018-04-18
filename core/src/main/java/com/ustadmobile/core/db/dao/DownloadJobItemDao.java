package com.ustadmobile.core.db.dao;

import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.lib.database.annotation.UmInsert;
import com.ustadmobile.lib.database.annotation.UmQuery;
import com.ustadmobile.lib.db.entities.DownloadJobItem;

import java.util.List;

/**
 * Created by mike on 2/5/18.
 */

public abstract class DownloadJobItemDao {

    @UmInsert
    public abstract void insertList(List<DownloadJobItem> jobItems);

    @UmInsert
    public abstract long insert(DownloadJobItem item);


    @UmQuery("Update DownloadJobItem SET " +
            "status = :status, downloadedSoFar = :downlaodedSoFar, " +
            "downloadLength = :downloadLength, currentSpeed = :currentSpeed" +
            " WHERE id = :downloadJobItemId")
    public abstract void updateDownloadJobItemStatus(int downloadJobItemId, int status,
                                                     long downloadedSoFar, long downloadLength,
                                                     long currentSpeed);

    public void updateDownloadJobItemStatus(DownloadJobItem item) {
        updateDownloadJobItemStatus(item.getId(), item.getStatus(), item.getDownloadedSoFar(),
                item.getDownloadLength(), item.getCurrentSpeed());
    }

    @UmQuery("Select * FROM DownloadJobItem WHERE entryId = :entryId AND status BETWEEN :statusFrom AND :statusTo")
    public abstract UmLiveData<DownloadJobItem> findDownloadJobItemByEntryIdAndStatusRangeLive(String entryId,
                                                                                               int statusFrom,
                                                                                               int statusTo);

    @UmQuery("Select * FROM DownloadJobItem WHERE entryId = :entryId AND status BETWEEN :statusFrom AND :statusTo")
    public abstract List<DownloadJobItem> findDownloadJobItemByEntryIdAndStatusRange(String entryId,
                                                                                   int statusFrom,
                                                                                   int statusTo);



    @UmQuery("SELECT * FROM DownloadJobItem WHERE downloadJobId = :downloadJobId")
    public abstract List<DownloadJobItem> findAllByDownloadJob(int downloadJobId);

    @UmQuery("SELECT id FROM DownloadJobItem WHERE downloadJobId = :downloadJobId")
    public abstract int[] findAllIdsByDownloadJob(int downloadJobId);

    @UmQuery("SELECT * FROM DownloadJobItem WHERE downloadJobId = :downloadJobId AND status BETWEEN :statusFrom AND :statusTo LIMIT 1")
    public abstract DownloadJobItem findByDownloadJobAndStatusRange(int downloadJobId, int statusFrom,
                                                                       int statusTo);



}
