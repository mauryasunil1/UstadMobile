package com.ustadmobile.libuicompose.view.site.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ustadmobile.core.MR
import com.ustadmobile.core.viewmodel.site.detail.SiteDetailUiState
import com.ustadmobile.core.viewmodel.site.detail.SiteDetailViewModel
import com.ustadmobile.lib.db.composites.SiteTermsAndLangName
import com.ustadmobile.libuicompose.components.UstadDetailField2
import com.ustadmobile.libuicompose.components.UstadDetailHeader
import com.ustadmobile.libuicompose.components.UstadVerticalScrollColumn
import com.ustadmobile.libuicompose.util.compose.yesNoStringResource
import dev.icerock.moko.resources.compose.stringResource
import moe.tlaster.precompose.flow.collectAsStateWithLifecycle

@Composable
fun SiteDetailScreen(
    viewModel: SiteDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(SiteDetailUiState())

    SiteDetailScreen(
        uiState = uiState,
        onClickTerms = viewModel::onClickTerms
    )
}

@Composable
fun SiteDetailScreen(
    uiState: SiteDetailUiState,
    onClickTerms: (SiteTermsAndLangName) -> Unit = {},
){

    UstadVerticalScrollColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        UstadDetailField2(
            valueText = uiState.site?.siteName ?: "",
            labelText = stringResource(MR.strings.name_key),
            icon = Icons.Default.DriveFileRenameOutline,
        )
        UstadDetailField2(
            valueText = yesNoStringResource(uiState.site?.guestLogin == true),
            labelText = stringResource(MR.strings.guest_login_enabled),
            icon = Icons.Default.Luggage,
        )
        UstadDetailField2(
            valueText = yesNoStringResource(uiState.site?.registrationAllowed == true),
            labelText = stringResource(MR.strings.registration_allowed),
            icon = Icons.Default.HowToReg,
        )

        UstadDetailHeader { Text(stringResource(MR.strings.terms_and_policies)) }

        uiState.siteTerms.forEach { siteTermsWithLanguage ->
            ListItem(
                modifier = Modifier.clickable {
                    onClickTerms(siteTermsWithLanguage)
                },
                leadingContent = {
                    Icon(Icons.Outlined.Article, contentDescription = null)
                },
                headlineContent = { Text(siteTermsWithLanguage.langDisplayName) },
            )
        }
    }
}
