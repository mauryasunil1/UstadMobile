package com.ustadmobile.core.networkmanager

import com.ustadmobile.core.account.Endpoint
import com.ustadmobile.core.contentformats.ContentImportManager
import com.ustadmobile.core.db.JobStatus
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.networkmanager.downloadmanager.ContainerDownloadManager
import com.ustadmobile.core.util.UMFileUtil
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.door.DoorMutableLiveData
import com.ustadmobile.lib.db.entities.ConnectivityStatus
import com.ustadmobile.lib.db.entities.ContainerImportJob
import com.ustadmobile.lib.db.entities.ContainerWithContainerEntryWithMd5
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.HttpStatement
import io.ktor.http.HttpStatusCode
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import org.kodein.di.on

class ImportJobRunner(private val containerImportJob: ContainerImportJob, private val retryDelay: Long = DEFAULT_RETRY_DELAY, private val endpointUrl: String, override val di: DI) : DIAware {

    private val db: UmAppDatabase by di.on(Endpoint(endpointUrl)).instance(tag = UmAppDatabase.TAG_DB)

    private val containerUploader: ContainerUploaderCommon by instance()

    private val contentImportManager: ContentImportManager by on(Endpoint(endpointUrl)).instance()

    private val containerManager: ContainerDownloadManager by on(Endpoint(endpointUrl)).instance()

    private val currentUploadAttempt = atomic(null as Deferred<Int>?)

    private val currentHttpClient = defaultHttpClient()

    private val _connectivityStatus = DoorMutableLiveData<ConnectivityStatus>()

    val connectivityStatus: DoorLiveData<ConnectivityStatus>
        get() = _connectivityStatus

    /**
     * Lock used for any operation that is changing the status of the Fetch download
     */
    private val downloadStatusLock = Mutex()

    suspend fun importContainer(isFileImport: Boolean = false) {

        val filePath = if(isFileImport) containerImportJob.filePath?.removePrefix("file://") else containerImportJob.filePath

        val container = contentImportManager.importFileToContainer(
                filePath!!,
                containerImportJob.mimeType!!,
                containerImportJob.contentEntryUid,
                containerImportJob.containerBaseDir!!){

        } ?: return
        containerImportJob.cujContainerUid = container.containerUid
        db.containerImportJobDao.updateImportComplete()

        if(isFileImport){
            containerManager.handleContainerLocalImport(container)
        }
    }

    suspend fun startUpload(): Int {

        var attemptNum = 0
        var uploadAttemptStatus = JobStatus.FAILED
        while (attemptNum++ < 3) {

            try {

                val containerEntryWithFileList = db.containerEntryDao
                        .findByContainer(containerImportJob.cujContainerUid)

                var containerEntryUidList = containerImportJob.containerEntryFileUids
                if (containerEntryUidList.isNullOrEmpty()) {

                    val listOfMd5SumStr = containerEntryWithFileList.map { it.containerEntryFile?.cefMd5 }
                            .joinToString(";")

                    val md5sServerDoesntHave = currentHttpClient.post<List<String>> {
                        url(UMFileUtil.joinPaths(endpointUrl, "/ContainerUpload/checkExistingMd5/"))
                        body = listOfMd5SumStr
                    }

                    val containerEntriesServerDoesntHave = containerEntryWithFileList
                            .filter { it.containerEntryFile?.cefMd5 in md5sServerDoesntHave }

                    containerEntryUidList = containerEntriesServerDoesntHave.map { it.containerEntryFile?.cefUid }
                            .joinToString(";")

                    containerImportJob.containerEntryFileUids = containerEntryUidList
                    db.containerImportJobDao.update(containerImportJob)
                }

                if (containerEntryUidList.isNotEmpty()) {

                    val request = ContainerUploaderRequest(containerImportJob.cujUid,
                            containerEntryUidList, UMFileUtil.joinPaths(endpointUrl, "/upload/"), endpointUrl)


                    var jobDeferred: Deferred<Int>? = null

                    downloadStatusLock.withLock {

                        jobDeferred = containerUploader.enqueue(request)
                        currentUploadAttempt.value = jobDeferred

                    }
                    uploadAttemptStatus = jobDeferred?.await() ?: JobStatus.FAILED

                } else {
                    uploadAttemptStatus = JobStatus.COMPLETE
                }

                val containerEntries = db.containerEntryDao.findByContainerWithMd5(containerImportJob.cujContainerUid)

                if (uploadAttemptStatus == JobStatus.COMPLETE) {

                    val container = db.containerDao.findByUid(containerImportJob.cujContainerUid)
                            ?: throw Exception()

                    val job = db.containerImportJobDao.findByUid(containerImportJob.cujUid)
                    val code = currentHttpClient.post<HttpStatement>() {
                        url(UMFileUtil.joinPaths(endpointUrl,
                                "/ContainerUpload/finalizeEntries/"))
                        if(!job?.sessionId.isNullOrEmpty()){
                            parameter("sessionId", job?.sessionId)
                        }
                        header("content-type", "application/json")
                        body = ContainerWithContainerEntryWithMd5(container, containerEntries)
                    }.execute().status

                    if (code != HttpStatusCode.NoContent) {
                        throw Exception()
                    }

                    return uploadAttemptStatus

                }

            } catch (e: Exception) {

                println("${e.cause?.message}")
                val connectivityState = connectivityStatus.getValue()?.connectivityState
                if (connectivityState != ConnectivityStatus.STATE_UNMETERED && connectivityState != ConnectivityStatus.STATE_METERED) {
                    return JobStatus.QUEUED
                }

                delay(retryDelay)
            }
        }

        return uploadAttemptStatus

    }


    companion object {
        const val DEFAULT_RETRY_DELAY = 1000L
    }


}