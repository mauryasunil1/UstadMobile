package com.ustadmobile.core.domain.contententry.launchcontent.xapi

import com.ustadmobile.core.domain.contententry.launchcontent.LaunchContentEntryVersionUseCase
import com.ustadmobile.core.domain.openlink.OpenExternalLinkUseCase
import com.ustadmobile.core.impl.nav.UstadNavController
import com.ustadmobile.core.util.ext.asWindowTarget
import com.ustadmobile.lib.db.entities.ContentEntryVersion
import web.location.location
import web.window.window

class LaunchXapiUseCaseJs(
    private val resolveXapiLaunchHrefUseCase: ResolveXapiLaunchHrefUseCase,
) : LaunchXapiUseCase{

    override suspend fun invoke(
        contentEntryVersion: ContentEntryVersion,
        navController: UstadNavController,
        target: OpenExternalLinkUseCase.Companion.LinkTarget
    ): LaunchContentEntryVersionUseCase.LaunchResult {
        val resolveResult = resolveXapiLaunchHrefUseCase(
            contentEntryVersion.cevUid,
        )

        if(target != OpenExternalLinkUseCase.Companion.LinkTarget.TOP) {
            window.open(resolveResult.url, target.asWindowTarget(), features = "popup=true,noopener,noreferrer")
        }else {
            location.href = resolveResult.url
        }

        return LaunchContentEntryVersionUseCase.LaunchResult()
    }
}