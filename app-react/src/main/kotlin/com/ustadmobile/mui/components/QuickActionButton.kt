package com.ustadmobile.mui.components

import csstype.AlignContent
import csstype.Display
import csstype.px
import mui.material.*
import mui.system.Stack
import mui.system.StackDirection
import mui.system.responsive
import mui.system.sx
import react.FC
import react.Props
import react.dom.events.MouseEventHandler

external interface QuickActionButtonProps : Props {
    var onClick: MouseEventHandler<*>

    var text: String

    var icon: react.ReactNode

}

val QuickActionButton = FC<QuickActionButtonProps> { props ->
    Button {
        variant = ButtonVariant.text
        onClick = props.onClick

        Stack {
            direction = responsive(StackDirection.column)

            Box {
                sx {
                    width = 80.px
                    alignContent = AlignContent.center
                    display = Display.block
                }
                +props.icon
            }

            Typography {
                align = TypographyAlign.center
                +props.text
            }
        }
    }
}