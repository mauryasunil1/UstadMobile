package com.ustadmobile.lib.db.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.ustadmobile.lib.annotation.SyncablePrimaryKey
import com.ustadmobile.lib.database.annotation.UmEntity
import com.ustadmobile.lib.database.annotation.UmSyncLastChangedBy
import com.ustadmobile.lib.database.annotation.UmSyncLocalChangeSeqNum
import com.ustadmobile.lib.database.annotation.UmSyncMasterChangeSeqNum
import com.ustadmobile.lib.db.entities.Clazz.Companion.TABLE_ID


@UmEntity(tableId = TABLE_ID)
@Entity
open class Clazz {

    @SyncablePrimaryKey
    @PrimaryKey(autoGenerate = true)
    var clazzUid: Long = 0

    var clazzName: String? = null

    var attendanceAverage: Float = 0.toFloat()

    @UmSyncMasterChangeSeqNum
    var clazzMasterChangeSeqNum: Long = 0

    @UmSyncLocalChangeSeqNum
    var clazzLocalChangeSeqNum: Long = 0

    @UmSyncLastChangedBy
    var clazzLastChangedBy: Int = 0

    var clazzLocationUid: Long = 0

    constructor()

    constructor(clazzName: String) {
        this.clazzName = clazzName
    }

    constructor(clazzName: String, clazzLocationUid: Long) {
        this.clazzName = clazzName
        this.clazzLocationUid = clazzLocationUid
    }

    companion object {

        const val TABLE_ID = 6
    }
}
