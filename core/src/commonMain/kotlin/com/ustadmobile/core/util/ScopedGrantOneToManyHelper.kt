package com.ustadmobile.core.util

import com.ustadmobile.core.controller.UstadEditPresenter
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.impl.nav.UstadSavedStateHandle
import com.ustadmobile.core.view.ListViewMode
import com.ustadmobile.core.view.PersonListView
import com.ustadmobile.core.view.ScopedGrantEditView
import com.ustadmobile.core.view.UstadView
import com.ustadmobile.core.view.UstadView.Companion.ARG_GO_TO_COMPLETE
import com.ustadmobile.door.DoorObserver
import com.ustadmobile.lib.db.entities.ScopedGrant
import com.ustadmobile.lib.db.entities.ScopedGrantAndName
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * @param editPresenter The editpresenter that this helper is working for - used to listen for json
 * saved and restore events.
 * @param tableId the table id for which scopedgrants are being created for. This is passed to
 * ScopedGrantEdit to determine which permissions are displayed
 */
class ScopedGrantOneToManyHelper(val editPresenter: UstadEditPresenter<*, *>,
    val savedStateHandle: UstadSavedStateHandle,
    val entityTableId: Int)
    : DefaultOneToManyJoinEditHelper<ScopedGrantAndName>(
    pkGetter = {it.scopedGrant?.sgGroupUid ?: 0L},
    serializationKey = "ScopedGrantAndName",
    serializationStrategy = ListSerializer(ScopedGrantAndName.serializer()),
    deserializationStrategy = ListSerializer(ScopedGrantAndName.serializer()),
    editPresenter = editPresenter,
    entityClass = ScopedGrantAndName::class,
    pkSetter = {scopedGrant?.sgEntityUid = it}), ScopedGrantOneToManyListener {


    init {
        //Get the current back stack, then watch / observe for when the result comes back from the edit screen
        // e.g. what we would otherwise be doing in the Android fragment onViewCreated

        onStartObservingResults()
    }

    /**
     * This function is responsible to observe for incoming results (e.g. when the user navigates
     * away to pick something, the other destination will save the picked value in the
     * savedStateHandle).
     */
    fun onStartObservingResults() {
        savedStateHandle.getLiveData<String?>("ScopedGrant").observe(editPresenter.lifecycleOwner,
            DoorObserver {
                if(it == null)
                    return@DoorObserver

                val newValue = Json.decodeFromString(ListSerializer(ScopedGrant.serializer()), it)
                onEditResult(ScopedGrantAndName().apply {
                    scopedGrant = newValue.first()
                    name = "Name me"
                })

                savedStateHandle["ScopedGrant"] = null
            })
    }

    suspend fun commitToDatabase(repo: UmAppDatabase, entityUid: Long) {
        //function to set the table and entity uid on scopedgrants
        val scopedGrantForeignKeyFn : (ScopedGrant) -> Unit = {
            it.sgTableId = entityTableId
            it.sgEntityUid = entityUid

            //TODO here: replace special values for teacher/student groups to use correct persongroupuid
        }

        repo.scopedGrantDao.insertListAsync(
            entitiesToInsert.mapNotNull {
                it.scopedGrant?.also(scopedGrantForeignKeyFn)
            })

        repo.scopedGrantDao.updateListAsync(
            entitiesToUpdate.mapNotNull {
                it.scopedGrant?.also(scopedGrantForeignKeyFn)
            })
    }


    override fun onClickAddNewScopedGrant() {
        editPresenter.saveStateToNavController()

        editPresenter.navigateToEditEntity(null,
            PersonListView.VIEW_NAME,
            ScopedGrant::class,
            ScopedGrant.serializer(),
            args = mutableMapOf(
                ScopedGrantEditView.ARG_PERMISSION_LIST to entityTableId.toString(),
                ARG_GO_TO_COMPLETE to ScopedGrantEditView.VIEW_NAME,
                UstadView.ARG_LISTMODE to ListViewMode.PICKER.toString()))

        //TODO("Show the multiplatform based dialog, then navigate accordingly")
    }

    override fun onClickEditScopedGrant(scopedGrantAndName: ScopedGrantAndName) {
        //TODO("Not yet implemented")
    }

    override fun onClickDeleteScopedGrant(scopedGrantAndName: ScopedGrantAndName) {
        //TODO("Not yet implemented")
    }
}