package com.example.userauthentication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class AddAuthentication extends AppCompatActivity implements CompassFragment.OnConfirmButtonClickListener {
    private GestureOverlayView gestureOverlayView; // 手势编辑区域
    private Gesture mGesture; // 手势
    private EditText addGestureNameET; // 手势名称编辑框
    private GestureLibrary gestureLibrary; // 在类的成员变量中声明手势库对象
    private String direction; // 手机朝向
    private FragmentManager fragmentManager; // 认证步骤子页面管理器
    private static final String GESTURE_LIBRARY = "/sdcard/userAuthenticationGestures";
    private static final String COMPASS_PREFERENCES = "userAuthenticationCompass";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_authentication_page);

        // 左上方返回主页面的按钮
        findViewById(R.id.addAuthBackButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // 关闭当前子页面返回主页面
            }
        });

        fragmentManager = getSupportFragmentManager();

        // Step 1. 手势添加
        FragmentTransaction fragmentTransaction1 = fragmentManager.beginTransaction();
        fragmentTransaction1.replace(R.id.addAuthStepContent, new GestureFragment());
        fragmentTransaction1.addToBackStack(null); // 将Fragment添加到BackStack
        fragmentTransaction1.commit();

        // fragmentTransaction1.replace为异步操作，所以需要监听
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                GestureFragment gestureFragment = (GestureFragment) getSupportFragmentManager().findFragmentById(R.id.addAuthStepContent);
                if (gestureFragment != null) {
                    // 获取手势编辑视图
                    gestureOverlayView = (GestureOverlayView) gestureFragment.getGestureOverlayView();
                    gestureOverlayView.setGestureColor(Color.BLUE); // 绘制颜色
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
        fragmentTransaction2.replace(R.id.addAuthStepContent, compassFragment);
        fragmentTransaction2.commit();
    }


    private void saveAuthentication() {
        // 显示保存视图的界面
        View dialog = getLayoutInflater().inflate(R.layout.save_authentication_dialog, null);
        TextView addDirectionTV = dialog.findViewById(R.id.addDirectionTV); // 朝向
        ImageView image = dialog.findViewById(R.id.addGestureImg); // 显示手势的图片
        addGestureNameET = dialog.findViewById(R.id.addGestureNameET); // 手势名称编辑框

        addDirectionTV.setText(direction);
        // 根据mGesture中包含的手势信息创建一个位图
        Bitmap bitmap = mGesture.toBitmap(128, 128, 10, 0xff0000ff);
        image.setImageBitmap(bitmap);

        // 使用对话框显示dialog控件
        new AlertDialog.Builder(AddAuthentication.this).setView(dialog)
                .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                    // 点击保存按钮后的处理
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 请求保存权限
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {//30
                            // 先判断有没有权限
                            if (!Environment.isExternalStorageManager()) {
                                //跳转到设置界面引导用户打开
                                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivityForResult(intent, 1);
                            } else {
                                // 有权限
                                initGestureLibrary();
                                saveGestureAndCompass(); // 保存文件
                            }
                        }
                    }
                })
                .setNegativeButton("取消", null).show();
    }


    @SuppressLint("NewApi")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (Environment.isExternalStorageManager()) {
                // 用户允许了所有权限
                initGestureLibrary();
                saveGestureAndCompass(); // 保存文件
            } else {
                showToast("存储权限获取失败");
            }
        }
    }

    private void initGestureLibrary() {
        // 初始化手势库
        if (gestureLibrary == null) {
            gestureLibrary = GestureLibraries.fromFile(GESTURE_LIBRARY);
            gestureLibrary.load();  //判断手势文件是否存在以及加载，一定要load，不然会覆盖之前的手势
        }
    }

    // 保存手势文件
    private void saveGestureAndCompass() {
        String gestureName = addGestureNameET.getText().toString().trim();

        if (TextUtils.isEmpty(gestureName)) {
            showToast("请先填写手势名称。");
            addGestureNameET.requestFocus(); // editText获取焦点
        } else {
            if (mGesture != null) {
                // 获取指定文件对应的手势库
                gestureLibrary.addGesture(gestureName, mGesture); // 保存手势到库中
                boolean result = gestureLibrary.save();
                if (result) {
                    saveDirection(gestureName, direction);
                    showToast("认证信息保存成功");
                    finish(); // 关闭当前子页面返回主页面
                } else {
                    showToast("手势保存失败");
                }
            }
        }
    }

    // 存储键值对
    public void saveDirection(String key, String value) {
        SharedPreferences.Editor editor = this.getSharedPreferences(COMPASS_PREFERENCES, Context.MODE_PRIVATE).edit();
        editor.putString(key, value);
        editor.apply();
    }

    // 显示提示信息
    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConfirmButtonClicked(String result) {
        direction = result;
        showToast(direction);
        // 保存Auth
        saveAuthentication();
    }
}
