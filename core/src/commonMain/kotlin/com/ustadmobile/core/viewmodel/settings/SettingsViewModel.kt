package com.ustadmobile.core.viewmodel.settings

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.ustadmobile.core.impl.nav.UstadSavedStateHandle
import com.ustadmobile.core.viewmodel.UstadViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.kodein.di.DI
import com.ustadmobile.core.MR
import com.ustadmobile.core.db.PermissionFlags
import com.ustadmobile.core.domain.getversion.GetVersionUseCase
import com.ustadmobile.core.domain.htmlcontentdisplayengine.GetHtmlContentDisplayEngineOptionsUseCase
import com.ustadmobile.core.domain.htmlcontentdisplayengine.GetHtmlContentDisplayEngineUseCase
import com.ustadmobile.core.domain.htmlcontentdisplayengine.HtmlContentDisplayEngineOption
import com.ustadmobile.core.domain.htmlcontentdisplayengine.SetHtmlContentDisplayEngineUseCase
import com.ustadmobile.core.domain.language.SetLanguageUseCase
import com.ustadmobile.core.impl.UstadMobileSystemCommon
import com.ustadmobile.core.impl.appstate.Snack
import com.ustadmobile.core.impl.config.SupportedLanguagesConfig
import com.ustadmobile.core.viewmodel.deleteditem.DeletedItemListViewModel
import com.ustadmobile.core.viewmodel.settings.DeveloperSettingsViewModel.Companion.PREFKEY_DEVSETTINGS_ENABLED
import com.ustadmobile.core.viewmodel.site.detail.SiteDetailViewModel
import kotlinx.atomicfu.atomic
import org.kodein.di.instance
import org.kodein.di.instanceOrNull

data class SettingsUiState(

    val htmlContentDisplayOptions: List<HtmlContentDisplayEngineOption> = emptyList(),

    val currentHtmlContentDisplayOption: HtmlContentDisplayEngineOption? = null,

    val holidayCalendarVisible: Boolean = false,

    val workspaceSettingsVisible: Boolean = false,

    val reasonLeavingVisible: Boolean = false,

    val langDialogVisible: Boolean = false,

    val htmlContentDisplayDialogVisible: Boolean = false,

    val currentLanguage: String = "",

    val availableLanguages: List<UstadMobileSystemCommon.UiLanguage> = emptyList(),

    val waitForRestartDialogVisible: Boolean = false,

    val showDeveloperOptions: Boolean = false,

    val version: String = "",

) {
    val htmlContentDisplayEngineVisible: Boolean
        get() = htmlContentDisplayOptions.isNotEmpty()

    val advancedSectionVisible: Boolean
        get() = htmlContentDisplayEngineVisible

}

class SettingsViewModel(
    di: DI,
    savedStateHandle: UstadSavedStateHandle,
) : UstadViewModel(di, savedStateHandle, DEST_NAME){

    private val _uiState = MutableStateFlow(SettingsUiState())

    val uiState: Flow<SettingsUiState> = _uiState.asStateFlow()

    private val supportedLangConfig: SupportedLanguagesConfig by instance()

    private val setLanguageUseCase: SetLanguageUseCase by instance()

    private val availableLangs = supportedLangConfig.supportedUiLanguagesAndSysDefault(systemImpl)

    private val getHtmlContentDisplayOptsUseCase: GetHtmlContentDisplayEngineOptionsUseCase? by instanceOrNull()

    private val getHtmlContentDisplaySettingUseCase: GetHtmlContentDisplayEngineUseCase? by instanceOrNull()

    private val setHtmlContentDisplaySettingUseCase: SetHtmlContentDisplayEngineUseCase? by instanceOrNull()

    private val getVersionUseCase: GetVersionUseCase by instance()

    private val versionClickCount = atomic(0)

    private val settings: Settings by instance()

    init {
        _appUiState.update { prev ->
            prev.copy(
                title = systemImpl.getString(MR.strings.settings),
                hideBottomNavigation = true,
            )
        }

        val langSetting = supportedLangConfig.localeSetting ?: UstadMobileSystemCommon.LOCALE_USE_SYSTEM

        val currentLang = availableLangs.first {
            it.langCode == langSetting
        }

        _uiState.update { prev ->
            prev.copy(
                currentLanguage = currentLang.langDisplay,
                availableLanguages = availableLangs,
                htmlContentDisplayOptions = getHtmlContentDisplayOptsUseCase?.invoke() ?: emptyList(),
                currentHtmlContentDisplayOption = getHtmlContentDisplaySettingUseCase?.invoke(),
                version = getVersionUseCase().versionString,
                showDeveloperOptions = settings.getBoolean(PREFKEY_DEVSETTINGS_ENABLED, false)
            )
        }

        viewModelScope.launch {
            activeRepo.systemPermissionDao.personHasSystemPermissionAsFlow(
                activeUserPersonUid, PermissionFlags.MANAGE_SITE_SETTINGS
            ).collect { siteAdminSettingsVisible ->
                _uiState.update { prev ->
                    prev.copy(workspaceSettingsVisible = siteAdminSettingsVisible)
                }
            }
        }

    }

    fun onClickLanguage() {
        _uiState.update { prev ->
            prev.copy(
                langDialogVisible = true
            )
        }
    }

    fun onClickHtmlContentDisplayEngine() {
        _uiState.update { prev ->
            prev.copy(
                htmlContentDisplayDialogVisible = true,
            )
        }
    }

    fun onDismissHtmlContentDisplayEngineDialog() {
        _uiState.update { prev ->
            prev.copy(
                htmlContentDisplayDialogVisible = false,
            )
        }
    }

    fun onClickHtmlContentDisplayEngineOption(option: HtmlContentDisplayEngineOption) {
        setHtmlContentDisplaySettingUseCase?.invoke(option)
        _uiState.update { prev ->
            prev.copy(
                currentHtmlContentDisplayOption = option,
                htmlContentDisplayDialogVisible = false,
            )
        }

    }

    fun onClickLang(lang: UstadMobileSystemCommon.UiLanguage) {
        _uiState.update { prev ->
            prev.copy(langDialogVisible = false)
        }

        val result = setLanguageUseCase(
            uiLang = lang,
            currentDestination = DEST_NAME,
            navController = navController
        )

        if(result.waitForRestart) {
            _uiState.update { prev ->
                prev.copy(
                    waitForRestartDialogVisible = true,
                )
            }
        }else {
            _uiState.update { prev ->
                prev.copy(
                    currentLanguage = lang.langDisplay
                )
            }
        }
    }

    fun onDismissLangDialog() {
        _uiState.update { prev ->
            prev.copy(langDialogVisible = false)
        }
    }

    fun onClickSiteSettings() {
        navController.navigate(SiteDetailViewModel.DEST_NAME, emptyMap())
    }

    fun onClickDeveloperOptions() {
        navController.navigate(DeveloperSettingsViewModel.DEST_NAME, emptyMap())
    }

    fun onClickDeletedItems() {
        navController.navigate(DeletedItemListViewModel.DEST_NAME, emptyMap())
    }

    fun onClickVersion() {
        if(_uiState.value.showDeveloperOptions)
            return

        val newClickCount = versionClickCount.incrementAndGet()
        if(newClickCount >= 7){
            settings[PREFKEY_DEVSETTINGS_ENABLED] = true
            _uiState.update { prev ->
                prev.copy(showDeveloperOptions = true)
            }
            snackDispatcher.showSnackBar(Snack("Developer options enabled"))
        }
    }


    companion object {
        const val DEST_NAME = "Settings"
    }


}
