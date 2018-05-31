package com.qiyue.jia.calllogplugin;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.widget.Toast;

import com.mylhyl.acp.Acp;
import com.mylhyl.acp.AcpListener;
import com.mylhyl.acp.AcpOptions;
import com.qiyue.jia.calllogplugin.Util.TransitionTime;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jia on 2018/5/24.
 */

public class CallLogPlugin extends CordovaPlugin {


    private Activity activity;
    private CallbackContext callbackContext;
    private List<CallInfoLog> callInfoList = new ArrayList<CallInfoLog>();
    private CallInfoLog callInfoLog;
    private String callNumber;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        activity = cordova.getActivity();
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
        if (action.equals("getCallLog")) {
            callNumber = args.getString(0);
            initData(callNumber);
            callbackContext.success(toJsonArray(callInfoList));
            return true;
        }
        return false;
    }

    private void initData(final String callNumber) {
        //获取通话记录
        Acp.getInstance(activity).request(new AcpOptions.Builder().setPermissions(
                Manifest.permission.READ_CALL_LOG).build(),
                new AcpListener() {
                    @Override
                    public void onGranted() {
                        //权限允许就获取通话记录
                        getCallLog(activity, callNumber);
                    }

                    @Override
                    public void onDenied(List<String> permissions) {
                        Toast.makeText(activity, "权限拒绝", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 获取所有的通话记录
     *
     * @param context
     */
    public void getCallLog(Context context, String callNumber) {
        try {
            callInfoList.clear();
            ContentResolver cr = context.getContentResolver();
            Uri uri = CallLog.Calls.CONTENT_URI;
            String[] projection = new String[]{CallLog.Calls.NUMBER, CallLog.Calls.DATE,
                    CallLog.Calls.TYPE, CallLog.Calls.CACHED_NAME, CallLog.Calls.DURATION, CallLog.Calls.GEOCODED_LOCATION};
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            }
            Cursor cursor = cr.query(uri, projection, null, null, CallLog.Calls.DATE + " DESC");
            while (cursor.moveToNext()) {
                callInfoLog = new CallInfoLog();
                String number = cursor.getString(0);//电话号码
                long date = cursor.getLong(1);//通话时间
                int type = cursor.getInt(2);//通话类型
                String name = cursor.getString(3);//名字
                String duration = cursor.getString(4);//通话时长
                String areaCode = cursor.getString(5);//归属地
                String callTime = TransitionTime.convertTimeFirstStyle(date);
                if (TransitionTime.getTodayData().equals(callTime) && number.equals(callNumber) && !duration.equals("0") && type == 2) {//如果是今天的话,通话时长>0的,去电,通话记录
                    callInfoLog.setNumber(number);
                    callInfoLog.setDate(date);
                    callInfoLog.setType(type);
                    callInfoLog.setName(name);
                    callInfoLog.setCountType(1);
                    callInfoLog.setDuration(duration);
                    callInfoLog.setCode(areaCode);
                    //筛选数据
                    if (TextUtils.isEmpty(number)) {
                        callInfoList.add(callInfoLog);
                        continue;
                    }
                    boolean isadd = screenData(callInfoList, callInfoLog);
                    if (isadd) {
                        callInfoList.add(callInfoLog);
                    }
                }

            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 筛选数据
     *
     * @param callInfoList
     * @param info
     * @return
     */
    private boolean screenData(List<CallInfoLog> callInfoList, CallInfoLog info) {
        if (callInfoList.size() > 0) {
            for (int i = 0; i < callInfoList.size(); i++) {
                CallInfoLog callInfoLog = callInfoList.get(i);
                //                callInfoLog = callInfoLogs.get(i);
                //如果说是日期和类型全部一样的话那么这个通话记录就不要,变成一个数量归为最近一次记录里面
                if (callInfoLog.getCallTime().equals(info.getCallTime()) && callInfoLog.getType() == info.getType() && callInfoLog.getNumber().equals(info.getNumber())) {
                    callInfoLog.setCountType(callInfoLog.getCountType() + 1);//递增一次
                    //结束这次数据查找
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * toJsonArray
     *
     * @param callInfoList
     * @throws JSONException
     */
    public JSONArray toJsonArray(List<CallInfoLog> callInfoList) throws JSONException {

        JSONArray jsonArray = new JSONArray();
        JSONObject tmpObj = null;
        int count = callInfoList.size();

        for (int i = 0; i < count; i++) {
            tmpObj = new JSONObject();
            tmpObj.put("CallDate", TransitionTime.convertTimeFirstStyle(callInfoList.get(i).getDate()));
            tmpObj.put("Duration", callInfoList.get(i).getDuration());
            jsonArray.put(tmpObj);
            tmpObj = null;
        }

        // 将JSONArray转换得到String
        //        String callLogInfos = jsonArray.toString();
        //        Toast.makeText(activity,callLogInfos,Toast.LENGTH_LONG).show();

        return jsonArray;

    }


}
