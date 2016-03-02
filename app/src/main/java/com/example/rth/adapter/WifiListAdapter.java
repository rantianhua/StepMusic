package com.example.rth.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.Layout;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.rth.base.OnRecylerViewItemClickListener;
import com.example.rth.stepmusic.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by rth on 16-2-21.
 */
public class WifiListAdapter extends RecyclerView.Adapter<WifiListAdapter.ViewHolder> {

    //数据源
    private List<Map<String,String>> wifiAps = new ArrayList<>();
    //单击事件的监听
    private OnRecylerViewItemClickListener clickListener;
    //LayoutInflater
    LayoutInflater inflater;
    //item的背景色
    private int background;

    public WifiListAdapter(Context context,
                           OnRecylerViewItemClickListener listener) {
        clickListener = listener;
        inflater = LayoutInflater.from(context);
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.selectableItemBackground,typedValue,
                true);
        background = typedValue.resourceId;
    }

    public void addWifiAp(Map<String,String> ap) {
        wifiAps.add(ap);
    }

    /**
     * 清除现有数据
     */
    public void clearCurrentData() {
        if(wifiAps.size() > 0) wifiAps.clear();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        //点击的item的序号
        public int boundPositon;
        public TextView tvApName;
        public View view;

        public ViewHolder(View itemView) {
            super(itemView);
            view = itemView;
            tvApName = (TextView)view.findViewById(R.id.item_tv_wifiap_name);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_wifiaps,parent,false);
        v.setBackgroundResource(background);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.boundPositon = position;
        holder.tvApName.setText(wifiAps.get(position).get("name"));
        holder.view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clickListener.onItemClicked(holder.boundPositon);
            }
        });
    }

    @Override
    public int getItemCount() {
        return wifiAps.size();
    }
}
