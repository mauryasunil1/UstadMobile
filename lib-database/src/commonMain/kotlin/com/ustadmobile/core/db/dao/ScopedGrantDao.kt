package com.ustadmobile.core.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ustadmobile.door.annotation.*
import com.ustadmobile.lib.db.entities.*

@Dao
@Repository
abstract class ScopedGrantDao {

    @Query("""
     REPLACE INTO ScopedGrantReplicate(sgPk, sgDestination)
      SELECT ScopedGrantWithPerm.sgUid AS sgPk,
             :newNodeId AS sgDestination
        FROM UserSession
             JOIN PersonGroupMember
                    ON UserSession.usPersonUid = PersonGroupMember.groupMemberPersonUid
               ${Person.JOIN_FROM_PERSONGROUPMEMBER_TO_PERSON_VIA_SCOPEDGRANT_PT1}
                    ${Role.PERMISSION_PERSON_SELECT}
                    ${Person.JOIN_FROM_PERSONGROUPMEMBER_TO_PERSON_VIA_SCOPEDGRANT_PT2}
             JOIN PersonGroupMember PersonsWithPerm_GroupMember
                    ON PersonsWithPerm_GroupMember.groupMemberPersonUid = Person.personUid
             JOIN ScopedGrant ScopedGrantWithPerm
                    ON PersonsWithPerm_GroupMember.groupMemberGroupUid = ScopedGrantWithPerm.sgGroupUid
       WHERE UserSession.usClientNodeId = :newNodeId
         AND UserSession.usStatus = ${UserSession.STATUS_ACTIVE}
         AND ScopedGrantWithPerm.sgLct != COALESCE(
             (SELECT sgVersionId
                FROM ScopedGrantReplicate
               WHERE sgPk = ScopedGrantWithPerm.sgUid
                 AND sgDestination = :newNodeId), 0) 
      /*psql ON CONFLICT(sgPk, sgDestination) DO UPDATE
             SET sgPending = true
      */       
    """)
    @ReplicationRunOnNewNode
    @ReplicationCheckPendingNotificationsFor([ScopedGrant::class])
    abstract suspend fun replicateOnNewNode(@NewNodeIdParam newNodeId: Long)

    @Query("""
 REPLACE INTO ScopedGrantReplicate(sgPk, sgDestination)
  SELECT ScopedGrantEntity.sgUid AS sgPk,
         UserSession.usClientNodeId AS sgDestination
    FROM ChangeLog
         JOIN ScopedGrant ScopedGrantEntity
             ON ChangeLog.chTableId = 48
                AND ChangeLog.chEntityPk = ScopedGrantEntity.sgUid
         JOIN PersonGroupMember
              ON PersonGroupMember.groupMemberGroupUid = ScopedGrantEntity.sgGroupUid
         JOIN Person
              ON PersonGroupMember.groupMemberPersonUid = Person.personUid
         ${Person.JOIN_FROM_PERSON_TO_USERSESSION_VIA_SCOPEDGRANT_PT1}
              ${Role.PERMISSION_PERSON_SELECT}
              ${Person.JOIN_FROM_PERSON_TO_USERSESSION_VIA_SCOPEDGRANT_PT2}
   WHERE UserSession.usClientNodeId != (
         SELECT nodeClientId 
           FROM SyncNode
          LIMIT 1)
     AND ScopedGrantEntity.sgLct != COALESCE(
         (SELECT sgVersionId
            FROM ScopedGrantReplicate
           WHERE sgPk = ScopedGrantEntity.sgUid
             AND sgDestination = UserSession.usClientNodeId), 0)
 /*psql ON CONFLICT(sgPk, sgDestination) DO UPDATE
     SET sgPending = true
  */               
    """)
    @ReplicationRunOnChange([ScopedGrant::class])
    @ReplicationCheckPendingNotificationsFor([ScopedGrant::class])
    abstract suspend fun replicateOnChange()

    @Insert
    abstract suspend fun insertAsync(scopedGrant: ScopedGrant): Long

    @Insert
    abstract suspend fun insertListAsync(scopedGrantList: List<ScopedGrant>)

    @Update
    abstract suspend fun updateAsync(scopedGrant: ScopedGrant)

    @Update
    abstract suspend fun updateListAsync(scopedGrantList: List<ScopedGrant>)

    @Query("""
        SELECT ScopedGrant.*,
               CASE
               WHEN Person.firstNames IS NOT NULL THEN Person.firstNames
               ELSE PersonGroup.groupName 
               END AS name
          FROM ScopedGrant
               JOIN PersonGroup 
                    ON ScopedGrant.sgGroupUid = PersonGroup.groupUid
               LEFT JOIN Person
                         ON Person.personGroupUid = PersonGroup.groupUid
         WHERE ScopedGrant.sgTableId = :tableId
               AND ScopedGrant.sgEntityUid = :entityUid 
    """)
    abstract suspend fun findByTableIdAndEntityUid(tableId: Int, entityUid: Long): List<ScopedGrantAndName>


}