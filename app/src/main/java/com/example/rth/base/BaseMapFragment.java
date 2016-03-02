package com.example.rth.base;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;

import com.amap.api.maps2d.MapFragment;

import java.util.Map;

/**
 * Created by rth on 16-2-28.
 */
public class BaseMapFragment extends MapFragment implements ViewInitWork {

    //展示提示信息
    public ProgressDialog pd;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        pd = new ProgressDialog(getActivity());
    }

    @Override
    public void setupView() {

    }

    @Override
    public void setupView(View view) {

    }

    @Override
    public void bindsEvents() {

    }
}
