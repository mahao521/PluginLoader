package com.moudle.pluginloader;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

public class AidlService extends Service {

    private String currName;
    public AidlService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
       return new MyBinder();
    }

    class MyBinder extends IMaHaoAidlInterface.Stub{

        @Override
        public String getBookName() throws RemoteException {
            return "马豪-" + currName;
        }

        @Override
        public void setName(String name) throws RemoteException {
            AidlService.this.currName = name;
        }
    }
}
