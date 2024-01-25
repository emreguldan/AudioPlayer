package com.example.audioplayer.data.local.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class Audio(
    val uri: Uri?,
    val displayName: String?,
    val id: Long,
    val artist: String?,
    val data: String?,
    val duration: Int,
    val title: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(Uri::class.java.classLoader),
        parcel.readString(),
        parcel.readLong(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uri, flags)
        parcel.writeString(displayName)
        parcel.writeLong(id)
        parcel.writeString(artist)
        parcel.writeString(data)
        parcel.writeInt(duration)
        parcel.writeString(title)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Audio> {
        override fun createFromParcel(parcel: Parcel): Audio {
            return Audio(parcel)
        }

        override fun newArray(size: Int): Array<Audio?> {
            return arrayOfNulls(size)
        }
    }
}
