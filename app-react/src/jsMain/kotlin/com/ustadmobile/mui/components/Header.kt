package com.ustadmobile.mui.components

import com.ustadmobile.core.MR
import com.ustadmobile.core.hooks.useStringProvider
import com.ustadmobile.core.impl.appstate.AppUiState
import com.ustadmobile.core.viewmodel.UstadViewModel
import com.ustadmobile.core.viewmodel.settings.SettingsViewModel
import com.ustadmobile.mui.common.Area
import js.core.jso
import web.cssom.*
import mui.system.sx
import mui.material.*
import mui.material.styles.TypographyVariant.Companion.h6
import react.*
import react.dom.aria.AriaHasPopup
import react.dom.aria.ariaExpanded
import react.dom.aria.ariaHasPopup
import react.dom.aria.ariaLabel
import react.dom.aria.ariaLabelledBy
import react.dom.html.ReactHTML.div
import react.router.useLocation
import react.router.useNavigate
import web.dom.Element
import web.dom.document
import web.html.HTMLElement
import mui.icons.material.MoreVert as MoreVertIcon
import mui.icons.material.Settings as SettingsIcon
import mui.icons.material.Menu as MenuIcon

private val ROOT_LOCATIONS = UstadViewModel.ROOT_DESTINATIONS.map {
    "/$it"
}

external interface HeaderProps: Props {
    var appUiState: AppUiState

    var setAppBarHeight: (Int) -> Unit

    var showMenuIcon: Boolean

    var onClickMenuIcon: () -> Unit
}

val Header = FC<HeaderProps> { props ->
    val theme by useRequiredContext(ThemeContext)
    val appBarRef = useRef<HTMLElement>(null)
    val strings = useStringProvider()
    var overflowAnchor by useState<Element?> { null }
    val navigateFn = useNavigate()
    val location = useLocation()


    var appBarTitle by useState {
        strings[MR.strings.app_name]
    }

    val appUiStateTitle = props.appUiState.title
    useEffect(appUiStateTitle) {
        if(appUiStateTitle != null && appUiStateTitle != appBarTitle) {
            appBarTitle = appUiStateTitle
            document.title = "${strings[MR.strings.app_name]} : $appUiStateTitle"
        }
    }

    useEffect(appBarRef.current?.clientHeight){
        appBarRef.current?.also {
            props.setAppBarHeight(it.clientHeight)
        }
    }

    AppBar {
        position = AppBarPosition.fixed
        ref = appBarRef
        sx {
            gridArea = Area.Header
            zIndex = integer(1_500)
        }

        Toolbar {
            if(props.showMenuIcon) {
                IconButton {
                    ariaLabel = strings[MR.strings.menu]
                    onClick = {
                        props.onClickMenuIcon()
                    }

                    MenuIcon {
                        sx {
                            color = theme.palette.primary.contrastText
                        }
                    }
                }
            }


            Typography{
                sx { flexGrow = number(1.0) }
                variant = h6
                noWrap = true
                component = div

                + appBarTitle
            }

            if(location.pathname in ROOT_LOCATIONS) {
                IconButton {
                    id = "settings_button"
                    onClick = {
                        navigateFn("/${SettingsViewModel.DEST_NAME}")
                    }
                    ariaLabel = strings[MR.strings.settings]

                    SettingsIcon {
                        sx {
                            color = theme.palette.primary.contrastText
                        }
                    }
                }
            }

            if(props.appUiState.searchState.visible) {
                AppBarSearch {
                    onTextChanged = props.appUiState.searchState.onSearchTextChanged
                    searchText = props.appUiState.searchState.searchText
                }
            }

            /**
             * On the right hand side:
             * Show action bar button (e.g. save) if present - highest priority
             * Else show overflow menu if present
             * Else show avatar icon
             */
            if(props.appUiState.actionBarButtonState.visible) {
                Button {
                    sx {
                        color = theme.palette.common.white
                    }
                    id = "actionBarButton"
                    onClick = {
                        props.appUiState.actionBarButtonState.onClick()
                    }
                    + props.appUiState.actionBarButtonState.text
                }
            }else if(props.appUiState.overflowItems.isNotEmpty()) {
                val overflowAnchorVal = overflowAnchor

                IconButton {
                    ariaHasPopup = AriaHasPopup.`true`
                    ariaExpanded = overflowAnchorVal != null
                    onClick = {
                        overflowAnchor = if(overflowAnchor == null) {
                            it.currentTarget
                        }else {
                            null
                        }
                    }

                    id = "header_overflow_menu_expand_button"

                    MoreVertIcon {
                        sx {
                            color = theme.palette.primary.contrastText
                        }
                    }
                }

                if(overflowAnchorVal != null) {
                    Menu {
                        id = "header_overflow_menu"
                        open = true
                        anchorEl = {
                            overflowAnchorVal
                        }
                        sx {
                            marginTop = theme.spacing(2)
                        }

                        onClose = {
                            overflowAnchor = null
                        }

                        MenuListProps = jso {
                            ariaLabelledBy = "header_overflow_menu_expand_button"
                        }

                        props.appUiState.overflowItems.forEach { item ->
                            MenuItem {
                                onClick = {
                                    overflowAnchor = null
                                    item.onClick()
                                }
                                + item.label
                            }
                        }
                    }
                }

            }else if(props.appUiState.userAccountIconVisible) {
                HeaderAvatar()
            }
        }
    }
}
