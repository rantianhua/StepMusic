package com.example.rth.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by rth on 15-10-5.
 * 数据库操作的辅助类
 */
public class DBOpenHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "musics_db";  //数据库名
    private static final int VERSION = 1;   //数据库版本

    public DBOpenHelper(Context context) {
        super(context,DB_NAME,null,VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        //创建下载音乐文件的记录表
        sqLiteDatabase.execSQL("create table if not exists download_records (id integer primary key autoincrement, downurl varchar(200), threadid INTEGER, downlength INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("drop table if exists download_records");
        onCreate(sqLiteDatabase);
    }
}
