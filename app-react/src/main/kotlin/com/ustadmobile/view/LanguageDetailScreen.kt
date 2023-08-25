package com.ustadmobile.view

import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.hooks.useStringsXml
import com.ustadmobile.core.viewmodel.LanguageDetailUiState
import com.ustadmobile.lib.db.entities.Language
import com.ustadmobile.mui.components.UstadDetailField
import web.cssom.px
import mui.material.Container
import mui.system.Stack
import mui.system.StackDirection
import mui.system.responsive
import react.FC
import react.Props
import react.ReactNode

external interface LanguageDetailProps : Props {
    var uiState: LanguageDetailUiState
}

val LanguageDetailComponent2 = FC<LanguageDetailProps> { props ->

    val strings = useStringsXml()

    Container {
        maxWidth = "lg"

        Stack {
            direction = responsive(StackDirection.column)
            spacing = responsive(10.px)

            UstadDetailField {
                labelText = strings[MessageID.name]
                valueText = ReactNode(props.uiState.language?.name.toString())
            }

            UstadDetailField {
                labelText = strings[MessageID.two_letter_code]
                valueText = ReactNode(props.uiState.language?.iso_639_1_standard.toString())
            }

            UstadDetailField {
                labelText = strings[MessageID.three_letter_code]
                valueText = ReactNode(props.uiState.language?.iso_639_2_standard.toString())
            }
        }
    }
}

val LanguageDetailPreview = FC<Props> {
    LanguageDetailComponent2 {
        uiState = LanguageDetailUiState(
            language = Language().apply {
                name = "فارسی"
                iso_639_1_standard = "fa"
                iso_639_2_standard = "per"
            }
        )
    }
}