package com.ustadmobile.libuicompose.view.courseterminology.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.ustadmobile.core.viewmodel.courseterminology.list.CourseTerminologyListUiState
import com.ustadmobile.core.viewmodel.courseterminology.list.CourseTerminologyListViewModel
import com.ustadmobile.lib.db.entities.CourseTerminology
import com.ustadmobile.core.MR
import com.ustadmobile.libuicompose.components.UstadLazyColumn
import dev.icerock.moko.resources.compose.stringResource

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CourseTerminologyListScreen(
    uiState: CourseTerminologyListUiState,
    onClickAddNewItem: () -> Unit = { },
    onClickItem: (CourseTerminology) -> Unit = { },
) {
    val pager = remember(uiState.terminologyList) {
        Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = true, maxSize = 200),
            pagingSourceFactory = uiState.terminologyList
        )
    }

    val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

    UstadLazyColumn(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth()
    ){
        if(uiState.showAddItemInList) {
            item {
                ListItem(
                    modifier = Modifier.clickable {
                        onClickAddNewItem()
                    },
                    headlineContent = { Text(stringResource(MR.strings.add_new_terminology)) },
                    leadingContent = {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    }
                )
            }
        }

        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it.ctUid }
        ) { index ->
            val terminology = lazyPagingItems[index]
            ListItem(
                modifier = Modifier.clickable {
                    terminology?.also { onClickItem(it) }
                },
                headlineContent = { Text(terminology?.ctTitle ?: "") },
                leadingContent = {
                    Spacer(modifier = Modifier.size(24.dp))
                }
            )
        }
    }
}

@Composable
fun CourseTerminologyListScreen(
    viewModel: CourseTerminologyListViewModel
) {
    val uiState: CourseTerminologyListUiState by viewModel.uiState.collectAsState(
        CourseTerminologyListUiState()
    )

    CourseTerminologyListScreen(
        uiState = uiState,
        onClickAddNewItem = viewModel::onClickAdd,
        onClickItem = viewModel::onClickEntry
    )

}

