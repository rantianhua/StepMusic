package com.example.rth.util.downloads;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.rth.db.DBOpenHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by rth on 15-10-5.
 * 保存下载记录以及下载的文件
 */
public class FileManager {

    private DBOpenHelper openHelper;    //获取数据库操作

    public FileManager(Context context) {
        openHelper = new DBOpenHelper(context);
    }

    /**
     * 获取每条线程下载的文件长度
     * @param url   下载文件的url
     * @return 每条线程的下载记录
     */
    public Map<Integer,Integer> getDownloadData(String url) {
        SQLiteDatabase db = openHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select threadid,downlength from download_records where downurl=?", new String[]{url});
        Map<Integer,Integer> data = new HashMap<>();
        while (cursor.moveToNext()) {
            data.put(cursor.getInt(0),cursor.getInt(1));
        }
        cursor.close();
        db.close();
        return data;
    }

    /**
     * 保存每线程已经下载的长度
     * @param url    下载的链接
     * @param map    下载的长度和线程id等等数据
     */
    public void saveLengths(String url,Map<Integer,Integer> map) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            for(Map.Entry<Integer,Integer> entry : map.entrySet()) {
                //插入数据
                db.execSQL("insert into download_records(downurl, threadid, downlength) values(?,?,?)",
                        new Object[]{url,entry.getKey(),entry.getValue()});
            }
            db.setTransactionSuccessful();
        }finally {
            db.endTransaction();
        }
        db.close();
    }

    /**
     * 更新表中每条线程已经下载的文件长度
     * @param url   //下载的url
     * @param thId  //下载的线程id
     * @param length    //当前线程下载的长度
     */
    public void update(String url,int thId,int length) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.execSQL(
                "update download_records set downlength=? where downurl=? and threadid=?",
                new Object[]{length, url, thId});
        db.close();
    }

    /**
     * 删除已经下载完成的下载记录
     * @param url   下载的链接
     */
    public void delete(String url) {
        SQLiteDatabase db = openHelper.getWritableDatabase();
        db.execSQL("delete from download_records where downurl=?",
                new Object[] { url });
        db.close();
    }
}
