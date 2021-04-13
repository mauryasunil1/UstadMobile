package com.ustadmobile.core.util.ext

import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.door.asRepository
import com.ustadmobile.lib.db.entities.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.*
import io.ktor.client.features.json.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class UmAppDatabaseExtTest {


    private lateinit var db: UmAppDatabase

    private lateinit var repo: UmAppDatabase

    private val context = Any()

    private lateinit var mockSystemImpl: UstadMobileSystemImpl

    private lateinit var httpClient: HttpClient

    @Before
    fun setup() {
        db = UmAppDatabase.getInstance(context).also {
            it.clearAllTables()
        }

        httpClient = HttpClient(OkHttp) {
            install(JsonFeature)
            install(HttpTimeout)
        }

        repo = db.asRepository(context, "http://localhost/dummy/", "",
                httpClient)

        mockSystemImpl = mock {
            on { getString(any(), any())}.thenAnswer {
                "${it.arguments[0]}"
            }
        }
    }

    @After
    fun tearDown() {
        httpClient.close()
    }

    @Test
    fun givenClazzDoesNotExist_whenCreateClazzAndGroupsCalled_thenClazzGroupsAndEntityRolesCreated() = runBlocking {
        val testClazz = Clazz("Test name")

        repo.createNewClazzAndGroups(testClazz, mockSystemImpl, context)

        val clazzInDb = db.clazzDao.findByUid(testClazz.clazzUid)
        Assert.assertEquals("Stored class has same name", testClazz.clazzName,
                clazzInDb?.clazzName)

        val teacherGroup = db.personGroupDao.findByUid(clazzInDb!!.clazzTeachersPersonGroupUid)
        Assert.assertNotNull("Teacher PersonGroup created", teacherGroup)

        val studentGroup = db.personGroupDao.findByUid(clazzInDb!!.clazzStudentsPersonGroupUid)
        Assert.assertNotNull("Student person group created", studentGroup)

        Assert.assertEquals("Teacher group has entity role", 1,
                db.entityRoleDao.findByEntitiyAndPersonGroupAndRole(
                        Clazz.TABLE_ID, testClazz.clazzUid, teacherGroup!!.groupUid,
                        Role.ROLE_CLAZZ_TEACHER_UID.toLong()).size)

        Assert.assertEquals("Student group has entity role", 1,
                db.entityRoleDao.findByEntitiyAndPersonGroupAndRole(
                        Clazz.TABLE_ID, testClazz.clazzUid, studentGroup!!.groupUid,
                        Role.ROLE_CLAZZ_STUDENT_UID.toLong()).size)
    }

    @Test
    fun givenExistingClazz_whenEnrolMemberCalled_thenClazzMemberIsCreatedAndPersonGroupMemberCreated() = runBlocking {
        val testClazz = Clazz("Test name")
        val testPerson = Person("teacher", "Teacher", "Test")

        repo.createNewClazzAndGroups(testClazz, mockSystemImpl, context)
        repo.personDao.insert(testPerson)

        repo.enrolPersonIntoClazzAtLocalTimezone(testPerson, testClazz.clazzUid, ClazzEnrolment.ROLE_TEACHER)

        val personClazzes = db.clazzEnrolmentDao.findAllClazzesByPersonWithClazzAsListAsync(
                testPerson.personUid)

        Assert.assertTrue("PersonMember was created", personClazzes.any { it.clazzEnrolmentClazzUid == testClazz.clazzUid })

        val personGroups = db.personGroupMemberDao.findAllGroupWherePersonIsIn(testPerson.personUid)
        Assert.assertEquals("Person is now teacher group",
                testClazz.clazzTeachersPersonGroupUid,
                personGroups.first().groupMemberGroupUid)
    }


    @Test
    fun givenExistingSchool_whenEnrolMemberCalled_thenSchoolMemberIsCreatedAndPersonGroupMemberCreated() = runBlocking {
        val testSchool = School("School A")
        testSchool.schoolActive =true
        val testPerson = Person("teacher", "Teacher", "Test")

        testSchool.schoolUid = repo.createNewSchoolAndGroups(testSchool, mockSystemImpl, context)
        testPerson.personUid = repo.personDao.insert(testPerson)

        repo.enrollPersonToSchool(testSchool.schoolUid, testPerson.personUid,
                Role.ROLE_SCHOOL_STAFF_UID)

        val schoolMembers = db.schoolMemberDao.findBySchoolAndPersonAndRole(
                testSchool.schoolUid,
                testPerson.personUid, Role.ROLE_SCHOOL_STAFF_UID)

        Assert.assertTrue("PersonMember was created", schoolMembers.any {
            it.schoolMemberSchoolUid == testSchool.schoolUid })

        val personGroups = db.personGroupMemberDao.findAllGroupWherePersonIsIn(testPerson.personUid)
        Assert.assertEquals("Person is now teacher group",
                testSchool.schoolTeachersPersonGroupUid,
                personGroups.first().groupMemberGroupUid)
    }
}