package com.ustadmobile.view

import com.ccfraser.muirwik.components.list.MListItemAlignItems
import com.ccfraser.muirwik.components.list.alignItems
import com.ccfraser.muirwik.components.list.mList
import com.ccfraser.muirwik.components.list.mListItem
import com.ustadmobile.core.controller.UstadListPresenter
import com.ustadmobile.util.StyleManager
import com.ustadmobile.util.StyleManager.horizontalList
import com.ustadmobile.util.StyleManager.listComponentContainer
import com.ustadmobile.util.StyleManager.theme
import com.ustadmobile.view.ext.renderCreateNewItemView
import kotlinx.css.Color
import kotlinx.css.LinearDimension
import kotlinx.css.backgroundColor
import kotlinx.css.width
import react.RBuilder
import react.RProps
import react.RState
import styled.css
import styled.styledDiv

interface ListProps<T>: RProps {
    var entries: List<T>
    var onEntryClicked: ((entry: dynamic) -> Unit)?
    var createNewItem: CreateNewItem?
    var presenter: UstadListPresenter<*,*>
}

data class CreateNewItem(var visible: Boolean = false, var labelId: Int = 0, var onClickCreateNew: (() -> Unit)? = null)

abstract class UstadSimpleList<P: ListProps<*>>(mProps: P) : UstadBaseComponent<P,RState>(mProps){

    override val viewName: String?
        get() = ""

    override fun RBuilder.render() {
        styledDiv {
            css{
                +listComponentContainer
                width = LinearDimension("98%")
            }
            renderList()
        }
    }

    private fun RBuilder.renderList(){
        mList {
            css(horizontalList)

            if(props.createNewItem?.visible == true && props.createNewItem?.labelId != 0){
                mListItem {
                    css(StyleManager.listCreateNewContainer)
                    attrs.alignItems = MListItemAlignItems.flexStart
                    attrs.button = true
                    attrs.divider = true
                    attrs.onClick = {
                        props.createNewItem?.onClickCreateNew?.invoke()
                    }
                    renderCreateNewItemView(getString(props.createNewItem?.labelId ?: 0))
                }
            }

            props.entries.forEach {entry->
                mListItem {
                    css{
                        backgroundColor = Color(theme.palette.background.paper)
                        width = LinearDimension("100%")
                    }
                    attrs.alignItems = MListItemAlignItems.flexStart
                    attrs.button = true
                    attrs.divider = true
                    attrs.onClick = {
                        props.onEntryClicked?.invoke(entry)
                    }
                    renderListItem(entry)
                }
            }
        }
    }

    abstract fun RBuilder.renderListItem(item: dynamic)
}