package cn.misection.autocall.call.controller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import cn.misection.autocall.R;
import cn.misection.autocall.databinding.ActivityMainBinding;

/**
 * @author javaman
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    private ActivityMainBinding mBinding;

    private PhoneStateListener phoneStateListener;

    private TelephonyManager telephonyManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        initBinding();
        initState();
        initController();
        initActionListener();
    }

    private void initBinding() {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    }

    private void initState() {
    }

    private void initController() {

    }

    private void initActionListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                super.onCallStateChanged(state, phoneNumber);
                switch (state) {
                    // 响铃，来电时候触发
                    case TelephonyManager.CALL_STATE_RINGING:
                        mBinding.phoneCallStateTextView.setText("通话状态: 正在拨打");
                        mBinding.callButton.setEnabled(false);
                        break;
                    // 摘机，接听或拨出电话时触发
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        mBinding.phoneCallStateTextView.setText("通话状态: 正在通话中");
                        //设置按钮不可点击
                        mBinding.callButton.setEnabled(false);
                        break;
                    // 待机，即无电话时候，挂断时候触发
                    case TelephonyManager.CALL_STATE_IDLE:
                    default:
                        mBinding.phoneCallStateTextView.setText("通话状态: 未在通话");
                        mBinding.callButton.setEnabled(true);
                        break;
                }
            }
        };
        //监听电话通话状态的改变
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public void onCallButtonClicked(View view) {
        // 检查是否获得了权限（Android6.0运行时权限）
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            // 没有获得授权，申请授权
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    Manifest.permission.CALL_PHONE)) {
                // 返回值：
                //如果app之前请求过该权限,被用户拒绝, 这个方法就会返回true.
                //如果用户之前拒绝权限的时候勾选了对话框中”Don’t ask again”的选项,那么这个方法会返回false.
                //如果设备策略禁止应用拥有这条权限, 这个方法也返回false.
                // 弹窗需要解释为何需要该权限，再次请求授权
                Toast.makeText(MainActivity.this, "请授权！", Toast.LENGTH_LONG).show();
                // 帮跳转到该应用的设置界面，让用户手动授权
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            } else {
                // 不需要解释为何需要该权限，直接请求授权
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CALL_PHONE},
                        1);
            }
        } else {
            //拨打电话
            Intent intent = new Intent(Intent.ACTION_CALL);
            String phone = String.valueOf(mBinding.phoneCallStateTextView.getText());
            Log.d(TAG, "即将拨打的电话信息" + phone);
            Uri data = Uri.parse("tel:" + phone);
            intent.setData(data);
            startActivity(intent);
        }
    }

    public void onInternalButtonClick(View view) {

    }
}