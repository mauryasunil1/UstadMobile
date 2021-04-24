package com.ustadmobile.db

import androidx.paging.DataSource
import com.ustadmobile.core.db.dao.ContentEntryDao
import com.ustadmobile.door.DoorLiveData
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.util.UmReactUtil.loadList
import kotlinx.serialization.builtins.ListSerializer

class ReactContentEntryDao: ContentEntryDao() {

    private val sourcePath = "entries.json"

    override suspend fun insertListAsync(entityList: List<ContentEntry>) {
        TODO("Not yet implemented")
    }

    override fun downloadedRootItemsAsc(personUid: Long): DataSource.Factory<Int, ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer> {
        TODO("Not yet implemented")
    }

    override fun downloadedRootItemsDesc(personUid: Long): DataSource.Factory<Int, ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer> {
        TODO("Not yet implemented")
    }

    override suspend fun findEntryWithLanguageByEntryIdAsync(entryUuid: Long): ContentEntryWithLanguage? {
        TODO("Not yet implemented")
    }

    override suspend fun findEntryWithContainerByEntryId(entryUuid: Long): ContentEntryWithMostRecentContainer? {
        val data = loadList(sourcePath,
            ListSerializer(ContentEntryWithMostRecentContainer.serializer()))
        return (data.firstOrNull { it.contentEntryUid == entryUuid } as ContentEntryWithMostRecentContainer).apply {
            container = Container()
        }
    }

    override fun findBySourceUrl(sourceUrl: String): ContentEntry? {
        TODO("Not yet implemented")
    }

    override suspend fun findTitleByUidAsync(contentEntryUid: Long): String? {
        val data = loadList(sourcePath,
            ListSerializer(ContentEntry.serializer()))
        return data.firstOrNull { it.contentEntryUid == contentEntryUid }?.title
    }

    override fun getChildrenByParentUid(parentUid: Long): DataSource.Factory<Int, ContentEntry> {
        TODO("Not yet implemented")
    }

    override suspend fun getChildrenByParentAsync(parentUid: Long): List<ContentEntry> {
        TODO("Not yet implemented")
    }

    override suspend fun getCountNumberOfChildrenByParentUUidAsync(parentUid: Long): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getContentByUuidAsync(parentUid: Long): ContentEntry? {
        TODO("Not yet implemented")
    }

    override suspend fun findAllLanguageRelatedEntriesAsync(entryUuid: Long): List<ContentEntry> {
        TODO("Not yet implemented")
    }

    override suspend fun findListOfCategoriesAsync(parentUid: Long): List<DistinctCategorySchema> {
        TODO("Not yet implemented")
    }

    override suspend fun findUniqueLanguagesInListAsync(parentUid: Long): List<Language> {
        TODO("Not yet implemented")
    }

    override suspend fun findUniqueLanguageWithParentUid(parentUid: Long): List<LangUidAndName> {
        TODO("Not yet implemented")
    }

    override fun update(entity: ContentEntry) {
        TODO("Not yet implemented")
    }

    override suspend fun findByUidAsync(entryUid: Long): ContentEntry? {
        val data = loadList(sourcePath, ListSerializer(ContentEntry.serializer()))
        return data.firstOrNull { it.contentEntryUid == entryUid }
    }

    override fun findByUid(entryUid: Long): ContentEntry? {
        TODO("Not yet implemented")
    }

    override fun findByTitle(title: String): DoorLiveData<ContentEntry?> {
        TODO("Not yet implemented")
    }

    override suspend fun findBySourceUrlWithContentEntryStatusAsync(sourceUrl: String): ContentEntry? {
        TODO("Not yet implemented")
    }

    override fun getChildrenByParentUidWithCategoryFilterOrderByNameAsc(
        parentUid: Long,
        langParam: Long,
        categoryParam0: Long,
        personUid: Long,
        showHidden: Boolean,
        onlyFolder: Boolean
    ): DataSource.Factory<Int, ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer> {
        return DataSourceFactoryJs<Int,ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer, Any>("entryId",parentUid,sourcePath,
            ListSerializer(ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer.serializer()))
    }

    override fun getChildrenByParentUidWithCategoryFilterOrderByNameDesc(
        parentUid: Long,
        langParam: Long,
        categoryParam0: Long,
        personUid: Long,
        showHidden: Boolean,
        onlyFolder: Boolean
    ): DataSource.Factory<Int, ContentEntryWithParentChildJoinAndStatusAndMostRecentContainer> {
        TODO("Not yet implemented")
    }

    override suspend fun updateAsync(entity: ContentEntry): Int {
        TODO("Not yet implemented")
    }

    override fun getChildrenByAll(parentUid: Long): List<ContentEntry> {
        TODO("Not yet implemented")
    }

    override fun findLiveContentEntry(parentUid: Long): DoorLiveData<ContentEntry?> {
        TODO("Not yet implemented")
    }

    override fun getContentEntryUidFromXapiObjectId(objectId: String): Long {
        TODO("Not yet implemented")
    }

    override fun findSimilarIdEntryForKhan(sourceUrl: String): List<ContentEntry> {
        TODO("Not yet implemented")
    }

    override suspend fun getRecursiveDownloadTotals(contentEntryUid: Long): DownloadJobSizeInfo? {
        TODO("Not yet implemented")
    }

    override fun getAllEntriesRecursively(contentEntryUid: Long): DataSource.Factory<Int, ContentEntryWithParentChildJoinAndMostRecentContainer> {
        TODO("Not yet implemented")
    }

    override fun getAllEntriesRecursivelyAsList(contentEntryUid: Long): List<ContentEntryWithParentChildJoinAndMostRecentContainer> {
        TODO("Not yet implemented")
    }

    override fun updateContentEntryInActive(contentEntryUid: Long, ceInactive: Boolean) {
        TODO("Not yet implemented")
    }

    override fun updateContentEntryContentFlag(contentFlag: Int, contentEntryUid: Long) {
        TODO("Not yet implemented")
    }

    override fun replaceList(entries: List<ContentEntry>) {
        TODO("Not yet implemented")
    }

    override suspend fun getContentEntryFromUids(contentEntryUids: List<Long>): List<UidAndLabel> {
        TODO("Not yet implemented")
    }

    override fun insertWithReplace(entry: ContentEntry) {
        TODO("Not yet implemented")
    }

    override fun findAllLive(): DoorLiveData<List<ContentEntryWithLanguage>> {
        TODO("Not yet implemented")
    }

    override suspend fun personHasPermissionWithContentEntry(
        accountPersonUid: Long,
        contentEntryUid: Long,
        permission: Long
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun toggleVisibilityContentEntryItems(
        toggleVisibility: Boolean,
        selectedItem: List<Long>
    ) {
        TODO("Not yet implemented")
    }

    override fun insert(entity: ContentEntry): Long {
        TODO("Not yet implemented")
    }

    override suspend fun insertAsync(entity: ContentEntry): Long {
        TODO("Not yet implemented")
    }

    override fun insertList(entityList: List<ContentEntry>) {
        TODO("Not yet implemented")
    }

    override fun updateList(entityList: List<ContentEntry>) {
        TODO("Not yet implemented")
    }

}