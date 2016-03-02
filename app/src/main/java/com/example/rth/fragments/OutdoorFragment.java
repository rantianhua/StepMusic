package com.example.rth.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.SupportMapFragment;
import com.amap.api.maps2d.model.Circle;
import com.amap.api.maps2d.model.CircleOptions;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.Polyline;
import com.amap.api.maps2d.model.PolylineOptions;
import com.example.rth.util.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by rth on 16-2-21.
 */
public class OutdoorFragment extends SupportMapFragment
        implements AMapLocationListener,LocationSource {

    //Amap
    private AMap aMap;
    //定位
    private AMapLocationClient locationClient;
    //定位参数
    private AMapLocationClientOption locationOption;
    //定位监听器
    private OnLocationChangedListener listener;
    //'我的位置'的标记
    private Marker myMarker;
    private MarkerOptions mypositionMaker;
    //是否首次定位
    private boolean firstLocate = true;
    //目的地的坐标
    private LatLng desCoordination;
    //我的位置
    private AMapLocation myPoint;
    //Timer用来模拟人物走动
    private Timer timer;
    //画折线图
    private Polyline polyline;
    private PolylineOptions polylineOptions;
    //圆环的宽度,单位为米
    private final float ringWidth = 20;
    //当前显示的圆环
    private Circle currentRing;
    //我前一次与环的状态
    private int preStateOfRing = OUT_A_RING;
    //存储圆
    private Map<Float,Circle> circleMaps = new HashMap<>();
    //生成一个随机数
    private final Random random = new Random();
    //回调函数
    private CallbackInOutdoor callback;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        //标记我的位置
        mypositionMaker = new MarkerOptions();
        mypositionMaker.draggable(false);
        //折线图，显示我的轨迹
        polylineOptions = new PolylineOptions();
        polylineOptions.setDottedLine(true);
        polylineOptions.zIndex(1);
    }

    /**
     * 初始化定位参数
     */
    private void initLocateOption() {
        if(locationClient == null) {
            locationClient = new AMapLocationClient(this.getActivity().getApplicationContext());
            locationOption = new AMapLocationClientOption();
            // 设置定位模式为高精度模式
            locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);
            // 设置定位监听
            locationClient.setLocationListener(this);
            //停止强制刷新wifi
            locationOption.setWifiActiveScan(false);
            //定位时间间隔
            //locationOption.setInterval(4000);
            locationOption.setOnceLocation(true);
            locationClient.setLocationOption(locationOption);
        }
    }

    public void setCallback(CallbackInOutdoor callback) {
        this.callback = callback;
    }

    @Override
    public void onStart() {
        super.onStart();
        setUpAmap();
    }

    /**
     * 配置Amap的一些属性
     */
    private void setUpAmap() {
        if(aMap == null) {
            aMap = getMap();
            // 矢量地图模式
            aMap.setMapType(AMap.MAP_TYPE_NORMAL);
            //设置定位监听
            aMap.setLocationSource(this);
            //显示定位按钮
            aMap.getUiSettings().setMyLocationButtonEnabled(true);
            //比例尺功能
            aMap.getUiSettings().setScaleControlsEnabled(true);
            //缩放按钮
            aMap.getUiSettings().setZoomControlsEnabled(true);
            aMap.getUiSettings().setZoomGesturesEnabled(true);
            //开启定位层
            aMap.setMyLocationEnabled(true);
        }
    }

    @Override
    public void onLocationChanged(AMapLocation aMapLocation) {
        if(aMapLocation != null) {
            if(aMapLocation.getErrorCode() == 0) {
                //定位成功
                handler.obtainMessage(LOCATE_FIRST,-1,-1
                        ,aMapLocation).sendToTarget();
            }else {
                //定位失败
                handler.obtainMessage(LOCATE_FAILED
                        ,aMapLocation.getErrorCode(),-1,
                        aMapLocation.getErrorInfo()).sendToTarget();
            }

        }
    }

    @Override
    public void onStop() {
        super.onStop();
        deactivate();
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    //Handler处理异步消息
    private final Handler handler = new Handler() {
        @Override
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case LOCATE_FIRST:
                    //将缩放比例放到最大
                    myPoint = (AMapLocation) msg.obj;
                    dramMyPosition();
                    break;
                case LOCATE_FAILED:
                    showLongMessage("定位失败:" + msg.arg1
                            + (String) msg.obj);
                    break;
                case START_LOCATE:
                    showLongMessage("激活定位");
                    break;
                case STOP_LOCATE:
                    showLongMessage("停止定位");
                    break;
                case MOVE_LOCATE:
                    LatLng newP = (LatLng)msg.obj;
                    myPoint.setLatitude(newP.latitude);
                    myPoint.setLongitude(newP.longitude);
                    dramMyPosition();
                    break;
            }
        }
    };

    /**
     * 画出我的位置
     */
    private void dramMyPosition() {
        LatLng myLatlng = new LatLng(myPoint.getLatitude(),myPoint.getLongitude());
        mypositionMaker = mypositionMaker.position(myLatlng);
        polylineOptions.add(myLatlng);
        if(firstLocate) {
            firstLocate = false;
            //在地图上标记我的位置
            myMarker = aMap.addMarker(mypositionMaker);
            aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLatlng, 19));
            //在地图上标记目的地的位置
            desCoordination = new LatLng(myLatlng.latitude,myLatlng.longitude + 0.0012);
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(desCoordination);
            aMap.addMarker(markerOptions);
            //画出第一个圆环
            drawRing(desCoordination, ringWidth * 3 - ringWidth / 2);
            test(myPoint);
        }else {
            //更新我的位置标记并画出折线图
            aMap.moveCamera(CameraUpdateFactory.changeLatLng(myLatlng));
            myMarker.remove();
            myMarker = aMap.addMarker(mypositionMaker);
            if(polyline != null) {
                polyline.remove();
            }
            polyline = aMap.addPolyline(polylineOptions);
            //判断我是否在当前圆内
            boolean inRing = false;
            float distance = AMapUtils.calculateLineDistance(myLatlng,currentRing.getCenter());
            if(currentRing.getRadius() == ringWidth) {
                //此时是最里面的一个圆环，实际是个圆
                if(distance <= ringWidth) {
                    inRing = true;
                }
            }else {
                float half = ringWidth / 2;
                float radius = (float) currentRing.getRadius();
                if(radius-half <= distance && distance <= radius+half) {
                    //在圆环内
                    inRing = true;
                }
            }
            //判断我是离开还是进入圆环
            if(inRing && preStateOfRing == OUT_A_RING) {
                //进入当前圆环
                preStateOfRing = IN_A_RING;
                //showLongMessage("进入圆环");
                String musicKey = String.valueOf(currentRing.getRadius());
                Log.e("准备播放",musicKey);
                if(callback != null) {
                    callback.requireMusic(musicKey);
                }
            }
            if(!inRing && preStateOfRing == IN_A_RING) {
                //离开当前圆环
                preStateOfRing = OUT_A_RING;
                //showLongMessage("离开圆环");
                drawNextCircle(distance);
            }
        }
    }

    /**
     * 找出要画的下一个圆
     * @param distance
     */
    private void drawNextCircle(float distance) {
        float newRadius = 0;
        if(distance > currentRing.getRadius()) {
            if(currentRing.getRadius() == ringWidth) {
                newRadius = (float)currentRing.getRadius()+ringWidth/2;
            }else {
                newRadius = (float)currentRing.getRadius()+ringWidth;
            }
        }else {
            if((currentRing.getRadius()-ringWidth) == ringWidth/2) {
                newRadius = (float)currentRing.getRadius()-ringWidth/2;
            }else {
                newRadius = (float)currentRing.getRadius()-ringWidth;
            }
        }
        if(newRadius > (3*ringWidth - ringWidth/2)) {
            return;
        }
        currentRing.setVisible(false);
        drawRing(desCoordination, newRadius);
    }

    /**
     * 画一个表示范围的圆环
     * @param des 圆心点
     * @param i 圆的半径
     */
    private void drawRing(LatLng des, float i) {
        Circle circle = null;
        int ringColor = Constants.CIRCLE_COLORS.get(random.nextInt(3));
        //先看集合里有没有圆
        if(circleMaps.containsKey(i)) {
            circle = circleMaps.get(i);
            circle.setVisible(true);
        }else {
            if(i == ringWidth) {
                //直接画圆
                circle = aMap.addCircle(new CircleOptions()
                        .center(des).radius(ringWidth).fillColor(ringColor)
                        .strokeWidth(3).strokeColor(Color.TRANSPARENT));
            }else {
                circle = aMap.addCircle(new CircleOptions().center(des).
                        radius(i).strokeWidth(ringWidth/aMap.getScalePerPixel())
                        .fillColor(Color.TRANSPARENT).strokeColor(ringColor));
            }
            circleMaps.put(i,circle);
        }
        setCurrentRing(circle);
    }

    private synchronized void setCurrentRing(Circle circle) {
        this.currentRing = circle;
    }

    private synchronized Circle getCurrentRing() {
        return this.currentRing;
    }

    /**
     * 模拟移动的测试方法
     * @param location 我的初始位置
     */
    private void test(final AMapLocation location) {
        if(timer == null) timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                LatLng newPoint = new LatLng(location.getLatitude(),
                        location.getLongitude() + 0.00006);
                handler.obtainMessage(MOVE_LOCATE, -1, -1, newPoint).sendToTarget();
            }
        }, 100, 1500);
//        initLocateOption();
//        locationOption.setOnceLocation(false);
//        locationOption.setInterval(2000);
//        locationClient.startLocation();
    }

    /**
     * 激活定位
     * @param onLocationChangedListener 定位的监听函数
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        //handler.obtainMessage(START_LOCATE).sendToTarget();
        listener = onLocationChangedListener;
        initLocateOption();
        locationClient.startLocation();
    }

    /**
     * 停止定位
     */
    @Override
    public void deactivate() {
        if(locationClient != null) {
            locationClient.stopLocation();
            locationClient.onDestroy();
            locationClient = null;
            locationOption = null;
            if(callback != null) callback.stopLocate();
        }
        //handler.obtainMessage(STOP_LOCATE).sendToTarget();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        setCallback(null);
    }

    private void showLongMessage(String message) {
        Toast.makeText(getActivity(),message,Toast.LENGTH_SHORT).show();
    }

    /**
     * 暂停定位
     */
    public void pauseLocate() {
        if(timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /**
     * 继续定位
     *
     */
    public void resumeLocate() {
        if(timer == null) test(myPoint);
    }

    //首次定位
    private static final int LOCATE_FIRST = 0;
    //定位失败
    private static final int LOCATE_FAILED = 1;
    //激活定位
    private static final int START_LOCATE = 2;
    //停止定位
    private static final int STOP_LOCATE = 3;
    //新的移动的定位点
    private static final int MOVE_LOCATE = 4;
    //表示我在一个环内
    private static final int IN_A_RING = 5;
    //表示我没在一个环内
    private static final int OUT_A_RING = 6;

    /**
     * 与Activity通信
     */
    public interface CallbackInOutdoor {
        /**
         * 请求播放音乐
         * @param musicKey
         */
        void requireMusic(String musicKey);

        void stopLocate();

    }

}
