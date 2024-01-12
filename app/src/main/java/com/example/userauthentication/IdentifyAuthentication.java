package com.example.userauthentication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import java.util.ArrayList;


/**
 * 识别用户认证逻辑
 */
public class IdentifyAuthentication extends AppCompatActivity implements CompassFragment.OnConfirmButtonClickListener {
    private GestureOverlayView gestureOverlayView; // 手势编辑区域
    private GestureLibrary gestureLibrary; // 手势库
    private static final String GESTURE_LIBRARY = "/sdcard/userAuthenticationGestures";
    private static final String COMPASS_PREFERENCES = "userAuthenticationCompass";
    private FragmentManager fragmentManager; // 步骤子页面管理器
    private Gesture mGesture; // 手势
    private String direction; // 手机朝向

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.identify_authentication_page);

        // 左上方返回主页面的按钮
        findViewById(R.id.identifyAuthBackButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 关闭当前子页面返回主页面
            }
        });

        fragmentManager = getSupportFragmentManager();

        // Step 1. 手势添加
        FragmentTransaction fragmentTransaction1 = fragmentManager.beginTransaction();
        fragmentTransaction1.replace(R.id.identifyAuthStepContent, new GestureFragment());
        fragmentTransaction1.addToBackStack(null); // 将Fragment添加到BackStack
        fragmentTransaction1.commit();

        // fragmentTransaction1.replace为异步操作，所以需要监听
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                GestureFragment gestureFragment = (GestureFragment) getSupportFragmentManager().findFragmentById(R.id.identifyAuthStepContent);
                if (gestureFragment != null) {
                    // 获取手势编辑视图
                    gestureOverlayView = (GestureOverlayView) gestureFragment.getGestureOverlayView();
                    gestureOverlayView.setGestureColor(Color.GREEN); // 绘制颜色
                    gestureOverlayView.setGestureStrokeWidth(16); // 绘制宽度

                    // 添加手势完成事件的监听器，手势完成自动触发
                    gestureOverlayView.addOnGesturePerformedListener(new GestureOverlayView.OnGesturePerformedListener() {
                        @Override
                        public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
                            mGesture = gesture;
                            Log.d("AddAuthentication", "onGesturePerformed");

                            // Step 2. 方向添加
                            confirmCompassAuth();
                        }
                    });
                }

                // 移除监听器，避免多次调用
                getSupportFragmentManager().removeOnBackStackChangedListener(this);
            }
        });
    }

    private void confirmCompassAuth() {
        FragmentTransaction fragmentTransaction2 = fragmentManager.beginTransaction();
        CompassFragment compassFragment = new CompassFragment();
        compassFragment.setOnConfirmButtonClickListener(this);
        fragmentTransaction2.replace(R.id.identifyAuthStepContent, compassFragment);
        fragmentTransaction2.commit();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (Environment.isExternalStorageManager()) {
                // 用户允许了所有权限
                identifyAuthentication(); // 身份认证
            } else {
                showToast("存储权限获取失败");
            }
        }
    }

    private void identifyAuthentication() {
        // 加载手势库
        gestureLibrary = GestureLibraries.fromFile(GESTURE_LIBRARY);    //获取手势文件
        if (!gestureLibrary.load()) {  //判断手势文件是否存在以及加载
            showToast("手势库加载失败");
        }
        // 判断手势
        String name = "";
        if (gestureLibrary.load()) {
            ArrayList<Prediction> pres = gestureLibrary.recognize(mGesture);
            if (!pres.isEmpty()) {
                Prediction pre = pres.get(0); // 获取识别率最高的对象
                if (pre.score > 4.0) {
                    name = pre.name;
                    showToast("手势名称：" + pre.name);
                } else {
                    showToast("手势匹配失败");
                    return;
                }
            }
        } else {
            showToast("手势库加载失败");
        }

        // 判断朝向
        if(direction.equals(loadDirection(name))) {
            showToast(name + "匹配成功！");
            finish(); // 关闭当前子页面返回主页面
        } else {
            showToast("朝向匹配失败");
        }
    }

    // 读取键值对
    public String loadDirection(String key) {
        SharedPreferences preferences = this.getSharedPreferences(COMPASS_PREFERENCES, Context.MODE_PRIVATE);
        return preferences.getString(key, null);
    }

    // 显示提示信息
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConfirmButtonClicked(String result) {
        direction = result;
        showToast(direction);
        // 识别
        // 获取读取本地文件权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {//30
            // 先判断有没有权限
            if (!Environment.isExternalStorageManager()) {
                //跳转到设置界面引导用户打开
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1);
            } else {
                // 有权限
                identifyAuthentication();
            }
        }
    }
}
