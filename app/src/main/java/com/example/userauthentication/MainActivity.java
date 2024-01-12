package com.example.userauthentication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;


/**
 * 添加手势界面逻辑
 */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 添加认证按钮
        findViewById(R.id.addAuthButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 启动第一个子页面
                startActivity(new Intent(MainActivity.this, AddAuthentication.class));
            }
        });

        // 认证识别按钮
        findViewById(R.id.identifyAuthButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 启动第二个子页面
                startActivity(new Intent(MainActivity.this, IdentifyAuthentication.class));
            }
        });
    }
}