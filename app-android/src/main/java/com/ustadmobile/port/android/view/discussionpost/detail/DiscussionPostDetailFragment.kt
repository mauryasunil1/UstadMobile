package com.ustadmobile.port.android.view.discussionpost.detail


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.google.accompanist.themeadapter.material.MdcTheme
import com.toughra.ustadmobile.R
import com.ustadmobile.core.paging.ListPagingSource
import com.ustadmobile.core.viewmodel.discussionpost.detail.DiscussionPostDetailUiState2
import com.ustadmobile.core.viewmodel.discussionpost.detail.DiscussionPostDetailViewModel
import com.ustadmobile.door.util.systemTimeInMillis
import com.ustadmobile.lib.db.composites.DiscussionPostAndPosterNames
import com.ustadmobile.lib.db.entities.DiscussionPost
import com.ustadmobile.port.android.view.UstadBaseMvvmFragment
import com.ustadmobile.port.android.view.composable.UstadAddCommentListItem
import com.ustadmobile.port.android.view.composable.UstadListSpacerItem
import com.ustadmobile.core.R as CR


class DiscussionPostDetailFragment: UstadBaseMvvmFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
            )

            setContent {
                MdcTheme {

                }
            }
        }
    }


    companion object {
        //If anything..
    }

}

@Composable
fun DiscussionPostDetailFragmentScreen(viewModel: DiscussionPostDetailViewModel){
    val uiState: DiscussionPostDetailUiState2 by viewModel.uiState.collectAsState(
        DiscussionPostDetailUiState2()
    )


    DiscussionPostDetailFragmentScreen(
        uiState = uiState,
        onClickAddReply = viewModel::onClickEditReplyHtml
    )


}
@Composable
private fun DiscussionPostDetailFragmentScreen(
    uiState: DiscussionPostDetailUiState2 = DiscussionPostDetailUiState2(),
    onClickAddReply: () -> Unit = { },
) {

    val pager = remember(uiState.discussionPosts) {
        Pager(
            pagingSourceFactory = uiState.discussionPosts,
            config = PagingConfig(pageSize = 50, enablePlaceholders = true)
        )
    }

    val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            count = lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it.discussionPost?.discussionPostUid ?: 0}
        ) { index ->
            val discussionPostItem = lazyPagingItems[index]
            DiscussionPostListItem(
                discussionPost = discussionPostItem
            )

            //This is the root item - show add a reply here
            if(discussionPostItem?.discussionPost?.discussionPostReplyToPostUid == 0L) {
                UstadAddCommentListItem(
                    text = stringResource(CR.string.add_a_reply),
                    personUid = uiState.loggedInPersonUid,
                    onClickAddComment =  onClickAddReply,
                )

                Divider(
                    thickness = 1.dp
                )
            }
        }

        UstadListSpacerItem()
    }
}


@Composable
@Preview
fun DiscussionPostDetailScreenFragmentPreview(){
    val uiState = DiscussionPostDetailUiState2(
        discussionPosts = {
            ListPagingSource(listOf(
                DiscussionPostAndPosterNames(
                    discussionPost = DiscussionPost().apply {
                        discussionPostTitle = "Submitting an assignment"
                        discussionPostVisible = true
                        discussionPostStartedPersonUid = 1
                        discussionPostReplyToPostUid = 0
                        discussionPostUid = 1
                        discussionPostMessage = "How can I get the best grade?"
                        discussionPostStartDate = systemTimeInMillis()
                    },
                    firstNames = "Mohammed",
                    lastName = "Iqbaal",
                ),
                DiscussionPostAndPosterNames(
                    discussionPost = DiscussionPost().apply {
                        discussionPostReplyToPostUid = 42
                        discussionPostTitle = null
                        discussionPostVisible = true
                        discussionPostStartedPersonUid = 1
                        discussionPostUid = 2
                        discussionPostMessage = "Use ChatGPT"
                        discussionPostStartDate = systemTimeInMillis()
                    },
                    firstNames = "Cheaty",
                    lastName = "McCheatface",
                ),
                DiscussionPostAndPosterNames(
                    discussionPost = DiscussionPost().apply {
                        discussionPostReplyToPostUid = 42
                        discussionPostVisible = true
                        discussionPostStartedPersonUid = 1

                        discussionPostUid = 3
                        discussionPostMessage = "Use BARD"
                        discussionPostStartDate = systemTimeInMillis()
                    },
                    firstNames = "Chester",
                    lastName = "Cheetah",
                ),
                DiscussionPostAndPosterNames(
                    discussionPost = DiscussionPost().apply {
                        discussionPostVisible = true
                        discussionPostStartedPersonUid = 1
                        discussionPostReplyToPostUid = 42
                        discussionPostUid = 4
                        discussionPostMessage = "Ask Jeeves"
                        discussionPostStartDate = systemTimeInMillis()
                    },
                    firstNames = "Uncle",
                    lastName = "Brandon",
                ),
            ))
        },
        loggedInPersonUid = 1
    )

    MdcTheme{
        DiscussionPostDetailFragmentScreen(uiState)
    }
}