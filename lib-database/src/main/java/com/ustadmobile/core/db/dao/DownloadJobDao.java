package com.ustadmobile.core.db.dao;

import com.ustadmobile.core.db.UmLiveData;
import com.ustadmobile.lib.database.annotation.UmDao;
import com.ustadmobile.lib.database.annotation.UmInsert;
import com.ustadmobile.lib.database.annotation.UmQuery;
import com.ustadmobile.lib.database.annotation.UmUpdate;
import com.ustadmobile.lib.db.entities.DownloadJob;

import java.util.List;

/**
 * DAO for the DownloadJob class
 */
@UmDao
public abstract class DownloadJobDao {

    /**
     * IInsert a new DownloadJob
     *
     * @param job DownloadJob entity to insert
     *
     * @return The Primary Key value assigned to the inserted object
     */
    @UmInsert
    public abstract long insert(DownloadJob job);

    /**
     * Mark the status in bulk of DownloadJob, useful for testing purposes to cancel other downloads
     *
     * @param rangeFrom The minimum existing status of a job
     * @param rangeTo The maximum existing status of a job
     * @param djStatus The status to set on a job
     */
    @UmQuery("UPDATE DownloadJob SET djStatus = :djStatus WHERE djStatus BETWEEN :rangeFrom AND :rangeTo")
    @Deprecated
    public abstract void updateJobStatusByRange(int rangeFrom, int rangeTo, int djStatus);

    @UmQuery("UPDATE  DownloadJob SET djStatus = :djStatus WHERE djUid = :djUid")
    public abstract void updateByJobUid(long djUid, int djStatus);


    /**
     * Update all fields on the given DownloadJob
     *
     * @param job The DownloadJob to update
     */
    @UmUpdate
    public abstract void update(DownloadJob job);


    /**
     * Find a DownloadJob by the downloadJobId (primary key)
     *
     * @param djUid downloadJobId to search for.
     *
     * @return The DownloadJob with the given id, or null if no such DownloadJob exists
     */
    @UmQuery("SELECT * From DownloadJob WHERE djUid = :djUid")
    public abstract DownloadJob findById(int djUid);


    /**
     * Get a list of all DownloadJob items. Used for debugging purposes.
     *
     * @return A list of all DownloadJob entity objects
     */
    @UmQuery("SELECT * From DownloadJob")
    public abstract List<DownloadJob> findAll();

    @UmQuery("SELECT * FROM DownloadJob WHERE djUid = :djUid")
    public abstract UmLiveData<DownloadJob>  getJobLive(long djUid);

}
