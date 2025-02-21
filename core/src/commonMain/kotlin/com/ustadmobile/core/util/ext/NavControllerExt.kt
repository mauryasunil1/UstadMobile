package com.ustadmobile.core.util.ext

import com.ustadmobile.core.account.UstadAccountManager
import com.ustadmobile.core.domain.openlink.OpenExternalLinkUseCase
import com.ustadmobile.core.domain.openlink.OpenExternalLinkUseCase.Companion.LinkTarget
import com.ustadmobile.core.impl.UstadMobileConstants
import com.ustadmobile.core.impl.UstadMobileSystemCommon
import com.ustadmobile.core.impl.nav.UstadNavController
import com.ustadmobile.core.util.UMFileUtil
import com.ustadmobile.core.util.UstadUrlComponents
import com.ustadmobile.core.util.UstadUrlComponents.Companion.DEFAULT_DIVIDER
import com.ustadmobile.core.view.*
import com.ustadmobile.core.view.UstadView.Companion.ARG_NEXT
import com.ustadmobile.core.view.UstadView.Companion.ARG_API_URL
import com.ustadmobile.core.viewmodel.parentalconsentmanagement.ParentalConsentManagementViewModel
import com.ustadmobile.core.viewmodel.accountlist.AccountListViewModel
import com.ustadmobile.core.viewmodel.login.LoginViewModel
import com.ustadmobile.core.viewmodel.siteenterlink.SiteEnterLinkViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
/**
 * Navigate to a given viewUri
 *
 * e.g. ViewName?arg=value&arg2=value2
 */
fun UstadNavController.navigateToViewUri(
    viewUri: String,
    goOptions: UstadMobileSystemCommon.UstadGoOptions
) {
    val questionIndex = viewUri.indexOf('?')
    val viewName = if(questionIndex != -1) viewUri.substring(0, questionIndex) else viewUri
    val args = if(questionIndex > 0) {
        UMFileUtil.parseURLQueryString(viewUri.substring(questionIndex))
    }else {
        emptyMap()
    }

    navigate(viewName, args, goOptions)
}



/**
 * Open the given link. This will handle redirecting the user to the accountlist, login, or enter
 * site link as needed.
 *
 * Note: if we are opening an external link, this must be done synchronously. On Javascript opening
 * tabs is only allowed in response to events.
 *
 * @return If the link is internal, then opening the link will be done asynchronously (required to
 * check existing accounts etc) a Job will be returned. If the link is external, it is opened
 * synchronously and null will be returned.
 */
@OptIn(DelicateCoroutinesApi::class)
fun UstadNavController.navigateToLink(
    link: String,
    accountManager: UstadAccountManager,
    openExternalLinkUseCase: OpenExternalLinkUseCase,
    goOptions: UstadMobileSystemCommon.UstadGoOptions = UstadMobileSystemCommon.UstadGoOptions.Default,
    forceAccountSelection: Boolean = false,
    userCanSelectServer: Boolean = true,
    accountName: String? = null,
    scope: CoroutineScope = GlobalScope,
    linkTarget: LinkTarget = LinkTarget.DEFAULT,
) : Job? {
    var endpointUrl: String? = null
    var viewUri: String? = null


    when {
        link.startsWithHttpProtocol() && link.contains(DEFAULT_DIVIDER) -> {
            val urlComponents = UstadUrlComponents.parse(link)
            endpointUrl = urlComponents.endpoint
            viewUri = urlComponents.viewUri
        }

        !link.startsWithHttpProtocol() -> {
            viewUri = link
        }
    }

    val maxDateOfBirth = if(viewUri?.startsWith(ParentalConsentManagementViewModel.DEST_NAME) == true) {
        Clock.System.now().minus(UstadMobileConstants.ADULT_AGE_THRESHOLD, DateTimeUnit.YEAR, TimeZone.UTC)
            .toEpochMilliseconds()
    }else {
        0L
    }

    /**
     * Where the link is not an Ustad link, or the link is an ustad link but the system does not
     * allow the user to select to connect to another server, then we need to open the link in a
     * via openExternalLinkUseCase
     */
    return if(viewUri == null ||
        !userCanSelectServer && endpointUrl != null && endpointUrl != accountManager.activeEndpoint.url
    ) {
        //when the link is not an ustad link, open in browser
        openExternalLinkUseCase(link, linkTarget)
        null
    }else {
        scope.launch {
            when {
                //When the account has already been selected and the endpoint url is known.
                accountName != null && endpointUrl != null -> {
                    val session = accountManager.activeSessionsList { filterUrl ->
                        filterUrl == endpointUrl
                    }.firstOrNull {
                        it.person.username == accountName.substringBefore("@")
                    }
                    if(session != null) {
                        accountManager.currentUserSession = session
                        navigateToViewUri(viewUri, goOptions)
                    }
                }

                //when the current account is already on the given endpoint, or there is no endpoint
                //specified, then go directly to the given view (unless the force account selection option
                //is set)
                !forceAccountSelection
                        && !accountManager.currentUserSession.userSession.isTemporary()
                        && (endpointUrl == null || accountManager.activeEndpoint.url == endpointUrl) ->
                {
                    navigateToViewUri(viewUri, goOptions)
                }

                //If the endpoint Url is known and there are no active accounts for this server,
                // go directly to login
                (endpointUrl != null
                        && accountManager.activeSessionCount(maxDateOfBirth) { it == endpointUrl } == 0 ) ||
                //... or when the endpoint url is not known, but there are no accounts at all, and the user cannot
                ///select a server, go directly to login
                (endpointUrl == null && accountManager.activeSessionCount(maxDateOfBirth) == 0
                && !userCanSelectServer) ->
                {
                    val args = mutableMapOf(ARG_NEXT to viewUri)
                    if(endpointUrl != null)
                        args[ARG_API_URL] = endpointUrl

                    navigate(LoginViewModel.DEST_NAME, args.toMap(), goOptions)
                }
                //If there are no accounts, the endpoint url is not specified, and the user can select the server, go to EnterLink
                endpointUrl == null && accountManager.activeSessionCount(maxDateOfBirth) == 0 && userCanSelectServer -> {
                    navigate(SiteEnterLinkViewModel.DEST_NAME, mapOf(ARG_NEXT to viewUri), goOptions)
                }

                //else - go to the account manager
                else -> {
                    val args = mutableMapOf(ARG_NEXT to viewUri)
                    if(endpointUrl != null)
                        args[AccountListViewModel.ARG_FILTER_BY_ENDPOINT] = endpointUrl

                    args[AccountListViewModel.ARG_ACTIVE_ACCOUNT_MODE] = AccountListViewModel.ACTIVE_ACCOUNT_MODE_INLIST
                    args[UstadView.ARG_LISTMODE] = ListViewMode.PICKER.toString()
                    args[UstadView.ARG_MAX_DATE_OF_BIRTH] = maxDateOfBirth.toString()

                    navigate(AccountListViewModel.DEST_NAME, args.toMap(), goOptions)
                }
            }
        }
    }
}