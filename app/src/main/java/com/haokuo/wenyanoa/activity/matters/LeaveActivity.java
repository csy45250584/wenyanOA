package com.haokuo.wenyanoa.activity.matters;

import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.haokuo.midtitlebar.MidTitleBar;
import com.haokuo.wenyanoa.R;
import com.haokuo.wenyanoa.bean.PrepareMatterResultBean;
import com.haokuo.wenyanoa.bean.UserInfoBean;
import com.haokuo.wenyanoa.network.HttpHelper;
import com.haokuo.wenyanoa.network.NetworkCallback;
import com.haokuo.wenyanoa.network.bean.LaunchLeaveParams;
import com.haokuo.wenyanoa.network.bean.base.UserIdApiKeyParams;
import com.haokuo.wenyanoa.util.OaSpUtil;
import com.haokuo.wenyanoa.util.utilscode.ToastUtils;
import com.haokuo.wenyanoa.view.ApprovalItem1;
import com.haokuo.wenyanoa.view.ApprovalItem2;
import com.rey.material.widget.Button;

import butterknife.BindView;
import butterknife.OnClick;
import okhttp3.Call;

/**
 * Created by zjf on 2018-08-14.
 */

public class LeaveActivity extends BaseCcActivity {
    @BindView(R.id.mid_title_bar)
    MidTitleBar mMidTitleBar;
    @BindView(R.id.ai_leave_type)
    ApprovalItem1 mAiLeaveType;
    @BindView(R.id.ai_start_date)
    ApprovalItem1 mAiStartDate;
    @BindView(R.id.ai_end_date)
    ApprovalItem1 mAiEndDate;
    @BindView(R.id.ai_duration)
    ApprovalItem1 mAiDuration;
    @BindView(R.id.ai_detail_info)
    ApprovalItem2 mAiDetailInfo;
    @BindView(R.id.ai_approvers)
    ApprovalItem2 mAiApprovers;
    @BindView(R.id.ai_cc)
    ApprovalItem2 mAiCc;
    @BindView(R.id.btn_commit)
    Button mBtnCommit;
    private UserInfoBean mUserInfo;
    private PrepareMatterResultBean mApproverList;

    @Override
    protected int initContentLayout() {
        return R.layout.activity_leave;
    }

    @Override
    protected void initData() {
        setSupportActionBar(mMidTitleBar);
        mMidTitleBar.addBackArrow(this);
        mAiStartDate.setDateAndTimeSelector(this, "请选择开始日期");
        mAiEndDate.setDateAndTimeSelector(this, "请选择结束日期");
        String[] leaveTypes = {"病假", "事假", "其他"};
        mAiLeaveType.setSingleChoiceSelector("请选择请假类别", leaveTypes);
        mUserInfo = OaSpUtil.getUserInfo();
        //数据准备
        showLoading("正在准备数据...");
        UserIdApiKeyParams params = new UserIdApiKeyParams(mUserInfo.getUserId(), mUserInfo.getApikey());
        HttpHelper.getInstance().prepareLeave(params, new NetworkCallback() {
            @Override
            public void onSuccess(Call call, String json) {
                mApproverList = JSON.parseObject(json, PrepareMatterResultBean.class);
                loadClose();
                mAiApprovers.applyApproverList(mApproverList);
                mAiCc.setCc(mApproverList.getCc(), true);
            }

            @Override
            public void onFailure(Call call, String message) {
                ToastUtils.showShort("准备数据失败，" + message);
                loadClose(true);
            }
        });
    }

    @Override
    protected void initListener() {

    }

    @OnClick(R.id.btn_commit)
    public void onViewClicked() {
        //检查数据
        String startDate = mAiStartDate.getContentText();
        String endDate = mAiEndDate.getContentText();
        String leaveType = mAiLeaveType.getContentText();
        String duration = mAiDuration.getContentText();
        String detailInfo = mAiDetailInfo.getContentText();
        if (TextUtils.isEmpty(startDate) || TextUtils.isEmpty(endDate) || TextUtils.isEmpty(leaveType) || TextUtils.isEmpty(duration)) {
            ToastUtils.showShort("请填写完整信息");
            return;
        }
        //发起请求
        showLoading("正在提交请求...");
        int ccId = 0;
        if (mCcBean != null) {
            ccId = mCcBean.getId();
        }
        LaunchLeaveParams params = new LaunchLeaveParams(mUserInfo.getUserId(), mUserInfo.getApikey(),
                mApproverList.getOneLevelId(), mApproverList.getTwoLevelId(), mApproverList.getThreeLevelId(),
                ccId, leaveType, detailInfo, startDate, endDate, duration);
        HttpHelper.getInstance().launchLeave(params, new NetworkCallback() {
            @Override
            public void onSuccess(Call call, String json) {
                loadSuccess("提交成功");
            }

            @Override
            public void onFailure(Call call, String message) {
                loadFailed("提交失败，" + message);
            }
        });
    }
}
