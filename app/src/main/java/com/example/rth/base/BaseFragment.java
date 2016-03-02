package com.example.rth.base;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by rth on 16-2-21.
 */
public class BaseFragment extends Fragment implements ViewInitWork{

    //展示提示信息
    public ProgressDialog pd;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pd = new ProgressDialog(getContext());
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
