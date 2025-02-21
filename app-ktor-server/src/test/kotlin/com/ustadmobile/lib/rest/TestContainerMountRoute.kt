@file:Suppress("unused")

package com.ustadmobile.lib.rest

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.door.entities.NodeIdAndAuth
import com.ustadmobile.lib.db.entities.Container
import io.ktor.client.HttpClient
import io.ktor.server.engine.ApplicationEngine
import java.io.File
import okhttp3.OkHttpClient

/**
 * This test is BROKEN 16/Dec/2020
 */
class TestContainerMountRoute {

    lateinit var server: ApplicationEngine

    lateinit var db: UmAppDatabase

    lateinit var repo: UmAppDatabase

    private lateinit var nodeIdAndAuth: NodeIdAndAuth

    lateinit var container: Container

    lateinit var epubTmpFile: File

    lateinit var containerTmpDir: File

    private val defaultPort = 8098

    //private lateinit var containerManager: ContainerManager

    private var testPath: String = ""

    private lateinit var httpClient: HttpClient

    private lateinit var okHttpClient: OkHttpClient

//    //@Before
//    fun setup() {
//        okHttpClient = OkHttpClient()
//        httpClient = HttpClient(OkHttp){
//            install(JsonFeature)
//            engine {
//                preconfigured = okHttpClient
//            }
//        }
//        nodeIdAndAuth = NodeIdAndAuth(Random.nextLong(0, Long.MAX_VALUE),
//            randomUuid().toString())
//
//        db = DatabaseBuilder.databaseBuilder(Any(), UmAppDatabase::class, "UmAppDatabase")
//            .addSyncCallback(nodeIdAndAuth)
//            .build()
//            .clearAllTablesAndResetNodeId(nodeIdAndAuth.nodeId)
//
//        repo = db.asRepository(repositoryConfig(Any(), "http://localhost/dummy",
//            nodeIdAndAuth.nodeId, nodeIdAndAuth.auth, httpClient, okHttpClient))
//        server = embeddedServer(Netty, port = defaultPort) {
//            install(ContentNegotiation) {
//                gson {
//                    register(ContentType.Application.Json, GsonConverter())
//                    register(ContentType.Any, GsonConverter())
//                }
//            }
//
//            install(Routing) {
//                ContainerMountRoute()
//            }
//        }.start(wait = false)
//
//        containerTmpDir = UmFileUtilSe.makeTempDir("testcontainermountroute", "tmpdir")
//        container = Container()
//        container.containerUid = repo.containerDao.insert(container)
//
//        runBlocking {
//            repo.addEntriesToContainerFromZipResource(container.containerUid, this::class.java,
//                    "/testfiles/thelittlechicks.epub",
//                    ContainerAddOptions(storageDirUri = containerTmpDir.toDoorUri()))
//        }
//
//        testPath = db.containerEntryDao.findByContainer(container.containerUid)[13].cePath!!
//    }
//
//    //@After
//    fun tearDown() {
//        server.stop(0, 7000)
//        httpClient.close()
//    }
//
//    //@Test
//    fun givenMountRequest_whenNoContainerExists_shouldRespondWithNotFound() {
//        runBlocking {
//            val httpClient = HttpClient {
//                install(JsonFeature)
//            }
//
//            httpClient.use {
//                val mountResponse = httpClient.get<HttpStatement>(
//                        "http://localhost:${defaultPort}/ContainerMount/${container.containerUid + 1}/epub.css").execute()
//                Assert.assertEquals("Container to be mounted was not found",HttpResponseStatus.NOT_FOUND.code(), mountResponse.status.value)
//            }
//        }
//    }
//
//    //@Test
//    fun givenMountRequest_whenContainerExistsAndFileExists_shouldMountAndServeTheFile() {
//        runBlocking {
//            val httpClient = HttpClient {
//                install(JsonFeature)
//            }
//
//            httpClient.use {
//                val mountResponse = httpClient.get<HttpStatement>(
//                        "http://localhost:${defaultPort}/ContainerMount/${container.containerUid}/${testPath}").execute()
//                Assert.assertEquals("Container was mounted and requested file found",HttpResponseStatus.OK.code(), mountResponse.status.value)
//            }
//        }
//    }
//
//    //@Test
//    fun givenMountRequest_whenHeadRequestedOnExistingFile_shouldMountAndServeRequiredDetails() {
//        runBlocking {
//            val httpClient = HttpClient {
//                install(JsonFeature)
//            }
//
//            httpClient.use {
//                val mountResponse = httpClient.head<HttpStatement>(
//                        "http://localhost:${defaultPort}/ContainerMount/${container.containerUid}/${testPath}").execute()
//                Assert.assertTrue("Container mounted and responded with content length",
//                        200 == mountResponse.status.value && mountResponse.headers[HttpHeaders.ContentLength]!!.toInt() > 0)
//            }
//        }
//    }


}

