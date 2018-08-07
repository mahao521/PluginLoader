package com.moudle.pluginloader;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.text.NoCopySpan;

import com.moudle.pluginloader.Utils.HookUtils;

import java.lang.reflect.Method;

/**
 * Created by Administrator on 2018/8/6.
 */

public class MyApplication extends Application {

    public static boolean isPlugin = false;
    private static Context instace;
    private AssetManager mAssetManager;
    private Resources newResource;
    private Resources.Theme mTheme;

    public static Context getInstace(){
        return instace;
    }
    @Override
    public void onCreate() {
        super.onCreate();
        instace = this;

        //通过第二种方式合并dex加载
        HookUtils utils = new HookUtils();
        utils.hookStartActivity(this);
        utils.hookHookMh(this);
        utils.injectPluginClass();

        //通过替换classLoader加载
       /*
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/plugina.apk";
        utils.putLoadedApk(path);

        try {
            mAssetManager = AssetManager.class.newInstance();
            Method addAssetPathMethod = mAssetManager.getClass().getDeclaredMethod("addAssetPath",String.class);
            addAssetPathMethod.setAccessible(true);
            addAssetPathMethod.invoke(mAssetManager,path);

            Method ensureStringBlocks = AssetManager.class.getDeclaredMethod("ensureStringBlocks");
            ensureStringBlocks.setAccessible(true);
            ensureStringBlocks.invoke(mAssetManager);
            Resources supResource = getResources();
            newResource = new Resources(mAssetManager,supResource.getDisplayMetrics(),supResource.getConfiguration());

            Class<?> mAssetmanager = Class.forName("android.content.res.AssetManager");
            Method createTheme = mAssetmanager.getDeclaredMethod("createTheme");
            createTheme.setAccessible(true);
            createTheme.invoke(mAssetManager);

        } catch (Exception e) {
            e.printStackTrace();
        }*/
    }

    public AssetManager getAssetManager(){
        return mAssetManager == null ? super.getAssets() : mAssetManager;
    }

    @Override
    public Resources getResources() {
        return  newResource == null ? super.getResources() : newResource;
    }

    @Override
    public Resources.Theme getTheme()

    {
        return super.getTheme();
    }
}
