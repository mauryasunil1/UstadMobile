package com.ustadmobile.lib.contentscrapers.abztract

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.dao.ScrapeQueueItemDao
import com.ustadmobile.lib.contentscrapers.ContentScraperUtil
import com.ustadmobile.lib.contentscrapers.LanguageList
import com.ustadmobile.lib.contentscrapers.ScraperConstants
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.ScrapeQueueItem
import kotlinx.coroutines.runBlocking

abstract class Indexer(val parentContentEntryUid: Long, val runUid: Int, val db: UmAppDatabase) {


    var parentcontentEntry: ContentEntry? = null
    val contentEntryDao = db.contentEntryDao
    val contentEntryParentChildJoinDao = db.contentEntryParentChildJoinDao
    val queueDao = db.scrapeQueueItemDao
    val languageDao = db.languageDao
    val englishLang = ContentScraperUtil.insertOrUpdateLanguageByName(languageDao, "English")
    val masterRootParent =  ContentScraperUtil.createOrUpdateContentEntry(ScraperConstants.ROOT, ScraperConstants.USTAD_MOBILE,
            ScraperConstants.ROOT, ScraperConstants.USTAD_MOBILE, ContentEntry.LICENSE_TYPE_CC_BY, englishLang.langUid, null,
            ScraperConstants.EMPTY_STRING, false, ScraperConstants.EMPTY_STRING, ScraperConstants.EMPTY_STRING,
            ScraperConstants.EMPTY_STRING, ScraperConstants.EMPTY_STRING, 0, contentEntryDao)


    init {
        runBlocking {
            parentcontentEntry = db.contentEntryDao.findByUidAsync(parentContentEntryUid)
        }
        LanguageList().addAllLanguages()
    }

    fun createQueueItem(queueUrl: String, contentEntry: ContentEntry, contentType: String, scraperType: Int){
        var item = queueDao.getExistingQueueItem(runUid, queueUrl)
        if (item == null) {
            item = ScrapeQueueItem()
            item.scrapeUrl = queueUrl
            item.sqiContentEntryParentUid = contentEntry.contentEntryUid
            item.status = ScrapeQueueItemDao.STATUS_PENDING
            item.contentType = contentType
            item.runId = runUid
            item.itemType = scraperType
            item.timeAdded = System.currentTimeMillis()
            queueDao.insert(item)
        }
    }

    abstract fun indexUrl(sourceUrl: String)

    abstract fun close()

}