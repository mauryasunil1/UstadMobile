package com.ustadmobile.db

import androidx.paging.DataSource
import com.ustadmobile.core.db.dao.ContentEntryRelatedEntryJoinDao
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.util.UmReactUtil
import com.ustadmobile.util.UmReactUtil.loadList
import kotlinx.serialization.builtins.ListSerializer

class ReactContentEntryRelatedEntryJoinDao: ContentEntryRelatedEntryJoinDao() {

    private val languagePath = "languages.json"

    private val relationPath = "entry_relation.json"

    override fun publicContentEntryRelatedEntryJoins(): List<ContentEntryRelatedEntryJoin> {
        TODO("Not yet implemented")
    }

    override fun findPrimaryByTranslation(contentEntryUid: Long): ContentEntryRelatedEntryJoin? {
        TODO("Not yet implemented")
    }

    override suspend fun findAllTranslationsForContentEntryAsync(contentEntryUid: Long): List<ContentEntryRelatedEntryJoinWithLangName> {
        TODO("Not yet implemented")
    }

    override fun findAllTranslationsWithContentEntryUid(contentEntryUid: Long): DataSource.Factory<Int, ContentEntryRelatedEntryJoinWithLanguage> {
        return DataSourceFactoryJs(null,contentEntryUid,relationPath,
            ListSerializer(ContentEntryRelatedEntryJoinWithLanguage.serializer()),
            ListSerializer(Language.serializer()), "language","cerejRelLanguageUid",
            "langUid","languages.json"
        )
    }

    override fun update(entity: ContentEntryRelatedEntryJoin) {
        TODO("Not yet implemented")
    }

    override fun insert(entity: ContentEntryRelatedEntryJoin): Long {
        TODO("Not yet implemented")
    }

    override suspend fun insertAsync(entity: ContentEntryRelatedEntryJoin): Long {
        TODO("Not yet implemented")
    }

    override fun insertList(entityList: List<ContentEntryRelatedEntryJoin>) {
        TODO("Not yet implemented")
    }

    override fun updateList(entityList: List<ContentEntryRelatedEntryJoin>) {
        TODO("Not yet implemented")
    }
}