package com.ustadmobile.core.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.ustadmobile.door.annotation.*
import com.ustadmobile.lib.db.entities.ContentEntryContentCategoryJoin

@Dao
@Repository
abstract class ContentEntryContentCategoryJoinDao : BaseDao<ContentEntryContentCategoryJoin> {

    @Query("""
     REPLACE INTO ContentEntryContentCategoryJoinReplicate(ceccjPk, ceccjDestination)
      SELECT ContentEntryContentCategoryJoin.ceccjUid AS ceccjPk,
             :newNodeId AS ceccjDestination
        FROM ContentEntryContentCategoryJoin
       WHERE ContentEntryContentCategoryJoin.ceccjLct != COALESCE(
             (SELECT ceccjVersionId
                FROM ContentEntryContentCategoryJoinReplicate
               WHERE ceccjPk = ContentEntryContentCategoryJoin.ceccjUid
                 AND ceccjDestination = :newNodeId), 0) 
      /*psql ON CONFLICT(ceccjPk, ceccjDestination) DO UPDATE
             SET ceccjPending = true
      */       
    """)
    @ReplicationRunOnNewNode
    @ReplicationCheckPendingNotificationsFor([ContentEntryContentCategoryJoin::class])
    abstract suspend fun replicateOnNewNode(@NewNodeIdParam newNodeId: Long)

    @Query("""
 REPLACE INTO ContentEntryContentCategoryJoinReplicate(ceccjPk, ceccjDestination)
  SELECT ContentEntryContentCategoryJoin.ceccjUid AS ceccjUid,
         UserSession.usClientNodeId AS ceccjDestination
    FROM ChangeLog
         JOIN ContentEntryContentCategoryJoin
             ON ChangeLog.chTableId = ${ContentEntryContentCategoryJoin.TABLE_ID}
                AND ChangeLog.chEntityPk = ContentEntryContentCategoryJoin.ceccjUid
         JOIN UserSession
   WHERE UserSession.usClientNodeId != (
         SELECT nodeClientId 
           FROM SyncNode
          LIMIT 1)
     AND ContentEntryContentCategoryJoin.ceccjLct != COALESCE(
         (SELECT ceccjVersionId
            FROM ContentEntryContentCategoryJoinReplicate
           WHERE ceccjPk = ContentEntryContentCategoryJoin.ceccjUid
             AND ceccjDestination = UserSession.usClientNodeId), 0)
 /*psql ON CONFLICT(ceccjPk, ceccjDestination) DO UPDATE
     SET ceccjPending = true
  */               
    """)
    @ReplicationRunOnChange([ContentEntryContentCategoryJoin::class])
    @ReplicationCheckPendingNotificationsFor([ContentEntryContentCategoryJoin::class])
    abstract suspend fun replicateOnChange()

    @Query("SELECT ContentEntryContentCategoryJoin.* FROM ContentEntryContentCategoryJoin " +
            "LEFT JOIN ContentEntry ON ContentEntryContentCategoryJoin.ceccjContentEntryUid = ContentEntry.contentEntryUid " +
            "WHERE ContentEntry.publik")
    abstract fun publicContentEntryContentCategoryJoins(): List<ContentEntryContentCategoryJoin>

    @Query("SELECT * from ContentEntryContentCategoryJoin WHERE " + "ceccjContentCategoryUid = :categoryUid AND ceccjContentEntryUid = :contentEntry")
    abstract fun findJoinByParentChildUuids(categoryUid: Long, contentEntry: Long): ContentEntryContentCategoryJoin?

}
