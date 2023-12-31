package com.ustadmobile.core.db.dao

import androidx.room.Insert
import androidx.room.Query
import com.ustadmobile.door.annotation.DoorDao
import com.ustadmobile.lib.db.entities.TransferJobItem

@DoorDao
expect abstract class TransferJobItemDao {

    @Insert
    abstract suspend fun insertList(items: List<TransferJobItem>)

    @Insert
    abstract suspend fun insert(item: TransferJobItem): Long

    @Query("""
        SELECT TransferJobItem.*
          FROM TransferJobItem
         WHERE TransferJobItem.tjiTjUid = :jobUid
    """)
    abstract suspend fun findByJobUid(jobUid: Int): List<TransferJobItem>


    @Query("""
        UPDATE TransferJobItem
           SET tjTransferred = :transferred
         WHERE tjiUid = :jobItemUid
    """)
    abstract suspend fun updateTransferredProgress(
        jobItemUid: Int,
        transferred: Long,
    )

    @Query("""
        UPDATE TransferJobItem
           SET tjiStatus = :status
         WHERE tjiUid = :jobItemUid  
    """)
    abstract suspend fun updateStatus(
        jobItemUid: Int,
        status: Int,
    )


    @Query("""
        INSERT INTO OutgoingReplication(destNodeId, orTableId, orPk1, orPk2)
        SELECT :destNodeId AS destNodeId, 
              TransferJobItem.tjiTableId AS orTableId,
              TransferJobItem.tjiEntityUid AS orPk1,
              0 AS orPk2
        FROM TransferJobItem
       WHERE TransferJobItem.tjiUid = :transferJobItemUid
         AND TransferJobItem.tjiTableId != 0
         AND TransferJobItem.tjiStatus = 21
    """)
    abstract suspend fun insertOutgoingReplicationForTransferJobItemIfDone(
        destNodeId: Long,
        transferJobItemUid: Int,
    )

}