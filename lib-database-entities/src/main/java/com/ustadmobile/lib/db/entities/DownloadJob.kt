package com.ustadmobile.lib.db.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.ustadmobile.lib.database.annotation.UmEntity
import com.ustadmobile.lib.database.annotation.UmPrimaryKey

/**
 * A DownloadJob represents a specific run of downloading a DownloadSet. The DownloadSet contains
 * the list of entries that are to be downloaded. One DownloadSet can have multiple DownloadJobs, e.g.
 * one DownloadJob that initially downloads it, and then further DownloadJobs when it is updated, when
 * new entries become available, etc.
 */
@UmEntity
@Entity
open class DownloadJob {

    @UmPrimaryKey(autoIncrement = true)
    @PrimaryKey(autoGenerate = true)
    var djUid: Long = 0

    var djDsUid: Long = 0

    var timeCreated: Long = 0

    var timeRequested: Long = 0

    var timeCompleted: Long = 0

    var totalBytesToDownload: Long = 0

    var bytesDownloadedSoFar: Long = 0

    /**
     * Status as per flags on NetworkTask
     */
    var djStatus: Int = 0


    /**
     * Empty constructor
     */
    constructor()

    constructor(downloadSet: DownloadSet) {
        this.djDsUid = downloadSet.dsUid.toLong()
    }
}
