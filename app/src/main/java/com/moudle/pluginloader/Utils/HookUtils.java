package com.moudle.pluginloader.Utils;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import com.moudle.pluginloader.LoginActivity;
import com.moudle.pluginloader.MyApplication;
import com.moudle.pluginloader.ProxyActivity;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;

/**
 * Created by Administrator on 2018/8/6.
 */

public class HookUtils {

    private static final String TAG = "HookUtils";
    private Context mContext;
    public void hookHookMh(Context context){
        this.mContext = context;
        try {
            Class<?> forName = Class.forName("android.app.ActivityThread");
            Field currentActivityField = forName.getDeclaredField("sCurrentActivityThread");
            currentActivityField.setAccessible(true);
            //还原系统的ActivityThread
            Object activitObj = currentActivityField.get(null);
            Field handlerField = forName.getDeclaredField("mH");
            handlerField.setAccessible(true);
            //hook点找到了
            Handler mh = (Handler) handlerField.get(activitObj);
            Field callBackField = Handler.class.getDeclaredField("mCallback");
            callBackField.setAccessible(true);
            callBackField.set(mh,new ActivityMH(mh));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class ActivityMH implements Handler.Callback{

        private Handler mHandler;
        public  ActivityMH(Handler handler){
            this.mHandler = handler;
        }
        @Override
        public boolean handleMessage(Message msg) {

            //LAUNCHER_ACTIVITY == 100 即将加载一个activity了
            if(msg.what == 100){
                handleLauncherActivity(msg);
            }
            mHandler.handleMessage(msg);
            return true;
        }

        private void handleLauncherActivity(Message msg) {
            //还原ActivityCleentRecord obj
            Object object = msg.obj;
            try {
                Field intentFiled = object.getClass().getDeclaredField("intent");
                intentFiled.setAccessible(true);
                Intent realyIntent = (Intent) intentFiled.get(object);
                Intent oldIntent = realyIntent.getParcelableExtra("oldIntent");
                if(oldIntent != null){
                    MyApplication.isPlugin = true;
                    //集中式登录
                    SharedPreferences sharedPreferences = mContext.getSharedPreferences("mahao",Context.MODE_PRIVATE);
                    if(sharedPreferences.getBoolean("login",false)){
                        //登录 还远原来的意图
                        realyIntent.setComponent(oldIntent.getComponent());
                    }else {
                        MyApplication.isPlugin = false;
                        ComponentName componentName = new ComponentName(mContext, LoginActivity.class);
                        realyIntent.putExtra("extraIntent",oldIntent.getComponent().getClassName());
                        realyIntent.setComponent(componentName);
                    }
//                    realyIntent.setComponent(oldIntent.getComponent());
                    Field activityInfoField = object.getClass().getDeclaredField("activityInfo");
                    activityInfoField.setAccessible(true);
                    ActivityInfo activityInfo = (ActivityInfo) activityInfoField.get(object);
                    activityInfo.applicationInfo.packageName = oldIntent.getPackage() == null ? oldIntent.getComponent().getPackageName()
                            : oldIntent.getPackage();
                    //ACtivityThread : Application app = r.packageInfo.makeApplication(false, mInstrumentation);
                    //会检查是否安装了APK 没有安装，会抛出异常。
                    Log.d(TAG, "handleLauncherActivity: " + activityInfo.applicationInfo.packageName);
                    hookPackManager();
                }else {
                    MyApplication.isPlugin = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void hookPackManager() {
            //hook方法   hook   方法  IPackageManager.getPackgeInfo
            Class<?> activityThreadClass = null;
            try {
                activityThreadClass = Class.forName("android.app.ActivityThread");
                try {
                    Method currentThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
                    currentThreadMethod.setAccessible(true);
                    Object currentThreadThread = currentThreadMethod.invoke(null);
                    //获取ActivityTHread里面原始的 sPackageManager
                    Field sPackageField  = activityThreadClass.getDeclaredField("sPackageManager");
                    sPackageField.setAccessible(true);
                    Object sPackageManager = sPackageField.get(currentThreadMethod);
                    Log.d(TAG, "hookPackManager: ");
                    //准备好代理对象，用来替换原始的对象，所有的方法被代理
                    Class<?> iPackageInterface = Class.forName("android.content.pm.IPackageManager");
                    Object proxy = Proxy.newProxyInstance(iPackageInterface.getClassLoader(),new Class[]{iPackageInterface},new IPackageManager(sPackageManager));
                    sPackageField.set(currentThreadMethod,proxy);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public void  hookStartActivity(Context context){
        this.mContext = context;
        try{
            Class<?> activityManagerClass = Class.forName("android.app.ActivityManagerNative");
            Field gDefault = activityManagerClass.getDeclaredField("gDefault");
            gDefault.setAccessible(true);
            Object defaultValue  = gDefault.get(null);
            Class<?> singleTon = Class.forName("android.util.Singleton");
            Field mInstance = singleTon.getDeclaredField("mInstance");
            mInstance.setAccessible(true);
            Object iActivityManager = mInstance.get(defaultValue);
            Class<?> iActivityManagerInter = Class.forName("android.app.IActivityManager");
            StartActivity startMethod = new StartActivity(iActivityManager);
            //第二个参数，是返回的对象需要实现那写接口
            Object oldActivityManager = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader()
                    ,new Class[]{iActivityManagerInter, View.OnClickListener.class},startMethod);
            //将系统iActivityManager 替换成 自己通过动态代理实现的对象，
            //oldActivityManager 实现了iActivitynanagerI这个接口
            mInstance.set(defaultValue,oldActivityManager);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    class StartActivity implements InvocationHandler{

        private Object iActivityManagerObject;
        public StartActivity(Object iActivityManagerObject){
            this.iActivityManagerObject = iActivityManagerObject;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.i(TAG, "invoke:-------startActivity-------" );
            if("startActivity".equals(method.getName())){
                //寻找传进来的intent
                Intent intent = null;
                int index = 0;
                for(int i = 0;i < args.length; i++){
                    Object arg = args[i];
                    if (arg instanceof Intent){
                        intent = (Intent) args[i];
                        index = i;
                        break;
                    }
                }
                if(intent != null){
                    String packageName = intent.getComponent().getPackageName();
                    String className = intent.getComponent().getClassName();
                    Log.d(TAG, "StartActivity: " + packageName +" class " + className);
                }
                //目的  --- 载入activity 将它还原
                Intent newIntent = new Intent();
                ComponentName componentName = new ComponentName(mContext,ProxyActivity.class);
                newIntent.setComponent(componentName);
                //真是意图 被隐藏到了， 键值对
                newIntent.putExtra("oldIntent",intent);
                args[index] = newIntent;
            }
            return method.invoke(iActivityManagerObject,args);
        }
    }

    public void putLoadedApk(String path){
        try {
            Class<?> activityThreadClas = Class.forName("android.app.ActivityThread");
            //还原activityThread
            Method currentThread = activityThreadClas.getDeclaredMethod("currentActivityThread");
            currentThread.setAccessible(true);
            Object currentActivityThread = currentThread.invoke(null);
            //获取到mpackage 这个静态成员，这里缓存了apk的信息
            Field mpackageField = activityThreadClas.getDeclaredField("mPackages");
            mpackageField.setAccessible(true);
            Map  mPackages = (Map) mpackageField.get(currentActivityThread);
            //获取getPackageInfoNoCheck method方法
            Class<?> compatibiliInfo = Class.forName("android.content.res.CompatibilityInfo");
            Method getPackageInfoNoCheck = activityThreadClas.getDeclaredMethod("getPackageInfoNoCheck"
            , ApplicationInfo.class,compatibiliInfo);
            //得到CompatiblityInfo里面的静态成员变量  DEFAULT_COMPATBILITY_INFO 类型 compilityINFO;
            Field defaultCompatibilityInfoField = compatibiliInfo.getDeclaredField("DEFAULT_COMPATIBILITY_INFO");
            Object defaultCompatibilityInfo = defaultCompatibilityInfoField.get(null);
            ApplicationInfo applicationInfo = parseReceivers(path,mContext);

            Object loadeApk = getPackageInfoNoCheck.invoke(currentActivityThread,applicationInfo,defaultCompatibilityInfo);
            String libDir = Utils.getPluginOptDexDir(applicationInfo.packageName).getPath();
            String odexPath = Utils.getPluginLibDir(applicationInfo.packageName).getPath();
            ClassLoader classLoader = new MyCustomLoader(path,odexPath,libDir,mContext.getClassLoader());
            Field mClassLoadField = loadeApk.getClass().getDeclaredField("mClassLoader");
            mClassLoadField.setAccessible(true);
            mClassLoadField.set(loadeApk,classLoader);
            WeakReference weakReference = new WeakReference(loadeApk);
            mPackages.put(applicationInfo.packageName,weakReference);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ApplicationInfo parseReceivers(String path,Context context) {
        try {
            Class packageParseClass = Class.forName("android.content.pm.PackageParser");
            Method parsePackageMethod = packageParseClass.getDeclaredMethod("parsePackage", File.class, int.class);
            Object packageParse = packageParseClass.newInstance();
            Object parseObj = parsePackageMethod.invoke(packageParse, new File(path), PackageManager.GET_ACTIVITIES);
            Field received = parseObj.getClass().getDeclaredField("receivers");
            List receivers = (List) received.get(parseObj);

            Class<?> componentClass = Class.forName("android.content.pm.PackageParser$Component");
            Field intentsField = componentClass.getDeclaredField("intents");
            //调用generateActivityInfo 方法，
            Class<?> packageParse$ActivityClass = Class.forName("android.content.pm.PackageParser$Activity");
            Class<?> packageUserState = Class.forName("android.content.pm.PackageUserState");
            Object defaultUserState = packageUserState.newInstance();
            Method generateReceiverInfo = packageParseClass.getDeclaredMethod("generateActivityInfo", packageParse$ActivityClass
                    , int.class, packageUserState, int.class);
            Class<?> userHandler = Class.forName("android.os.UserHandle");
            Method getCallingUserMethod = userHandler.getDeclaredMethod("getCallingUserId");
            int userId = (int) getCallingUserMethod.invoke(null);

            Method generateApplicationMethod = packageParseClass.getDeclaredMethod("generateApplicationInfo"
            ,parseObj.getClass(),int.class,packageUserState);
            ApplicationInfo applicationInfo = (ApplicationInfo) generateApplicationMethod.invoke(packageParse,parseObj,0,defaultUserState);
            applicationInfo.sourceDir = path;
            applicationInfo.publicSourceDir = path;
            return applicationInfo;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public void injectPluginClass(){
        String cachepath = mContext.getCacheDir().getAbsolutePath();
        String apkPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/plugina.apk";
        DexClassLoader dexClassLoader = new DexClassLoader(apkPath,cachepath,cachepath,mContext.getClassLoader());
        //第一步 ： 找到插件的Elements数组，dexpathList ---dexElement
        try {
            Class myDexClassLoader = Class.forName("dalvik.system.BaseDexClassLoader");
            Field myPathListField = myDexClassLoader.getDeclaredField("pathList");
            myPathListField.setAccessible(true);
            Object myPathListObject = myPathListField.get(dexClassLoader);
            Class myPathClass = myPathListObject.getClass();
            Field myElementField = myPathClass.getDeclaredField("dexElements");
            myElementField.setAccessible(true);
            Object myElement = myElementField.get(myPathListObject);

            //找到系统的dexClassLoader
            PathClassLoader pathClassLoader = (PathClassLoader) mContext.getClassLoader();
            Class systemClassLoader = Class.forName("dalvik.system.BaseDexClassLoader");
            Field systemPathListFied = systemClassLoader.getDeclaredField("pathList");
            systemPathListFied.setAccessible(true);
            Object systemLoaderObj = systemPathListFied.get(pathClassLoader);
            Class sysPathClass = systemLoaderObj.getClass();
            Field sysEleMentField = sysPathClass.getDeclaredField("dexElements");
            sysEleMentField.setAccessible(true);
            Object sysElement = sysEleMentField.get(systemLoaderObj);

            int sysLength = Array.getLength(sysElement);
            int myLength = Array.getLength(myElement);
            Class<?> singleElementClazz = sysElement.getClass().getComponentType();
            int newLength = myLength + sysLength;
            Object newElementsArray = Array.newInstance(singleElementClazz,newLength);
            for (int i = 0; i < newLength ; i++){
               if(i < myLength){
                   Array.set(newElementsArray,i,Array.get(myElement,i));
               }else {
                   Array.set(newElementsArray,i,Array.get(sysElement,i-myLength));
               }
            }
            Field elements = pathClassLoader.getClass().getDeclaredField("dexElements");
            elements.setAccessible(true);
            elements.set(systemLoaderObj,elements);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
