package com.ustadmobile.core.viewmodel

import com.ustadmobile.core.account.UstadAccountManager
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.impl.UstadMobileSystemCommon
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.impl.appstate.AppUiState
import com.ustadmobile.core.impl.appstate.LoadingUiState
import com.ustadmobile.core.impl.appstate.SnackBarDispatcher
import com.ustadmobile.core.impl.nav.*
import com.ustadmobile.core.util.UMFileUtil
import com.ustadmobile.core.view.UstadEditView
import com.ustadmobile.core.view.UstadView
import com.ustadmobile.core.view.UstadView.Companion.ARG_RESULT_DEST_KEY
import com.ustadmobile.core.view.UstadView.Companion.ARG_RESULT_DEST_VIEWNAME
import com.ustadmobile.door.ext.DoorTag
import com.ustadmobile.door.util.systemTimeInMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import org.kodein.di.*

/**
 * @param di the KodeIn DI
 * @param savedStateHandle the SavedStateHandle
 * @param destinationName The name of this destination as per the navigation view stack, normally as
 * per the related VIEW_NAME. This might NOT be the VIEW_NAME that relates to this screen e.g. when
 * this ViewModel is being used within a tab or other component that is not directly part of the
 * navigation.
 */
abstract class UstadViewModel(
    override val di: DI,
    protected val savedStateHandle: UstadSavedStateHandle,
    protected val destinationName: String,
): ViewModel(savedStateHandle), DIAware {

    protected val navController = CommandFlowUstadNavController()

    val navCommandFlow = navController.commandFlow

    protected val _appUiState = MutableStateFlow(AppUiState())

    val appUiState: Flow<AppUiState> = _appUiState.asStateFlow()

    protected val accountManager: UstadAccountManager by instance()

    protected val activeDb: UmAppDatabase by on(accountManager.activeEndpoint)
        .instance(tag = DoorTag.TAG_DB)

    protected val activeRepo: UmAppDatabase by on(accountManager.activeEndpoint)
        .instance(tag = DoorTag.TAG_REPO)

    protected val navResultReturner: NavResultReturner by instance()

    protected val json: Json by instance()

    protected val snackDispatcher: SnackBarDispatcher by instance()

    protected val resultReturner: NavResultReturner by instance()

    protected val systemImpl: UstadMobileSystemImpl by instance()

    private var lastNavResultTimestampCollected: Long = savedStateHandle[KEY_LAST_COLLECTED_TS]?.toLong() ?: 0L
        set(value) {
            field = value
            savedStateHandle[KEY_LAST_COLLECTED_TS] = value.toString()
        }

    /**
     * If navigation for a result is in progress, this will be non-null
     */
    protected val expectedResultDest: NavResultDest?
        get()  {
            val popUpToViewName = savedStateHandle[ARG_RESULT_DEST_VIEWNAME]
            val saveToKey = savedStateHandle[ARG_RESULT_DEST_KEY]
            return if(popUpToViewName != null && saveToKey != null) {
                NavResultDest(popUpToViewName, saveToKey)
            }else {
                null
            }
        }

    init {
        if(lastNavResultTimestampCollected == 0L)
            lastNavResultTimestampCollected = systemTimeInMillis()
    }
    /**
     * Shorthand to make it easier to update the loading state
     */
    protected var loadingState: LoadingUiState
        get() = _appUiState.value.loadingState
        set(value) {
            _appUiState.update {
                it.copy(loadingState = value)
            }
        }

    /**
     * Shorthand to set the title
     */
    protected var title: String?
        get() = _appUiState.value.title
        set(value) {
            _appUiState.update {
                it.copy(title = value)
            }
        }

    /**
     * Shorthand to observe results. Avoids two edge cases:
     *
     * 1. "Replay" - when the ViewModel is recreated, if no other result has been returned in the
     *    meantime, the last result would be collected again. The flow of NavResultReturner always
     *    replays the most recent result returned (required to allow a collector which starts after
     *    the result was sent to collect it).
     *
     *    This is avoided by tracking the timestamp of the last item collected.
     *
     * 2. Replay from previous viwemodel: when the user goes from screen A to screen B, then C,
     *    returns a result to screen A, and then navigates forward to screen B again with new arguments.
     *    The new instance of screen B does not remember receiving any results, so the result from
     *    the old instance of screen C looks new.
     *
     *    This is avoided by setting the alstNavResultTimestampCollected to the first start time
     *    on init.
     *
     */
    fun NavResultReturner.filteredResultFlowForKey(
        key: String,
    ) : Flow<NavResult> {
        return resultFlowForKey(key).filter {
            val isNew = it.timestamp > lastNavResultTimestampCollected
            if(isNew)
                lastNavResultTimestampCollected = it.timestamp

            isNew
        }
    }

    protected suspend fun <T> UstadSavedStateHandle.getJson(
        key: String,
        deserializer: DeserializationStrategy<T>
    ): T? {
        val jsonStr = get(key)
        return if(jsonStr != null) {
            withContext(Dispatchers.Default) {
                json.decodeFromString(deserializer, jsonStr)
            }
        }else {
            null
        }
    }

    protected suspend fun <T> UstadSavedStateHandle.setJson(
        key: String,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        val jsonStr = withContext(Dispatchers.Default){
            json.encodeToString(serializer, value)
        }
        set(key, jsonStr)
    }

    /**
     * Parse the query parameters (if any). this is a placeholder, will be removed when the authenticator
     * branch is merged.
     */
    protected fun UstadNavController.navigateToViewUri(
        viewUri: String,
        goOptions: UstadMobileSystemCommon.UstadGoOptions = UstadMobileSystemCommon.UstadGoOptions.Default,
    ) {
        val viewName = viewUri.substringBefore("?")
        val args = if(viewName.contains("?")) {
            UMFileUtil.parseURLQueryString(viewUri)
        }else {
            mapOf()
        }

        navigate(viewName, args, goOptions)

    }

    /**
     * Return a result to the screen that is expecting it, if any. See CODING-STYLE.md README for an
     * overview of how this works.
     *
     * @param result: the result that is being provided (e.g. selected Person etc)
     */
    protected fun finishWithResult(result: Any?) {
        val resultDest = expectedResultDest
        if(resultDest != null) {
            navResultReturner.sendResult(NavResult(resultDest.key, systemTimeInMillis(), result))
            navController.popBackStack(resultDest.viewName, false)
        }else {
            navController.popBackStack(UstadView.CURRENT_DEST, true)
        }
    }


    fun <T> navigateForResult(
        nextViewName: String,
        key: String,
        currentValue: T?,
        serializer: SerializationStrategy<T>,
        args: Map<String, String> = emptyMap(),
        goOptions: UstadMobileSystemCommon.UstadGoOptions = UstadMobileSystemCommon.UstadGoOptions.Default,
        overwriteDestination: Boolean = (this is UstadEditViewModel),
    ) {
        val navArgs = args.toMutableMap()

        if(!args.containsKey(UstadView.ARG_RESULT_DEST_KEY) || overwriteDestination)
            navArgs[UstadView.ARG_RESULT_DEST_KEY] = key

        if(!args.containsKey(UstadView.ARG_RESULT_DEST_VIEWNAME) || overwriteDestination)
            navArgs[UstadView.ARG_RESULT_DEST_VIEWNAME] = destinationName

        if(currentValue != null) {
            navArgs[UstadEditView.ARG_ENTITY_JSON] = json.encodeToString(serializer, currentValue)
        }

        navController.navigate(nextViewName, navArgs.toMap(), goOptions)
    }


    companion object {
        /**
         * Saved state key for the current value of the entity itself. This is different to
         * ARG_ENTITY_JSON which provides a starting value
         */
        const val KEY_ENTITY_STATE = "entityState"

        const val KEY_LAST_COLLECTED_TS = "collectedTs"

        const val KEY_INIT_STATE = "initState"

        /**
         * Used to store the time that the viwemodel has first initialized. This
         */
        const val KEY_FIRST_INIT_TIME = "firstInit"
    }

}