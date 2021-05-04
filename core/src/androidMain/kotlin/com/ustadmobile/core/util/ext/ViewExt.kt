package com.ustadmobile.core.util.ext

import android.view.View
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import org.kodein.di.DI
import org.kodein.di.android.di
import org.kodein.di.direct
import org.kodein.di.instance

/**
 * Shorthand to get the UstadMobileSystemImpl using DI from the context associated with the view
 */
val View.systemImpl: UstadMobileSystemImpl
    get() {
        val di: DI by di()
        return di.direct.instance()
    }
