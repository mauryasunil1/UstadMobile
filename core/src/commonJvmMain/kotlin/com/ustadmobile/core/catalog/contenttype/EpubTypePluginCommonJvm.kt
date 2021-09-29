package com.ustadmobile.core.catalog.contenttype

import com.ustadmobile.core.account.Endpoint
import com.ustadmobile.core.container.ContainerAddOptions
import com.ustadmobile.core.contentformats.epub.ocf.OcfDocument
import com.ustadmobile.core.contentformats.epub.opf.OpfDocument
import com.ustadmobile.core.contentjob.*
import com.ustadmobile.core.contentjob.ext.processMetadata
import com.ustadmobile.core.contentjob.ext.uploadContentIfNeeded
import com.ustadmobile.core.db.JobStatus
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.io.ext.*
import com.ustadmobile.core.torrent.UstadTorrentManager
import com.ustadmobile.core.util.DiTag
import com.ustadmobile.core.util.ext.alternative
import com.ustadmobile.core.view.EpubContentView
import com.ustadmobile.door.DoorUri
import com.ustadmobile.door.ext.DoorTag
import com.ustadmobile.door.ext.openInputStream
import com.ustadmobile.door.ext.toFile
import com.ustadmobile.lib.db.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.kodein.di.DI
import org.kodein.di.instance
import org.kodein.di.direct
import org.kodein.di.on
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.util.*
import io.ktor.client.*
import java.util.zip.ZipInputStream

class EpubTypePluginCommonJvm(private var context: Any, private val endpoint: Endpoint, override val di: DI) : ContentPlugin {

    val viewName: String
        get() = EpubContentView.VIEW_NAME

    override val supportedMimeTypes: List<String>
        get() = SupportedContent.EPUB_MIME_TYPES

    override val supportedFileExtensions: List<String>
        get() = SupportedContent.EPUB_EXTENSIONS

    override val pluginId: Int
        get() = PLUGIN_ID

    private val httpClient: HttpClient = di.direct.instance()

    private val defaultContainerDir: File by di.on(endpoint).instance(tag = DiTag.TAG_DEFAULT_CONTAINER_DIR)

    private val torrentDir: File by di.on(endpoint).instance(tag = DiTag.TAG_TORRENT_DIR)

    private val repo: UmAppDatabase by di.on(endpoint).instance(tag = DoorTag.TAG_REPO)

    private val db: UmAppDatabase by di.on(endpoint).instance(tag = DoorTag.TAG_DB)

    private val ustadTorrentManager: UstadTorrentManager by di.on(endpoint).instance()

    override suspend fun extractMetadata(uri: DoorUri, process: ProcessContext): MetadataResult? {
            val mimeType = uri.guessMimeType(context, di)
            if(mimeType != null && !supportedMimeTypes.contains(mimeType)){
                return null
            }
            return withContext(Dispatchers.Default) {
                val xppFactory = XmlPullParserFactory.newInstance()
                try {
                    val localUri = process.getLocalUri(uri, context, di)
                    val opfPath = ZipInputStream(localUri.openInputStream(context)).use {
                        it.skipToEntry { entry -> entry.name == OCF_CONTAINER_PATH } ?: return@use null

                        val ocfContainer = OcfDocument()
                        val xpp = xppFactory.newPullParser()
                        xpp.setInput(it, "UTF-8")
                        ocfContainer.loadFromParser(xpp)

                        return@use ocfContainer.rootFiles.firstOrNull()?.fullPath
                    } ?: return@withContext null

                    return@withContext  ZipInputStream(localUri.openInputStream(context)).use {

                        it.skipToEntry { it.name == opfPath } ?: return@use null

                        val xpp = xppFactory.newPullParser()
                        xpp.setInput(it, "UTF-8")
                        val opfDocument = OpfDocument()
                        opfDocument.loadFromOPF(xpp)

                        val entry = ContentEntryWithLanguage().apply {
                            contentFlags = ContentEntry.FLAG_IMPORTED
                            contentTypeFlag = ContentEntry.TYPE_EBOOK
                            licenseType = ContentEntry.LICENSE_TYPE_OTHER
                            title = if (opfDocument.title.isNullOrEmpty()) localUri.getFileName(context)
                            else opfDocument.title
                            author = opfDocument.getCreator(0)?.creator
                            description = opfDocument.description
                            leaf = true
                            sourceUrl = uri.uri.toString()
                            entryId = opfDocument.id.alternative(UUID.randomUUID().toString())
                            val languageCode = opfDocument.getLanguage(0)
                            if (languageCode != null) {
                                this.language = Language().apply {
                                    iso_639_1_standard = languageCode
                                }
                            }
                        }
                        return@use MetadataResult(entry, PLUGIN_ID)
                    }
                } catch (e: Exception) {
                    null
                }
            }
        }

        override suspend fun processJob(jobItem: ContentJobItemAndContentJob, process: ProcessContext, progress: ContentJobProgressListener): ProcessResult {
            val contentJobItem = jobItem.contentJobItem ?: throw IllegalArgumentException("missing job item")
            val jobUri = contentJobItem.sourceUri ?: return ProcessResult(JobStatus.FAILED)
            val container = withContext(Dispatchers.Default) {

                val uri = DoorUri.parse(jobUri)
                val localUri = process.getLocalUri(uri, context, di)
                val trackerUrl = db.siteDao.getSiteAsync()?.torrentAnnounceUrl
                        ?: throw IllegalArgumentException("missing tracker url")
                val contentNeedUpload = !uri.isRemote()

                val container = db.containerDao.findByUid(contentJobItem.cjiContainerUid) ?:
                    Container().apply {
                        containerContentEntryUid = contentJobItem.cjiContentEntryUid
                        cntLastModified = System.currentTimeMillis()
                        mimeType = supportedMimeTypes.first()
                        containerUid = repo.containerDao.insertAsync(this)
                        contentJobItem.cjiContainerUid = containerUid
                    }

                db.contentJobItemDao.updateContainer(contentJobItem.cjiUid, container.containerUid)

                val containerFolder = jobItem.contentJob?.toUri ?: defaultContainerDir.toURI().toString()
                val containerFolderUri = DoorUri.parse(containerFolder)

                repo.addEntriesToContainerFromZip(container.containerUid,
                        localUri,
                        ContainerAddOptions(storageDirUri = containerFolderUri), context)

                repo.addTorrentFileFromContainer(
                        container.containerUid,
                        DoorUri.parse(torrentDir.toURI().toString()),
                        trackerUrl, containerFolderUri
                )

                val containerUidFolder = File(containerFolderUri.toFile(), container.containerUid.toString())
                containerUidFolder.mkdirs()
                ustadTorrentManager.addTorrent(container.containerUid, containerUidFolder.path)

                val torrentFileBytes = File(torrentDir, "${container.containerUid}.torrent").readBytes()
                uploadContentIfNeeded(contentNeedUpload, contentJobItem, progress, httpClient,  torrentFileBytes, endpoint)


                val containerWithSize = repo.containerDao.findByUid(container.containerUid) ?: container

                containerWithSize
            }
            return ProcessResult(JobStatus.COMPLETE)
        }


        suspend fun findOpfPath(uri: DoorUri, process: ProcessContext): String? {
            val xppFactory = XmlPullParserFactory.newInstance()
            try {

                val localUri = process.getLocalUri(uri, context,di)

                ZipInputStream(localUri.openInputStream(context)).use {
                    it.skipToEntry { entry -> entry.name == OCF_CONTAINER_PATH } ?: return null

                    val ocfContainer = OcfDocument()
                    val xpp = xppFactory.newPullParser()
                    xpp.setInput(it, "UTF-8")
                    ocfContainer.loadFromParser(xpp)

                    return ocfContainer.rootFiles.firstOrNull()?.fullPath
                }
            } catch (e: Exception) {
                return null
            }
        }

        companion object {

        private const val OCF_CONTAINER_PATH = "META-INF/container.xml"

        const val PLUGIN_ID = 2
    }
}
