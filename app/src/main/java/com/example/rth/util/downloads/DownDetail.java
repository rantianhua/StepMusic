package com.example.rth.util.downloads;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by rth on 15-10-5.
 * 实现每条线程下载的细节
 */
public class DownDetail extends Thread{
    private static final String TAG = "DownDetail";

    private File saveFile;  //保存文件
    private URL url;    //下载链接
    private int block;  //该线程需要下载的长度
    private int threadId;   //线程id
    private int downLen;    //已经下载的长度u
    private boolean finish = false; //标识下载是否已经完成
    private DownloadDispacher dispacher;    //下载任务的分配器


    public DownDetail(DownloadDispacher downloadDispacher, URL url,
                      File saveFile, int block, int downLen,int threadId) {
        this.url = url;
        this.saveFile = saveFile;
        this.block = block;
        this.dispacher = downloadDispacher;
        this.downLen = downLen;
        this.threadId = threadId;
    }

    @Override
    public void run() {
        if(downLen < block) {
            //执行下载
            try {
                HttpURLConnection http = HttpConfig.getHttpConnection(url);
                int startPos = block * (threadId - 1) + downLen;    //开始位置
                int endPos = block * threadId - 1;  //结束位置
                if(http == null) throw new RuntimeException("cannot initial http");
                //设置数据范围
                http.setRequestProperty("Range","bytes="+startPos+"-"+endPos);
                //得到输入流
                InputStream in = http.getInputStream();
                byte[] buffer = new byte[1024];
                int offset = 0;
                RandomAccessFile threadFile = new RandomAccessFile(saveFile,"rwd");
                //定位pos位置
                threadFile.seek(startPos);
                while (!dispacher.isExit() && (offset = in.read(buffer,0,1024)) != -1) {
                    //写入文件
                    threadFile.write(buffer,0,offset);
                    downLen += offset;  //累计下载大小
                    dispacher.update(threadId,downLen);
                    //已下载的大小
                    dispacher.append(offset);
                }
                threadFile.close();
                in.close();
            }catch (Exception e) {
                e.printStackTrace();
                downLen = -1;
            }
        }
    }

    /**
     * 判断下载是否已经完成
     * @return
     */
    public boolean isFinish() {return finish;}

    /**
     * 已经下载的内容大小
     * @return -1表示下载失败
     */
    public long getDownLength() {
        return downLen;
    }
}
