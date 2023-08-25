package com.ustadmobile.view.discussionpost.coursediscussiondetail

import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.hooks.collectAsState
import com.ustadmobile.core.hooks.useStringsXml
import com.ustadmobile.core.impl.appstate.AppUiState
import com.ustadmobile.core.paging.ListPagingSource
import com.ustadmobile.core.viewmodel.discussionpost.courediscussiondetail.CourseDiscussionDetailUiState
import com.ustadmobile.core.viewmodel.discussionpost.courediscussiondetail.CourseDiscussionDetailViewModel
import com.ustadmobile.door.util.systemTimeInMillis
import com.ustadmobile.hooks.useMuiAppState
import com.ustadmobile.hooks.usePagingSource
import com.ustadmobile.hooks.useUstadViewModel
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.mui.components.*
import com.ustadmobile.view.components.UstadFab
import com.ustadmobile.view.components.UstadPersonAvatar
import com.ustadmobile.view.components.virtuallist.VirtualList
import com.ustadmobile.view.components.virtuallist.VirtualListOutlet
import com.ustadmobile.view.components.virtuallist.virtualListContent
import web.cssom.Contain
import web.cssom.Height
import web.cssom.Overflow
import web.cssom.pct
import js.core.jso
import mui.material.*
import mui.icons.material.Chat as ChatIcon
import mui.icons.material.ReplyAll as ReplyAllIcon
import mui.material.List
import mui.material.styles.TypographyVariant
import mui.system.responsive
import react.*

external interface CourseDiscussionDetailProps: Props {
    var uiState: CourseDiscussionDetailUiState
    var onClickPost: (DiscussionPostWithDetails) -> Unit
}

val CourseDiscussionDetailComponent = FC<CourseDiscussionDetailProps> { props ->

    val strings = useStringsXml()

    val infiniteQueryResult = usePagingSource(
        pagingSourceFactory = props.uiState.posts,
        placeholdersEnabled = true
    )

    val muiAppState = useMuiAppState()

    VirtualList {
        style = jso {
            height = "calc(100vh - ${muiAppState.appBarHeight}px)".unsafeCast<Height>()
            width = 100.pct
            contain = Contain.strict
            overflowY = Overflow.scroll
        }

        content = virtualListContent {
            item(key = "description") {
                UstadRawHtml.create {
                    html = props.uiState.courseBlock?.cbDescription ?: ""
                }
            }
            item(key = "divider") {
                Divider.create()
            }
            infiniteQueryPagingItems(
                items = infiniteQueryResult,
                key = { it.discussionPostUid.toString() }
            ) { discussionPostItem ->
                CourseDiscussionDetailPostListItem.create {
                    discussionPost = discussionPostItem
                    onClick = {
                        discussionPostItem?.also(props.onClickPost)
                    }
                }
            }
        }

        Container {
            List {
                VirtualListOutlet()
            }
        }
    }
}

val CourseDiscussionDetailPreview = FC<Props> {
    CourseDiscussionDetailComponent {
        uiState = CourseDiscussionDetailUiState(

            courseBlock = CourseBlock().apply {
                cbTitle = "Sales and Marketting Discussion"
                cbDescription =
                    "This discussion group is for conversations and posts about Sales and Marketting course"

            },
            posts = {
                ListPagingSource(listOf(
                    DiscussionPostWithDetails().apply {
                        discussionPostTitle = "Can I join after week 2?"
                        discussionPostUid = 0L
                        discussionPostMessage = "Iam late to class, CAn I join after?"
                        discussionPostVisible = true
                        postRepliesCount = 4
                        postLatestMessage = "Just make sure you submit a late assignment."
                        authorPersonFirstNames = "Mike"
                        authorPersonLastName = "Jones"
                        discussionPostStartDate = systemTimeInMillis()
                    },
                    DiscussionPostWithDetails().apply {
                        discussionPostTitle = "How to install xlib?"
                        discussionPostMessage = "Which version of python do I need?"
                        discussionPostVisible = true
                        discussionPostUid = 1L
                        postRepliesCount = 2
                        postLatestMessage = "I have the same question"
                        authorPersonFirstNames = "Bodium"
                        authorPersonLastName = "Carafe"
                        discussionPostStartDate = systemTimeInMillis()
                    }
                ))
            },
        )
    }
}

val CourseDiscussionDetailScreen = FC<Props> {
    val viewModel = useUstadViewModel { di, savedStateHandle ->
        CourseDiscussionDetailViewModel(di, savedStateHandle)
    }

    val uiStateVal by viewModel.uiState.collectAsState(CourseDiscussionDetailUiState())
    val appUiState by viewModel.appUiState.collectAsState(AppUiState())

    CourseDiscussionDetailComponent {
        uiState = uiStateVal
        onClickPost = viewModel::onClickPost
    }

    UstadFab {
        fabState = appUiState.fabState
    }

}
