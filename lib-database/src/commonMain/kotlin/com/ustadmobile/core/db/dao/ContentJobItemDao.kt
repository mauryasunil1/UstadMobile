package com.ustadmobile.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ustadmobile.core.db.JobStatus
import com.ustadmobile.lib.db.entities.ConnectivityStatus
import com.ustadmobile.lib.db.entities.ContentJobItem
import com.ustadmobile.lib.db.entities.ContentJobItem.Companion.ACCEPT_NONE
import com.ustadmobile.lib.db.entities.ContentJobItem.Companion.ACCEPT_UNMETERED

@Dao
abstract class ContentJobItemDao {

    @Query("""
        WITH ConnectivityStateCte(state) AS 
             (SELECT COALESCE(
                     (SELECT connectivityState 
                        FROM ConnectivityStatus 
                       LIMIT 1), 0))
                       
        SELECT ContentJobItem.*
          FROM ContentJobItem
         WHERE ContentJobItem.cjiJobUid = :contentJobUid
           AND ContentJobItem.cjiStatus BETWEEN ${JobStatus.QUEUED} AND ${JobStatus.COMPLETE_MIN}
           AND (
                ((cjiConnectivityAcceptable & $ACCEPT_NONE) = $ACCEPT_NONE)
                OR (((cjiConnectivityAcceptable & $ACCEPT_UNMETERED) = $ACCEPT_UNMETERED) 
                     AND (SELECT state FROM ConnectivityStateCte) = ${ConnectivityStatus.STATE_UNMETERED})
                )
         LIMIT :limit
    """)
    abstract suspend fun findNextItemsInQueue(contentJobUid: Long, limit: Int) : List<ContentJobItem>

    @Insert
    abstract suspend fun insertJobItem(jobItem: ContentJobItem) : Long

    @Insert
    abstract suspend fun insertJobItems(jobItems: List<ContentJobItem>)


    @Query("""
        UPDATE ContentJobItem 
           SET cjiStatus = :status
         WHERE cjiUid= :cjiUid  
    """)
    abstract suspend fun updateItemStatus(cjiUid: Long, status: Int)

    @Query("""
        SELECT NOT EXISTS(
               SELECT cjiUid 
                 FROM ContentJobItem
                WHERE cjiStatus < ${JobStatus.COMPLETE_MIN}) 
    """)
    abstract suspend fun isJobDone(jobUid: Long): Boolean

}