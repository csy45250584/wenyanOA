package com.haokuo.wenyanoa.fragment;

import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.alibaba.fastjson.JSON;
import com.chad.library.adapter.base.BaseQuickAdapter;
import com.haokuo.wenyanoa.R;
import com.haokuo.wenyanoa.adapter.DishesAdapter;
import com.haokuo.wenyanoa.bean.GetFoodListResultBean;
import com.haokuo.wenyanoa.bean.UserInfoBean;
import com.haokuo.wenyanoa.eventbus.DishRefreshEvent;
import com.haokuo.wenyanoa.eventbus.WeekdaySelectedEvent;
import com.haokuo.wenyanoa.network.HttpHelper;
import com.haokuo.wenyanoa.network.NetworkCallback;
import com.haokuo.wenyanoa.network.bean.GetInFoodListParams;
import com.haokuo.wenyanoa.network.bean.SaveFoodInBasketParams;
import com.haokuo.wenyanoa.util.OaSpUtil;
import com.haokuo.wenyanoa.util.utilscode.TimeUtils;
import com.haokuo.wenyanoa.util.utilscode.ToastUtils;
import com.haokuo.wenyanoa.view.RecyclerViewDivider;
import com.scwang.smartrefresh.layout.SmartRefreshLayout;
import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Calendar;
import java.util.List;

import butterknife.BindView;
import okhttp3.Call;

/**
 * Created by zjf on 2018-08-08.
 */

public class DishesFragment extends BaseLazyLoadFragment {
    private static final String TYPE = "type";
    public static final int TYPE_BREAKFAST = 0;
    public static final int TYPE_LAUNCH = 1;
    public static final int TYPE_DINNER = 2;
    @BindView(R.id.rv_dishes)
    RecyclerView mRvDishes;
    @BindView(R.id.srl_dishes)
    SmartRefreshLayout mSrlDishes;
    private DishesAdapter mDishesAdapter;
    private int mWeekday;
    private UserInfoBean mUserInfo;
    private int mType;

    public static DishesFragment newInstance(int type) {
        DishesFragment frag = new DishesFragment();
        Bundle args = new Bundle();
        args.putInt(TYPE, type);
        frag.setArguments(args);
        return frag;
    }

    @Override
    protected int initContentLayout() {
        return R.layout.fragment_dishes;
    }

    @Override
    protected void initData() {
        EventBus.getDefault().register(this);
        Bundle arguments = getArguments();
        if (arguments != null) {
            mType = arguments.getInt(TYPE);
        }
        mWeekday = TimeUtils.getWeekIndex(TimeUtils.getNowDate());
        mWeekday = mWeekday == 1 ? 7 : mWeekday - 1;
        Resources resources = getResources();
        mRvDishes.setLayoutManager(new LinearLayoutManager(mContext));
        int dividerPadding = (int) (resources.getDimension(R.dimen.dp_16) + 0.5f);
        int dividerHeight = (int) (resources.getDimension(R.dimen.dp_1) + 0.5f);
        mRvDishes.addItemDecoration(new RecyclerViewDivider(mContext, LinearLayoutManager.HORIZONTAL, dividerHeight, R.color.divider,
                dividerPadding, dividerPadding));
        mDishesAdapter = new DishesAdapter(R.layout.item_dishes);
        mRvDishes.setAdapter(mDishesAdapter);
        mSrlDishes.setEnableLoadMore(false);
        mUserInfo = OaSpUtil.getUserInfo();
    }

    @Override
    protected void initListener() {
        mDishesAdapter.setOnItemChildClickListener(new BaseQuickAdapter.OnItemChildClickListener() {
            @Override
            public void onItemChildClick(BaseQuickAdapter adapter, View view, int position) {
                final GetFoodListResultBean.DishesBean item = mDishesAdapter.getItem(position);
                if (item == null) {
                    return;
                }
                switch (view.getId()) {
                    case R.id.btn_add_dish:
                        View inflate = LayoutInflater.from(mContext).inflate(R.layout.dialog_add_dish, null);
                        final EditText etPersonalInfo = inflate.findViewById(R.id.et_personal_info);
                        etPersonalInfo.setInputType(InputType.TYPE_CLASS_NUMBER);
                        etPersonalInfo.setText("1");
                        etPersonalInfo.setHint("数量");
                        new AlertDialog.Builder(mContext)
                                .setTitle("请输入菜品数量")
                                .setView(inflate)
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        int num = Integer.parseInt(etPersonalInfo.getEditableText().toString().trim());
                                        //添加到菜篮
                                        mContext.showLoading("添加中");
                                        SaveFoodInBasketParams params = new SaveFoodInBasketParams(mUserInfo.getUserId(), mUserInfo.getApikey(), item.getId(), num, item.getSalesStatus(), getSelectedDate(mWeekday));
                                        HttpHelper.getInstance().saveFoodInBasket(params, new NetworkCallback() {
                                            @Override
                                            public void onSuccess(Call call, String json) {
                                                mContext.loadClose();
                                                ToastUtils.showShort("添加菜品成功");
                                                EventBus.getDefault().post(new DishRefreshEvent());
                                            }

                                            @Override
                                            public void onFailure(Call call, String message) {
                                                mContext.loadClose();
                                                ToastUtils.showShort("添加菜品失败，" + message);
                                            }
                                        });
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .create().show();
                        break;
                    //                    case R.id.iv_delete_dish:
                    //                        mDishesAdapter.changeCount(position, false);
                    //                        GetFoodListResultBean.DishesBean item = mDishesAdapter.getItem(position);
                    //                        String selectedDate = getSelectedDate(mWeekday);
                    //                        EventBus.getDefault().post(new DishClickEvent(item, mWeekday, selectedDate));
                    //                        break;
                    //                    case R.id.iv_add_dish:
                    //                        mDishesAdapter.changeCount(position, true);
                    //
                    //                        break;
                }
            }
        });
        mSrlDishes.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(@NonNull final RefreshLayout refreshLayout) {
                GetInFoodListParams params = new GetInFoodListParams(mUserInfo.getUserId(), mUserInfo.getApikey(), mWeekday, mType);
                HttpHelper.getInstance().getFoodList(params, new NetworkCallback() {
                    @Override
                    public void onSuccess(Call call, String json) {
                        List<GetFoodListResultBean.DishesBean> data = JSON.parseObject(json, GetFoodListResultBean.class).getData();
                        mDishesAdapter.setNewData(data);
                        refreshLayout.finishRefresh();
                    }

                    @Override
                    public void onFailure(Call call, String message) {
                        ToastUtils.showShort("获取菜品列表失败，" + message);
                        refreshLayout.finishRefresh(false);
                    }
                });
            }
        });
    }

    private String getSelectedDate(int weekday) {
        int currentWeekday = TimeUtils.getWeekIndex(TimeUtils.getNowDate());
        currentWeekday = currentWeekday == 1 ? 7 : currentWeekday - 1;

        if (currentWeekday == weekday) {
            return TimeUtils.getNowString(TimeUtils.CUSTOM_FORMAT);
        }
        if (currentWeekday < weekday) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, weekday - currentWeekday);
            return TimeUtils.date2String(calendar.getTime(), TimeUtils.CUSTOM_FORMAT);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, weekday - currentWeekday + 7);
        return TimeUtils.date2String(calendar.getTime(), TimeUtils.CUSTOM_FORMAT);
    }

    @Subscribe(priority = 1)
    public void onWeekdaySelected(WeekdaySelectedEvent event) {
        mWeekday = event.getWeekday();
        mDishesAdapter.setNewData(null);
        resetLoadData();
    }

    @Override
    protected void loadData() {
        mSrlDishes.autoRefresh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}
