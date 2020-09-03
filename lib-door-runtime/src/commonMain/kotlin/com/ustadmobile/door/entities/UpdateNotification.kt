package com.ustadmobile.door.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(indices = [Index(value = ["pnDeviceId", "pnTableId"], unique = true),
        Index(value = ["pnDeviceId", "pnTimestamp"], unique = false)])
class UpdateNotification(
        @PrimaryKey
        var pnUid: Long = 0,
        var pnDeviceId: Int = 0,
        var pnTableId: Int = 0,
        var pnTimestamp: Long = 0)