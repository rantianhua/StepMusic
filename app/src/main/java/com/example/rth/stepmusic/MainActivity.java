package com.example.rth.stepmusic;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.rth.serviecs.MusicServices;


public class MainActivity extends Activity implements ServiceConnection,MusicServices.CallbackInMusicService{

    private MusicServices musicServices;    //播放音乐的服务

    private ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pd = new ProgressDialog(this);
        pd.setMessage("正在加载网络音乐...");
        //绑定服务
        bindService(new Intent(this, MusicServices.class), this, BIND_AUTO_CREATE);
    }

    /**
     * 播放网络音乐
     * @param view
     */
    public void playUrlMusic(View view) {
        if(musicServices == null) {
            Toast.makeText(this,"服务未绑定，请稍候！",Toast.LENGTH_SHORT).show();
        }else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    musicServices.playUrl("http://stepmusics-library.stor.sinaapp.com/%E6%AD%BB%E4%BA%86%E9%83%BD%E8%A6%81%E7%88%B1%20-%20%E4%BF%A1%E4%B9%90%E5%9B%A2.mp3");
                }
            }).start();
            //http://115.28.85.146/msg.wav
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        musicServices = ((MusicServices.LocalMusicBinder)iBinder).getService();
        musicServices.setCallbackInMusicService(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        musicServices.setCallbackInMusicService(null);
        musicServices = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(this);
    }

    /**
     * 在MusicService中回调
     * 正在加载网络音乐
     */
    @Override
    public void preparingMusic() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                pd.show();
            }
        });
    }

    /**
     * 在MusicService中回调
     * 开始播放网络音乐
     */
    @Override
    public void startPlay() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(pd.isShowing()) {
                    pd.dismiss();
                }
            }
        });
    }
}
