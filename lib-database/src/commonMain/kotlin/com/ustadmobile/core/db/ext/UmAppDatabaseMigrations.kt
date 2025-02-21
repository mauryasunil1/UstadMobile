package com.ustadmobile.core.db.ext

import com.ustadmobile.door.ext.dbType
import com.ustadmobile.door.migration.DoorMigrationStatementList
import com.ustadmobile.door.DoorDbType
import com.ustadmobile.door.migration.DoorMigration
import com.ustadmobile.door.util.systemTimeInMillis
import com.ustadmobile.lib.db.entities.CacheLockJoin
import com.ustadmobile.lib.db.entities.ClazzEnrolment
import com.ustadmobile.lib.db.entities.CoursePermission
import com.ustadmobile.lib.db.entities.Message
import com.ustadmobile.lib.db.entities.UserSession


val MIGRATION_105_106 = DoorMigrationStatementList(105, 106) { db ->
    val stmtList = mutableListOf<String>()
    stmtList += "ALTER TABLE CourseAssignmentSubmissionAttachment ADD COLUMN casaFileName TEXT"

    stmtList

}

val MIGRATION_106_107 = DoorMigrationStatementList(106, 107) {db ->
    mutableListOf<String>().apply {
        add("DROP TABLE IF EXISTS SqliteChangeSeqNums")
        add("DROP TABLE IF EXISTS UpdateNotification")
    }
}

val MIGRATION_107_108 = DoorMigrationStatementList(107, 108) { db ->
    val stmtList = mutableListOf<String>()
    stmtList += "ALTER TABLE CourseAssignmentMark ADD COLUMN camMarkerComment TEXT"
    stmtList += "ALTER TABLE ClazzAssignment ADD COLUMN caPeerReviewerCount  INTEGER  NOT NULL  DEFAULT 0"
    if (db.dbType() == DoorDbType.SQLITE) {

        stmtList += "ALTER TABLE CourseAssignmentMark ADD COLUMN camMarkerSubmitterUid  INTEGER  NOT NULL  DEFAULT 0"
        stmtList += "ALTER TABLE CourseAssignmentMark ADD COLUMN camMarkerPersonUid  INTEGER  NOT NULL  DEFAULT 0"

        stmtList += "CREATE TABLE IF NOT EXISTS PeerReviewerAllocation (`praUid` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `praMarkerSubmitterUid` INTEGER NOT NULL, `praToMarkerSubmitterUid` INTEGER NOT NULL, `praAssignmentUid` INTEGER NOT NULL, `praActive` INTEGER NOT NULL, `praLct` INTEGER NOT NULL)"
        stmtList += "CREATE TABLE IF NOT EXISTS PeerReviewerAllocationReplicate (`prarPk` INTEGER NOT NULL, `prarVersionId` INTEGER NOT NULL DEFAULT 0, `prarDestination` INTEGER NOT NULL, `prarPending` INTEGER NOT NULL DEFAULT 1, PRIMARY KEY(`prarPk`, `prarDestination`))"
        stmtList += "CREATE INDEX IF NOT EXISTS `index_PeerReviewerAllocationReplicate_prarPk_prarDestination_prarVersionId` ON PeerReviewerAllocationReplicate (`prarPk`, `prarDestination`, `prarVersionId`)"
        stmtList += "CREATE INDEX IF NOT EXISTS `index_PeerReviewerAllocationReplicate_prarDestination_prarPending` ON PeerReviewerAllocationReplicate (`prarDestination`, `prarPending`)"


        stmtList +=
            " CREATE TRIGGER ch_ins_140 AFTER INSERT ON PeerReviewerAllocation BEGIN INSERT INTO ChangeLog(chTableId, chEntityPk, chType) SELECT 140 AS chTableId, NEW.praUid AS chEntityPk, 1 AS chType WHERE NOT EXISTS( SELECT chTableId FROM ChangeLog WHERE chTableId = 140 AND chEntityPk = NEW.praUid); END "
        stmtList +=
            " CREATE TRIGGER ch_upd_140 AFTER UPDATE ON PeerReviewerAllocation BEGIN INSERT INTO ChangeLog(chTableId, chEntityPk, chType) SELECT 140 AS chTableId, NEW.praUid AS chEntityPk, 1 AS chType WHERE NOT EXISTS( SELECT chTableId FROM ChangeLog WHERE chTableId = 140 AND chEntityPk = NEW.praUid); END "
        stmtList +=
            " CREATE TRIGGER ch_del_140 AFTER DELETE ON PeerReviewerAllocation BEGIN INSERT INTO ChangeLog(chTableId, chEntityPk, chType) SELECT 140 AS chTableId, OLD.praUid AS chEntityPk, 2 AS chType WHERE NOT EXISTS( SELECT chTableId FROM ChangeLog WHERE chTableId = 140 AND chEntityPk = OLD.praUid); END "
        stmtList +=
            "CREATE VIEW PeerReviewerAllocation_ReceiveView AS  SELECT PeerReviewerAllocation.*, ClazzAssignmentReplicate.* FROM PeerReviewerAllocation LEFT JOIN ClazzAssignmentReplicate ON ClazzAssignmentReplicate.caPk = PeerReviewerAllocation.praUid "
        stmtList +=
            " CREATE TRIGGER peerreviewerallocation_remote_insert_ins INSTEAD OF INSERT ON PeerReviewerAllocation_ReceiveView FOR EACH ROW BEGIN REPLACE INTO PeerReviewerAllocation(praUid, praMarkerSubmitterUid, praToMarkerSubmitterUid, praAssignmentUid, praActive, praLct) VALUES (NEW.praUid, NEW.praMarkerSubmitterUid, NEW.praToMarkerSubmitterUid, NEW.praAssignmentUid, NEW.praActive, NEW.praLct) /*psql ON CONFLICT (praUid) DO UPDATE SET praMarkerSubmitterUid = EXCLUDED.praMarkerSubmitterUid, praToMarkerSubmitterUid = EXCLUDED.praToMarkerSubmitterUid, praAssignmentUid = EXCLUDED.praAssignmentUid, praActive = EXCLUDED.praActive, praLct = EXCLUDED.praLct */; END "


    }else{
        stmtList += "ALTER TABLE CourseAssignmentMark ADD COLUMN camMarkerSubmitterUid  BIGINT  NOT NULL  DEFAULT 0"
        stmtList += "ALTER TABLE CourseAssignmentMark ADD COLUMN camMarkerPersonUid  BIGINT  NOT NULL  DEFAULT 0"

        stmtList +=
            "CREATE TABLE IF NOT EXISTS PeerReviewerAllocation (  praMarkerSubmitterUid  BIGINT  NOT NULL , praToMarkerSubmitterUid  BIGINT  NOT NULL , praAssignmentUid  BIGINT  NOT NULL , praActive  BOOL  NOT NULL , praLct  BIGINT  NOT NULL , praUid  BIGSERIAL  PRIMARY KEY  NOT NULL )"
        stmtList +=
            "CREATE TABLE IF NOT EXISTS PeerReviewerAllocationReplicate (  prarPk  BIGINT  NOT NULL , prarVersionId  BIGINT  NOT NULL  DEFAULT 0 , prarDestination  BIGINT  NOT NULL , prarPending  BOOL  NOT NULL  DEFAULT true, PRIMARY KEY (prarPk, prarDestination) )"
        stmtList +=
            "CREATE INDEX index_PeerReviewerAllocationReplicate_prarPk_prarDestination_prarVersionId ON PeerReviewerAllocationReplicate (prarPk, prarDestination, prarVersionId)"
        stmtList +=
            "CREATE INDEX index_PeerReviewerAllocationReplicate_prarDestination_prarPending ON PeerReviewerAllocationReplicate (prarDestination, prarPending)"
        stmtList +=
            " CREATE OR REPLACE FUNCTION ch_upd_140_fn() RETURNS TRIGGER AS ${'$'}${'$'} BEGIN INSERT INTO ChangeLog(chTableId, chEntityPk, chType) VALUES (140, NEW.praUid, 1) ON CONFLICT(chTableId, chEntityPk) DO UPDATE SET chType = 1; RETURN NULL; END ${'$'}${'$'} LANGUAGE plpgsql "
        stmtList +=
            " CREATE TRIGGER ch_upd_140_trig AFTER UPDATE OR INSERT ON PeerReviewerAllocation FOR EACH ROW EXECUTE PROCEDURE ch_upd_140_fn(); "
        stmtList +=
            " CREATE OR REPLACE FUNCTION ch_del_140_fn() RETURNS TRIGGER AS ${'$'}${'$'} BEGIN INSERT INTO ChangeLog(chTableId, chEntityPk, chType) VALUES (140, OLD.praUid, 2) ON CONFLICT(chTableId, chEntityPk) DO UPDATE SET chType = 2; RETURN NULL; END ${'$'}${'$'} LANGUAGE plpgsql "
        stmtList +=
            " CREATE TRIGGER ch_del_140_trig AFTER DELETE ON PeerReviewerAllocation FOR EACH ROW EXECUTE PROCEDURE ch_del_140_fn(); "
        stmtList +=
            "CREATE VIEW PeerReviewerAllocation_ReceiveView AS  SELECT PeerReviewerAllocation.*, ClazzAssignmentReplicate.* FROM PeerReviewerAllocation LEFT JOIN ClazzAssignmentReplicate ON ClazzAssignmentReplicate.caPk = PeerReviewerAllocation.praUid "
        stmtList +=
            "CREATE OR REPLACE FUNCTION peerreviewerallocation_remote_insert_fn() RETURNS TRIGGER AS ${'$'}${'$'} BEGIN INSERT INTO PeerReviewerAllocation(praUid, praMarkerSubmitterUid, praToMarkerSubmitterUid, praAssignmentUid, praActive, praLct) VALUES (NEW.praUid, NEW.praMarkerSubmitterUid, NEW.praToMarkerSubmitterUid, NEW.praAssignmentUid, NEW.praActive, NEW.praLct) ON CONFLICT (praUid) DO UPDATE SET praMarkerSubmitterUid = EXCLUDED.praMarkerSubmitterUid, praToMarkerSubmitterUid = EXCLUDED.praToMarkerSubmitterUid, praAssignmentUid = EXCLUDED.praAssignmentUid, praActive = EXCLUDED.praActive, praLct = EXCLUDED.praLct ; IF (TG_OP = 'INSERT' OR TG_OP = 'UPDATE') THEN RETURN NEW; ELSE RETURN OLD; END IF; END ${'$'}${'$'} LANGUAGE plpgsql"
        stmtList +=
            " CREATE TRIGGER peerreviewerallocation_remote_insert_trig INSTEAD OF INSERT ON PeerReviewerAllocation_ReceiveView FOR EACH ROW EXECUTE PROCEDURE peerreviewerallocation_remote_insert_fn() "



    }

    stmtList

}

val MIGRATION_108_109 = DoorMigrationStatementList(108, 109) { db ->
    val stmtList = mutableListOf<String>()
    if (db.dbType() == DoorDbType.SQLITE) {
        stmtList += "CREATE TABLE IF NOT EXISTS ExternalAppPermission (  eapPersonUid  INTEGER  NOT NULL , eapPackageId  TEXT , eapStartTime  INTEGER  NOT NULL , eapExpireTime  INTEGER  NOT NULL , eapAuthToken  TEXT , eapAndroidAccountName  TEXT , eapUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )"
    } else {
        stmtList += "CREATE TABLE IF NOT EXISTS ExternalAppPermission (  eapPersonUid  BIGINT  NOT NULL , eapPackageId  TEXT , eapStartTime  BIGINT  NOT NULL , eapExpireTime  BIGINT  NOT NULL , eapAuthToken  TEXT , eapAndroidAccountName  TEXT , eapUid  SERIAL  PRIMARY KEY  NOT NULL )"
    }

    stmtList
}

/**
 * Add StudentResult table
 */
val MIGRATION_120_121 = DoorMigrationStatementList(120, 121) { db ->
    buildList {
        when(db.dbType()) {
            DoorDbType.SQLITE -> {
                add(
                    "CREATE TABLE IF NOT EXISTS StudentResult (  srSourcedId  TEXT , srCourseBlockUid  INTEGER  NOT NULL , srClazzUid  INTEGER  NOT NULL , srAssignmentUid  INTEGER  NOT NULL , srLineItemSourcedId  TEXT , srStatus  INTEGER  NOT NULL , srMetaData  TEXT , srStudentPersonUid  INTEGER  NOT NULL , srStudentGroupId  INTEGER  NOT NULL , srMarkerPersonUid  INTEGER  NOT NULL , srMarkerGroupId  INTEGER  NOT NULL , srScoreStatus  INTEGER  NOT NULL , srScore  REAl  NOT NULL , srScoreDate  INTEGER  NOT NULL , srLastModified  INTEGER  NOT NULL , srComment  TEXT , srAppId  TEXT , srActive  INTEGER  NOT NULL , srUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )"
                )
            }
            DoorDbType.POSTGRES -> {
                add(
                    "CREATE TABLE IF NOT EXISTS StudentResult (  srSourcedId  TEXT , srCourseBlockUid  BIGINT  NOT NULL , srClazzUid  BIGINT  NOT NULL , srAssignmentUid  BIGINT  NOT NULL , srLineItemSourcedId  TEXT , srStatus  INTEGER  NOT NULL , srMetaData  TEXT , srStudentPersonUid  BIGINT  NOT NULL , srStudentGroupId  INTEGER  NOT NULL , srMarkerPersonUid  BIGINT  NOT NULL , srMarkerGroupId  INTEGER  NOT NULL , srScoreStatus  INTEGER  NOT NULL , srScore  FLOAT  NOT NULL , srScoreDate  BIGINT  NOT NULL , srLastModified  BIGINT  NOT NULL , srComment  TEXT , srAppId  TEXT , srActive  BOOL  NOT NULL , srUid  BIGSERIAL  PRIMARY KEY  NOT NULL )"
                )
            }
        }
    }
}

//Add new door tables: OutgoingReplication, ReplicationOperation, PendingRepositorySession
val MIGRATION_121_122 = DoorMigrationStatementList(121, 122) { db ->
    buildList {
        when(db.dbType()) {
            DoorDbType.SQLITE -> {
                add(
                    "CREATE TABLE IF NOT EXISTS OutgoingReplication (  destNodeId  INTEGER  NOT NULL , orPk1  INTEGER  NOT NULL , orPk2  INTEGER  NOT NULL , orTableId  INTEGER  NOT NULL , orUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )"
                )
                add(
                    "CREATE TABLE IF NOT EXISTS ReplicationOperation (  repOpRemoteNodeId  INTEGER  NOT NULL , repOpStatus  INTEGER  NOT NULL , repOpTableId  INTEGER  NOT NULL , PRIMARY KEY (repOpRemoteNodeId, repOpTableId) )"
                )
                add(
                    "CREATE TABLE IF NOT EXISTS PendingRepositorySession (  endpointUrl  TEXT , remoteNodeId  INTEGER  NOT NULL , rsUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )"
                )
            }
            DoorDbType.POSTGRES -> {
                add(
                    "CREATE TABLE IF NOT EXISTS OutgoingReplication (  destNodeId  BIGINT  NOT NULL , orPk1  BIGINT  NOT NULL , orPk2  BIGINT  NOT NULL , orTableId  INTEGER  NOT NULL , orUid  BIGSERIAL  PRIMARY KEY  NOT NULL )"
                )
                add(
                    "CREATE TABLE IF NOT EXISTS ReplicationOperation (  repOpRemoteNodeId  BIGINT  NOT NULL , repOpStatus  INTEGER  NOT NULL , repOpTableId  INTEGER  NOT NULL , PRIMARY KEY (repOpRemoteNodeId, repOpTableId) )"
                )
                add(
                    "CREATE TABLE IF NOT EXISTS PendingRepositorySession (  endpointUrl  TEXT , remoteNodeId  BIGINT  NOT NULL , rsUid  BIGSERIAL  PRIMARY KEY  NOT NULL )"
                )
            }
        }
    }
}

val MIGRATION_122_123 = DoorMigrationStatementList(122, 123) { db ->
    listOf("DROP TABLE IF EXISTS CourseDiscussion")
}

/**
 * ContentJobItem is modified to use ReplicateEntity (adds a last modified time).
 */
val MIGRATION_123_124 = DoorMigrationStatementList(123, 124) { db ->
    buildList {
        add("DROP TABLE IF EXISTS ContentJobItem")
        add("DROP TABLE IF EXISTS ContentJob")

        when(db.dbType()) {
            DoorDbType.SQLITE -> {
                add("CREATE TABLE IF NOT EXISTS ContentJob (  toUri  TEXT , cjProgress  INTEGER  NOT NULL , cjTotal  INTEGER  NOT NULL , cjNotificationTitle  TEXT , cjIsMeteredAllowed  INTEGER  NOT NULL , params  TEXT , cjLct  INTEGER  NOT NULL , cjUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
                add("CREATE TABLE IF NOT EXISTS ContentJobItem (  cjiJobUid  INTEGER  NOT NULL , sourceUri  TEXT , cjiIsLeaf  INTEGER  NOT NULL , cjiContentEntryUid  INTEGER  NOT NULL , cjiParentContentEntryUid  INTEGER  NOT NULL , cjiContainerUid  INTEGER  NOT NULL , cjiItemProgress  INTEGER  NOT NULL , cjiItemTotal  INTEGER  NOT NULL , cjiRecursiveProgress  INTEGER  NOT NULL , cjiRecursiveTotal  INTEGER  NOT NULL , cjiStatus  INTEGER  NOT NULL , cjiRecursiveStatus  INTEGER  NOT NULL , cjiConnectivityNeeded  INTEGER  NOT NULL , cjiPluginId  INTEGER  NOT NULL , cjiAttemptCount  INTEGER  NOT NULL , cjiParentCjiUid  INTEGER  NOT NULL , cjiServerJobId  INTEGER  NOT NULL , cjiStartTime  INTEGER  NOT NULL , cjiFinishTime  INTEGER  NOT NULL , cjiUploadSessionUid  TEXT , cjiContentDeletedOnCancellation  INTEGER  NOT NULL , cjiContainerProcessed  INTEGER  NOT NULL , cjiLastModified  INTEGER  NOT NULL , cjiUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
                add("CREATE INDEX index_ContentJobItem_cjiContentEntryUid_cjiFinishTime ON ContentJobItem (cjiContentEntryUid, cjiFinishTime)")
            }
            DoorDbType.POSTGRES -> {
                add("CREATE TABLE IF NOT EXISTS ContentJob (  toUri  TEXT , cjProgress  BIGINT  NOT NULL , cjTotal  BIGINT  NOT NULL , cjNotificationTitle  TEXT , cjIsMeteredAllowed  BOOL  NOT NULL , params  TEXT , cjLct  BIGINT  NOT NULL , cjUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
                add("CREATE TABLE IF NOT EXISTS ContentJobItem (  cjiJobUid  BIGINT  NOT NULL , sourceUri  TEXT , cjiIsLeaf  BOOL  NOT NULL , cjiContentEntryUid  BIGINT  NOT NULL , cjiParentContentEntryUid  BIGINT  NOT NULL , cjiContainerUid  BIGINT  NOT NULL , cjiItemProgress  BIGINT  NOT NULL , cjiItemTotal  BIGINT  NOT NULL , cjiRecursiveProgress  BIGINT  NOT NULL , cjiRecursiveTotal  BIGINT  NOT NULL , cjiStatus  INTEGER  NOT NULL , cjiRecursiveStatus  INTEGER  NOT NULL , cjiConnectivityNeeded  BOOL  NOT NULL , cjiPluginId  INTEGER  NOT NULL , cjiAttemptCount  INTEGER  NOT NULL , cjiParentCjiUid  BIGINT  NOT NULL , cjiServerJobId  BIGINT  NOT NULL , cjiStartTime  BIGINT  NOT NULL , cjiFinishTime  BIGINT  NOT NULL , cjiUploadSessionUid  TEXT , cjiContentDeletedOnCancellation  BOOL  NOT NULL , cjiContainerProcessed  BOOL  NOT NULL , cjiLastModified  BIGINT  NOT NULL , cjiUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
                add("CREATE INDEX index_ContentJobItem_cjiContentEntryUid_cjiFinishTime ON ContentJobItem (cjiContentEntryUid, cjiFinishTime)")
            }
        }
    }
}

/**
 * Rename field on ContentJobItem, add new ContentEntryVersion table
 */
val MIGRATION_124_125 = DoorMigrationStatementList(124, 125) { db ->
    buildList {
        add("DROP TABLE IF EXISTS ContentJobItem")
        when(db.dbType()) {
            DoorDbType.SQLITE -> {
                add("CREATE TABLE IF NOT EXISTS ContentJobItem (  cjiJobUid  INTEGER  NOT NULL , sourceUri  TEXT , cjiOriginalFilename  TEXT , cjiIsLeaf  INTEGER  NOT NULL , cjiContentEntryUid  INTEGER  NOT NULL , cjiParentContentEntryUid  INTEGER  NOT NULL , cjiContentEntryVersion  INTEGER  NOT NULL , cjiItemProgress  INTEGER  NOT NULL , cjiItemTotal  INTEGER  NOT NULL , cjiRecursiveProgress  INTEGER  NOT NULL , cjiRecursiveTotal  INTEGER  NOT NULL , cjiStatus  INTEGER  NOT NULL , cjiRecursiveStatus  INTEGER  NOT NULL , cjiConnectivityNeeded  INTEGER  NOT NULL , cjiPluginId  INTEGER  NOT NULL , cjiAttemptCount  INTEGER  NOT NULL , cjiParentCjiUid  INTEGER  NOT NULL , cjiServerJobId  INTEGER  NOT NULL , cjiStartTime  INTEGER  NOT NULL , cjiFinishTime  INTEGER  NOT NULL , cjiUploadSessionUid  TEXT , cjiContentDeletedOnCancellation  INTEGER  NOT NULL , cjiContainerProcessed  INTEGER  NOT NULL , cjiLastModified  INTEGER  NOT NULL , cjiUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
                add("CREATE INDEX index_ContentJobItem_cjiContentEntryUid_cjiFinishTime ON ContentJobItem (cjiContentEntryUid, cjiFinishTime)")
                add("CREATE TABLE IF NOT EXISTS ContentEntryVersion (  cevContentEntryUid  INTEGER  NOT NULL , cevUrl  TEXT , cevContentType  TEXT , cevSitemapUrl  TEXT , cevSize  INTEGER  NOT NULL , cevInActive  INTEGER  NOT NULL , cevLastModified  INTEGER  NOT NULL , cevLct  INTEGER  NOT NULL , cevUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
            }
            DoorDbType.POSTGRES -> {
                add("CREATE TABLE IF NOT EXISTS ContentJobItem (  cjiJobUid  BIGINT  NOT NULL , sourceUri  TEXT , cjiOriginalFilename  TEXT , cjiIsLeaf  BOOL  NOT NULL , cjiContentEntryUid  BIGINT  NOT NULL , cjiParentContentEntryUid  BIGINT  NOT NULL , cjiContentEntryVersion  BIGINT  NOT NULL , cjiItemProgress  BIGINT  NOT NULL , cjiItemTotal  BIGINT  NOT NULL , cjiRecursiveProgress  BIGINT  NOT NULL , cjiRecursiveTotal  BIGINT  NOT NULL , cjiStatus  INTEGER  NOT NULL , cjiRecursiveStatus  INTEGER  NOT NULL , cjiConnectivityNeeded  BOOL  NOT NULL , cjiPluginId  INTEGER  NOT NULL , cjiAttemptCount  INTEGER  NOT NULL , cjiParentCjiUid  BIGINT  NOT NULL , cjiServerJobId  BIGINT  NOT NULL , cjiStartTime  BIGINT  NOT NULL , cjiFinishTime  BIGINT  NOT NULL , cjiUploadSessionUid  TEXT , cjiContentDeletedOnCancellation  BOOL  NOT NULL , cjiContainerProcessed  BOOL  NOT NULL , cjiLastModified  BIGINT  NOT NULL , cjiUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
                add("CREATE INDEX index_ContentJobItem_cjiContentEntryUid_cjiFinishTime ON ContentJobItem (cjiContentEntryUid, cjiFinishTime)")
                add("CREATE TABLE IF NOT EXISTS ContentEntryVersion (  cevContentEntryUid  BIGINT  NOT NULL , cevUrl  TEXT , cevContentType  TEXT , cevSitemapUrl  TEXT , cevSize  BIGINT  NOT NULL , cevInActive  BOOL  NOT NULL , cevLastModified  BIGINT  NOT NULL , cevLct  BIGINT  NOT NULL , cevUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
            }
        }
    }
}

/**
 * This migration is only here to force the regeneration of triggers. The trigger condition for Site
 * was changed to add TRIGGER_CONDITION_WHERE_NEWER
 */
val MIGRATION_125_126 = DoorMigrationStatementList(125, 126) { db ->
    emptyList()
}

/**
 * Add TransferJob and TransferJobItem
 */
val MIGRATION_126_127 = DoorMigrationStatementList(126, 127) { db ->
    buildList {
        if(db.dbType() == DoorDbType.SQLITE) {
            add("CREATE TABLE IF NOT EXISTS TransferJob (  tjType  INTEGER  NOT NULL , tjStatus  INTEGER  NOT NULL , tjName  TEXT , tjUuid  TEXT , tjUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
            add("CREATE TABLE IF NOT EXISTS TransferJobItem (  tjiTjUid  INTEGER  NOT NULL , tjTotalSize  INTEGER  NOT NULL , tjTransferred  INTEGER  NOT NULL , tjAttemptCount  INTEGER  NOT NULL , tjiSrc  TEXT , tjiDest  TEXT , tjiType  INTEGER  NOT NULL , tjiStatus  INTEGER  NOT NULL , tjiTableId  INTEGER  NOT NULL , tjiEntityUid  INTEGER  NOT NULL , tjiUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
        }else {
            add("CREATE TABLE IF NOT EXISTS TransferJob (  tjType  INTEGER  NOT NULL , tjStatus  INTEGER  NOT NULL , tjName  TEXT , tjUuid  TEXT , tjUid  SERIAL  PRIMARY KEY  NOT NULL )")
            add("CREATE TABLE IF NOT EXISTS TransferJobItem (  tjiTjUid  INTEGER  NOT NULL , tjTotalSize  BIGINT  NOT NULL , tjTransferred  BIGINT  NOT NULL , tjAttemptCount  INTEGER  NOT NULL , tjiSrc  TEXT , tjiDest  TEXT , tjiType  INTEGER  NOT NULL , tjiStatus  INTEGER  NOT NULL , tjiTableId  INTEGER  NOT NULL , tjiEntityUid  BIGINT  NOT NULL , tjiUid  SERIAL  PRIMARY KEY  NOT NULL )")
        }
    }
}

/**
 * Modify PersonPicture table - does not migrate previous (largely unused) data.
 * Add tjiEntityEtag column to TransferJobItem so a transferjobitem can be related to a specific
 * version of the entity
 */
val MIGRATION_127_128 = DoorMigrationStatementList(127, 128) { db ->
    buildList {
        if(db.dbType() == DoorDbType.SQLITE) {
            add("DROP TABLE IF EXISTS PersonPicture")
            add("CREATE TABLE IF NOT EXISTS PersonPicture (  personPictureLct  INTEGER  NOT NULL , personPictureUri  TEXT , personPictureThumbnailUri  TEXT , fileSize  INTEGER  NOT NULL , personPictureActive  INTEGER  NOT NULL , personPictureUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
            add("ALTER TABLE TransferJobItem ADD COLUMN tjiEntityEtag  INTEGER  NOT NULL  DEFAULT 0")
        }else {
            add("DROP TABLE IF EXISTS PersonPicture")
            add("CREATE TABLE IF NOT EXISTS PersonPicture (  personPictureLct  BIGINT  NOT NULL , personPictureUri  TEXT , personPictureThumbnailUri  TEXT , fileSize  INTEGER  NOT NULL , personPictureActive  BOOL  NOT NULL , personPictureUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
            add("ALTER TABLE TransferJobItem ADD COLUMN tjiEntityEtag  BIGINT  NOT NULL  DEFAULT 0")
        }
    }
}

val MIGRATION_128_129 = DoorMigrationStatementList(128, 129) { db ->
    buildList {
        add("DROP TABLE IF EXISTS CoursePicture")
        if(db.dbType() == DoorDbType.SQLITE) {
            add("CREATE TABLE IF NOT EXISTS CoursePicture (  coursePictureLct  INTEGER  NOT NULL , coursePictureUri  TEXT , coursePictureThumbnailUri  TEXT , coursePictureActive  INTEGER  NOT NULL , coursePictureUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
        }else {
            add("CREATE TABLE IF NOT EXISTS CoursePicture (  coursePictureLct  BIGINT  NOT NULL , coursePictureUri  TEXT , coursePictureThumbnailUri  TEXT , coursePictureActive  BOOL  NOT NULL , coursePictureUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
        }
    }
}

val MIGRATION_129_130 = DoorMigrationStatementList(129, 130) { db ->
    buildList {
        add("ALTER TABLE TransferJobItem ADD COLUMN tjiLockIdToRelease INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_130_131 = DoorMigrationStatementList(130, 131) { db ->
    buildList {
        if(db.dbType() == DoorDbType.SQLITE) {
            add("CREATE TABLE IF NOT EXISTS CacheLockJoin (  cljTableId  INTEGER  NOT NULL , cljEntityUid  INTEGER  NOT NULL , cljUrl  TEXT  NOT NULL , cljLockId  INTEGER  NOT NULL , cljStatus  INTEGER  NOT NULL , cljType  INTEGER  NOT NULL , cljId  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
            add("CREATE INDEX idx_clj_table_entity_url ON CacheLockJoin (cljTableId, cljEntityUid, cljUrl)")
        }else {
            add("CREATE TABLE IF NOT EXISTS CacheLockJoin (  cljTableId  INTEGER  NOT NULL , cljEntityUid  BIGINT  NOT NULL , cljUrl  TEXT  NOT NULL , cljLockId  INTEGER  NOT NULL , cljStatus  INTEGER  NOT NULL , cljType  INTEGER  NOT NULL , cljId  SERIAL  PRIMARY KEY  NOT NULL )")
            add("CREATE INDEX idx_clj_table_entity_url ON CacheLockJoin (cljTableId, cljEntityUid, cljUrl)")
        }
    }
}

// 131 to 132 is a migration that applies only to the server side to add uri retention triggers

/*
 * Added 07/Jan/24 - drop the old ContentJobItem table
 * Create ContentEntryImportJob
 */
val MIGRATION_132_133 = DoorMigrationStatementList(132, 133) { db ->
    buildList {
        add("DROP TABLE ContentJobItem")
        if(db.dbType() == DoorDbType.SQLITE) {
            add("CREATE TABLE IF NOT EXISTS ContentEntryImportJob (  sourceUri  TEXT , cjiOriginalFilename  TEXT , cjiContentEntryUid  INTEGER  NOT NULL , cjiParentContentEntryUid  INTEGER  NOT NULL , cjiContentEntryVersion  INTEGER  NOT NULL , cjiItemProgress  INTEGER  NOT NULL , cjiItemTotal  INTEGER  NOT NULL , cjiStatus  INTEGER  NOT NULL , cjiRecursiveStatus  INTEGER  NOT NULL , cjiPluginId  INTEGER  NOT NULL , cjiParentCjiUid  INTEGER  NOT NULL , cjiStartTime  INTEGER  NOT NULL , cjiFinishTime  INTEGER  NOT NULL , cjiContentDeletedOnCancellation  INTEGER  NOT NULL , cjiUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
            add("CREATE INDEX index_ContentEntryImportJob_cjiContentEntryUid_cjiFinishTime ON ContentEntryImportJob (cjiContentEntryUid, cjiFinishTime)")
        }else {
            add("CREATE TABLE IF NOT EXISTS ContentEntryImportJob (  sourceUri  TEXT , cjiOriginalFilename  TEXT , cjiContentEntryUid  BIGINT  NOT NULL , cjiParentContentEntryUid  BIGINT  NOT NULL , cjiContentEntryVersion  BIGINT  NOT NULL , cjiItemProgress  BIGINT  NOT NULL , cjiItemTotal  BIGINT  NOT NULL , cjiStatus  INTEGER  NOT NULL , cjiRecursiveStatus  INTEGER  NOT NULL , cjiPluginId  INTEGER  NOT NULL , cjiParentCjiUid  BIGINT  NOT NULL , cjiStartTime  BIGINT  NOT NULL , cjiFinishTime  BIGINT  NOT NULL , cjiContentDeletedOnCancellation  BOOL  NOT NULL , cjiUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
            add("CREATE INDEX index_ContentEntryImportJob_cjiContentEntryUid_cjiFinishTime ON ContentEntryImportJob (cjiContentEntryUid, cjiFinishTime)")
        }
    }
}

val MIGRATION_133_134 = DoorMigrationStatementList(133, 134) { db ->
    buildList {
        add("CREATE INDEX tji_table_entity_etag ON TransferJobItem (tjiTableId, tjiEntityUid, tjiEntityEtag)")
    }
}

val MIGRATION_134_135 = DoorMigrationStatementList(134, 135) { db ->
    buildList {
        if(db.dbType() == DoorDbType.SQLITE) {
            add("CREATE TABLE IF NOT EXISTS OfflineItem (  oiNodeId  INTEGER  NOT NULL , oiClazzUid  INTEGER  NOT NULL , oiCourseBlockUid  INTEGER  NOT NULL , oiContentEntryUid  INTEGER  NOT NULL , oiActive  INTEGER  NOT NULL , oiLct  INTEGER  NOT NULL , oiUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
            add("CREATE INDEX offline_item_node_content_entry ON OfflineItem (oiNodeId, oiContentEntryUid)")
        }else {
            add("CREATE TABLE IF NOT EXISTS OfflineItem (  oiNodeId  BIGINT  NOT NULL , oiClazzUid  BIGINT  NOT NULL , oiCourseBlockUid  BIGINT  NOT NULL , oiContentEntryUid  BIGINT  NOT NULL , oiActive  BOOL  NOT NULL , oiLct  BIGINT  NOT NULL , oiUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
            add("CREATE INDEX offline_item_node_content_entry ON OfflineItem (oiNodeId, oiContentEntryUid)")
        }
    }
}

val MIGRATION_135_136 = DoorMigrationStatementList(135, 136) { db ->
    buildList {
        add("ALTER TABLE TransferJob ADD COLUMN tjTableId INTEGER NOT NULL DEFAULT 0")
        add("ALTER TABLE TransferJob ADD COLUMN tjCreationType INTEGER NOT NULL DEFAULT 0")
        if(db.dbType() == DoorDbType.SQLITE) {
            add("ALTER TABLE TransferJob ADD COLUMN tjEntityUid INTEGER NOT NULL DEFAULT 0")
            add("ALTER TABLE TransferJob ADD COLUMN tjTimeCreated INTEGER NOT NULL DEFAULT 0")
        }else {
            add("ALTER TABLE TransferJob ADD COLUMN tjEntityUid BIGINT NOT NULL DEFAULT 0")
            add("ALTER TABLE TransferJob ADD COLUMN tjTimeCreated BIGINT NOT NULL DEFAULT 0")
        }
        add("CREATE INDEX TransferJob_idx_tjTableId_EntityUid ON TransferJob (tjTableId, tjEntityUid)")
    }
}

val MIGRATION_136_137 = DoorMigrationStatementList(136, 137) { db ->
    buildList {
        if(db.dbType() == DoorDbType.SQLITE) {
            add("CREATE TABLE IF NOT EXISTS OfflineItemPendingTransferJob (  oiptjOiUid  INTEGER  NOT NULL , oiptjTableId  INTEGER  NOT NULL , oiptjEntityUid  INTEGER  NOT NULL , oiptjUrl  TEXT , oiptjType  INTEGER  NOT NULL , oiptjId  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
        }else {
            add("CREATE TABLE IF NOT EXISTS OfflineItemPendingTransferJob (  oiptjOiUid  BIGINT  NOT NULL , oiptjTableId  INTEGER  NOT NULL , oiptjEntityUid  BIGINT  NOT NULL , oiptjUrl  TEXT , oiptjType  INTEGER  NOT NULL , oiptjId  SERIAL  PRIMARY KEY  NOT NULL )")
        }
    }
}

val MIGRATION_137_138 = DoorMigrationStatementList(137, 138) { db ->
    buildList {
        if(db.dbType() == DoorDbType.SQLITE) {
            add("ALTER TABLE ContentEntryVersion RENAME to ContentEntryVersion_OLD")
            add("CREATE TABLE IF NOT EXISTS ContentEntryVersion (  cevContentEntryUid  INTEGER  NOT NULL , cevUrl  TEXT , cevContentType  TEXT , cevManifestUrl  TEXT , cevSize  INTEGER  NOT NULL , cevInActive  INTEGER  NOT NULL , cevLastModified  INTEGER  NOT NULL , cevLct  INTEGER  NOT NULL , cevUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
            add("INSERT INTO ContentEntryVersion (cevContentEntryUid, cevUrl, cevContentType, cevManifestUrl, cevSize, cevInActive, cevLastModified, cevLct, cevUid) SELECT cevContentEntryUid, cevUrl, cevContentType, cevSitemapUrl, cevSize, cevInActive, cevLastModified, cevLct, cevUid FROM ContentEntryVersion_OLD")
            add("DROP TABLE ContentEntryVersion_OLD")
        }else {
            add("ALTER TABLE ContentEntryVersion RENAME COLUMN cevSitemapUrl TO cevManifestUrl")
        }
    }
}

val MIGRATION_138_139 = DoorMigrationStatementList(138, 139) { db ->
    buildList {
        if (db.dbType() == DoorDbType.SQLITE) {
            add("ALTER TABLE ContentEntryVersion RENAME to ContentEntryVersion_OLD")
            add("CREATE TABLE IF NOT EXISTS ContentEntryVersion (  cevContentEntryUid  INTEGER  NOT NULL , cevOpenUri  TEXT , cevContentType  TEXT , cevManifestUrl  TEXT , cevSize  INTEGER  NOT NULL , cevInActive  INTEGER  NOT NULL , cevLastModified  INTEGER  NOT NULL , cevLct  INTEGER  NOT NULL , cevUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
            add("INSERT INTO ContentEntryVersion (cevContentEntryUid, cevOpenUri, cevContentType, cevManifestUrl, cevSize, cevInActive, cevLastModified, cevLct, cevUid) SELECT cevContentEntryUid, cevUrl, cevContentType, cevManifestUrl, cevSize, cevInActive, cevLastModified, cevLct, cevUid FROM ContentEntryVersion_OLD")
            add("DROP TABLE ContentEntryVersion_OLD")
        }else {
            add("ALTER TABLE ContentEntryVersion RENAME COLUMN cevUrl to cevOpenUri")
        }
    }
}

val MIGRATION_139_140 = DoorMigrationStatementList(139, 140) { db ->
    buildList {
        add("CREATE INDEX transferjob_tjuid ON TransferJobItem (tjiTjUid)")
    }
}

val MIGRATION_140_141 = DoorMigrationStatementList(140, 141) {db ->
    buildList {
        if(db.dbType() == DoorDbType.POSTGRES) {
            add("ALTER TABLE UserSession ALTER COLUMN usLcb TYPE BIGINT")
        }
    }
}

val MIGRATION_141_142 = DoorMigrationStatementList(141, 142) { db ->
    listOf("DROP TABLE IF EXISTS ClazzAssignmentContentJoin")
}

val MIGRATION_142_143 = DoorMigrationStatementList(142, 143) { db ->
    buildList {
        if(db.dbType() == DoorDbType.SQLITE) {
            add("ALTER TABLE ContentEntryParentChildJoin ADD COLUMN cepcjDeleted INTEGER NOT NULL DEFAULT 0")
            add("CREATE TABLE IF NOT EXISTS DeletedItem (  delItemName  TEXT , delItemIconUri  TEXT , delItemLastModTime  INTEGER  NOT NULL , delItemTimeDeleted  INTEGER  NOT NULL , delItemEntityTable  INTEGER  NOT NULL , delItemEntityUid  INTEGER  NOT NULL , delItemDeletedByPersonUid  INTEGER  NOT NULL , delItemStatus  INTEGER  NOT NULL , delItemIsFolder  INTEGER  NOT NULL  DEFAULT 0 , delItemUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
            add("CREATE INDEX delitem_idx_status_time ON DeletedItem (delItemStatus, delItemTimeDeleted)")
        }else {
            add("ALTER TABLE ContentEntryParentChildJoin ADD COLUMN cepcjDeleted BOOL NOT NULL DEFAULT false")
            add("CREATE TABLE IF NOT EXISTS DeletedItem (  delItemName  TEXT , delItemIconUri  TEXT , delItemLastModTime  BIGINT  NOT NULL , delItemTimeDeleted  BIGINT  NOT NULL , delItemEntityTable  INTEGER  NOT NULL , delItemEntityUid  BIGINT  NOT NULL , delItemDeletedByPersonUid  BIGINT  NOT NULL , delItemStatus  INTEGER  NOT NULL , delItemIsFolder  BOOL  NOT NULL  DEFAULT false, delItemUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
            add("CREATE INDEX delitem_idx_status_time ON DeletedItem (delItemStatus, delItemTimeDeleted)")
        }
    }
}

/*
 * Update message table structure and create triggers that will
 */
val MIGRATION_143_144 = DoorMigrationStatementList(143, 144) { db ->
    buildList {
        add("DROP TABLE IF EXISTS Message")
        if(db.dbType() == DoorDbType.SQLITE) {
            add("CREATE TABLE IF NOT EXISTS Message (  messageSenderPersonUid  INTEGER  NOT NULL , messageToPersonUid  INTEGER  NOT NULL , messageText  TEXT , messageTimestamp  INTEGER  NOT NULL , messageLct  INTEGER  NOT NULL , messageUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
        }else {
            add("CREATE TABLE IF NOT EXISTS Message (  messageSenderPersonUid  BIGINT  NOT NULL , messageToPersonUid  BIGINT  NOT NULL , messageText  TEXT , messageTimestamp  BIGINT  NOT NULL , messageLct  BIGINT  NOT NULL , messageUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
        }
    }
}

//This migration adds triggers which will put any received Message into the outgoing replication for
// recipients (and other devices of senders)
val MIGRATION_144_145_SERVER = DoorMigrationStatementList(144, 145){ db ->
    val insertOutgoingReplicationSql ="""
           INSERT INTO OutgoingReplication(destNodeId, orTableId, orPk1, orPk2)
                SELECT UserSession.usClientNodeId AS destNodeId,
                       ${Message.TABLE_ID} AS orTableId,
                       NEW.messageUid AS orPk1,
                       0 as orPk2
                 FROM UserSession
                WHERE (   UserSession.usPersonUid = NEW.messageSenderPersonUid 
                       OR UserSession.usPersonUid = NEW.messageToPersonUid)
                  AND UserSession.usStatus = ${UserSession.STATUS_ACTIVE}     
                  AND UserSession.usClientNodeId NOT IN 
                      (SELECT ReplicationOperation.repOpRemoteNodeId
                         FROM ReplicationOperation
                        WHERE ReplicationOperation.repOpTableId = ${Message.TABLE_ID});
        """


    buildList {
        if(db.dbType() == DoorDbType.SQLITE) {
            add("""
                    CREATE TRIGGER IF NOT EXISTS message_send_trigger
                    AFTER INSERT ON Message
                    FOR EACH ROW
                    BEGIN
                    $insertOutgoingReplicationSql
                    END    
                """)
        }else {
            add("""
                    CREATE OR REPLACE FUNCTION message_send_fn() RETURNS TRIGGER AS $$
                    BEGIN
                    $insertOutgoingReplicationSql
                    RETURN NEW;
                    END $$ LANGUAGE plpgsql
                """)
            add("""
                    CREATE TRIGGER message_send_trig AFTER INSERT 
                    ON Message
                    FOR EACH ROW EXECUTE PROCEDURE message_send_fn()
                """)
        }
    }
}

//144-145 migration - empty - does nothing
val MIGRATION_144_145_CLIENT = DoorMigrationStatementList(144, 145) { db ->
    emptyList()
}

val MIGRATION_145_146 = DoorMigrationStatementList(145, 146) { db ->
    listOf("CREATE INDEX message_idx_send_to_time ON Message (messageSenderPersonUid, messageToPersonUid, messageTimestamp)")
}

val MIGRATION_146_147 = DoorMigrationStatementList(146, 147) { db ->
    buildList {
        if(db.dbType() == DoorDbType.POSTGRES) {
            add("ALTER TABLE CacheLockJoin ALTER COLUMN cljLockId TYPE BIGINT")
        }
    }
}

val MIGRATION_147_148 = DoorMigrationStatementList(147, 148) { db ->
    buildList {
        if(db.dbType()  == DoorDbType.POSTGRES) {
            add("ALTER TABLE TransferJob ADD COLUMN tjOiUid BIGINT NOT NULL DEFAULT 0")
            add("ALTER TABLE CacheLockJoin ADD COLUMN cljOiUid BIGINT NOT NULL DEFAULT 0")
        }else {
            add("ALTER TABLE TransferJob ADD COLUMN tjOiUid INTEGER NOT NULL DEFAULT 0")
            add("ALTER TABLE CacheLockJoin ADD COLUMN cljOiUid INTEGER NOT NULL DEFAULT 0")
        }
        add("CREATE INDEX idx_clj_offline_item_uid ON CacheLockJoin (cljOiUid)")
    }
}

/**
 * For clients with OfflineItem support (Android and Desktop),  add a trigger that will remove the
 * CacheLockJoin when an OfflineItem is made inactive. See AddOfflineItemInactiveTriggersCallback.
 */
val MIGRATION_148_149_CLIENT_WITH_OFFLINE_ITEMS = DoorMigrationStatementList(148, 149) {
    listOf(
        """
        CREATE TRIGGER IF NOT EXISTS offline_item_inactive_trig 
                AFTER UPDATE ON OfflineItem
                FOR EACH ROW WHEN NEW.oiActive = 0 AND OLD.oiActive = 1
                BEGIN 
                UPDATE CacheLockJoin
                   SET cljStatus = ${CacheLockJoin.STATUS_PENDING_DELETE}
                 WHERE cljOiUid = NEW.oiUid;  
                END
        """
    )
}

val MIGRATION_148_149_NO_OFFLINE_ITEMS = DoorMigrationStatementList(148, 149) {
    emptyList()
}

val MIGRATION_149_150 = DoorMigrationStatementList(149, 150) { db ->
    buildList {
        val fieldType = if(db.dbType() == DoorDbType.SQLITE) "INTEGER" else "BIGINT"

        add("ALTER TABLE ContentEntryVersion ADD COLUMN cevStorageSize $fieldType NOT NULL DEFAULT 0")
        add("ALTER TABLE ContentEntryVersion ADD COLUMN cevOriginalSize $fieldType NOT NULL DEFAULT 0")
    }
}

val MIGRATION_150_151 = DoorMigrationStatementList(150, 151) { db ->
    listOf(
        "ALTER TABLE TransferJobItem ADD COLUMN tjiPartialTmpFile TEXT"
    )
}

/**
 * Add triggers to be used on Postgres to retain all active URIs for PersonPicture and CoursePicture
 * by creating locks. See AddRetainAllActiveUriTriggersUseCase
 *
 * Note: this was already added on SQLite,
 */
val MIGRATION_151_152 = DoorMigrationStatementList(151, 152) { db ->
    buildList {
        if(db.dbType() == DoorDbType.POSTGRES) {
            add("""
                            CREATE OR REPLACE FUNCTION retain_c_clj_50_personPictureUri() RETURNS TRIGGER AS $$
                            BEGIN
                            INSERT INTO CacheLockJoin(cljTableId, cljEntityUid, cljUrl, cljLockId, cljStatus, cljType)
                            VALUES(50, NEW.personPictureUid, NEW.personPictureUri, 0, 1, 1);
                            RETURN NEW;
                            END $$ LANGUAGE plpgsql
                        """)
            add("""
                            CREATE OR REPLACE FUNCTION retain_d_clj_50_personPictureUri() RETURNS TRIGGER AS $$
                            BEGIN
                            UPDATE CacheLockJoin 
                               SET cljStatus = 3
                             WHERE cljTableId = 50
                               AND cljEntityUid = OLD.personPictureUid
                               AND cljUrl = OLD.personPictureUri;
                            RETURN OLD;
                            END $$ LANGUAGE plpgsql   
                        """)
            add("""
                            CREATE TRIGGER retain_c_clj_50_personPictureUri_ins_t
                            AFTER INSERT ON PersonPicture
                            FOR EACH ROW
                            WHEN (NEW.personPictureUri IS NOT NULL)
                            EXECUTE FUNCTION retain_c_clj_50_personPictureUri();
                        """)
            add("""
                            CREATE TRIGGER retain_c_clj_50_personPictureUri_upd_t
                            AFTER UPDATE ON PersonPicture
                            FOR EACH ROW
                            WHEN (NEW.personPictureUri IS DISTINCT FROM OLD.personPictureUri AND OLD.personPictureUri IS NOT NULL)
                            EXECUTE FUNCTION retain_c_clj_50_personPictureUri();
                        """)
            add("""
                            CREATE TRIGGER retain_d_clj_50_personPictureUri_upd_t
                            AFTER UPDATE ON PersonPicture
                            FOR EACH ROW
                            WHEN (NEW.personPictureUri IS DISTINCT FROM OLD.personPictureUri AND NEW.personPictureUri IS NOT NULL)
                            EXECUTE FUNCTION retain_d_clj_50_personPictureUri();
                        """)
            add("""
                            CREATE OR REPLACE FUNCTION retain_c_clj_50_personPictureThumbnailUr() RETURNS TRIGGER AS $$
                            BEGIN
                            INSERT INTO CacheLockJoin(cljTableId, cljEntityUid, cljUrl, cljLockId, cljStatus, cljType)
                            VALUES(50, NEW.personPictureUid, NEW.personPictureThumbnailUri, 0, 1, 1);
                            RETURN NEW;
                            END $$ LANGUAGE plpgsql
                        """)
            add("""
                            CREATE OR REPLACE FUNCTION retain_d_clj_50_personPictureThumbnailUr() RETURNS TRIGGER AS $$
                            BEGIN
                            UPDATE CacheLockJoin 
                               SET cljStatus = 3
                             WHERE cljTableId = 50
                               AND cljEntityUid = OLD.personPictureUid
                               AND cljUrl = OLD.personPictureThumbnailUri;
                            RETURN OLD;
                            END $$ LANGUAGE plpgsql   
                        """)
            add("""
                            CREATE TRIGGER retain_c_clj_50_personPictureThumbnailUr_ins_t
                            AFTER INSERT ON PersonPicture
                            FOR EACH ROW
                            WHEN (NEW.personPictureThumbnailUri IS NOT NULL)
                            EXECUTE FUNCTION retain_c_clj_50_personPictureThumbnailUr();
                        """)
            add("""
                            CREATE TRIGGER retain_c_clj_50_personPictureThumbnailUr_upd_t
                            AFTER UPDATE ON PersonPicture
                            FOR EACH ROW
                            WHEN (NEW.personPictureThumbnailUri IS DISTINCT FROM OLD.personPictureThumbnailUri AND OLD.personPictureThumbnailUri IS NOT NULL)
                            EXECUTE FUNCTION retain_c_clj_50_personPictureThumbnailUr();
                        """)
            add("""
                            CREATE TRIGGER retain_d_clj_50_personPictureThumbnailUr_upd_t
                            AFTER UPDATE ON PersonPicture
                            FOR EACH ROW
                            WHEN (NEW.personPictureThumbnailUri IS DISTINCT FROM OLD.personPictureThumbnailUri AND NEW.personPictureThumbnailUri IS NOT NULL)
                            EXECUTE FUNCTION retain_d_clj_50_personPictureThumbnailUr();
                        """)
                    add("""
                            CREATE OR REPLACE FUNCTION retain_c_clj_125_coursePictureUri() RETURNS TRIGGER AS $$
                            BEGIN
                            INSERT INTO CacheLockJoin(cljTableId, cljEntityUid, cljUrl, cljLockId, cljStatus, cljType)
                            VALUES(125, NEW.coursePictureUid, NEW.coursePictureUri, 0, 1, 1);
                            RETURN NEW;
                            END $$ LANGUAGE plpgsql
                        """)
                    add("""
                            CREATE OR REPLACE FUNCTION retain_d_clj_125_coursePictureUri() RETURNS TRIGGER AS $$
                            BEGIN
                            UPDATE CacheLockJoin 
                               SET cljStatus = 3
                             WHERE cljTableId = 125
                               AND cljEntityUid = OLD.coursePictureUid
                               AND cljUrl = OLD.coursePictureUri;
                            RETURN OLD;
                            END $$ LANGUAGE plpgsql   
                        """)
                    add("""
                            CREATE TRIGGER retain_c_clj_125_coursePictureUri_ins_t
                            AFTER INSERT ON CoursePicture
                            FOR EACH ROW
                            WHEN (NEW.coursePictureUri IS NOT NULL)
                            EXECUTE FUNCTION retain_c_clj_125_coursePictureUri();
                        """)
                    add("""
                            CREATE TRIGGER retain_c_clj_125_coursePictureUri_upd_t
                            AFTER UPDATE ON CoursePicture
                            FOR EACH ROW
                            WHEN (NEW.coursePictureUri IS DISTINCT FROM OLD.coursePictureUri AND OLD.coursePictureUri IS NOT NULL)
                            EXECUTE FUNCTION retain_c_clj_125_coursePictureUri();
                        """)
                    add("""
                            CREATE TRIGGER retain_d_clj_125_coursePictureUri_upd_t
                            AFTER UPDATE ON CoursePicture
                            FOR EACH ROW
                            WHEN (NEW.coursePictureUri IS DISTINCT FROM OLD.coursePictureUri AND NEW.coursePictureUri IS NOT NULL)
                            EXECUTE FUNCTION retain_d_clj_125_coursePictureUri();
                        """)
                    add("""
                            CREATE OR REPLACE FUNCTION retain_c_clj_125_coursePictureThumbnailUr() RETURNS TRIGGER AS $$
                            BEGIN
                            INSERT INTO CacheLockJoin(cljTableId, cljEntityUid, cljUrl, cljLockId, cljStatus, cljType)
                            VALUES(125, NEW.coursePictureUid, NEW.coursePictureThumbnailUri, 0, 1, 1);
                            RETURN NEW;
                            END $$ LANGUAGE plpgsql
                        """)
                    add("""
                            CREATE OR REPLACE FUNCTION retain_d_clj_125_coursePictureThumbnailUr() RETURNS TRIGGER AS $$
                            BEGIN
                            UPDATE CacheLockJoin 
                               SET cljStatus = 3
                             WHERE cljTableId = 125
                               AND cljEntityUid = OLD.coursePictureUid
                               AND cljUrl = OLD.coursePictureThumbnailUri;
                            RETURN OLD;
                            END $$ LANGUAGE plpgsql   
                        """)
                    add("""
                            CREATE TRIGGER retain_c_clj_125_coursePictureThumbnailUr_ins_t
                            AFTER INSERT ON CoursePicture
                            FOR EACH ROW
                            WHEN (NEW.coursePictureThumbnailUri IS NOT NULL)
                            EXECUTE FUNCTION retain_c_clj_125_coursePictureThumbnailUr();
                        """)
                    add("""
                            CREATE TRIGGER retain_c_clj_125_coursePictureThumbnailUr_upd_t
                            AFTER UPDATE ON CoursePicture
                            FOR EACH ROW
                            WHEN (NEW.coursePictureThumbnailUri IS DISTINCT FROM OLD.coursePictureThumbnailUri AND OLD.coursePictureThumbnailUri IS NOT NULL)
                            EXECUTE FUNCTION retain_c_clj_125_coursePictureThumbnailUr();
                        """)
                    add("""
                            CREATE TRIGGER retain_d_clj_125_coursePictureThumbnailUr_upd_t
                            AFTER UPDATE ON CoursePicture
                            FOR EACH ROW
                            WHEN (NEW.coursePictureThumbnailUri IS DISTINCT FROM OLD.coursePictureThumbnailUri AND NEW.coursePictureThumbnailUri IS NOT NULL)
                            EXECUTE FUNCTION retain_d_clj_125_coursePictureThumbnailUr();
                        """)
        }
    }
}

val MIGRATION_152_153 = DoorMigrationStatementList(152, 153) { db ->
    buildList {
        if(db.dbType() == DoorDbType.SQLITE) {
            add("CREATE TABLE IF NOT EXISTS EnrolmentRequest (  erClazzUid  INTEGER  NOT NULL , erClazzName  TEXT , erPersonUid  INTEGER  NOT NULL , erPersonFullname  TEXT , erPersonPictureUri  TEXT , erPersonUsername  TEXT , erRole  INTEGER  NOT NULL , erRequestTime  INTEGER  NOT NULL , erStatus  INTEGER  NOT NULL , erStatusSetByPersonUid  INTEGER  NOT NULL , erDeleted  INTEGER  NOT NULL , erStatusSetAuth  TEXT , erLastModified  INTEGER  NOT NULL , erUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
        }else {
            add("CREATE TABLE IF NOT EXISTS EnrolmentRequest (  erClazzUid  BIGINT  NOT NULL , erClazzName  TEXT , erPersonUid  BIGINT  NOT NULL , erPersonFullname  TEXT , erPersonPictureUri  TEXT , erPersonUsername  TEXT , erRole  INTEGER  NOT NULL , erRequestTime  BIGINT  NOT NULL , erStatus  INTEGER  NOT NULL , erStatusSetByPersonUid  BIGINT  NOT NULL , erDeleted  BOOL  NOT NULL , erStatusSetAuth  TEXT , erLastModified  BIGINT  NOT NULL , erUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
        }
        add("CREATE INDEX idx_enrolmentrequest_by_clazz ON EnrolmentRequest (erClazzUid, erStatus)")
        add("CREATE INDEX idx_enrolmentrequest_by_person ON EnrolmentRequest (erPersonUid, erStatus)")
    }
}

val MIGRATION_153_154 = DoorMigrationStatementList(153, 154) { db ->
    buildList {
        if(db.dbType() == DoorDbType.SQLITE) {
            add("CREATE TABLE IF NOT EXISTS CoursePermission (  cpLastModified  INTEGER  NOT NULL , cpClazzUid  INTEGER  NOT NULL , cpToEnrolmentRole  INTEGER  NOT NULL , cpToPersonUid  INTEGER  NOT NULL , cpToGroupUid  INTEGER  NOT NULL , cpPermissionsFlag  INTEGER  NOT NULL , cpIsDeleted  INTEGER  NOT NULL , cpUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
            add("CREATE TABLE IF NOT EXISTS SystemPermission (  spToPersonUid  INTEGER  NOT NULL , spToGroupUid  INTEGER  NOT NULL , spPermissionsFlag  INTEGER  NOT NULL , spLastModified  INTEGER  NOT NULL , spIsDeleted  INTEGER  NOT NULL , spUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
            add("ALTER TABLE Clazz ADD COLUMN clazzOwnerPersonUid INTEGER NOT NULL DEFAULT 0")
        }else {
            add("CREATE TABLE IF NOT EXISTS CoursePermission (  cpLastModified  BIGINT  NOT NULL , cpClazzUid  BIGINT  NOT NULL , cpToEnrolmentRole  INTEGER  NOT NULL , cpToPersonUid  BIGINT  NOT NULL , cpToGroupUid  BIGINT  NOT NULL , cpPermissionsFlag  BIGINT  NOT NULL , cpIsDeleted  BOOL  NOT NULL , cpUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
            add("CREATE TABLE IF NOT EXISTS SystemPermission (  spToPersonUid  BIGINT  NOT NULL , spToGroupUid  BIGINT  NOT NULL , spPermissionsFlag  BIGINT  NOT NULL , spLastModified  BIGINT  NOT NULL , spIsDeleted  BOOL  NOT NULL , spUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
            add("ALTER TABLE Clazz ADD COLUMN clazzOwnerPersonUid BIGINT NOT NULL DEFAULT 0")
        }
        add("CREATE INDEX idx_coursepermission_clazzuid ON CoursePermission (cpClazzUid)")
        add("CREATE INDEX idx_systempermission_personuid ON SystemPermission (spToPersonUid)")
    }
}

val MIGRATION_154_155 = DoorMigrationStatementList(154, 155) { db ->
    buildList {
        if(db.dbType() == DoorDbType.SQLITE) {
            add("ALTER TABLE CourseAssignmentSubmission ADD COLUMN casClazzUid INTEGER NOT NULL DEFAULT 0")
            add("ALTER TABLE CourseAssignmentMark ADD COLUMN camClazzUid INTEGER NOT NULL DEFAULT 0")
        }else {
            add("ALTER TABLE CourseAssignmentSubmission ADD COLUMN casClazzUid BIGINT NOT NULL DEFAULT 0")
            add("ALTER TABLE CourseAssignmentMark ADD COLUMN camClazzUid BIGINT NOT NULL DEFAULT 0")
        }
    }
}

/**
 * Migrate permissions. New entities should only be created on the server side.
 * No entities will be created on the client migration.
 */
val MIGRATION_155_156_SERVER = DoorMigrationStatementList(155, 156) { db ->
    buildList {
        //Add SystemPermission for existing entities
        val falseVal = if(db.dbType() == DoorDbType.SQLITE) "0" else "false"
        add("""
            INSERT INTO SystemPermission(spToPersonUid, spToGroupUid, spPermissionsFlag, spLastModified, spIsDeleted)
            SELECT Person.personUid AS spToPersonUid,
                   0 AS spToGroupUid,
                   CASE 
                   WHEN Person.username = 'admin' THEN ${Long.MAX_VALUE}
                   ELSE 0
                   END AS spPermissionsFlag,
                   ${systemTimeInMillis()} AS spLastModified,
                   $falseVal AS spIsDeleted
              FROM Person
        """)

        //Add CoursePermission for all courses for teachers
        add("""
            INSERT INTO CoursePermission(cpLastModified, cpClazzUid, cpToEnrolmentRole, cpToPersonUid, cpToGroupUid, cpPermissionsFlag, cpIsDeleted)
            SELECT ${systemTimeInMillis()} AS cpLastModified,
                   Clazz.clazzUid AS cpClazzUid,
                   ${ClazzEnrolment.ROLE_TEACHER} AS cpToEnrolmentRole,
                   0 AS cpToPersonUid,
                   0 AS cpToGroupUid,
                   ${CoursePermission.TEACHER_DEFAULT_PERMISSIONS} AS cpPermissionsFlag,
                   $falseVal AS cpIsDeleted
              FROM Clazz     
        """)

        //Add CoursePermission for all courses for students
        add("""
            INSERT INTO CoursePermission(cpLastModified, cpClazzUid, cpToEnrolmentRole, cpToPersonUid, cpToGroupUid, cpPermissionsFlag, cpIsDeleted)
            SELECT ${systemTimeInMillis()} AS cpLastModified,
                   Clazz.clazzUid AS cpClazzUid,
                   ${ClazzEnrolment.ROLE_STUDENT} AS cpToEnrolmentRole,
                   0 AS cpToPersonUid,
                   0 AS cpToGroupUid,
                   ${CoursePermission.STUDENT_DEFAULT_PERMISSIONS} AS cpPermissionsFlag,
                   $falseVal AS cpIsDeleted
              FROM Clazz     
        """)

        //Set the current owner of all courses to the admin user
        add("""
           UPDATE Clazz
              SET clazzOwnerPersonUid = 
                  (SELECT Person.personUid
                     FROM Person
                    WHERE Person.username = 'admin'
                    LIMIT 1) 
        """)

        //Disable old permissions
        add("""
            UPDATE ScopedGrant
               SET sgPermissions = 0,
                   sgLct = ${systemTimeInMillis()}
        """)

    }
}

val MIGRATION_155_156_CLIENT = DoorMigrationStatementList(155, 156) { db ->
    emptyList()
}


val MIGRATION_156_157 = DoorMigrationStatementList(156, 157) { db ->
    buildList {
        add("DROP TABLE IF EXISTS CourseAssignmentSubmissionAttachment")
        if(db.dbType() == DoorDbType.SQLITE) {
            add("CREATE TABLE IF NOT EXISTS CourseAssignmentSubmissionFile (  casaSubmissionUid  INTEGER  NOT NULL , casaCaUid  INTEGER  NOT NULL , casaClazzUid  INTEGER  NOT NULL , casaMimeType  TEXT , casaFileName  TEXT , casaUri  TEXT , casaSize  INTEGER  NOT NULL , casaTimestamp  INTEGER  NOT NULL , casaUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
        }else {
            add("CREATE TABLE IF NOT EXISTS CourseAssignmentSubmissionFile (  casaSubmissionUid  BIGINT  NOT NULL , casaCaUid  BIGINT  NOT NULL , casaClazzUid  BIGINT  NOT NULL , casaMimeType  TEXT , casaFileName  TEXT , casaUri  TEXT , casaSize  INTEGER  NOT NULL , casaTimestamp  BIGINT  NOT NULL , casaUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
        }
    }
}

val MIGRATION_157_158 = DoorMigrationStatementList(157, 158) { db ->
    buildList {
        if(db.dbType() == DoorDbType.SQLITE) {
            add("ALTER TABLE CourseAssignmentSubmissionFile ADD COLUMN casaDeleted INTEGER NOT NULL DEFAULT 0")
        }else {
            add("ALTER TABLE CourseAssignmentSubmissionFile ADD COLUMN casaDeleted BOOL NOT NULL DEFAULT FALSE")
        }
    }
}

val MIGRATION_158_159 = DoorMigrationStatementList(158, 159) { db ->
    buildList {
        val colType = if(db.dbType() == DoorDbType.SQLITE) "INTEGER" else "BIGINT"
        add("ALTER TABLE CourseAssignmentSubmissionFile ADD COLUMN casaSubmitterUid $colType NOT NULL DEFAULT 0")
    }
}

val MIGRATION_159_160 = DoorMigrationStatementList(159, 160) { db ->
    buildList {
        add("DROP TABLE IF EXISTS Comments")
        if(db.dbType() == DoorDbType.SQLITE) {
            add("CREATE TABLE IF NOT EXISTS Comments (  commentsText  TEXT , commentsEntityUid  INTEGER  NOT NULL , commentsStatus  INTEGER  NOT NULL , commentsFromPersonUid  INTEGER  NOT NULL , commentsForSubmitterUid  INTEGER  NOT NULL , commentsFromSubmitterUid  INTEGER  NOT NULL , commentsFlagged  INTEGER  NOT NULL , commentsDeleted  INTEGER  NOT NULL , commentsDateTimeAdded  INTEGER  NOT NULL , commentsLct  INTEGER  NOT NULL , commentsUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
        }else {
            add("CREATE TABLE IF NOT EXISTS Comments (  commentsText  TEXT , commentsEntityUid  BIGINT  NOT NULL , commentsStatus  INTEGER  NOT NULL , commentsFromPersonUid  BIGINT  NOT NULL , commentsForSubmitterUid  BIGINT  NOT NULL , commentsFromSubmitterUid  BIGINT  NOT NULL , commentsFlagged  BOOL  NOT NULL , commentsDeleted  BOOL  NOT NULL , commentsDateTimeAdded  BIGINT  NOT NULL , commentsLct  BIGINT  NOT NULL , commentsUid  BIGSERIAL  PRIMARY KEY  NOT NULL )")
        }

        add("CREATE INDEX idx_comments_entity_submitter ON Comments (commentsEntityUid, commentsForSubmitterUid)")
    }
}

val MIGRATION_160_161 = DoorMigrationStatementList(160, 161) { db ->
    buildList {
        if(db.dbType() == DoorDbType.POSTGRES) {
            add("ALTER TABLE DiscussionPost DROP COLUMN discussionPostVisible")
            add("ALTER TABLE DiscussionPost DROP COLUMN discussionPostArchive")
            add("ALTER TABLE DiscussionPost ADD COLUMN dpDeleted BOOL NOT NULL DEFAULT FALSE")
            add("ALTER TABLE DiscussionPost ADD COLUMN discussionPostReplyToPostUid BIGINT NOT NULL DEFAULT 0")
            add("ALTER TABLE DiscussionPost ADD COLUMN discussionPostCourseBlockUid BIGINT NOT NULL DEFAULT 0")
        }else {
            add("ALTER TABLE DiscussionPost RENAME to DiscussionPost_OLD")
            add("CREATE TABLE IF NOT EXISTS DiscussionPost (  discussionPostReplyToPostUid  INTEGER  NOT NULL , discussionPostTitle  TEXT , discussionPostMessage  TEXT , discussionPostStartDate  INTEGER  NOT NULL , discussionPostCourseBlockUid  INTEGER  NOT NULL , dpDeleted  INTEGER  NOT NULL , discussionPostStartedPersonUid  INTEGER  NOT NULL , discussionPostClazzUid  INTEGER  NOT NULL , discussionPostLct  INTEGER  NOT NULL , discussionPostUid  INTEGER  PRIMARY KEY  AUTOINCREMENT  NOT NULL )")
            add("INSERT INTO DiscussionPost (discussionPostReplyToPostUid, discussionPostTitle, discussionPostMessage, discussionPostStartDate, discussionPostCourseBlockUid, dpDeleted, discussionPostStartedPersonUid, discussionPostClazzUid, discussionPostLct, discussionPostUid) SELECT discussionPostReplyToPostUid, discussionPostTitle, discussionPostMessage, discussionPostStartDate, discussionPostCourseBlockUid, 0 AS dpDeleted, discussionPostStartedPersonUid, discussionPostClazzUid, discussionPostLct, discussionPostUid FROM DiscussionPost_OLD")
            add("DROP TABLE DiscussionPost_OLD")
        }
    }
}

/**
 * Add retention creation triggers (see AddRetainAllActiveTriggerUseCase) for CourseAssignmentSubmission
 * entity on server.
 */
val MIGRATION_161_162_SERVER = DoorMigrationStatementList(161, 162) { db ->
    //Add creation of retention locks for assignment file submissions
    buildList {
        if(db.dbType() == DoorDbType.SQLITE) {
            add("""
                        CREATE TRIGGER IF NOT EXISTS Retain_CourseAssignmentSubmissionFile_Ins_casaUri
                        AFTER INSERT ON CourseAssignmentSubmissionFile
                        FOR EACH ROW WHEN NEW.casaUri IS NOT NULL
                        BEGIN
                        INSERT OR REPLACE INTO CacheLockJoin(cljTableId, cljEntityUid, cljUrl, cljLockId, cljStatus, cljType)
                        VALUES(90, NEW.casaUid, NEW.casaUri, 0, 1, 1);
                        END
                    """)

            add("""
                    CREATE TRIGGER IF NOT EXISTS Retain_CourseAssignmentSubmissionFile_Upd_casaUri_New
                    AFTER UPDATE ON CourseAssignmentSubmissionFile
                    FOR EACH ROW WHEN NEW.casaUri != OLD.casaUri AND NEW.casaUri IS NOT NULL
                    BEGIN
                        INSERT OR REPLACE INTO CacheLockJoin(cljTableId, cljEntityUid, cljUrl, cljLockId, cljStatus, cljType)
                        VALUES(90, NEW.casaUid, NEW.casaUri, 0, 1, 1);
                    END   
                """)

            add("""CREATE TRIGGER IF NOT EXISTS Retain_CourseAssignmentSubmissionFile_Upd_casaUri_Old
AFTER UPDATE ON CourseAssignmentSubmissionFile
FOR EACH ROW WHEN NEW.casaUri != OLD.casaUri AND OLD.casaUri IS NOT NULL
BEGIN
    UPDATE CacheLockJoin 
       SET cljStatus = 3
     WHERE cljTableId = 90
       AND cljEntityUid = OLD.casaUid
       AND cljUrl = OLD.casaUri;
END        """)

            add("""CREATE TRIGGER IF NOT EXISTS Retain_CourseAssignmentSubmissionFile_Del_casaUri
AFTER DELETE ON CourseAssignmentSubmissionFile
FOR EACH ROW WHEN OLD.casaUri IS NOT NULL
BEGIN
    UPDATE CacheLockJoin 
       SET cljStatus = 3
     WHERE cljTableId = 90
       AND cljEntityUid = OLD.casaUid
       AND cljUrl = OLD.casaUri;
END       """)
        }else {
            add("""
                            CREATE OR REPLACE FUNCTION retain_c_clj_90_casaUri() RETURNS TRIGGER AS $$
                            BEGIN
                            INSERT INTO CacheLockJoin(cljTableId, cljEntityUid, cljUrl, cljLockId, cljStatus, cljType)
                            VALUES(90, NEW.casaUid, NEW.casaUri, 0, 1, 1);
                            RETURN NEW;
                            END $$ LANGUAGE plpgsql
                        """)

            add("""
                            CREATE OR REPLACE FUNCTION retain_d_clj_90_casaUri() RETURNS TRIGGER AS $$
                            BEGIN
                            UPDATE CacheLockJoin 
                               SET cljStatus = 3
                             WHERE cljTableId = 90
                               AND cljEntityUid = OLD.casaUid
                               AND cljUrl = OLD.casaUri;
                            RETURN OLD;
                            END $$ LANGUAGE plpgsql   
                        """)

            add("""
                            CREATE TRIGGER retain_c_clj_90_casaUri_ins_t
                            AFTER INSERT ON CourseAssignmentSubmissionFile
                            FOR EACH ROW
                            WHEN (NEW.casaUri IS NOT NULL)
                            EXECUTE FUNCTION retain_c_clj_90_casaUri();
                        """)

            add("""
                            CREATE TRIGGER retain_c_clj_90_casaUri_upd_t
                            AFTER UPDATE ON CourseAssignmentSubmissionFile
                            FOR EACH ROW
                            WHEN (NEW.casaUri IS DISTINCT FROM OLD.casaUri AND OLD.casaUri IS NOT NULL)
                            EXECUTE FUNCTION retain_c_clj_90_casaUri();
                        """)

            add("""
                            CREATE TRIGGER retain_d_clj_90_casaUri_upd_t
                            AFTER UPDATE ON CourseAssignmentSubmissionFile
                            FOR EACH ROW
                            WHEN (NEW.casaUri IS DISTINCT FROM OLD.casaUri AND NEW.casaUri IS NOT NULL)
                            EXECUTE FUNCTION retain_d_clj_90_casaUri();
                        """)
        }
    }
}

val MIGRATION_161_162_CLIENT = DoorMigrationStatementList(161, 162) {
    emptyList()
}

val MIGRATION_162_163 = DoorMigrationStatementList(162, 163) { db ->
    listOf("ALTER TABLE ContentEntry ADD COLUMN contentOwnerType INTEGER NOT NULL DEFAULT 0")
}


fun migrationList() = listOf<DoorMigration>(
    MIGRATION_105_106, MIGRATION_106_107,
    MIGRATION_107_108, MIGRATION_108_109,
    MIGRATION_120_121, MIGRATION_121_122, MIGRATION_122_123, MIGRATION_123_124,
    MIGRATION_124_125, MIGRATION_125_126, MIGRATION_126_127, MIGRATION_127_128,
    MIGRATION_128_129, MIGRATION_129_130, MIGRATION_130_131, MIGRATION_132_133,
    MIGRATION_133_134, MIGRATION_134_135, MIGRATION_135_136, MIGRATION_136_137,
    MIGRATION_137_138, MIGRATION_138_139, MIGRATION_139_140, MIGRATION_140_141,
    MIGRATION_141_142, MIGRATION_142_143, MIGRATION_143_144, MIGRATION_145_146,
    MIGRATION_146_147, MIGRATION_147_148, MIGRATION_149_150, MIGRATION_150_151,
    MIGRATION_151_152, MIGRATION_152_153, MIGRATION_153_154, MIGRATION_154_155,
    MIGRATION_156_157, MIGRATION_157_158, MIGRATION_158_159, MIGRATION_159_160,
    MIGRATION_160_161, MIGRATION_162_163,
)


