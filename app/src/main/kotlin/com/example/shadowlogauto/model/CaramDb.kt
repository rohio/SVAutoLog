package com.example.shadowlogauto.model

import android.os.Parcel
import android.os.Parcelable

data class CaramDb(val id: Int,
                   val result: String,
                   val myClass: String,
                   val myDeck: String,
                   val opponentClass: String,
                   val opponentDeck: String,
                   val turn: String,
                   val cardName: ArrayList<String>,
                   val cardMulligan: ArrayList<String>,
                   val timeStamp: String) : Parcelable{

    companion object {
        @JvmField
        val CREATOR : Parcelable.Creator<CaramDb> = object : Parcelable.Creator<CaramDb>{
            override fun createFromParcel(source: Parcel): CaramDb = source.run {
                /* NOT NULLとするために、 nullの場合エルビス演算子で空文字、空のListにする*/
                CaramDb(readInt(),
                        readString() ?: "",
                        readString() ?: "",
                        readString() ?: "",
                        readString() ?: "",
                        readString() ?: "",
                        readString() ?: "",
                        createStringArrayList() ?: arrayListOf(),
                        createStringArrayList() ?: arrayListOf(),
                        readString() ?: "")
            }

            override fun newArray(size: Int): Array<CaramDb?> = arrayOfNulls(size)
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.run{
            writeInt(id)
            writeString(result)
            writeString(myClass)
            writeString(myDeck)
            writeString(opponentClass)
            writeString(opponentDeck)
            writeString(turn)
            writeStringList(cardName)
            writeStringList(cardMulligan)
            writeString(timeStamp)
        }
    }


}