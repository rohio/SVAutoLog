package com.example.shadowlogauto.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context:Context):SQLiteOpenHelper(context, "sample.db", null, 1) {

    override fun onCreate(db:SQLiteDatabase) {
        /* テーブルを作成
            NULLを扱わないためにすべてのカラムにNOT NULL制約を付加
            DBへのinsertの際にcard_name,card_mulliganを指定されていない場合nullとなってしまうので、default制約(空文字)を付加
         */
        db.execSQL(
                "create table battles_logs ("
                + "_id  integer primary key autoincrement not null, "
                + "result text not null, "
                + "my_class text not null, "
                + "my_deck text not null, "
                + "opponent_class text not null, "
                + "opponent_deck text not null, "
                + "turn text not null, "
                + "card_name1 text not null default '', "
                + "card_name2 text not null default '', "
                + "card_name3 text not null default '', "
                + "card_mulligan1 text not null default '', "
                + "card_mulligan2 text not null default '', "
                + "card_mulligan3 text not null default '', "
                + "time_stamp text not null)"
        )
    }


//    アプリケーションの更新などによって、データベースのバージョンが上がった場合に実行される処理
    override fun onUpgrade(db:SQLiteDatabase, oldVersion:Int, newVersion:Int) {
    }
}