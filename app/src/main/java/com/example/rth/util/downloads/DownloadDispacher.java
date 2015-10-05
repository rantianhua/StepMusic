package com.example.rth.util.downloads;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by rth on 15-10-5.
 * 该类获取要下载的文件的大小，然后将任务分配给若干个工作线程
 */
public class DownloadDispacher {

    private static final String TAG = "DownloadDispacher";
    private Context context;
    private FileManager fileManager;    //下载记录的管理
    private boolean exit;   //标识是否停止下载
    private int downloadSize = 0;   //已经下载的文件长度
    private int fileSize =  0;  //要下载的文件的长度
    private DownDetail[] threads;   //下载的所有线程
    private File saveFile;  //本地保存文件
    private Map<Integer,Integer> records = new HashMap<>(); //缓存每个线程的下载进度
    private int block;  //每条线程下载的长度
    private String downUrl; //下载路径

    public DownloadDispacher(Context context,String url,File saveDir,int threadNum) {
        this.context = context;
        this.downUrl = url;
        this.threads = new DownDetail[threadNum];
        fileManager = new FileManager(context);
        if(!saveDir.exists()) {
            //保存目录不在则新建一个
            saveDir.mkdirs();
        }
        caculateBlock(saveDir);
    }

    /**
     * 获取文件大小并并计算每个线程要下载的长度
     */
    private void caculateBlock(File saveDir) {
        try {
            URL link = new URL(this.downUrl);
            //获取下载文件的长度
            HttpURLConnection conn = HttpConfig.getHttpConnection(link);
            if(conn == null) throw new RuntimeException("Connot initial httpurlconnection");
            conn.connect();
            if(conn.getResponseCode() == 200) {
                //响应成功
                this.fileSize = conn.getContentLength();
                if(this.fileSize < 0) throw new RuntimeException("Unkown file size");
                String fileName = getFileName(conn);
                this.saveFile = new File(saveDir,fileName);
                //检查是否已有下载记录
                Map<Integer,Integer> oldRecord = fileManager.getDownloadData(downUrl);
                if(oldRecord.size() > 0) {
                    //已有下载记录
                    for (Map.Entry<Integer,Integer> entry : oldRecord.entrySet()) {
                        //复制已有的下载记录
                        records.put(entry.getKey(),entry.getValue());
                    }
                }
                if(records.size() == threads.length) {
                    //计算已下载的总长度
                    for(int i = 0;i < threads.length;i++) {
                        downloadSize += records.get(i+1);
                    }
                }
                //计算每个线程需要下载的长度
                block = fileSize % threads.length == 0 ? fileSize / threads.length :
                         fileSize / threads.length + 1;
            }else {
                throw new RuntimeException("server no response");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从连接信息中获取要下载的文件名
     * @param conn 已经建立的http连接
     * @return 要下载的文件名称
     */
    private String getFileName(HttpURLConnection conn) {
        String name = this.downUrl.substring(downUrl.lastIndexOf('/') + 1);
        if(TextUtils.isEmpty(name)) {
            for (int i = 0;; i++) {
                String mine = conn.getHeaderField(i);
                if (mine == null)
                    break;
                if ("content-disposition".equals(conn.getHeaderFieldKey(i)
                        .toLowerCase())) {
                    Matcher m = Pattern.compile(".*filename=(.*)").matcher(
                            mine.toLowerCase());
                    if (m.find())
                        return m.group(1);
                }
            }
            name = UUID.randomUUID() + ".tmp";
        }
        return name;
    }

    public int dispacher(DownloadListener listener) {
        try {
            RandomAccessFile ranOut = new RandomAccessFile(saveFile,"rw");
            if(fileSize > 0) ranOut.setLength(fileSize);    //预分配ranOut的大小
            ranOut.close();
            URL url = new URL(downUrl);
            if(records.size() != threads.length) {
                //未曾下载过或与之前的下载线程数不一致
                records.clear();    //清空记录
                //重新初始化
                for (int i = 0;i < threads.length;i++) {
                    records.put(i+1,0);
                }
                downloadSize = 0;
            }
            //开启线程下载
            for(int i = 0;i < threads.length;i++) {
                int downLen = records.get(i+1);
                if(downLen < block && downloadSize < fileSize) {
                    //没有下载完成，继续下载
                    threads[i] = new DownDetail(this,url,saveFile,block,records.get(i+1),i+1);
                    threads[i].setPriority(7);  //线程优先级
                    threads[i].start();
                }else {
                    threads[i] = null;
                }
            }
            //若有下载记录，先删除，再重新添加
            fileManager.delete(downUrl);
            fileManager.saveLengths(downUrl,records);
            boolean notFinish =true;// 下载为完成
            while (notFinish) {
                Thread.sleep(900);
                notFinish = false;// 假定全部线程下载完成
                for (int i = 0; i < this.threads.length; i++) {
                    if (this.threads[i] != null && !this.threads[i].isFinish()) {// 如果发现线程未完成下载
                        notFinish = true;// 设置标志为下载没有完成
                        if (this.threads[i].getDownLength() == -1) {// 如果下载失败,再重新下载
                            this.threads[i] = new DownDetail(this, url,
                                    this.saveFile, this.block,
                                    this.records.get(i + 1), i + 1);
                            this.threads[i].setPriority(7);
                            this.threads[i].start();
                        }
                    }
                }
                if (listener != null)
                    listener.onDownloadSize(this.downloadSize);// 通知目前已经下载完成的数据长度
            }
            if (downloadSize == this.fileSize)
                fileManager.delete(this.downUrl);// 下载完成删除记录
        } catch (Exception e) {
            e.printStackTrace();
        }
        return downloadSize;
    }


    /**
     * 是否已经退出下载
     * @return
     */
    public boolean isExit() {
        return exit;
    }

    /**
     * 累计下载大小
     * @param size
     */
    public synchronized void append(int size) {
        downloadSize += size;
    }

    /**
     * 更新线程最后下载的位置
     * @param threadId  线程id
     * @param pos   最后下载的位置
     */
    public synchronized void update(int threadId,int pos) {
        records.put(threadId,pos);
        fileManager.update(downUrl,threadId,pos);
    }
}
