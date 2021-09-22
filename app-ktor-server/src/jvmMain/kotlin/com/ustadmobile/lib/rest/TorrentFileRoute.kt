package com.ustadmobile.lib.rest

import com.ustadmobile.core.account.Endpoint
import com.ustadmobile.core.contentjob.ContentJobManager
import com.ustadmobile.core.db.JobStatus
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.io.ext.getSize
import com.ustadmobile.core.torrent.ContainerTorrentDownloadJob
import com.ustadmobile.core.util.DiTag
import com.ustadmobile.door.DoorUri
import com.ustadmobile.lib.db.entities.ContentJob
import com.ustadmobile.lib.db.entities.ContentJobItem
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.kodein.di.direct
import org.kodein.di.instance
import org.kodein.di.ktor.closestDI
import org.kodein.di.ktor.di
import org.kodein.di.on
import java.io.File
import java.lang.Exception
import java.util.*

fun Route.TorrentFileRoute(){

    route("containers") {

        get("{containerUid}"){
            val torrentDir: File by closestDI().on(call).instance(tag = DiTag.TAG_TORRENT_DIR)
            val containerUid = call.parameters["containerUid"]
            val torrentFile = File(torrentDir, "$containerUid.torrent")
            call.respondFile(torrentFile)
        }

        post("{containerUid}/{contentEntryUid}/upload"){

            val torrentDir: File by closestDI().on(call).instance(tag = DiTag.TAG_TORRENT_DIR)
            val contentJobManager: ContentJobManager by closestDI().instance()
            val containerFolder: File by closestDI().on(call).instance(tag = DiTag.TAG_DEFAULT_CONTAINER_DIR)
            val db: UmAppDatabase by closestDI().on(call).instance(tag = UmAppDatabase.TAG_DB)
            // TODO handle virtualhostmode for endpoint
            val endpoint = Endpoint(call.request.header("Host") ?: "localhost")

            val containerUid = call.parameters["containerUid"]?.toLong() ?: 0L
            val contentEntryUid = call.parameters["contentEntryUid"]?.toLong() ?: 0L

            val torrentFile = File(torrentDir, "$containerUid.torrent")

            try {
                withContext(Dispatchers.IO) {
                    call.receiveMultipart().forEachPart {
                        when(it){
                            is PartData.FileItem ->{
                                it.streamProvider.invoke().use { stream ->
                                    torrentFile.writeBytes(stream.readBytes())
                                }
                                it.dispose
                            }
                        }
                    }
                }
            }catch(e: Exception) {
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, "Upload error: $e")
            }

            val job = ContentJob().apply {
                toUri = containerFolder.toURI().toString()
                cjUid = db.contentJobDao.insertAsync(this)
            }

            ContentJobItem().apply {
                cjiJobUid = job.cjUid
                cjiContainerUid = containerUid
                sourceUri = "content://${contentEntryUid}"
                cjiContentEntryUid = contentEntryUid
                cjiPluginId = ContainerTorrentDownloadJob.PLUGIN_ID
                cjiIsLeaf = true
                cjiConnectivityAcceptable = ContentJobItem.ACCEPT_ANY
                cjiUid = db.contentJobItemDao.insertJobItem(this)
            }

            contentJobManager.enqueueContentJob(endpoint, job.cjUid)

            call.respond(job.cjUid)

        }

        get("{jobUid}/status"){

            val db: UmAppDatabase by closestDI().on(call).instance(tag = UmAppDatabase.TAG_DB)
            val jobUid: Long = call.parameters["containerUid"]?.toLongOrNull() ?: 0L
            val jobItem = db.contentJobItemDao.findByJobId(jobUid).getValue()

            with(call) {
                if (jobItem != null) {
                    respond(jobItem.cjiStatus)
                }else{
                    respond(HttpStatusCode.NotFound)
                }
            }

        }


    }
}