package com.ustadmobile.lib.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.door.annotation.*
import com.ustadmobile.lib.db.entities.ContentEntry.Companion.TABLE_ID
import kotlinx.serialization.Serializable

/**
 * Entity that represents content as it is browsed by the user. A ContentEntry can be either:
 * 1. An actual piece of content (e.g. book, course, etc), in which case there should be an associated
 * ContentEntryFile.
 * 2. A navigation directory (e.g. a category as it is scraped from another site, etc), in which case
 * there should be the appropriate ContentEntryParentChildJoin entities present.
 */
@Entity
/*
@SyncableEntity(tableId = TABLE_ID,
        notifyOnUpdate = ["""
        SELECT DISTINCT UserSession.usClientNodeId AS deviceId, $TABLE_ID AS tableId 
        FROM UserSession 
    """])
 */
@Serializable
open class ContentEntry() {


    @PrimaryKey(autoGenerate = true)
    var contentEntryUid: Long = 0

    var title: String? = null

    var description: String? = null

    /**
     * Get the embedded unique ID which can be found in the underlying file, if any. For
     * example the EPUB identifier for EPUB files, or the ID attribute of an xAPI zip file.
     *
     * @return The embedded unique ID which can be found in the underlying file
     */
    /**
     * Set the embedded unique ID which can be found in the underlying file, if any. For
     * example the EPUB identifier for EPUB files, or the ID attribute of an xAPI zip file.
     *
     * @param entryId The embedded unique ID which can be found in the underlying file
     */
    var entryId: String? = null

    var author: String? = null

    var publisher: String? = null

    var licenseType: Int = 0

    var licenseName: String? = null

    var licenseUrl: String? = null

    /**
     * Get the original URL this resource came from. In the case of resources that
     * were generated by scraping, this refers to the URL that the scraper targeted to
     * generated the resource.
     *
     * @return the original URL this resource came from
     */
    /**
     * Set the original URL this resource came from. In the case of resources that
     * were generated by scraping, this refers to the URL that the scraper targeted to
     * generated the resource.
     *
     * @param sourceUrl the original URL this resource came from
     */
    var sourceUrl: String? = null

    var thumbnailUrl: String? = null

    var lastModified: Long = 0

    //TODO: Migration : add to migration
    @ColumnInfo(index = true)
    var primaryLanguageUid: Long = 0

    var languageVariantUid: Long = 0

    var contentFlags: Int = 0

    var leaf: Boolean = false

    /**
     * Represents if this content entry is public for anyone to use
     *
     * @return true if this content entry is public for anyone to use, false otherwise
     */
    /**
     * Set if this content entry is public for anyone to use
     *
     * @param publik true if this content entry is public for anyone to use, false otherwise
     */
    var publik: Boolean = true

    /**
     * Represents if this entry is being recycled or not.
     * @return true if entry was recycled false otherwise
     */
    var ceInactive: Boolean = false

    /**
     *  Represents if the entry is marked as completed by the content, student or min score
     */
    var completionCriteria: Int = COMPLETION_CRITERIA_AUTOMATIC

    /**
     * Minimum score for content to mark as complete if completion criteria is set to min score
     */
    var minScore: Int = 0

    var contentTypeFlag: Int = 0

    var contentOwner: Long = 0

    @LocalChangeSeqNum
    var contentEntryLocalChangeSeqNum: Long = 0

    @MasterChangeSeqNum
    var contentEntryMasterChangeSeqNum: Long = 0

    @LastChangedBy
    var contentEntryLastChangedBy: Int = 0

    @LastChangedTime
    var contentEntryLct: Long = 0

    constructor(title: String, description: String, leaf: Boolean, publik: Boolean) : this() {
        this.title = title
        this.description = description
        this.leaf = leaf
        this.publik = publik
    }




    companion object {

        const val TABLE_ID = 42

        const val LICENSE_TYPE_CC_BY = 1

        const val LICENSE_TYPE_CC_BY_SA = 2

        const val LICENSE_TYPE_CC_BY_SA_NC = 3

        const val LICENSE_TYPE_CC_BY_NC = 4

        const val ALL_RIGHTS_RESERVED = 5

        const val LICENSE_TYPE_CC_BY_NC_SA = 6

        const val LICENSE_TYPE_PUBLIC_DOMAIN = 7

        const val LICENSE_TYPE_OTHER = 8

        const val LICENSE_TYPE_CC_BY_ND = 10

        const val LICENSE_TYPE_CC_BY_NC_ND = 11

        const val LICENSE_TYPE_CC_0 = 9

        const val TYPE_UNDEFINED = 0

        const val TYPE_COLLECTION = 1

        const val TYPE_EBOOK = 2

        const val TYPE_INTERACTIVE_EXERCISE = 3

        const val TYPE_VIDEO = 4

        const val TYPE_AUDIO = 5

        const val TYPE_DOCUMENT = 6

        const val TYPE_ARTICLE = 7

        const val FLAG_IMPORTED = 1

        const val FLAG_CONTENT_EDITOR = 2

        const val FLAG_SCRAPPED = 4

        const val COMPLETION_CRITERIA_AUTOMATIC = 0
        const val COMPLETION_CRITERIA_MIN_SCORE = 1
        const val COMPLETION_CRITERIA_MARKED_BY_STUDENT = 2
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ContentEntry) return false

        if (contentEntryUid != other.contentEntryUid) return false
        if (title != other.title) return false
        if (description != other.description) return false
        if (entryId != other.entryId) return false
        if (author != other.author) return false
        if (publisher != other.publisher) return false
        if (licenseType != other.licenseType) return false
        if (licenseName != other.licenseName) return false
        if (licenseUrl != other.licenseUrl) return false
        if (sourceUrl != other.sourceUrl) return false
        if (thumbnailUrl != other.thumbnailUrl) return false
        if (lastModified != other.lastModified) return false
        if (primaryLanguageUid != other.primaryLanguageUid) return false
        if (languageVariantUid != other.languageVariantUid) return false
        if (contentFlags != other.contentFlags) return false
        if (leaf != other.leaf) return false
        if (publik != other.publik) return false
        if (ceInactive != other.ceInactive) return false
        if (contentTypeFlag != other.contentTypeFlag) return false
        if (completionCriteria != other.completionCriteria) return false
        if (minScore != other.minScore) return false
        if (contentEntryLocalChangeSeqNum != other.contentEntryLocalChangeSeqNum) return false
        if (contentEntryMasterChangeSeqNum != other.contentEntryMasterChangeSeqNum) return false
        if (contentEntryLastChangedBy != other.contentEntryLastChangedBy) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contentEntryUid.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (entryId?.hashCode() ?: 0)
        result = 31 * result + (author?.hashCode() ?: 0)
        result = 31 * result + (publisher?.hashCode() ?: 0)
        result = 31 * result + licenseType
        result = 31 * result + (licenseName?.hashCode() ?: 0)
        result = 31 * result + (licenseUrl?.hashCode() ?: 0)
        result = 31 * result + (sourceUrl?.hashCode() ?: 0)
        result = 31 * result + (thumbnailUrl?.hashCode() ?: 0)
        result = 31 * result + lastModified.hashCode()
        result = 31 * result + primaryLanguageUid.hashCode()
        result = 31 * result + languageVariantUid.hashCode()
        result = 31 * result + contentFlags
        result = 31 * result + leaf.hashCode()
        result = 31 * result + publik.hashCode()
        result = 31 * result + ceInactive.hashCode()
        result = 31 * result + contentTypeFlag
        result = 31 * result + completionCriteria
        result = 31 * result + minScore
        result = 31 * result + contentEntryLocalChangeSeqNum.hashCode()
        result = 31 * result + contentEntryMasterChangeSeqNum.hashCode()
        result = 31 * result + contentEntryLastChangedBy
        return result
    }

}
