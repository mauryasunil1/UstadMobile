package com.ustadmobile.libuicompose.view.contententry.edit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.ustadmobile.core.MR
import com.ustadmobile.core.impl.locale.entityconstants.LicenceConstants
import com.ustadmobile.core.viewmodel.contententry.edit.ContentEntryEditUiState
import com.ustadmobile.core.viewmodel.contententry.edit.ContentEntryEditViewModel
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.CourseBlock
import com.ustadmobile.lib.db.entities.ext.shallowCopy
import com.ustadmobile.libuicompose.components.UstadMessageIdOptionExposedDropDownMenuField
import com.ustadmobile.libuicompose.components.UstadRichTextEdit
import com.ustadmobile.libuicompose.components.UstadVerticalScrollColumn
import com.ustadmobile.libuicompose.util.ext.defaultItemPadding
import dev.icerock.moko.resources.compose.stringResource

@Composable
fun ContentEntryEditScreen(
    viewModel: ContentEntryEditViewModel
){
    val uiState by viewModel.uiState.collectAsState(ContentEntryEditUiState())

    ContentEntryEditScreen(
        uiState = uiState,
        onContentEntryChanged = viewModel::onContentEntryChanged,
        onClickEditDescription = viewModel::onEditDescriptionInNewWindow,
    )
}

@Composable
fun ContentEntryEditScreen(
    uiState: ContentEntryEditUiState = ContentEntryEditUiState(),
    onCourseBlockChange: (CourseBlock?) -> Unit = {},
    onClickEditDescription: () -> Unit = { },
    onClickUpdateContent: () -> Unit = { },
    onContentEntryChanged: (ContentEntry?) -> Unit = {},
) {
    UstadVerticalScrollColumn(
        modifier = Modifier.fillMaxSize()
    )  {

        val updateContentText =
            if (!uiState.importError.isNullOrBlank())
                stringResource(MR.strings.file_required_prompt)
            else
                stringResource(MR.strings.file_selected)

        if (uiState.updateContentVisible){
            Button(
                onClick = onClickUpdateContent,
                modifier = Modifier
                    .defaultItemPadding()
                    .fillMaxWidth(),
            ) {
                Text(stringResource(MR.strings.update_content))
            }

            Text(
                modifier = Modifier.defaultItemPadding(),
                text = updateContentText
            )
        }

        OutlinedTextField(
            modifier = Modifier.testTag("title")
                .defaultItemPadding()
                .fillMaxWidth(),
            value = uiState.entity?.entry?.title ?: "",
            label = { Text(stringResource(MR.strings.title) + "*") },
            isError = uiState.titleError != null,
            enabled = uiState.fieldsEnabled,
            singleLine = true,
            onValueChange = {
                onContentEntryChanged(
                    uiState.entity?.entry?.shallowCopy {
                        title = it
                    }
                )
            },
            supportingText = {
                Text(uiState.titleError ?: stringResource(MR.strings.required))
            }
        )

        UstadRichTextEdit(
            html = uiState.entity?.entry?.description ?: "",
            modifier = Modifier.fillMaxWidth().defaultItemPadding()
                .testTag("description"),
            onHtmlChange = {
                onContentEntryChanged(
                    uiState.entity?.entry?.shallowCopy {
                        description = it
                    }
                )
            },
            onClickToEditInNewScreen = onClickEditDescription,
            editInNewScreenLabel = stringResource(MR.strings.description),
            placeholderText = stringResource(MR.strings.description),
        )

        OutlinedTextField(
            modifier = Modifier.testTag("author").fillMaxWidth().defaultItemPadding(),
            value = uiState.entity?.entry?.author ?: "",
            label = { Text(stringResource(MR.strings.entry_details_author)) },
            enabled = uiState.fieldsEnabled,
            onValueChange = {
                onContentEntryChanged(
                    uiState.entity?.entry?.shallowCopy {
                        author = it
                    }
                )
            }
        )

        OutlinedTextField(
            modifier = Modifier.testTag("publisher").fillMaxWidth().defaultItemPadding(),
            value = uiState.entity?.entry?.publisher ?: "",
            label = { Text(stringResource(MR.strings.entry_details_publisher)) },
            enabled = uiState.fieldsEnabled,
            onValueChange = {
                onContentEntryChanged(
                    uiState.entity?.entry?.shallowCopy {
                        publisher = it
                    }
                )
            }
        )

        UstadMessageIdOptionExposedDropDownMenuField(
            modifier = Modifier.testTag("license").defaultItemPadding(),
            value = uiState.entity?.entry?.licenseType ?: 0,
            options = LicenceConstants.LICENSE_MESSAGE_IDS,
            label = stringResource(MR.strings.licence),
            enabled = uiState.fieldsEnabled,
            onOptionSelected = {
                onContentEntryChanged(
                    uiState.entity?.entry?.shallowCopy {
                        licenseType = it.value
                    }
                )
            }
        )
    }
}