package com.ustadmobile.core.viewmodel.contententry.detail

import com.ustadmobile.core.impl.appstate.TabItem
import com.ustadmobile.core.impl.nav.UstadSavedStateHandle
import com.ustadmobile.core.view.UstadView
import com.ustadmobile.core.viewmodel.DetailViewModel
import com.ustadmobile.core.viewmodel.contententry.detailoverviewtab.ContentEntryDetailOverviewViewModel
import com.ustadmobile.lib.db.entities.Clazz
import kotlinx.coroutines.flow.MutableStateFlow
import org.kodein.di.DI
import com.ustadmobile.core.MR
import kotlinx.coroutines.flow.asStateFlow

data class ContentEntryDetailUiState(
    val tabs: List<TabItem> = emptyList()
)

class ContentEntryDetailViewModel(
    di: DI,
    savedStateHandle: UstadSavedStateHandle,
) : DetailViewModel<Clazz>(di, savedStateHandle, ContentEntryDetailOverviewViewModel.DEST_NAME){

    private val _uiState = MutableStateFlow(
        ContentEntryDetailUiState(
            listOf(
                TabItem(
                    viewName = ContentEntryDetailOverviewViewModel.DEST_NAME,
                    args = mapOf(UstadView.ARG_ENTITY_UID to entityUidArg.toString()),
                    label = systemImpl.getString(MR.strings.overview)
                )
            )
        )
    )

    val uiState = _uiState.asStateFlow()


    companion object {
        const val DEST_NAME = "ContentEntry"
    }

}