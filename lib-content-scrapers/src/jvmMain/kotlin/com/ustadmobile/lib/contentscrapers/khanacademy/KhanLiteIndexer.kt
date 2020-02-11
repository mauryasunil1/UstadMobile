package com.ustadmobile.lib.contentscrapers.khanacademy

import ScraperTypes.KHAN_LITE_VIDEO_SCRAPER
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.lib.contentscrapers.ContentScraperUtil
import com.ustadmobile.lib.contentscrapers.ScraperConstants
import com.ustadmobile.lib.contentscrapers.UMLogUtil
import com.ustadmobile.lib.contentscrapers.abztract.ScraperException
import com.ustadmobile.lib.contentscrapers.abztract.SeleniumIndexer
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.Language
import com.ustadmobile.lib.db.entities.LanguageVariant
import com.ustadmobile.lib.db.entities.ScrapeQueueItem
import org.openqa.selenium.By
import org.openqa.selenium.support.ui.ExpectedConditions
import java.net.URL

class KhanLiteIndexer(parentContentEntry: Long, runUid: Int, db: UmAppDatabase) : SeleniumIndexer(parentContentEntry, runUid, db) {

    override fun indexUrl(sourceUrl: String) {

        val khanEntry = getKhanEntry(englishLang, contentEntryDao)

        ContentScraperUtil.insertOrUpdateParentChildJoin(contentEntryParentChildJoinDao, masterRootParent, khanEntry, 12)

        val lang = sourceUrl.substringBefore(".khan").substringAfter("://")

        val khanLang = KhanConstants.khanLangMap[lang]
                ?: throw ScraperException(0, "Do not have support for lite language: $lang")

        val parentEntry = createEntry(if (lang == "www") "en" else lang, khanLang.title, khanLang.url)

        ContentScraperUtil.insertOrUpdateParentChildJoin(contentEntryParentChildJoinDao, khanEntry, parentEntry, 0)

        val document = startSeleniumIndexer(sourceUrl) {

            it.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("div#library-content-main")))

        }

        val fullList = document.select("div#library-content-main div[data-role=page]")

        fullList.forEachIndexed { count, element ->

            val header = element.selectFirst("div.library-content-header h2")?.text() ?: ""

            if (header.isNullOrEmpty()) {
                UMLogUtil.logError("page had a missing header text for count $count for url $sourceUrl")
                return@forEachIndexed
            }

            val description = element.select("div.library-content-list p.topic-desc")?.text() ?: ""

            val headerEntry = ContentScraperUtil.createOrUpdateContentEntry(header, header, header,
                    ScraperConstants.KHAN, ContentEntry.LICENSE_TYPE_CC_BY_NC,
                    parentEntry.primaryLanguageUid, parentEntry.languageVariantUid,
                    description, false, ScraperConstants.EMPTY_STRING, "",
                    ScraperConstants.EMPTY_STRING, ScraperConstants.EMPTY_STRING,
                    0, contentEntryDao)

            ContentScraperUtil.insertOrUpdateParentChildJoin(contentEntryParentChildJoinDao, parentEntry, headerEntry, count)

            val contentList = element.select("div.library-content-list li.subjects-row-first td a.subject-link")

            contentList.forEachIndexed { contentCount, contentElement ->

                val href = contentElement.attr("href")
                val title = contentElement.text()

                val contentUrl = URL(URL(sourceUrl), href)

                val contentId = contentUrl.toString().substringAfter("v=")

                if (contentId.isNullOrEmpty()) {
                    UMLogUtil.logError("no Content Id found for element $title  with href $href in heading $header on url $sourceUrl")
                    return@forEachIndexed
                }

                val entry = ContentScraperUtil.createOrUpdateContentEntry(contentId, title,
                        KhanContentIndexer.KHAN_PREFIX + contentId, ScraperConstants.KHAN,
                        ContentEntry.LICENSE_TYPE_CC_BY_NC, parentEntry.primaryLanguageUid,
                        parentEntry.languageVariantUid, "", true,
                        ScraperConstants.EMPTY_STRING, "",
                        ScraperConstants.EMPTY_STRING, ScraperConstants.EMPTY_STRING,
                        ContentEntry.VIDEO_TYPE, contentEntryDao)

                ContentScraperUtil.insertOrUpdateParentChildJoin(contentEntryParentChildJoinDao, headerEntry, entry, contentCount)

                createQueueItem(contentUrl.toString(), entry, KHAN_LITE_VIDEO_SCRAPER, ScrapeQueueItem.ITEM_TYPE_SCRAPE)

            }

        }

    }

    private fun createEntry(langCode: String, langName: String, url: String): ContentEntry {

        val langSplitArray = langCode.split("-")
        val langEntity: Language
        langEntity = if(langSplitArray[0].length == 2){
            ContentScraperUtil.insertOrUpdateLanguageByTwoCode(languageDao, langSplitArray[0])
        }else{
            ContentScraperUtil.insertOrUpdateLanguageByThreeCode(languageDao, langSplitArray[0])
        }

        var langVariantEntity: LanguageVariant? = null
        if (langSplitArray.size > 1) {
            langVariantEntity = ContentScraperUtil.insertOrUpdateLanguageVariant(db.languageVariantDao, langSplitArray[1], langEntity)
        }

        return ContentScraperUtil.createOrUpdateContentEntry(url, langName,
                url, ScraperConstants.KHAN, ContentEntry.LICENSE_TYPE_CC_BY_NC,
                langEntity.langUid, langVariantEntity?.langVariantUid ?: 0,
                ScraperConstants.EMPTY_STRING, false, ScraperConstants.EMPTY_STRING, ScraperConstants.EMPTY_STRING,
                ScraperConstants.EMPTY_STRING, ScraperConstants.EMPTY_STRING,
                0, contentEntryDao)

    }


    override fun close() {

    }
}