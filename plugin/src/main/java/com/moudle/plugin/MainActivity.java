package com.moudle.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.moudle.pluginloader.IMaHaoAidlInterface;

/**
 *   aidl 必须包名一直
 *
 *   隐式调用 ： 传递action之外，还需要传递包名
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SecondActivity";
    private IMaHaoAidlInterface mIMahaoAidlInterface;
    private MyConnection mConnection;
    private Button mBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mConnection = new MyConnection();
        mBtn = findViewById(R.id.btn_set_name);
        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mIMahaoAidlInterface != null){
                    try {
                        mIMahaoAidlInterface.setName("财富自由之路");
                        String bookName = mIMahaoAidlInterface.getBookName();
                        mBtn.setText(bookName);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void click(View view) {
        Intent intent = new Intent();
        intent.setAction("com.mahao.aidl");
        intent.setPackage("com.moudle.pluginloader");
        bindService(intent,mConnection, Context.BIND_AUTO_CREATE);
    }

    class MyConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            try {
                mIMahaoAidlInterface = IMaHaoAidlInterface.Stub.asInterface(service);
                String bookName = mIMahaoAidlInterface.getBookName();
                Log.d(TAG, "onServiceConnected: "  + bookName);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }
}
