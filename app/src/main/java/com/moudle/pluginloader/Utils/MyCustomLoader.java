package com.moudle.pluginloader.Utils;

import android.content.Context;
import android.content.Loader;

import dalvik.system.DexClassLoader;

/**
 * Created by Administrator on 2018/8/6.
 */

public class MyCustomLoader extends DexClassLoader {

    public MyCustomLoader(String dexPath, String optimizedDirectory, String librarySearchPath, ClassLoader parent) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
    }
}
