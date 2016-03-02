package com.example.rth.stepmusic;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.example.rth.base.BaseActivity;
import com.example.rth.fragments.IndoorFragment;
import com.example.rth.fragments.OutdoorFragment;
import com.example.rth.serviecs.MusicServices;


/**
 * Created by rth on 16-2-20.
 */
public class LauncherActivity extends BaseActivity implements
        TabLayout.OnTabSelectedListener,ServiceConnection,MusicServices.CallbackInMusicService,
        IndoorFragment.CallbackInIndoorFrag,OutdoorFragment.CallbackInOutdoor{

    //TabLayout
    private TabLayout tabLayout;
    //Toolbar
    private Toolbar toolbar;
    //IndoorFragment的实例
    private IndoorFragment indoorFragment;
    //OutdoorFragment的实例
    private OutdoorFragment outdoorFragment;
    //播放音乐的服务
    private MusicServices musicServices;
    //标识需不需要播放音乐
    private boolean needPlayMusic = false;
    //需要播放音乐的wifi
    private String palyMusicKey;
    //标识是否正在播放音乐
    private boolean isPlaying = false;
    //当前显示的Fragment
    private Fragment currentFrag;
    //当前控制定位动作
    private String currentControll;
    //管理Fragment
    private FragmentPagerAdapter fragmentPagerAdapter;
    //显示Fragment的container
    private FrameLayout container;
    //标示是否实力化过fragment
    private boolean instantiated = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
        currentControll = getString(R.string.controll_locate_pause);
        indoorFragment = new IndoorFragment();
        indoorFragment.setCallback(this);
        outdoorFragment = new OutdoorFragment();
        outdoorFragment.setCallback(this);
        fragmentPagerAdapter = new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                if(position == 0) {
                    return outdoorFragment;
                }else if(position == 1) {
                    return indoorFragment;
                }
                return null;
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
        setupView();
        bindsEvents();
        //绑定服务
        bindService(new Intent(this, MusicServices.class),
                this, BaseActivity.BIND_AUTO_CREATE);
    }

    @Override
    public void setupView() {
        toolbar = (Toolbar) findViewById(R.id.main_aty_toolbar);
        tabLayout = (TabLayout) findViewById(R.id.main_aty_tablayout);
        setSupportActionBar(toolbar);
        container = (FrameLayout)findViewById(R.id.aty_main_frame_container);
        tabLayout.addTab(tabLayout.newTab().setText("室外"), true);
        tabLayout.addTab(tabLayout.newTab().setText("室内"));
        tabLayout.setOnTabSelectedListener(this);
        showFragment(0,outdoorFragment);
    }

    /**
     * 显示Fragment
     * @param i 0显示OutdoorFragment
     *          1显示IndoorFragment
     */
    private void showFragment(int i,Fragment fragment) {
        fragmentPagerAdapter.instantiateItem(container,i);
        fragmentPagerAdapter.setPrimaryItem(container,i,fragment);
        fragmentPagerAdapter.finishUpdate(container);
        currentFrag = fragment;
    }

    @Override
    public void bindsEvents() {
        super.bindsEvents();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.indoor_actions, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem itemSearch = menu.findItem(R.id.action_scanwifi);
        MenuItem itemLocate = menu.findItem(R.id.action_locate_controll);
        if(currentFrag instanceof IndoorFragment) {
            itemSearch.setVisible(true);
            itemLocate.setVisible(false);
        }else {
            itemSearch.setVisible(false);
            itemLocate.setVisible(true);
            itemLocate.setTitle(currentControll);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scanwifi:
                indoorFragment.refreshWifi();
                return true;
            case R.id.action_locate_controll:
                if(currentControll.equals(getString(R.string.controll_locate_pause))) {
                    currentControll = getString(R.string.controll_locate_continue);
                    //暂停定位
                    outdoorFragment.pauseLocate();
                }else {
                    currentControll = getString(R.string.controll_locate_pause);
                    //继续定位
                    outdoorFragment.resumeLocate();
                }
                item.setTitle(currentControll);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(TabLayout.Tab tab) {
        showFragment(tab.getPosition(),
                tab.getPosition() == 0 ? outdoorFragment : indoorFragment);
        invalidateOptionsMenu();
    }

    @Override
    public void onTabUnselected(TabLayout.Tab tab) {
        if(tab.getPosition() == 0) {
            fragmentPagerAdapter.destroyItem(container, 0, outdoorFragment);
        }else {
            fragmentPagerAdapter.destroyItem(container,1,indoorFragment);
        }
        fragmentPagerAdapter.finishUpdate(container);
    }

    @Override
    public void onTabReselected(TabLayout.Tab tab) {
        //shortToast("重新选中Tab:"+tab.getText());
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        musicServices = ((MusicServices.LocalMusicBinder)iBinder).getService();
        musicServices.setCallbackInMusicService(this);
        if(needPlayMusic) {
            needPlayMusic = false;
            musicServices.requirePlay(palyMusicKey);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        musicServices.setCallbackInMusicService(null);
        musicServices = null;
    }

    /**
     * 在MusicService中回调
     * 正在加载网络音乐
     */
    @Override
    public void preparingMusic() {
        showPd("正在加载新的音乐...");
    }

    /**
     * 在MusicService中回调
     * 开始播放网络音乐
     */
    @Override
    public void startPlay() {
        dismissPd();
        isPlaying = true;
    }

    /**
     * 在MusicService中回调
     * @param flag    0表示开始加载,1表示加载完毕,2表示加载信息失败
     */
    @Override
    public void loadingData(int flag) {
        if(flag == 0) {
            showPd("正在加载热点资源...");
        }else if(flag == 1) {
            dismissPd();
        }else if(flag == 2) {
            dismissPd();
            shortToast("未获取到热点信息");
        }
    }

    /**
     *  在IndoorFragment中回调
     * @param ssid 当前连接的wifi
     */
    @Override
    public void playMusic(String ssid) {
        palyMusicKey = ssid;
        if(musicServices != null) {
            musicServices.requirePlay(ssid);
        }else {
            needPlayMusic = true;
        }
    }

    /**
     *  在IndoorFragment中回调
     */
    @Override
    public boolean isPlaying() {
        return this.isPlaying;
    }

    /**
     *  在OutdoorFragment中回调
     */
    @Override
    public void requireMusic(String musicKey) {
        palyMusicKey = musicKey;
        if(musicServices != null) {
            musicServices.requirePlay(musicKey);
        }else {
            needPlayMusic = true;
        }
    }

    /**
     *  在OutdoorFragment中回调
     */
    @Override
    public void stopLocate() {
        currentControll = getString(R.string.controll_locate_continue);
        invalidateOptionsMenu();
    }

    @Override
    protected void onDestroy() {
        unbindService(this);
        super.onDestroy();
    }
}
