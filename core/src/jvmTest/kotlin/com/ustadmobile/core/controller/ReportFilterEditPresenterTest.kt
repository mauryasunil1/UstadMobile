package com.ustadmobile.core.controller

import com.nhaarman.mockitokotlin2.*
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.dao.ReportDao
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.util.MessageIdOption
import com.ustadmobile.core.util.UstadTestRule
import com.ustadmobile.core.util.activeRepoInstance
import com.ustadmobile.core.util.ext.captureLastEntityValue
import com.ustadmobile.core.view.*
import com.ustadmobile.door.DoorLifecycleObserver
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.lib.db.entities.Person
import com.ustadmobile.lib.db.entities.ReportFilter
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.instance

class ReportFilterEditPresenterTest {

    @JvmField
    @Rule
    var ustadTestRule = UstadTestRule()

    private lateinit var mockView: ReportFilterEditView

    private lateinit var context: Any

    private lateinit var mockLifecycleOwner: DoorLifecycleOwner

    private lateinit var repoReportDaoSpy: ReportDao

    private lateinit var di: DI

    @Before
    fun setup() {
        mockView = mock { }
        mockLifecycleOwner = mock {
            on { currentState }.thenReturn(DoorLifecycleObserver.RESUMED)
        }
        context = Any()

        di = DI {
            import(ustadTestRule.diModule)
        }

        val repo: UmAppDatabase by di.activeRepoInstance()

        repoReportDaoSpy = spy(repo.reportDao)
        whenever(repo.reportDao).thenReturn(repoReportDaoSpy)
    }

    @Test
    fun givenNoDataWasEntered_whenClickSavedIsClicked_thenShowErrors(){

        val systemImpl: UstadMobileSystemImpl by di.instance()

        val reportFilter = ReportFilter().apply {
            reportFilterSeriesUid = 1
        }

        val presenterArgs = mapOf(UstadEditView.ARG_ENTITY_JSON to Json.stringify(ReportFilter.serializer(), reportFilter))
        val presenter = ReportFilterEditPresenter(context,
                presenterArgs, mockView, di, mockLifecycleOwner)
        presenter.onCreate(null)

        val initialEntity = mockView.captureLastEntityValue()!!

        presenter.handleClickSave(initialEntity)

        verify(systemImpl, timeout(5000)).getString(eq(MessageID.field_required_prompt), any())

        verify(mockView, never()).finishWithResult(any())

    }

    @Test
    fun givenDataWasEntered_whenClickedSaved_thenFinishResult(){


        val reportFilter = ReportFilter().apply {
            reportFilterSeriesUid = 1
        }

        val presenterArgs = mapOf(UstadEditView.ARG_ENTITY_JSON to Json.stringify(ReportFilter.serializer(), reportFilter))
        val presenter = ReportFilterEditPresenter(context,
                presenterArgs, mockView, di, mockLifecycleOwner)
        presenter.onCreate(null)

        val initialEntity: ReportFilter = mockView.captureLastEntityValue()!!
        initialEntity.reportFilterField = ReportFilter.FIELD_PERSON_GENDER
        initialEntity.reportFilterCondition = ReportFilter.CONDITION_IS_NOT
        initialEntity.reportFilterDropDownValue = Person.GENDER_MALE
        initialEntity.reportFilterUid = 1L

        presenter.handleClickSave(initialEntity)

        verify(mockView, timeout(5000)).finishWithResult(eq(listOf(initialEntity)))


    }

}