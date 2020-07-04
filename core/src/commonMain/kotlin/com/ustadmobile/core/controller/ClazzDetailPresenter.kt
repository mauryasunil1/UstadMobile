package com.ustadmobile.core.controller

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.view.ClazzDetailOverviewView
import com.ustadmobile.core.view.ClazzDetailView
import com.ustadmobile.core.view.ClazzLogListAttendanceView
import com.ustadmobile.core.view.ClazzMemberListView
import com.ustadmobile.core.view.UstadEditView.Companion.ARG_ENTITY_JSON
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.lib.db.entities.Clazz

import com.ustadmobile.lib.db.entities.UmAccount
import kotlinx.coroutines.*
import com.ustadmobile.core.view.UstadView.Companion.ARG_ENTITY_UID
import com.ustadmobile.core.view.UstadView.Companion.ARG_FILTER_BY_CLAZZUID
import kotlinx.serialization.json.Json
import org.kodein.di.DI


typealias ClazzPermissionChecker = suspend (db: UmAppDatabase, personUid: Long, clazzUid: Long) -> Boolean

class ClazzDetailPresenter(context: Any,
                           arguments: Map<String, String>, view: ClazzDetailView,
                           lifecycleOwner: DoorLifecycleOwner,
                           di: DI)
    : UstadDetailPresenter<ClazzDetailView, Clazz>(context, arguments, view, lifecycleOwner, di) {

    override val persistenceMode: PersistenceMode
        get() = PersistenceMode.DB

    /*
     * TODO: Add any required one to many join helpers here - use these templates (type then hit tab)
     * onetomanyhelper: Adds a one to many relationship using OneToManyJoinEditHelper
     */
    override fun onCreate(savedState: Map<String, String>?) {
        super.onCreate(savedState)

        //TODO: Set any additional fields (e.g. joinlist) on the view
    }

    override suspend fun onCheckEditPermission(account: UmAccount?): Boolean {
        return true
    }

    override fun onLoadFromJson(bundle: Map<String, String>): Clazz? {
        super.onLoadFromJson(bundle)

        val entityJsonStr = bundle[ARG_ENTITY_JSON]
        var editEntity: Clazz? = null
        if(entityJsonStr != null) {
            editEntity = Json.parse(Clazz.serializer(), entityJsonStr)
        }else {
            editEntity = Clazz()
        }

        val activePersonUid = activeAccount.getValue()?.personUid ?: 0L

        val entityUid = editEntity.clazzUid

        GlobalScope.launch(Dispatchers.Main) {
            view.tabs = listOf("${ClazzDetailOverviewView.VIEW_NAME}?$ARG_ENTITY_UID=$entityUid",
                    "${ClazzMemberListView.VIEW_NAME}?$ARG_FILTER_BY_CLAZZUID=$entityUid") +
                    CLAZZ_FEATURES.filter {
                        PERMISSION_CHECKER_MAP[it]?.invoke(db, activePersonUid, entityUid) ?: false
                    }.map {
                        (VIEWNAME_MAP[it] ?: "INVALID}") + "?$ARG_FILTER_BY_CLAZZUID=$entityUid"
                    }
        }
        return editEntity
    }

    override suspend fun onLoadEntityFromDb(db: UmAppDatabase): Clazz? {
        val entityUid = arguments[ARG_ENTITY_UID]?.toLong() ?: 0L
        val clazz = withTimeoutOrNull(2000) {
             db.clazzDao.findByUid(entityUid)
        } ?: Clazz()

        val activePersonUid = activeAccount.getValue()?.personUid ?: 0L

        view.tabs = listOf("${ClazzDetailOverviewView.VIEW_NAME}?$ARG_ENTITY_UID=$entityUid",
                "${ClazzMemberListView.VIEW_NAME}?$ARG_FILTER_BY_CLAZZUID=$entityUid") +
                CLAZZ_FEATURES.filter {
                PERMISSION_CHECKER_MAP[it]?.invoke(db, activePersonUid, entityUid) ?: false
        }.map { (VIEWNAME_MAP[it] ?: "INVALID}") + "?$ARG_FILTER_BY_CLAZZUID=$entityUid"}

        return clazz
    }

    companion object {

        val CLAZZ_FEATURES = listOf(Clazz.CLAZZ_FEATURE_ATTENDANCE)

        val PERMISSION_CHECKER_MAP = mapOf<Long, ClazzPermissionChecker>(
                Clazz.CLAZZ_FEATURE_ATTENDANCE to {db, personUid, clazzUid ->
                    true /*db.clazzDao.personHasPermissionWithClazz(personUid, clazzUid, Role.PERMISSION_CLAZZ_LOG_ATTENDANCE_SELECT)*/
                }
        )

        val VIEWNAME_MAP = mapOf<Long, String>(
                Clazz.CLAZZ_FEATURE_ATTENDANCE to ClazzLogListAttendanceView.VIEW_NAME
        )
    }

}