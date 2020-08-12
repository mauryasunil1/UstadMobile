
package com.ustadmobile.core.controller

import com.nhaarman.mockitokotlin2.*
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.dao.ContentEntryDao
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.networkmanager.downloadmanager.ContainerDownloadManager
import com.ustadmobile.core.util.UstadTestRule
import com.ustadmobile.core.util.activeDbInstance
import com.ustadmobile.core.view.ContentEntry2DetailView
import com.ustadmobile.core.view.ContentEntryEdit2View
import com.ustadmobile.core.view.UstadView.Companion.ARG_ENTITY_UID
import com.ustadmobile.door.DoorLifecycleObserver
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.ContentEntryWithMostRecentContainer
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.kodein.di.DI
import org.kodein.di.instance
import java.lang.Thread.sleep

class ContentEntry2DetailPresenterTest {

    @JvmField
    @Rule
    var ustadTestRule = UstadTestRule()

    private lateinit var mockView: ContentEntry2DetailView

    private lateinit var context: Any

    private lateinit var mockLifecycleOwner: DoorLifecycleOwner

    private lateinit var repoContentEntrySpyDao: ContentEntryDao

    private var createdEntry: ContentEntry? = null

    private val defaultTimeout = 3000L

    private lateinit var containerManager: ContainerDownloadManager

    private var presenterArgs: Map<String, String>? = null

    private lateinit var di: DI

    @Before
    fun setup() {
        mockView = mock { }
        containerManager = spy{}
        mockLifecycleOwner = mock {
            on { currentState }.thenReturn(DoorLifecycleObserver.RESUMED)
        }
        context = Any()

        di = DI {
            import(ustadTestRule.diModule)
        }

        val db: UmAppDatabase by di.activeDbInstance()
        val repo: UmAppDatabase by di.activeDbInstance()

        repoContentEntrySpyDao = spy(repo.contentEntryDao)
        whenever(repo.contentEntryDao).thenReturn(repoContentEntrySpyDao)
        createdEntry = ContentEntry().apply {
            title = "Dummy Entry"
            leaf = true
            contentEntryUid = db.contentEntryDao.insert(this)
        }

        presenterArgs = mapOf(ARG_ENTITY_UID to createdEntry?.contentEntryUid.toString())
    }

    @Test
    fun givenContentEntryExists_whenLaunched_thenShouldShowContentEntry(){
        val presenter = ContentEntry2DetailPresenter(context,
                presenterArgs!!, mockView, di, mockLifecycleOwner)

        presenter.onCreate(null)

        nullableArgumentCaptor<ContentEntryWithMostRecentContainer>().apply {
            verify(mockView, timeout(defaultTimeout).atLeastOnce()).entity = capture()
            Assert.assertEquals("Expected entry was set on view",
                    createdEntry?.contentEntryUid, lastValue!!.contentEntryUid)
        }
    }


    @Test
    fun givenContentEntryExists_whenHandleOnClickEditCalled_thenSystemImplGoToEditViewIsCalled(){
        val presenter = ContentEntry2DetailPresenter(context,
                presenterArgs!!, mockView, di, mockLifecycleOwner)
        val systemImpl: UstadMobileSystemImpl by di.instance()

        presenter.onCreate(null)

        sleep(defaultTimeout)

        presenter.handleClickEdit()

        verify(systemImpl).go(eq(ContentEntryEdit2View.VIEW_NAME),
                eq(mapOf(ARG_ENTITY_UID to createdEntry?.contentEntryUid.toString())), any())
    }

}