package com.moudle.plugin;

import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by Administrator on 2018/8/6.
 */

public class BaseActivity extends Activity {

    @Override
    public Resources getResources() {
        if(getApplication() != null && getApplication().getResources() != null){
            return  getApplication().getResources();
        }
        return super.getResources();
    }

    @Override
    public AssetManager getAssets() {
        if(getApplication() != null && getApplication().getAssets() != null){
            return getApplication().getAssets();
        }
        return super.getAssets();
    }
}
