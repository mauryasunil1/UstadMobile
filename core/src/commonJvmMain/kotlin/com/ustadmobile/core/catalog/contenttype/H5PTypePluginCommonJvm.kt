package com.ustadmobile.core.catalog.contenttype

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.io.ext.addEntriesToContainerFromZip
import com.ustadmobile.core.util.getAssetFromResource
import com.ustadmobile.lib.db.entities.Container
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.ContentEntryWithLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import com.ustadmobile.core.container.ContainerAddOptions
import com.ustadmobile.core.io.ext.addFileToContainer
import com.ustadmobile.door.DoorUri
import com.ustadmobile.door.ext.toDoorUri
import com.ustadmobile.door.ext.writeToFile
import com.ustadmobile.door.ext.openInputStream
import com.ustadmobile.core.container.PrefixContainerFileNamer
import kotlinx.serialization.json.*


val licenseMap = mapOf(
        "CC-BY" to ContentEntry.LICENSE_TYPE_CC_BY,
        "CC BY-SA" to ContentEntry.LICENSE_TYPE_CC_BY_SA,
        "CC BY-ND" to ContentEntry.LICENSE_TYPE_CC_BY,
        "CC BY-NC" to ContentEntry.LICENSE_TYPE_OTHER,
        "CC BY-NC-SA" to ContentEntry.LICENSE_TYPE_CC_BY_NC_SA,
        "CC CC-BY-NC-CD" to ContentEntry.LICENSE_TYPE_OTHER,
        "CC0 1.0" to ContentEntry.LICENSE_TYPE_CC_0,
        "GNU GPL" to ContentEntry.LICENSE_TYPE_OTHER,
        "PD" to ContentEntry.LICENSE_TYPE_PUBLIC_DOMAIN,
        "ODC PDDL" to ContentEntry.LICENSE_TYPE_OTHER,
        "CC PDM" to ContentEntry.LICENSE_TYPE_OTHER,
        "C" to ContentEntry.ALL_RIGHTS_RESERVED,
        "U" to ContentEntry.LICENSE_TYPE_OTHER
)

class H5PTypePluginCommonJvm(): H5PTypePlugin() {

    override suspend fun extractMetadata(uri: String, context: Any): ContentEntryWithLanguage? {
        return withContext(Dispatchers.Default){
            var contentEntry: ContentEntryWithLanguage? = null
            try {
                val doorUri = DoorUri.parse(uri)
                val inputStream = doorUri.openInputStream(context)

                ZipInputStream(inputStream).use {
                    var zipEntry: ZipEntry? = null
                    while ({ zipEntry = it.nextEntry; zipEntry }() != null) {

                        val fileName = zipEntry?.name
                        if (fileName?.toLowerCase() == "h5p.json") {

                            val data = String(it.readBytes())

                            val json = Json.parseToJsonElement(data) as JsonObject

                            // take the name from the role Author otherwise take last one
                            var author: String? = ""
                            var name: String? = ""
                            json["authors"]?.jsonArray?.forEach {
                                name = it.jsonObject["name"]?.jsonPrimitive?.content ?: ""
                                val role = it.jsonObject["role"]?.jsonPrimitive?.content ?: ""
                                if (role == "Author") {
                                    author = name
                                }
                            }
                            if (author.isNullOrEmpty()) {
                                author = name
                            }

                            contentEntry = ContentEntryWithLanguage().apply {
                                contentFlags = ContentEntry.FLAG_IMPORTED
                                contentTypeFlag = ContentEntry.TYPE_INTERACTIVE_EXERCISE
                                licenseType = licenseMap[json.jsonObject["license"] ?: ""]
                                        ?: ContentEntry.LICENSE_TYPE_OTHER
                                title = json.jsonObject["title"]?.jsonPrimitive?.content
                                this.author = author
                                leaf = true
                            }
                            break
                        }

                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: XmlPullParserException) {
                e.printStackTrace()
            }

            contentEntry
        }
    }

    override suspend fun importToContainer(uri: String, conversionParams: Map<String, String>,
                                           contentEntryUid: Long, mimeType: String, containerBaseDir: String,
                                           context: Any,
                                           db: UmAppDatabase, repo: UmAppDatabase,
                                           progressListener: (Int) -> Unit): Container {
        val doorUri = DoorUri.parse(uri)
        val container = Container().apply {
            containerContentEntryUid = contentEntryUid
            cntLastModified = System.currentTimeMillis()
            this.mimeType = mimeType
            containerUid = repo.containerDao.insert(this)
        }

        val entry = db.contentEntryDao.findByUid(contentEntryUid)

        val containerAddOptions = ContainerAddOptions(storageDirUri = File(containerBaseDir).toDoorUri())
        repo.addEntriesToContainerFromZip(container.containerUid, doorUri,
                ContainerAddOptions(storageDirUri = File(containerBaseDir).toDoorUri(),
                        fileNamer = PrefixContainerFileNamer("workspace/")), context)

        val h5pDistTmpFile = File.createTempFile("h5p-dist", "zip")
        val h5pDistIn = getAssetFromResource("/com/ustadmobile/sharedse/h5p/dist.zip", context)
                ?: throw IllegalStateException("Could not find h5p dist file")
        h5pDistIn.writeToFile(h5pDistTmpFile)
        repo.addEntriesToContainerFromZip(container.containerUid, h5pDistTmpFile.toDoorUri(),
                containerAddOptions, context)
        h5pDistTmpFile.delete()


        // generate tincan.xml
        val tinCan = """
            <?xml version="1.0" encoding="UTF-8"?>
            <tincan xmlns="http://projecttincan.com/tincan.xsd">
                <activities>
                    <activity id="${entry?.entryId ?: ""}" type="http://adlnet.gov/expapi/activities/module">
                        <name>${entry?.title ?: ""}</name>
                        <description lang="en-US">${entry?.description ?: ""}</description>
                        <launch lang="en-us">index.html</launch>
                    </activity>
                </activities>
            </tincan>
        """.trimIndent()

        val tmpTinCanFile = File.createTempFile("h5p-tincan", "xml")
        tmpTinCanFile.writeText(tinCan)
        repo.addFileToContainer(container.containerUid, tmpTinCanFile.toDoorUri(),
                "tincan.xml", containerAddOptions)
        tmpTinCanFile.delete()


        // generate index.html
        val tmpIndexHtmlFile = File.createTempFile("h5p-index", "html")
        val h5pIndexIn = getAssetFromResource("/com/ustadmobile/sharedse/h5p/index.html", context)
                ?: throw IllegalStateException("Could not open h5p index.html file")
        h5pIndexIn.writeToFile(tmpIndexHtmlFile)
        repo.addFileToContainer(container.containerUid, tmpIndexHtmlFile.toDoorUri(),
                "index.html", containerAddOptions)
        tmpIndexHtmlFile.delete()

        return repo.containerDao.findByUid(container.containerUid) ?: container

    }
}