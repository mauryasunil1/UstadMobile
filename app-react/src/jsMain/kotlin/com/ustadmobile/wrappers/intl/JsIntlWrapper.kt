package com.ustadmobile.wrappers.intl

import kotlin.js.Date

@Suppress("NAME_CONTAINS_ILLEGAL_CHARS")
// language=JavaScript
@JsName("""(/*union*/{numeric: 'numeric', twodigit: 'two-digit'}/*union*/)""")
external enum class IntlDateTimeNumericProp {
    numeric,

    //This is valid
    @Suppress("unused")
    twodigit,
    ;
}

@Suppress("NAME_CONTAINS_ILLEGAL_CHARS")
// language=JavaScript
@JsName("""(/*union*/{full: 'full', long: 'long', medium: 'medium', short: 'short'}/*union*/)""")
external enum class IntlDateTimeStyleProp  {
    @Suppress("unused")
    full,
    @Suppress("unused")
    long,
    @Suppress("unused")
    medium,
    @Suppress("unused")
    short,
    ;
}

external interface DateTimeFormatOptions {
    var hour: IntlDateTimeNumericProp
    var minute: IntlDateTimeNumericProp

    var dateStyle: IntlDateTimeStyleProp
    var timeStyle: IntlDateTimeStyleProp
}


/**
 * Wrapper for required Intl functions
 *
 * https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/DateTimeFormat
 */
external class Intl {

    companion object {

        class DateTimeFormat(
            locale: String = definedExternally,
            options: DateTimeFormatOptions = definedExternally,
        ) {

            fun format(date: Date): String

        }

    }
}