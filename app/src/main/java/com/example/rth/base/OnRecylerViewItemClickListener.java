package com.example.rth.base;

/**
 * Created by rth on 16-2-21.
 */
public interface OnRecylerViewItemClickListener {
    /**
     * 点击RecylerView的item
     * @param ots　点击事件要传递的参数，供方法实体调用
     */
    void onItemClicked(Object... ots);
}
