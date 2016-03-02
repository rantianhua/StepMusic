package com.example.rth.base;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

/**
 * Created by rth on 16-2-21.
 */
public class BaseActivity extends AppCompatActivity implements ViewInitWork{
    //显示提示信息
    protected ProgressDialog pd;

    @Override
    public void setupView() {

    }

    @Override
    public void bindsEvents() {

    }

    @Override
    public void setupView(View view) {

    }

    protected void shortToast(String message) {
        Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
    }

    protected void longToast(String message) {
        Toast.makeText(this,message,Toast.LENGTH_LONG).show();
    }

    /**
     * 显示指定提示信息
     * @param message
     */
    protected void showPd(String message) {
        if(pd == null) pd = new ProgressDialog(this);
        if(!TextUtils.isEmpty(message)) pd.setMessage(message);
        pd.show();
    }

    protected void dismissPd() {
        if(pd != null && pd.isShowing()) {
            pd.dismiss();
            pd = null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        dismissPd();
    }
}
