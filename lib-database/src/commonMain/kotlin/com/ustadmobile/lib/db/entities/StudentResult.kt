package com.ustadmobile.lib.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.door.annotation.ReplicateEntity
import com.ustadmobile.door.annotation.ReplicateEtag
import com.ustadmobile.door.annotation.ReplicateLastModified
import com.ustadmobile.door.annotation.Trigger
import com.ustadmobile.door.annotation.Triggers
import kotlinx.serialization.Serializable


/**
 * Represents a result for a particular student for a particular CourseBlock. There may
 * be more than one Result for one student for the same CourseBlock (e.g. if there are multiple
 * attempts, or peer marking with multiple reviewers is used).
 *
 * Implements Result as per the OneRoster API - see
 * https://www.imsglobal.org/oneroster-v11-final-specification Section 4.11
 *
 * Used to store the marks given for any assignment (coming soon).
 *
 * In case of a group assignment, this result will be copied / filed for all group members. The
 * timestamp and sourcedId would be exactly the same for all group members.
 */
@Serializable
@Entity
@ReplicateEntity(
    tableId = StudentResult.TABLE_ID,
    remoteInsertStrategy = ReplicateEntity.RemoteInsertStrategy.INSERT_INTO_RECEIVE_VIEW,
)
@Triggers(
    arrayOf(
        Trigger(
            name = "studentresult_remote_insert",
            order = Trigger.Order.INSTEAD_OF,
            on = Trigger.On.RECEIVEVIEW,
            events = [Trigger.Event.INSERT],
            conditionSql = TRIGGER_CONDITION_WHERE_NEWER,
            sqlStatements = [ TRIGGER_UPSERT ],
        )
    )
)
data class StudentResult(
    @PrimaryKey(autoGenerate = true)
    var srUid: Long = 0,

    @ColumnInfo(index = true)
    var srSourcedId: String? = "",

    /**
     * The CourseBlock that this result is for (mandatory)
     */
    var srCourseBlockUid: Long = 0,

    /**
     * The clazzUid for the class/course. Whilst this can be obtained through the CourseBlock, it is
     * better for any key that is used in permission scopes to be present directly
     */
    var srClazzUid: Long = 0,

    /**
     * If this result represents an assignment mark, then this will contain the uid of the assignment
     */
    var srAssignmentUid: Long = 0,

    /**
     * The sourcedId of the related LineItem (e.g. CourseBlock)
     */
    var srLineItemSourcedId: String? = "",

    var srStatus: Int = 0,

    /**
     * Metadata JSON as per the OneRoster API
     */
    var srMetaData: String? = null,

    /**
     * The personUid of the student that this result is for
     */
    var srStudentPersonUid: Long = 0,

    /**
     * If this result is for a group assignment, then this is the group that submitted the assignment
     */
    var srStudentGroupId: Int = 0,

    /**
     * The person uid of the person who did the marking e.g. the personuid of the teacher or the
     * personUid of the peer who marked it. If it was not marked by a person (e.g. automatically
     * scored by an external app / content etc), then this is 0
     */
    var srMarkerPersonUid: Long = 0,

    /**
     * If this result is for a group assignment, and it is being peer marked, then this represents
     * the id of the group that marked it.
     */
    var srMarkerGroupId: Int = 0,

    /**
     *
     */
    var srScoreStatus: Int = 0,

    /**
     * The final score (after any late penalty has been applied). E.g. if the student was given
     * a score of 10, and a 20% penalty was applied for a late submission, then resScore = 8 and
     * resPenalty = 2
     */
    var srScore: Float  = 0.toFloat(),


    var srScoreDate: Long = 0,

    @ReplicateLastModified
    @ReplicateEtag
    var srLastModified: Long = 0,

    /**
     * Comment (if any) by the marker.
     */
    var srComment: String? = null,

    /**
     * The appId if this comes from an external app, null if the result is from within the platform
     */
    var srAppId: String? = null,

    var srActive: Boolean = true,

) {
    companion object {

        const val TABLE_ID = 471

    }
}
