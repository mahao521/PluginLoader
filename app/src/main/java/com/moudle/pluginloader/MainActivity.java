package com.moudle.pluginloader;

import android.content.ComponentName;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_jump1).setOnClickListener(this);
        findViewById(R.id.btn_jump2).setOnClickListener(this);
        findViewById(R.id.btn_jump3).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_jump1:
                jump1();
                break;
            case R.id.btn_jump2:
                jump1();
                break;
            case R.id.btn_jump3:
                jump1();
                break;
        }
    }

    public void jump1(){
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.moudle.plugin","com.moudle.plugin.SecondActivity"));
        startActivity(intent);
    }
}
