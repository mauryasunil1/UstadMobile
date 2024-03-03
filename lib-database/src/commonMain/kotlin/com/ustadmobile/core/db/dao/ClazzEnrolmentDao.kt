package com.ustadmobile.core.db.dao

import com.ustadmobile.door.annotation.DoorDao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ustadmobile.core.db.dao.ClazzEnrolmentDaoCommon.FILTER_ACTIVE_ONLY
import com.ustadmobile.core.db.dao.ClazzEnrolmentDaoCommon.SORT_DATE_LEFT_ASC
import com.ustadmobile.core.db.dao.ClazzEnrolmentDaoCommon.SORT_DATE_LEFT_DESC
import com.ustadmobile.core.db.dao.ClazzEnrolmentDaoCommon.SORT_DATE_REGISTERED_ASC
import com.ustadmobile.core.db.dao.ClazzEnrolmentDaoCommon.SORT_DATE_REGISTERED_DESC
import com.ustadmobile.core.db.dao.ClazzEnrolmentDaoCommon.SORT_FIRST_NAME_ASC
import com.ustadmobile.core.db.dao.ClazzEnrolmentDaoCommon.SORT_FIRST_NAME_DESC
import com.ustadmobile.core.db.dao.ClazzEnrolmentDaoCommon.SORT_LAST_NAME_ASC
import com.ustadmobile.core.db.dao.ClazzEnrolmentDaoCommon.SORT_LAST_NAME_DESC
import com.ustadmobile.door.annotation.*
import app.cash.paging.PagingSource
import com.ustadmobile.core.db.PermissionFlags
import com.ustadmobile.core.db.dao.CoursePermissionDaoCommon.PERSON_COURSE_PERMISSION_CLAUSE_FOR_ACCOUNT_PERSON_UID_AND_CLAZZUID_SQL
import com.ustadmobile.lib.db.composites.CourseNameAndPersonName
import com.ustadmobile.lib.db.composites.PersonAndClazzMemberListDetails
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.db.entities.ClazzLogAttendanceRecord.Companion.STATUS_ATTENDED
import kotlinx.coroutines.flow.Flow

@Repository
@DoorDao
expect abstract class ClazzEnrolmentDao : BaseDao<ClazzEnrolment> {

    /**
     * Note: When actually enroling into a class, use UmAppDatbaseExt#processEnrolmentIntoClass
     * to ensure that permissions, group membership, etc. are taken care of
     */
    @Insert
    abstract fun insertListAsync(entityList: List<ClazzEnrolment>)

    @Query("""SELECT * FROM ClazzEnrolment WHERE clazzEnrolmentPersonUid = :personUid 
        AND clazzEnrolmentClazzUid = :clazzUid 
        AND clazzEnrolmentOutcome = ${ClazzEnrolment.OUTCOME_IN_PROGRESS} LIMIT 1""")
    abstract suspend fun findByPersonUidAndClazzUidAsync(personUid: Long, clazzUid: Long): ClazzEnrolment?

    @Query("""
        SELECT ClazzEnrolment.*, LeavingReason.*, 
               COALESCE(Clazz.clazzTimeZone, COALESCE(School.schoolTimeZone, 'UTC')) as timeZone
          FROM ClazzEnrolment 
               LEFT JOIN LeavingReason 
                         ON LeavingReason.leavingReasonUid = ClazzEnrolment.clazzEnrolmentLeavingReasonUid
               LEFT JOIN Clazz 
                         ON Clazz.clazzUid = ClazzEnrolment.clazzEnrolmentClazzUid
               LEFT JOIN School 
                         ON School.schoolUid = Clazz.clazzSchoolUid
         WHERE clazzEnrolmentPersonUid = :personUid 
           AND ClazzEnrolment.clazzEnrolmentActive 
           AND clazzEnrolmentClazzUid = :clazzUid 
      ORDER BY clazzEnrolmentDateLeft DESC
           """)
    abstract fun findAllEnrolmentsByPersonAndClazzUid(personUid: Long, clazzUid: Long):
            Flow<List<ClazzEnrolmentWithLeavingReason>>


    @HttpAccessible
    @Query("""
            SELECT ClazzEnrolment.*, 
                   LeavingReason.*,
                   COALESCE(Clazz.clazzTimeZone, COALESCE(School.schoolTimeZone, 'UTC')) AS timeZone
              FROM ClazzEnrolment 
                   LEFT JOIN LeavingReason 
                             ON LeavingReason.leavingReasonUid = ClazzEnrolment.clazzEnrolmentLeavingReasonUid
                   LEFT JOIN Clazz 
                             ON Clazz.clazzUid = ClazzEnrolment.clazzEnrolmentClazzUid
                   LEFT JOIN School 
                             ON School.schoolUid = Clazz.clazzSchoolUid
             WHERE ClazzEnrolment.clazzEnrolmentUid = :enrolmentUid
             """)
    abstract suspend fun findEnrolmentWithLeavingReason(
        enrolmentUid: Long
    ): ClazzEnrolmentWithLeavingReason?

    @Query("""
        UPDATE ClazzEnrolment 
          SET clazzEnrolmentDateLeft = :endDate,
              clazzEnrolmentLct = :updateTime
        WHERE clazzEnrolmentUid = :clazzEnrolmentUid""")
    abstract suspend fun updateDateLeftByUid(clazzEnrolmentUid: Long, endDate: Long, updateTime: Long)

    @Update
    abstract suspend fun updateAsync(entity: ClazzEnrolment): Int

    /**
     * Provide a list of the classes a given person is in with the class information itself (e.g.
     * for person detail).
     *
     * @param personUid
     */
    @Query("""SELECT ClazzEnrolment.*, Clazz.*, (SELECT ((CAST(COUNT(DISTINCT CASE WHEN 
        ClazzLogAttendanceRecord.attendanceStatus = $STATUS_ATTENDED THEN 
        ClazzLogAttendanceRecord.clazzLogAttendanceRecordUid ELSE NULL END) AS REAL) / 
        MAX(COUNT(ClazzLogAttendanceRecord.clazzLogAttendanceRecordUid),1)) * 100) 
        FROM ClazzLogAttendanceRecord LEFT JOIN ClazzLog ON 
        ClazzLogAttendanceRecord.clazzLogAttendanceRecordClazzLogUid = ClazzLog.clazzLogUid WHERE 
        ClazzLogAttendanceRecord.clazzLogAttendanceRecordPersonUid = :personUid 
        AND ClazzLog.clazzLogClazzUid = Clazz.clazzUid AND ClazzLog.logDate 
        BETWEEN ClazzEnrolment.clazzEnrolmentDateJoined AND ClazzEnrolment.clazzEnrolmentDateLeft) 
        as attendance
        FROM ClazzEnrolment
        LEFT JOIN Clazz ON ClazzEnrolment.clazzEnrolmentClazzUid = Clazz.clazzUid
        WHERE ClazzEnrolment.clazzEnrolmentPersonUid = :personUid
        AND ClazzEnrolment.clazzEnrolmentActive
        ORDER BY ClazzEnrolment.clazzEnrolmentDateLeft DESC
    """)
    @PostgresQuery("""SELECT ClazzEnrolment.*, Clazz.*, (SELECT ((CAST(COUNT(DISTINCT CASE WHEN 
        ClazzLogAttendanceRecord.attendanceStatus = $STATUS_ATTENDED THEN 
        ClazzLogAttendanceRecord.clazzLogAttendanceRecordUid ELSE NULL END) AS REAL) / 
        GREATEST(COUNT(ClazzLogAttendanceRecord.clazzLogAttendanceRecordUid),1)) * 100) 
        FROM ClazzLogAttendanceRecord LEFT JOIN ClazzLog ON 
        ClazzLogAttendanceRecord.clazzLogAttendanceRecordClazzLogUid = ClazzLog.clazzLogUid WHERE 
        ClazzLogAttendanceRecord.clazzLogAttendanceRecordPersonUid = :personUid 
        AND ClazzLog.clazzLogClazzUid = Clazz.clazzUid AND ClazzLog.logDate 
        BETWEEN ClazzEnrolment.clazzEnrolmentDateJoined AND ClazzEnrolment.clazzEnrolmentDateLeft) 
        as attendance
        FROM ClazzEnrolment
        LEFT JOIN Clazz ON ClazzEnrolment.clazzEnrolmentClazzUid = Clazz.clazzUid
        WHERE ClazzEnrolment.clazzEnrolmentPersonUid = :personUid
        AND ClazzEnrolment.clazzEnrolmentActive
        ORDER BY ClazzEnrolment.clazzEnrolmentDateLeft DESC
    """)
    abstract fun findAllClazzesByPersonWithClazz(
        personUid: Long
    ): Flow<List<ClazzEnrolmentWithClazzAndAttendance>>

    @Query("""SELECT COALESCE(MAX(clazzEnrolmentDateLeft),0) FROM ClazzEnrolment WHERE 
        ClazzEnrolment.clazzEnrolmentPersonUid = :selectedPerson 
        AND ClazzEnrolment.clazzEnrolmentActive 
        AND clazzEnrolmentClazzUid = :selectedClazz AND clazzEnrolmentUid != :selectedEnrolment
    """)
    abstract suspend fun findMaxEndDateForEnrolment(selectedClazz: Long, selectedPerson: Long,
                                            selectedEnrolment: Long): Long

    @Query("""SELECT ClazzEnrolment.*, Clazz.* 
        FROM ClazzEnrolment 
        LEFT JOIN Clazz ON ClazzEnrolment.clazzEnrolmentClazzUid = Clazz.clazzUid 
        WHERE ClazzEnrolment.clazzEnrolmentPersonUid = :personUid 
        AND ClazzEnrolment.clazzEnrolmentActive
        ORDER BY ClazzEnrolment.clazzEnrolmentDateLeft DESC
    """)
    abstract suspend fun findAllClazzesByPersonWithClazzAsListAsync(personUid: Long): List<ClazzEnrolmentWithClazz>

    @Query("""
        SELECT ClazzEnrolment.*, Person.*
          FROM ClazzEnrolment
    LEFT JOIN Person ON ClazzEnrolment.clazzEnrolmentPersonUid = Person.personUid
        WHERE ClazzEnrolment.clazzEnrolmentClazzUid = :clazzUid
              AND :date BETWEEN ClazzEnrolment.clazzEnrolmentDateJoined 
              AND ClazzEnrolment.clazzEnrolmentDateLeft
              AND CAST(clazzEnrolmentActive AS INTEGER) = 1
              AND (:roleFilter = 0 OR ClazzEnrolment.clazzEnrolmentRole = :roleFilter)
              AND (:personUidFilter = 0 OR ClazzEnrolment.clazzEnrolmentPersonUid = :personUidFilter)
    """)
    abstract suspend fun getAllClazzEnrolledAtTimeAsync(
        clazzUid: Long,
        date: Long,
        roleFilter: Int,
        personUidFilter: Long = 0
    ): List<ClazzEnrolmentWithPerson>

    @HttpAccessible(
        clientStrategy = HttpAccessible.ClientStrategy.PULL_REPLICATE_ENTITIES
    )
    @Query("""
        SELECT ClazzEnrolment.*
          FROM ClazzEnrolment
         WHERE ClazzEnrolment.clazzEnrolmentClazzUid = :clazzUid
           AND ClazzEnrolment.clazzEnrolmentPersonUid = :accountPersonUid
           AND :time BETWEEN ClazzEnrolment.clazzEnrolmentDateJoined 
                         AND ClazzEnrolment.clazzEnrolmentDateLeft
           AND ClazzEnrolment.clazzEnrolmentActive              
    """)
    abstract suspend fun getAllEnrolmentsAtTimeByClazzAndPerson(
        clazzUid: Long,
        accountPersonUid: Long,
        time: Long,
    ): List<ClazzEnrolment>


    @Query("SELECT * FROM ClazzEnrolment WHERE clazzEnrolmentUid = :uid")
    abstract suspend fun findByUid(uid: Long): ClazzEnrolment?

    @Query("SELECT * FROM ClazzEnrolment WHERE clazzEnrolmentUid = :uid")
    abstract fun findByUidLive(uid: Long): Flow<ClazzEnrolment?>

    @Query("""
                UPDATE ClazzEnrolment
                   SET clazzEnrolmentActive = :active,
                       clazzEnrolmentLct= :changeTime
                WHERE clazzEnrolmentPersonUid = :personUid 
                      AND clazzEnrolmentClazzUid = :clazzUid
                      AND clazzEnrolmentRole = :roleId""")
    abstract suspend fun updateClazzEnrolmentActiveForPersonAndClazz(
        personUid: Long,
        clazzUid: Long,
        roleId: Int,
        active: Boolean,
        changeTime: Long
    ): Int


    /*
     * Note: SELECT * FROM (Subquery) AS CourseMember is needed so that sorting by
     * earliestJoinDate/latestDateLeft will work as expected on postgres.
     *
     * This query uses a permission check so that users will only see participants that they have
     * permission to see (e.g. on some courses / MOOC style students might not have permission to
     * see other students etc).
     *
     * This Query is used by ClazzMemberListViewModel.
     *
     *
     AND (
                           ($PERSON_COURSE_PERMISSION_CLAUSE_FOR_ACCOUNT_PERSON_UID_AND_CLAZZUID_SQL)
                        OR Person.personUid = :accountPersonUid
                       )
     * */
    @Query("""
        SELECT * 
          FROM (SELECT Person.*, PersonPicture.*,
                       (SELECT MIN(ClazzEnrolment.clazzEnrolmentDateJoined) 
                          FROM ClazzEnrolment 
                         WHERE Person.personUid = ClazzEnrolment.clazzEnrolmentPersonUid) AS earliestJoinDate, 
        
                       (SELECT MAX(ClazzEnrolment.clazzEnrolmentDateLeft) 
                          FROM ClazzEnrolment 
                         WHERE Person.personUid = ClazzEnrolment.clazzEnrolmentPersonUid) AS latestDateLeft, 
        
                       (SELECT ClazzEnrolment.clazzEnrolmentRole 
                          FROM ClazzEnrolment 
                         WHERE Person.personUid = ClazzEnrolment.clazzEnrolmentPersonUid 
                           AND ClazzEnrolment.clazzEnrolmentClazzUid = :clazzUid 
                           AND ClazzEnrolment.clazzEnrolmentActive
                      ORDER BY ClazzEnrolment.clazzEnrolmentDateLeft DESC
                         LIMIT 1) AS enrolmentRole
                  FROM Person
                       LEFT JOIN PersonPicture
                                 ON PersonPicture.personPictureUid = Person.personUid
                 WHERE Person.personUid IN 
                       (SELECT DISTINCT ClazzEnrolment.clazzEnrolmentPersonUid 
                          FROM ClazzEnrolment 
                         WHERE ClazzEnrolment.clazzEnrolmentClazzUid = :clazzUid 
                           AND ClazzEnrolment.clazzEnrolmentActive 
                           AND ClazzEnrolment.clazzEnrolmentRole = :roleId 
                           AND (:filter != $FILTER_ACTIVE_ONLY 
                                 OR (:currentTime 
                                      BETWEEN ClazzEnrolment.clazzEnrolmentDateJoined 
                                      AND ClazzEnrolment.clazzEnrolmentDateLeft))) 
                   /* Begin permission check */
                   AND (
                           ($PERSON_COURSE_PERMISSION_CLAUSE_FOR_ACCOUNT_PERSON_UID_AND_CLAZZUID_SQL)
                        OR Person.personUid = :accountPersonUid
                       )  
                   /* End permission check */                   
                   AND Person.firstNames || ' ' || Person.lastName LIKE :searchText
               GROUP BY Person.personUid, PersonPicture.personPictureUid) AS CourseMember
      ORDER BY CASE(:sortOrder)
                WHEN $SORT_FIRST_NAME_ASC THEN CourseMember.firstNames
                WHEN $SORT_LAST_NAME_ASC THEN CourseMember.lastName
                ELSE ''
            END ASC,
            CASE(:sortOrder)
                WHEN $SORT_FIRST_NAME_DESC THEN CourseMember.firstNames
                WHEN $SORT_LAST_NAME_DESC THEN CourseMember.lastName
                ELSE ''
            END DESC,
            CASE(:sortOrder)
                WHEN $SORT_DATE_REGISTERED_ASC THEN CourseMember.earliestJoinDate
                WHEN $SORT_DATE_LEFT_ASC THEN CourseMember.latestDateLeft
                ELSE 0
            END ASC,
            CASE(:sortOrder)
                WHEN $SORT_DATE_REGISTERED_DESC THEN CourseMember.earliestJoinDate
                WHEN $SORT_DATE_LEFT_DESC THEN CourseMember.latestDateLeft
                ELSE 0
            END DESC
    """)
    @QueryLiveTables(value = ["Clazz", "Person", "ClazzEnrolment", "PersonPicture", "CoursePermission"])
    @HttpAccessible(
        pullQueriesToReplicate = arrayOf(
            HttpServerFunctionCall("findByClazzUidAndRole"),
            HttpServerFunctionCall("findEnrolmentsByClazzUidAndRole"),
        )
    )
    abstract fun findByClazzUidAndRole(
        clazzUid: Long,
        roleId: Int,
        sortOrder: Int,
        searchText: String? = "%",
        filter: Int,
        accountPersonUid: Long,
        currentTime: Long,
        permission: Long,
    ): PagingSource<Int, PersonAndClazzMemberListDetails>

    @Query("""
       SELECT ClazzEnrolment.*
         FROM ClazzEnrolment
        WHERE ClazzEnrolment.clazzEnrolmentClazzUid = :clazzUid
          AND ClazzEnrolment.clazzEnrolmentRole = :roleId
              /* Begin permission check*/
          AND (
                   ($PERSON_COURSE_PERMISSION_CLAUSE_FOR_ACCOUNT_PERSON_UID_AND_CLAZZUID_SQL)
                OR ClazzEnrolment.clazzEnrolmentPersonUid = :accountPersonUid
              )  
              /* End permission check */
    """)
    abstract suspend fun findEnrolmentsByClazzUidAndRole(
        clazzUid: Long,
        accountPersonUid: Long,
        roleId: Int,
        permission: Long,
    ): List<ClazzEnrolment>


    @Query("""
        SELECT ClazzEnrolment.*
          FROM ClazzEnrolment
         WHERE ClazzEnrolment.clazzEnrolmentClazzUid = :clazzUid
           AND ClazzEnrolment.clazzEnrolmentRole = :roleId
    """)
    abstract suspend fun findAllEnrolmentsByClazzUidAndRole(
        clazzUid: Long,
        roleId: Int
    ): List<ClazzEnrolment>

    @Query("""
        UPDATE ClazzEnrolment 
          SET clazzEnrolmentActive = :enrolled,
              clazzEnrolmentLct = :timeChanged
        WHERE clazzEnrolmentUid = :clazzEnrolmentUid""")
    abstract fun updateClazzEnrolmentActiveForClazzEnrolment(
        clazzEnrolmentUid: Long,
        enrolled: Boolean,
        timeChanged: Long,
    ): Int

    @Query("""
            UPDATE ClazzEnrolment 
               SET clazzEnrolmentRole = :newRole,
                   clazzEnrolmentLct = :updateTime      
             -- Avoid potential for duplicate approvals if user was previously refused      
             WHERE clazzEnrolmentUid = COALESCE( 
                    (SELECT clazzEnrolmentUid
                       FROM ClazzEnrolment
                      WHERE clazzEnrolmentPersonUid = :personUid 
                            AND clazzEnrolmentClazzUid = :clazzUid
                            AND clazzEnrolmentRole = :oldRole
                            AND CAST(clazzEnrolmentActive AS INTEGER) = 1
                      LIMIT 1), 0)""")
    abstract suspend fun updateClazzEnrolmentRole(
        personUid: Long,
        clazzUid: Long,
        newRole: Int,
        oldRole: Int,
        updateTime: Long
    ): Int

    @Query("""
        SELECT Person.firstNames, Person.lastName, Clazz.clazzName
          FROM Person
               LEFT JOIN Clazz
                         ON Clazz.clazzUid = :clazzUid
        WHERE Person.personUid = :personUid                 
    """)
    abstract suspend fun getClazzNameAndPersonName(
        personUid: Long,
        clazzUid: Long,
    ): CourseNameAndPersonName?


    /**
     * Find the enrolments required for a given accountPersonUid to check their permissions to view
     * another person, optionally filtered by person.
     *
     * This will include the ClazzEnrolment(s) of accountPersonUid themselves and all ClazzEnrolment
     * entities for any Clazz where they have permission to view members.
     *
     * @param accountPersonUid the active user for whom we are checking permissions
     * @param otherPersonUid the person that we want to check if accountPersonUid has permission to
     *        view when checking for a specific person, or 0 if fetching all (e.g. listing all persons)
     */
    @Query("""
          WITH CanViewMembersClazzesViaCoursePermission(clazzUid) AS
               /* Get clazzuids where active user can view members based on their own enrolment role */
               (SELECT CoursePermission.cpClazzUid
                  FROM ClazzEnrolment ClazzEnrolment_ActiveUser
                       JOIN CoursePermission 
                            ON CoursePermission.cpClazzUid = ClazzEnrolment_ActiveUser.clazzEnrolmentClazzUid
                           AND CoursePermission.cpToEnrolmentRole = ClazzEnrolment_ActiveUser.clazzEnrolmentRole
                 WHERE ClazzEnrolment_ActiveUser.clazzEnrolmentPersonUid = :accountPersonUid 
                   AND (CoursePermission.cpPermissionsFlag & ${PermissionFlags.COURSE_VIEW_MEMBERS}) > 0 
                UNION
                /* Get ClazzUids where the active user can view members based a grant directly to them */
                SELECT CoursePermission.cpClazzUid
                  FROM CoursePermission
                 WHERE CoursePermission.cpToPersonUid  = :accountPersonUid
                   AND (CoursePermission.cpPermissionsFlag & ${PermissionFlags.COURSE_VIEW_MEMBERS}) > 0
               )
        SELECT ClazzEnrolment.*
          FROM ClazzEnrolment
         WHERE ClazzEnrolment.clazzEnrolmentPersonUid = :accountPersonUid
            OR (    ClazzEnrolment.clazzEnrolmentClazzUid IN 
                        (SELECT CanViewMembersClazzesViaCoursePermission.clazzUid
                           FROM CanViewMembersClazzesViaCoursePermission)
                AND (:otherPersonUid = 0 OR ClazzEnrolment.clazzEnrolmentPersonUid = :otherPersonUid)   
                )
    """)
    abstract suspend fun findClazzEnrolmentEntitiesForPersonViewPermissionCheck(
        accountPersonUid: Long,
        otherPersonUid: Long,
    ): List<ClazzEnrolment>

}
