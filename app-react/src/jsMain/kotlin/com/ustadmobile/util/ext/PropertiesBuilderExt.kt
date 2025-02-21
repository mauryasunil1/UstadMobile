package com.ustadmobile.util.ext

import com.ustadmobile.MuiAppState
import com.ustadmobile.mui.common.DisplayWebkitBox
import com.ustadmobile.mui.common.webKitLineClamp
import com.ustadmobile.mui.common.webkitBoxOrient
import csstype.PropertiesBuilder
import web.cssom.AlignItems
import web.cssom.Height
import web.cssom.JustifyContent
import web.cssom.Overflow
import web.cssom.TextAlign
import web.cssom.TextOverflow

fun PropertiesBuilder.useLineClamp(numLines: Int) {
    webKitLineClamp = numLines
    display = DisplayWebkitBox
    webkitBoxOrient = "vertical"
    overflow = Overflow.hidden
    textOverflow = TextOverflow.ellipsis
}

/**
 * Can be used to create a grid that is center aligned vertically and horizontally
 */
fun PropertiesBuilder.useCenterAlignGridContainer(
    muiAppState: MuiAppState
) {
    alignItems = AlignItems.center
    justifyContent = JustifyContent.center
    height = "calc(100vh - ${muiAppState.appBarHeight}px)".unsafeCast<Height>()
    textAlign = TextAlign.center
}

