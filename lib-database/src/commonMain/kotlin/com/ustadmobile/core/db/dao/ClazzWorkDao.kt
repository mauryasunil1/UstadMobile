package com.ustadmobile.core.db.dao

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.ustadmobile.lib.database.annotation.UmDao
import com.ustadmobile.lib.database.annotation.UmRepository
import com.ustadmobile.lib.db.entities.ClazzWork
import com.ustadmobile.lib.db.entities.ClazzWorkWithMetrics
import com.ustadmobile.lib.db.entities.ClazzWorkWithSubmission

@UmDao
@UmRepository
@Dao
abstract class ClazzWorkDao : BaseDao<ClazzWork> {

    @Query("SELECT * FROM ClazzWork WHERE clazzWorkUid = :clazzWorkUid " +
            " AND CAST(clazzWorkActive AS INTEGER) = 1")
    abstract fun findByUidAsync(clazzWorkUid: Long): ClazzWork?


    @Update
    abstract suspend fun updateAsync(entity: ClazzWork) : Int

    @Query("""
        SELECT ClazzWork.*, ClazzWorkSubmission.* FROM ClazzWork 
        LEFT JOIN ClazzMember ON ClazzMember.clazzMemberPersonUid = :personUid
			AND ClazzMember.clazzMemberClazzUid = ClazzWork.clazzWorkClazzUid 
			AND CAST(ClazzMember.clazzMemberActive AS INTEGER) = 1
        LEFT JOIN ClazzWorkSubmission ON ClazzWorkSubmission.clazzWorkSubmissionClazzMemberUid = ClazzMember.clazzMemberUid 
		WHERE ClazzWork.clazzWorkUid = :uid
    """)
    abstract suspend fun findWithSubmissionByUidAndPerson(uid: Long, personUid: Long): ClazzWorkWithSubmission?

    @Query(FIND_WITH_METRICS_BY_CLAZZUID + " ORDER BY ClazzWork.clazzWorkTitle ASC")
    abstract fun findWithMetricsByClazzUidLiveAsc(clazzUid: Long): DataSource.Factory<Int,ClazzWorkWithMetrics>

    @Query(FIND_WITH_METRICS_BY_CLAZZUID + " ORDER BY ClazzWork.clazzWorkTitle DESC")
    abstract fun findWithMetricsByClazzUidLiveDesc(clazzUid: Long): DataSource.Factory<Int,ClazzWorkWithMetrics>


    companion object{

        const val FIND_WITH_METRICS_BY_CLAZZUID = """
            SELECT ClazzWork.*, 
             0 as totalStudents, 
             0 as submittedStudents, 
             0 as notSubmittedStudents, 
             0 as completedStudents, 
             0 as markedStudents, 
             0 as firstContentEntryUid,
             Clazz.clazzTimeZone as clazzTimeZone 
             FROM ClazzWork 
             LEFT JOIN Clazz ON Clazz.clazzUid = ClazzWork.clazzWorkClazzUid 
             WHERE clazzWorkClazzUid = :clazzUid
            AND CAST(clazzWorkActive as INTEGER) = 1
        """
    }
}
