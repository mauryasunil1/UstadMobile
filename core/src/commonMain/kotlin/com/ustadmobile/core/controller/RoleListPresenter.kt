package com.ustadmobile.core.controller

import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.util.MessageIdOption
import com.ustadmobile.core.view.ListViewMode
import com.ustadmobile.core.view.RoleEditView
import com.ustadmobile.core.view.RoleListView
import com.ustadmobile.core.view.UstadView
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.lib.db.entities.Role
import com.ustadmobile.lib.db.entities.UmAccount
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.kodein.di.DI

class RoleListPresenter(context: Any, arguments: Map<String, String>, view: RoleListView,
                        lifecycleOwner: DoorLifecycleOwner, di: DI)
    : UstadListPresenter<RoleListView, Role>(context, arguments, view, lifecycleOwner, di) {

    var currentSortOrder: SortOrder = SortOrder.ORDER_NAME_ASC

    enum class SortOrder(val messageId: Int) {
        ORDER_NAME_ASC(MessageID.sort_by_name_asc),
        ORDER_NAME_DSC(MessageID.sort_by_name_desc)
    }

    class RoleListSortOption(val sortOrder: SortOrder, context: Any)
        : MessageIdOption(sortOrder.messageId, context)

    override fun onCreate(savedState: Map<String, String>?) {
        super.onCreate(savedState)
        updateListOnView()
        view.sortOptions = SortOrder.values().toList().map { RoleListSortOption(it, context) }
    }

    override suspend fun onCheckAddPermission(account: UmAccount?): Boolean {
        //TODO: Get permission for this access.
        return true
    }

    private fun updateListOnView() {
        view.list = when(currentSortOrder) {
            SortOrder.ORDER_NAME_ASC -> repo.roleDao.findAllActiveRoles()
            SortOrder.ORDER_NAME_DSC -> repo.roleDao.findAllActiveRoles()
        }
    }

    override fun handleClickEntry(entry: Role) {
        when(mListMode) {
            ListViewMode.PICKER -> view.finishWithResult(listOf(entry))
            ListViewMode.BROWSER -> systemImpl.go(RoleEditView.VIEW_NAME,
                mapOf(UstadView.ARG_ENTITY_UID to entry.roleUid.toString()), context)
        }
    }

    override fun handleClickCreateNewFab() {
        systemImpl.go(RoleEditView.VIEW_NAME, mapOf(), context)
    }

    override fun handleClickSortOrder(sortOption: MessageIdOption) {
        val sortOrder = (sortOption as? RoleListSortOption)?.sortOrder ?: return
        if(sortOrder != currentSortOrder) {
            currentSortOrder = sortOrder
            updateListOnView()
        }
    }

    fun handleRemoveRole(role:Role){
        GlobalScope.launch {
            role.roleActive = false
            repo.roleDao.updateAsync(role)
        }
    }
}