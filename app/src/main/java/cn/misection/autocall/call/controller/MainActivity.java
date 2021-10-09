package cn.misection.autocall.call.controller;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import cn.misection.autocall.R;
import cn.misection.autocall.databinding.ActivityMainBinding;
import cn.misection.util.oututil.system.AppSystem;

/**
 * @author javaman
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();

    private ActivityMainBinding mBinding;

    private PhoneStateListener mPhoneStateListener;

    private TelephonyManager mTelephonyManager;

    private ScheduledExecutorService mMakePhoneCallThreadPool;

    private SharedPreferences mPreferences;

//    private CountDownTimer mCountDownTimer;

    private long mPeriod = 10;

    private long mNextCallCountDown = 1;

    private long mLastExitTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    @Override
    public void onBackPressed() {
        if ((System.currentTimeMillis() - mLastExitTime) > 2000) {
            AppSystem.out.printt(this, "再按一次返回键退出 App");
            mLastExitTime = System.currentTimeMillis();
        } else {
            AppSystem.out.printt(this, "Bye!");
            super.onBackPressed();
        }
    }

    private void init() {
        initBinding();
        initController();
        initState();
        initActionListener();
    }

    private void initBinding() {
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
    }

    private void initController() {
        mPreferences = getPreferences(MODE_PRIVATE);
//        mCountDownTimer = new CountDownTimer(mPeriod * 1000, 1000) {
//            @Override
//            public void onTick(long millisUntilFinished) {
//                mBinding.countdownTextView.setText(String.format("倒计时 %d 秒", millisUntilFinished / 1000));
//            }
//
//            @Override
//            public void onFinish() {
//
//            }
//        };
    }

    private void initState() {
        mBinding.telphoneNumEditText.setText(mPreferences.getString("phoneNum", ""));

        mPeriod = mPreferences.getLong("period", 10);
        mBinding.periodEditText.setText(String.valueOf(mPeriod));
    }

    private void initActionListener() {
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mPhoneStateListener = new PhoneStateListener() {
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
        // 监听电话通话状态的改变
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    public void onCallButtonClicked(View view) {
        savePhoneNum();
        // 检查是否获得了权限（Android6.0运行时权限）
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            applyPermission();
        } else {
            // 拨打电话
            startMakePhoneCallLoop(String.valueOf(mBinding.telphoneNumEditText.getText()).trim());
        }
    }

    private void applyPermission() {
        // 没有获得授权，申请授权
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE)) {
            // 返回值：
            // 如果app之前请求过该权限,被用户拒绝, 这个方法就会返回true.
            // 如果用户之前拒绝权限的时候勾选了对话框中”Don’t ask again”的选项,那么这个方法会返回false.
            // 如果设备策略禁止应用拥有这条权限, 这个方法也返回false.
            // 弹窗需要解释为何需要该权限，再次请求授权
            AppSystem.out.printt(this, "请授权!");
            // 帮跳转到该应用的设置界面，让用户手动授权
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        } else {
            // 不需要解释为何需要该权限，直接请求授权
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    1);
        }
    }

    public void onInternalButtonClick(View view) {
        savePeriod();
        startMakePhoneCallLoop(String.valueOf(mBinding.telphoneNumEditText.getText()).trim());
    }

    public void onCancelCallButtonClicked(View view) {
        if (mMakePhoneCallThreadPool != null) {
            mMakePhoneCallThreadPool.shutdown();
        }
        AppSystem.out.printt(this, "暂停拨号成功");
    }

    private void savePhoneNum() {
        mPreferences.edit()
                .putString("phoneNum", String.valueOf(mBinding.telphoneNumEditText.getText()).trim())
                .apply();
        AppSystem.out.printt(this, "电话号码保存成功");
    }

    private void savePeriod() {
        mPreferences.edit()
                .putLong("period", Long.parseLong(String.valueOf(mBinding.periodEditText.getText()).trim()))
                .apply();
        AppSystem.out.printt(this, "间隔时间保存成功");
    }

    private void startMakePhoneCallLoop(String phoneNum) {
        Log.e(TAG, "即将拨打的电话信息" + phoneNum);
        mMakePhoneCallThreadPool = Executors.newSingleThreadScheduledExecutor();
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phoneNum));
        mMakePhoneCallThreadPool.scheduleAtFixedRate(
                () -> startActivity(intent),
                mNextCallCountDown,
                mPeriod,
                TimeUnit.SECONDS
        );
    }
}