package com.ustadmobile.core.controller

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.view.ClazzWorkDetailView
import com.ustadmobile.core.view.ClazzWorkEditView
import com.ustadmobile.core.view.UstadView.Companion.ARG_ENTITY_UID
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.lib.db.entities.ClazzMember
import com.ustadmobile.lib.db.entities.ClazzWork
import com.ustadmobile.lib.db.entities.UmAccount
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.kodein.di.DI


class ClazzWorkDetailPresenter(context: Any,
                          arguments: Map<String, String>, view: ClazzWorkDetailView,
                           di: DI, lifecycleOwner: DoorLifecycleOwner)
    : UstadDetailPresenter<ClazzWorkDetailView, ClazzWork>(context, arguments, view, di, lifecycleOwner) {

    override val persistenceMode: PersistenceMode
        get() = PersistenceMode.DB

    override suspend fun onLoadEntityFromDb(db: UmAppDatabase): ClazzWork? {
        val entityUid = arguments[ARG_ENTITY_UID]?.toLong() ?: 0L

        val clazzWork = withTimeoutOrNull(2000) {
            db.clazzWorkDao.findByUidAsync(entityUid)
        } ?: ClazzWork()

        view.clazzWorkTitle = clazzWork.clazzWorkTitle

        return clazzWork
    }

    override fun onCreate(savedState: Map<String, String>?) {
        super.onCreate(savedState)

        val loggedInPersonUid = accountManager.activeAccount.personUid

        GlobalScope.launch {
            val clazzMember: ClazzMember? = withTimeoutOrNull(2000) {
                db.clazzMemberDao.findByPersonUidAndClazzUid(loggedInPersonUid,
                        entity?.clazzWorkClazzUid?: 0L)
            }
            view.isStudent = (clazzMember != null && clazzMember.clazzMemberRole == ClazzMember.ROLE_STUDENT)
        }

    }

    override suspend fun onCheckEditPermission(account: UmAccount?): Boolean {
        //TODO: this
        return true
    }

    override fun handleClickEdit() {
        val entityUid = arguments[ARG_ENTITY_UID]?.toLong() ?: 0L
        systemImpl.go(ClazzWorkEditView.VIEW_NAME, mapOf(ARG_ENTITY_UID to entityUid.toString()),
                context)
    }
}