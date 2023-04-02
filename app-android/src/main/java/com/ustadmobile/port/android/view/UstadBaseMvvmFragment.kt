package com.ustadmobile.port.android.view

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.toughra.ustadmobile.R
import com.ustadmobile.core.impl.appstate.AppUiState
import com.ustadmobile.core.impl.appstate.FabUiState
import com.ustadmobile.core.impl.appstate.LoadingUiState
import com.ustadmobile.core.viewmodel.UstadViewModel
import com.ustadmobile.port.android.view.util.UstadActivityWithProgressBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.android.x.closestDI
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.navGraphViewModels
import com.ustadmobile.core.impl.nav.*
import com.ustadmobile.port.android.impl.ViewNameToDestMap
import kotlinx.coroutines.flow.map
import org.kodein.di.*


/**
 * This Fragment is designed to contain Jetpack compose content.
 */
abstract class UstadBaseMvvmFragment: Fragment(), DIAware {

    private val navResultReturnerViewModel: NavResultReturnerViewModel by navGraphViewModels(R.id.mobile_navigation) {
        NavResultReturnerViewModelFactory(requireActivity(), arguments)
    }


    /**
     * Shortcut to use the UstadViewModelProviderFactory and reduce boilerplate
     *
     * @param lookupDestinationName Some fragments are used by multiple destination ids on the
     * navigation graph. In this case the destinationName must be looked up, otherwise this is
     * not needed
     */
    inline fun <reified VM: ViewModel> ustadViewModels(
        lookupDestinationName: Boolean = false
    ): Lazy<VM> = viewModels {
        val destinationName = if(lookupDestinationName) {
            requireDestinationViewName()
        }else {
            null
        }


        UstadViewModelProviderFactory(
            di = di,
            owner = this,
            defaultArgs = arguments,
            destinationName = destinationName
        )
    }

    override val di by DI.lazy {
        val closestDi: DI by closestDI()
        extend(closestDi)
        bind<NavResultReturner>() with singleton {
            navResultReturnerViewModel
        }
    }

    private val navCommandExecTracker: NavCommandExecutionTracker by instance()

    class AppUiStateMenuProvider(
        initAppUiState: AppUiState?
    ): MenuProvider, SearchView.OnQueryTextListener, SearchView.OnCloseListener {

        private var searchView: SearchView? = null

        var appUiState: AppUiState? = initAppUiState
            set(value) {
                field = value

                val searchViewVal = searchView ?: return
                if(value != null && value.searchState.searchText != searchViewVal.query)
                    searchViewVal.setQuery(value.searchState.searchText, false)
            }

        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_done, menu)
            val buttonMenuItem = menu.findItem(R.id.menu_done)
            val appStateVal = appUiState ?: return

            buttonMenuItem.title = appStateVal.actionBarButtonState.text ?: ""
            buttonMenuItem.isVisible = appStateVal.actionBarButtonState.visible

            val searchMenuItem = menu.findItem(R.id.menu_search)
            searchMenuItem.isVisible = appStateVal.searchState.visible
            searchView = (searchMenuItem?.actionView as SearchView).also {
                it.setOnQueryTextListener(this)
                it.setOnCloseListener(this)
                it.setQuery(appStateVal.searchState.searchText, false)
                it.isIconified = appStateVal.searchState.searchText == ""
            }
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            if(menuItem.itemId == R.id.menu_done) {
                appUiState?.actionBarButtonState?.onClick?.invoke()
                return true
            }

            return false
        }

        override fun onQueryTextSubmit(query: String?): Boolean {
            appUiState?.searchState?.onSearchTextChanged?.invoke(query ?: "")
            return false
        }

        override fun onQueryTextChange(newText: String?): Boolean {
            appUiState?.searchState?.onSearchTextChanged?.invoke(newText ?: "")
            return false
        }

        override fun onClose(): Boolean {
            appUiState?.searchState?.onSearchTextChanged?.invoke("")
            return false
        }

        fun detach() {
            searchView?.setOnQueryTextListener(null)
            searchView?.setOnCloseListener(null)
        }
    }

    private var mMenuProvider: AppUiStateMenuProvider? = null

    /**
     * Collect and run navigation commands when this fragment is active.
     */
    fun CoroutineScope.launchNavigatorCollector(viewModel: UstadViewModel) {
        launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.navCommandFlow.collect {
                    navCommandExecTracker.runIfNotExecutedOrTimedOut(it) { cmd ->
                        when(cmd) {
                            is NavigateNavCommand -> {
                                //if this is run directly (without launch) then we will get a memory leak
                                launch {
                                    delay(20)
                                    val navAdapter = NavControllerAdapter(findNavController(),
                                        direct.instance())
                                    navAdapter.navigate(cmd.viewName, cmd.args, cmd.goOptions)
                                }
                            }
                            is PopNavCommand -> {
                                launch {
                                    val navAdapter = NavControllerAdapter(findNavController(),
                                        direct.instance())
                                    navAdapter.popBackStack(cmd.viewName, cmd.inclusive)
                                }
                            }
                            else -> {
                                //do nothing
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Collect the AppUiState flow from the given ViewModel, then apply it as required to the AppBar,
     * floating action button, etc.
     *
     * The collection only runs when the fragment is in the resumed state.
     */
    fun CoroutineScope.launchAppUiStateCollector(
        viewModel: UstadViewModel,
        transform: (AppUiState) -> AppUiState = { it },
    ) {
        mMenuProvider = AppUiStateMenuProvider(null).also {
            requireActivity().addMenuProvider(it, viewLifecycleOwner)
        }

        launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.appUiState.map(transform).collect { appUiState ->
                    (activity as? AppCompatActivity)?.supportActionBar
                        ?.takeIf { it.title != appUiState.title }
                        ?.title = appUiState.title

                    val progressBar = (activity as? UstadActivityWithProgressBar)?.activityProgressBar
                    progressBar?.visibility = if(appUiState.loadingState.loadingState == LoadingUiState.State.INDETERMINATE){
                        View.VISIBLE
                    }else {
                        View.GONE
                    }

                    val fab = (activity as? UstadActivityWithFab)?.activityFloatingActionButton
                    fab.takeIf { it?.text != appUiState.fabState.text }?.text = appUiState.fabState.text

                    if(fab != null && fab.getTag(R.id.tag_fab_uistate_icon) != appUiState.fabState.icon) {
                        fab.icon = if(appUiState.fabState.icon != FabUiState.FabIcon.NONE) {
                            ContextCompat.getDrawable(requireContext(),
                                ICON_MAP[appUiState.fabState.icon] ?: 0)
                        }else{
                            null
                        }

                        fab.setTag(R.id.tag_fab_uistate_icon, appUiState.fabState.icon)
                    }

                    //If this is set by anything else, then the tag could be out of sync with the actual value,
                    // so can't rely on tab until presenter based screens are removed.
                    if(fab != null) {
                        fab.setOnClickListener {
                            appUiState.fabState.onClick()
                        }
                        fab.setTag(R.id.tag_fab_uistate_listener, appUiState.fabState.onClick)
                    }

                    val fabVisibility = if(appUiState.fabState.visible) {
                        View.VISIBLE
                    }else {
                        View.GONE
                    }

                    fab?.takeIf { it.visibility != fabVisibility }?.visibility = fabVisibility


                    val currentActionBarState = mMenuProvider?.appUiState?.actionBarButtonState
                    val currentSearchBarState = mMenuProvider?.appUiState?.searchState
                    val newActionBarState = appUiState.actionBarButtonState
                    val newSearchBarState = mMenuProvider?.appUiState?.searchState
                    if(mMenuProvider?.appUiState != appUiState) {
                        mMenuProvider?.appUiState = appUiState
                        if(
                            currentActionBarState == null ||
                            currentSearchBarState == null ||
                            newActionBarState.visible != currentActionBarState.visible ||
                            newActionBarState.enabled != currentActionBarState.enabled ||
                            newActionBarState.text != currentActionBarState.text ||
                            newSearchBarState?.visible != currentSearchBarState.visible
                        ) {
                            requireActivity().invalidateMenu()
                        }
                    }
                }
            }
        }
    }

    /**
     * Some screens are used by multiple view names (e.g. for Android to recognize them as part of
     * bottom navigation etc). In these cases, the viewname must be explicitly provided
     */
    fun requireDestinationViewName() : String {
        val currentDest = findNavController().currentDestination
            ?: throw IllegalStateException("No current destination")
        return ViewNameToDestMap().lookupViewNameById(currentDest.id)
            ?: throw IllegalArgumentException("Could not find viewname for $currentDest")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mMenuProvider = null
    }

    companion object {

        val ICON_MAP = mapOf(
            FabUiState.FabIcon.EDIT to R.drawable.ic_baseline_edit_24,
            FabUiState.FabIcon.ADD to R.drawable.ic_add_white_24dp,
            FabUiState.FabIcon.NONE to 0,
        )

    }
}