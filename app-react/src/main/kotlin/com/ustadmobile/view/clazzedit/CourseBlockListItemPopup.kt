package com.ustadmobile.view.clazzedit

import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.hooks.useStringsXml
import com.ustadmobile.core.viewmodel.ClazzEditUiState
import com.ustadmobile.lib.db.entities.CourseBlockWithEntity
import js.core.jso
import mui.icons.material.MoreVert
import mui.material.IconButton
import mui.material.Menu
import mui.material.MenuItem
import mui.material.PopoverReference
import react.FC
import react.Props
import react.create
import react.dom.aria.ariaLabel
import react.dom.events.MouseEvent
import react.dom.events.MouseEventHandler
import react.dom.html.ReactHTML
import react.useState


external interface PopUpMenuProps : Props {

    var fieldsEnabled: Boolean

    var onClickHideBlockPopupMenu: (CourseBlockWithEntity) -> Unit

    var onClickUnHideBlockPopupMenu: (CourseBlockWithEntity) -> Unit

    var onClickIndentBlockPopupMenu: (CourseBlockWithEntity) -> Unit

    var onClickUnIndentBlockPopupMenu: (CourseBlockWithEntity) -> Unit

    var onClickDeleteBlockPopupMenu: (CourseBlockWithEntity) -> Unit

    var uiState: ClazzEditUiState.CourseBlockUiState

}

private data class Point(
    val x: Double = 10.0,
    val y: Double = 10.0,
)

val PopUpMenu = FC<PopUpMenuProps> { props ->

    val strings = useStringsXml()

    var point by useState<Point>()

    val handleContextMenu = { event: MouseEvent<*, *> ->
        event.preventDefault()
        point = if (point == null) {
            Point(
                x = event.clientX - 2,
                y = event.clientY - 4,
            )
        } else {
            null
        }
    }

    val handleClose: MouseEventHandler<*> = {
        point = null
    }

    ReactHTML.div {

        IconButton{
            disabled = !(props.fieldsEnabled)
            onClick = handleContextMenu
            ariaLabel = strings[MessageID.more_options]

            + MoreVert.create()
        }

        Menu {
            open = point != null
            onClose = handleClose

            anchorReference = PopoverReference.anchorPosition
            anchorPosition = if (point != null) {
                jso {
                    top = point!!.y
                    left = point!!.x
                }
            } else {
                undefined
            }

            if(props.uiState.showHide) {
                MenuItem {
                    onClick = {
                        props.onClickHideBlockPopupMenu(props.uiState.courseBlock)
                        point = null
                    }
                    + strings[MessageID.hide]
                }
            }

            if(props.uiState.showUnhide) {
                MenuItem {
                    onClick = {
                        props.onClickUnHideBlockPopupMenu(props.uiState.courseBlock)
                        point = null
                    }
                    + strings[MessageID.unhide]
                }
            }

            if(props.uiState.showIndent) {
                MenuItem {
                    onClick = {
                        props.onClickIndentBlockPopupMenu(props.uiState.courseBlock)
                        point = null
                    }
                    + strings[MessageID.indent]
                }
            }

            if (props.uiState.showUnindent) {
                MenuItem {
                    onClick = {
                        props.onClickUnIndentBlockPopupMenu(props.uiState.courseBlock)
                        point = null
                    }
                    + strings[MessageID.unindent]
                }
            }

            MenuItem {
                onClick = {
                    props.onClickDeleteBlockPopupMenu(props.uiState.courseBlock)
                    point = null
                }
                + strings[MessageID.delete]
            }
        }
    }
}
