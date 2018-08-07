package com.moudle.pluginloader.Utils;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import static android.content.ContentValues.TAG;

/**
 * Created by Administrator on 2018/8/6.
 */

class IPackageManager implements InvocationHandler {

    private  Object mObject;
    private Context mContext;
    public IPackageManager(Object sPackageField) {
        this.mObject = sPackageField;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Log.d(TAG, "invoke: " + method.getName());
        if(method.getName().equals("getPackageInfo")){
            return new PackageInfo();
        }/*else if(method.getName().equals("getActivityInfo")){
            ActivityInfo activityInfo = new ActivityInfo();
            activityInfo.theme = android.support.v7.appcompat.R.styleable.AppCompatTheme_windowNoTitle;
            return new ActivityInfo(activityInfo);
        }*/
        return method.invoke(mObject,args);
    }
}
