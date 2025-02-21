package com.ustadmobile.lib.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.door.annotation.*
import kotlinx.serialization.Serializable

@Entity
@ReplicateEntity(
    tableId = CourseAssignmentMark.TABLE_ID,
    remoteInsertStrategy = ReplicateEntity.RemoteInsertStrategy.INSERT_INTO_RECEIVE_VIEW,
)
@Triggers(arrayOf(
        Trigger(
            name = "courseassignmentmark_remote_insert",
            order = Trigger.Order.INSTEAD_OF,
            on = Trigger.On.RECEIVEVIEW,
            events = [Trigger.Event.INSERT],
            conditionSql = TRIGGER_CONDITION_WHERE_NEWER,
            sqlStatements = [TRIGGER_UPSERT],
        )
))
@Serializable
open class CourseAssignmentMark {

    @PrimaryKey(autoGenerate = true)
    var camUid: Long = 0

    var camAssignmentUid: Long = 0

    /**
     * The submitter uid of the group/person that this mark is for (e.g. the one being marked). See
     * CourseAssignmentSubmission.casSubmitterUid for details.
     */
    var camSubmitterUid: Long = 0

    /**
     * The submitter UID of the marker. If this mark is given by a teacher, then this is 0. If the
     * assignment is peer marked, then this is the submitter id of the peer that marked it (e.g. the
     * personUid if individual assignment, or the group number if this is a group-based assignment).
     */
    @ColumnInfo(defaultValue = "0")
    var camMarkerSubmitterUid: Long = 0

    /**
     * The personUid of the person who provided the mark. This is always the personUid of the person
     * who clicked the mark submit button.
     */
    @ColumnInfo(defaultValue = "0")
    var camMarkerPersonUid: Long = 0

    var camMarkerComment: String? = null

    /**
     * The mark issued to the submitter (e.g. group/student). This is the final mark, after
     * subtracting any late submission penalty
     */
    var camMark: Float = 0f

    /**
     * The maximum possible mark at the time the mark was given. Even if the CourseBlock is
     * changed later, this keeps a record
     */
    @ColumnInfo(defaultValue = "1")
    var camMaxMark: Float = 1f

    /**
     * The penalty for late submission, applied at the time that the mark is issued. Even if the
     * CourseBlock is changed later, this keeps a record.
     */
    var camPenalty: Float = 0f

    @ReplicateLastModified
    @ReplicateEtag
    var camLct: Long = 0

    @ColumnInfo(defaultValue = "0")
    var camClazzUid: Long = 0


    companion object {

        const val TABLE_ID = 523

    }
}