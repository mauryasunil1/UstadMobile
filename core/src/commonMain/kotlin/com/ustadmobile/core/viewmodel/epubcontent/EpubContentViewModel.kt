package com.ustadmobile.core.viewmodel.epubcontent

import com.ustadmobile.core.contentformats.epub.nav.ListItem
import com.ustadmobile.core.contentformats.epub.nav.NavigationDocument
import com.ustadmobile.core.contentformats.epub.ncx.NavPoint
import com.ustadmobile.core.contentformats.epub.ncx.NcxDocument
import com.ustadmobile.core.contentformats.epub.ncx.NcxDocument.Companion.MIMETYPE_NCX
import com.ustadmobile.core.contentformats.epub.opf.PackageDocument
import com.ustadmobile.core.domain.openexternallink.OpenExternalLinkUseCase
import com.ustadmobile.core.impl.appstate.OverflowItem
import com.ustadmobile.core.impl.nav.UstadSavedStateHandle
import com.ustadmobile.core.url.UrlKmp
import com.ustadmobile.core.view.UstadView
import com.ustadmobile.core.viewmodel.UstadViewModel
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.serialization.XML
import org.kodein.di.DI
import org.kodein.di.instance
import com.ustadmobile.core.MR
import com.ustadmobile.core.contentformats.epub.opf.Item
import kotlin.concurrent.Volatile

data class EpubContentUiState(
    val spineUrls: List<String> = emptyList(),
    val tableOfContents: List<EpubTocItem> = emptyList(),
    val tableOfContentsOpen: Boolean = false,
    val collapsedTocUids: Set<Int> = emptySet(),
) {

    /**
     * The table of contents to be displayed: this will hide any children of collapsed items.
     */
    val tableOfContentToDisplay: List<EpubTocItem> = tableOfContents.filter { tocItem ->
        !collapsedTocUids.any { tocItem.isChildOfUid(it) }
    }

}

data class EpubTocItem(
    val uid: Int,
    val label: String,
    val href: String?,
    val children: List<EpubTocItem>,
    /*
     * A list of all the ancestor uids - makes it easy to filter out children of collapsed toc items
     */
    val parentUids: Set<Int> = emptySet(),
    val indentLevel: Int = 0,
) {

    val hasChildren: Boolean = children.isNotEmpty()

    fun isChildOfUid(
        uid: Int
    ) : Boolean {
        return uid in parentUids
    }

}

/**
 * ScrollCommand - see epubScrollCommands
 *
 * @param spineIndex the index of to scroll to in the spine
 * @param hash if not null, the hash within the item to scroll to after loading
 */
data class EpubScrollCommand(
    val spineIndex: Int,
    val hash: String? = null
)

class EpubContentViewModel(
    di: DI,
    savedStateHandle: UstadSavedStateHandle,
) : UstadViewModel(di, savedStateHandle, DEST_NAME){

    private val entityUidArg: Long = savedStateHandle[UstadView.ARG_ENTITY_UID]?.toLong() ?: 0

    private val httpClient: HttpClient by instance()

    private val xml: XML by instance()

    private val _uiState = MutableStateFlow(EpubContentUiState())

    val uiState: Flow<EpubContentUiState> = _uiState.asStateFlow()

    private val openExternalLinkUseCase: OpenExternalLinkUseCase by instance()

    private val _epubScrollCommands = MutableSharedFlow<EpubScrollCommand>(
        replay = 1,
        extraBufferCapacity = 0,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Scroll commands that are to be observed by the view. These can be emitted when the user clicks
     * on an internal link (e.g. via onClickLink) and when the user clicks on an item from the table
     * of contents.
     *
     * These commands then need to be actioned by the view (e.g. using LazyColumn / TanStack query)
     */
    val epubScrollCommands: Flow<EpubScrollCommand> = _epubScrollCommands.asSharedFlow()

    val tocItemAtomicIds = atomic(0)

    @Volatile
    private var navUrl: String? =null

    /**
     * Convert a NCX navpoint to the common EpubTocItem
     */
    private fun NavPoint.toTocItem(
        indentLevel: Int,
        parentUids: Set<Int>,
    ): EpubTocItem {
        val uid = tocItemAtomicIds.incrementAndGet()
        val uidsForChildren = parentUids + uid
        return EpubTocItem(
            uid = uid,
            label = navLabels.firstOrNull()?.text?.content ?: "",
            href = content.src,
            indentLevel = indentLevel,
            parentUids = parentUids,
            children = this.childPoints.map {
                it.toTocItem(indentLevel + 1, uidsForChildren)
            }
        )
    }

    /**
     * Convert a Epub Nav document to the common EpubTocItem
     */
    private fun ListItem.toEpubTocItem(
        indentLevel: Int,
        parentUids: Set<Int>,
    ) : EpubTocItem {
        val uid = tocItemAtomicIds.incrementAndGet()
        val uidsForChildren = parentUids + uid
        return EpubTocItem(
            uid = tocItemAtomicIds.incrementAndGet(),
            label = anchor?.content ?: span?.content ?: "",
            href = anchor?.href,
            children = this.orderedList?.listItems?.map {
                it.toEpubTocItem(indentLevel + 1, uidsForChildren)
            } ?: emptyList(),
            indentLevel = indentLevel,
            parentUids = parentUids,
        )
    }

    init {
        viewModelScope.launch {
            val contentEntryVersion = activeRepo.contentEntryVersionDao
                .findByUidAsync(entityUidArg) ?: return@launch
            val cevUrl = contentEntryVersion.cevUrl ?: return@launch

            withContext(Dispatchers.Default) {
                try {
                    val opfStr = httpClient.get(cevUrl).bodyAsText()
                    val opfPackage = xml.decodeFromString(
                        deserializer = PackageDocument.serializer(),
                        string = opfStr
                    )
                    val cevUrlObj = UrlKmp(cevUrl)

                    val manifestItemsMap = opfPackage.manifest.items.associateBy { it.id }
                    val spineUrls = opfPackage.spine.itemRefs.mapNotNull { itemRef ->
                        manifestItemsMap[itemRef.idRef]?.let {
                            cevUrlObj.resolve(it.href)
                        }?.toString()
                    }

                    _uiState.update { prev ->
                        prev.copy(
                            spineUrls = spineUrls,
                        )
                    }

                    _appUiState.update { prev ->
                        prev.copy(
                            title = opfPackage.metadata.titles.firstOrNull()?.content ?: "",
                            overflowItems = listOf(
                                OverflowItem(
                                    label = systemImpl.getString(MR.strings.table_of_contents),
                                    onClick = {
                                        _uiState.update { prev ->
                                            prev.copy(
                                                tableOfContentsOpen = true,
                                            )
                                        }
                                    }
                                )
                            )
                        )
                    }

                    val whiteSpaceRegex = Regex("\\s+")

                    //Load and setup table of contents. As per the EPUB3 spec, an XHTML navigation
                    //document is preferred, which will have the "toc nav" property. If that is not
                    //found, then we will fallback to using the NCX
                    val tocCandidates = opfPackage.manifest.items.filter {item ->
                        item.properties?.split(whiteSpaceRegex)?.let { "toc" in it || "nav" in it } ?: false
                    }
                    val spineNcxCandidate: List<Item> = opfPackage.spine.toc?.let { spineTocid ->
                        opfPackage.manifest.items.firstOrNull { it.id == spineTocid }?.let { listOf(it) } ?: emptyList()
                    } ?: emptyList()
                    val allTocCandidates = (tocCandidates + spineNcxCandidate)

                    val tocToUse = allTocCandidates.firstOrNull {
                        it.properties?.contains("nav") ?: false
                    } ?: allTocCandidates.firstOrNull()

                    val tocItems = if(tocToUse != null && tocToUse.mediaType.startsWith("application/xhtml")) {
                        val urlResolved = cevUrlObj.resolve(tocToUse.href).toString().also {
                            navUrl = it
                        }
                        val docStr = httpClient.get(urlResolved).bodyAsText()
                        val navDoc: NavigationDocument = xml.decodeFromString(docStr)
                        navDoc.bodyElement.navigationElements
                            .first().orderedList.listItems
                            .flatMap { listItem ->
                                listItem.toEpubTocItem(0, emptySet()).let { listOf(it) + it.children }
                            }
                    }else if(tocToUse != null && tocToUse.mediaType == MIMETYPE_NCX) {
                        val urlResolved = cevUrlObj.resolve(tocToUse.href).toString().also {
                            navUrl = it
                        }
                        val docStr = httpClient.get(urlResolved).bodyAsText()
                        val ncxDoc: NcxDocument = xml.decodeFromString(docStr)
                        ncxDoc.navMap.navPoints.flatMap { navPoint ->
                            navPoint.toTocItem(0, emptySet()).let { listOf(it) + it.children }
                        }
                    }else {
                        emptyList()
                    }

                    _uiState.update { prev ->
                        prev.copy(
                            tableOfContents = tocItems,
                        )
                    }
                }catch(e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun onClickLink(
        baseUrl: String,
        href: String,
    ) {
        val url = UrlKmp(baseUrl).resolve(href)
        val urlStr = url.toString()
        val hashIndex = urlStr.indexOf("#")
        val urlWithoutHash = urlStr.substringBefore("#")
        val indexInSpine = _uiState.value.spineUrls.indexOf(urlWithoutHash)
        if(indexInSpine >= 0) {
            _epubScrollCommands.tryEmit(
                EpubScrollCommand(
                    spineIndex = indexInSpine,
                    hash = if(hashIndex > 0) {
                        urlStr.substring(hashIndex)
                    }else {
                        null
                    }
                )
            )
        }else {
            openExternalLinkUseCase(url.toString())
        }
    }

    fun onClickTocItem(
        tocItem: EpubTocItem,
    ) {
        _uiState.update { prev ->
            prev.copy(tableOfContentsOpen = false)
        }

        //NavUrl will always be set before the table of contents is displayed, so this is safe
        val baseHref = navUrl ?: return
        val itemUrl = tocItem.href ?: return
        onClickLink(baseHref, itemUrl)
    }

    fun onClickToggleTocItem(tocItem: EpubTocItem) {
        _uiState.update { prev ->
            prev.copy(
                collapsedTocUids = if(tocItem.uid in prev.collapsedTocUids) {
                    prev.collapsedTocUids.filter { it != tocItem.uid }.toSet()
                }else {
                    prev.collapsedTocUids + tocItem.uid
                }
            )
        }
    }

    fun onDismissTableOfContentsDrawer() {
        _uiState.update { prev ->
            prev.copy(
                tableOfContentsOpen = false,
            )
        }
    }

    companion object {

        const val DEST_NAME = "EpubContent"

    }

}