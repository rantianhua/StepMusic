package com.example.rth.serviecs;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by rth on 15-10-4.
 * 播放音乐的服务
 */
public class MusicServices extends Service {

    private MediaPlayer player; //音乐播放器
    //网络流媒体的缓冲监听
    private final MediaPlayer.OnBufferingUpdateListener bufferingUpdateListener = new MediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {

        }
    };
    //
    private final MediaPlayer.OnPreparedListener preparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {
            if(callbackInMusicService != null) callbackInMusicService.startPlay();
            mediaPlayer.start();
        }
    };
    //错误监听器
    private final MediaPlayer.OnErrorListener errorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
            return false;
        }
    };

    //返回MusicService实例
    public class LocalMusicBinder extends Binder {
        public MusicServices getService() {
            return MusicServices.this;
        }
    }
    private LocalMusicBinder localMusicBinder = new LocalMusicBinder();

    //回调接口
    private CallbackInMusicService callbackInMusicService;


    @Override
    public void onCreate() {
        super.onCreate();
        initMediaPlayer();
    }

    /**
     * 初始化Mediaplayer
     */
    private void initMediaPlayer() {
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnBufferingUpdateListener(bufferingUpdateListener);
        player.setOnPreparedListener(preparedListener);
    }

    /**
     * 设置回调接口
     * @param callbackInMusicService
     */
    public void setCallbackInMusicService(CallbackInMusicService callbackInMusicService) {
        this.callbackInMusicService = callbackInMusicService;
    }

    /**
     * 播放音乐
     */
    public void play() {
        if(player != null) player.start();
    }

    /**
     * 暂停
     */
    public void pause() {
        if(player != null) player.pause();
    }

    /**
     * 停止播放音乐
     */
    public void stop() {
        if(player != null) {
            player.stop();
            player.release();
        }
    }

    /**
     * 播放网络音乐
     * @param url 网络音乐地址
     */
    public void playUrl(String url) {
        if(TextUtils.isEmpty(url)) return;
        try {
            if(callbackInMusicService != null) {
                callbackInMusicService.preparingMusic();
            }
            player.reset();
            player.setDataSource(url);
            player.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        localMusicBinder = null;
        stop();
        return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return localMusicBinder;
    }

    /**
     * 回调接口
     */
    public interface CallbackInMusicService {

        /**
         * 正在加载网络音乐
         */
        void preparingMusic();

        /**
         * 开始播放音乐
         */
        void startPlay();
    }
}
