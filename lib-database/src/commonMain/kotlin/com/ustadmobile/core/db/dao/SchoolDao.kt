package com.ustadmobile.core.db.dao

import app.cash.paging.PagingSource
import com.ustadmobile.door.annotation.DoorDao
import androidx.room.Query
import androidx.room.Update
import com.ustadmobile.core.db.dao.SchoolDaoCommon.SORT_NAME_ASC
import com.ustadmobile.core.db.dao.SchoolDaoCommon.SORT_NAME_DESC
import com.ustadmobile.door.annotation.*
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.db.entities.School.Companion.JOIN_FROM_PERSONGROUPMEMBER_TO_SCHOOL_VIA_SCOPEDGRANT_PT1
import com.ustadmobile.lib.db.entities.School.Companion.JOIN_FROM_PERSONGROUPMEMBER_TO_SCHOOL_VIA_SCOPEDGRANT_PT2
import com.ustadmobile.lib.db.entities.School.Companion.JOIN_FROM_SCHOOL_TO_USERSESSION_VIA_SCOPEDGRANT_PT1
import com.ustadmobile.lib.db.entities.School.Companion.JOIN_FROM_SCHOOL_TO_USERSESSION_VIA_SCOPEDGRANT_PT2

@Repository
@DoorDao
expect abstract class SchoolDao : BaseDao<School> {

    @Query("SELECT * FROM School WHERE schoolUid = :schoolUid AND CAST(schoolActive AS INTEGER) = 1")
    abstract suspend fun findByUidAsync(schoolUid: Long): School?

    @Query("""SELECT School.*, HolidayCalendar.* FROM School 
            LEFT JOIN HolidayCalendar ON School.schoolHolidayCalendarUid = HolidayCalendar.umCalendarUid
            WHERE School.schoolUid = :uid""")
    abstract suspend fun findByUidWithHolidayCalendarAsync(uid: Long): SchoolWithHolidayCalendar?


    @Query("SELECT * FROM School WHERE schoolCode = :code")
    abstract suspend fun findBySchoolCode(code: String): School?

    @Query("SELECT * FROM School WHERE schoolCode = :code")
    @Repository(Repository.METHOD_DELEGATE_TO_WEB)
    abstract suspend fun findBySchoolCodeFromWeb(code: String): School?


    /** Check if a permission is present on a specific entity e.g. updateState/modify etc */
    @Query("""
    Select EXISTS(
           SELECT School.schoolUid 
             FROM School
                  $JOIN_FROM_SCHOOL_TO_USERSESSION_VIA_SCOPEDGRANT_PT1 :permission) > 0
             JOIN PersonGroupMember AS PrsGrpMbr
                   ON ScopedGrant.sgGroupUid = PrsGrpMbr.groupMemberGroupUid
                      AND PrsGrpMbr.groupMemberPersonUid = :accountPersonUid
            WHERE School.schoolUid = :schoolUid)      
    """)
    abstract suspend fun personHasPermissionWithSchool(accountPersonUid: Long,
                                                       schoolUid: Long,
                                                      permission: Long) : Boolean


    @Query("""
       SELECT School.*, 
              (SELECT COUNT(*) 
                  FROM SchoolMember 
                 WHERE SchoolMember.schoolMemberSchoolUid = School.schoolUid 
                   AND CAST(SchoolMember.schoolMemberActive AS INTEGER) = 1 
                   AND SchoolMember.schoolMemberRole = ${Role.ROLE_SCHOOL_STUDENT_UID}) as numStudents,
              (SELECT COUNT(*) 
                 FROM SchoolMember 
                WHERE SchoolMember.schoolMemberSchoolUid = School.schoolUid 
                  AND CAST(SchoolMember.schoolMemberActive AS INTEGER) = 1 
                  AND SchoolMember.schoolMemberRole = ${Role.ROLE_SCHOOL_STAFF_UID}) as numTeachers, 
               '' as locationName,
              (SELECT COUNT(*) 
                 FROM Clazz 
                WHERE Clazz.clazzSchoolUid = School.schoolUid 
                  AND CAST(Clazz.clazzUid AS INTEGER) = 1 ) as clazzCount
         FROM PersonGroupMember
              $JOIN_FROM_PERSONGROUPMEMBER_TO_SCHOOL_VIA_SCOPEDGRANT_PT1
                    :permission
                    $JOIN_FROM_PERSONGROUPMEMBER_TO_SCHOOL_VIA_SCOPEDGRANT_PT2
        WHERE PersonGroupMember.groupMemberPersonUid = :personUid
          AND PersonGroupMember.groupMemberActive 
          AND CAST(schoolActive AS INTEGER) = 1
          AND schoolName LIKE :searchBit
     GROUP BY School.schoolUid
     ORDER BY CASE(:sortOrder)
              WHEN $SORT_NAME_ASC THEN School.schoolName
              ELSE ''
              END ASC,
              CASE(:sortOrder)
              WHEN $SORT_NAME_DESC THEN School.schoolName
              ELSE ''
              END DESC""")
    abstract fun findAllActiveSchoolWithMemberCountAndLocationName(searchBit: String,
                    personUid: Long, permission: Long, sortOrder: Int)
            : PagingSource<Int, SchoolWithMemberCountAndLocation>


    @Update
    abstract suspend fun updateAsync(entity: School): Int


}
