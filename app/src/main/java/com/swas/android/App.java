package com.swas.android;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.WebView;

import androidx.multidex.MultiDex;

import com.tencent.smtt.export.external.TbsCoreSettings;
import com.tencent.smtt.sdk.QbSdk;

import java.util.HashMap;

/**
 * @author jingbin
 * @data 2018/2/2
 */

public class App extends Application {

    private static App app;

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        initX5();
    }

    public static App getInstance() {
        return app;
    }

    private void initX5() {
        // 非wifi条件下允许下载X5内核
        QbSdk.setDownloadWithoutWifi(true);
        //搜集本地tbs内核信息并上报服务器，服务器返回结果决定使用哪个内核。
        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {

            @Override
            public void onViewInitFinished(boolean arg0) {
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
                if (!arg0) {
                    Log.e("ByWebView", "x5内核加载失败，自动切换到系统内核");
                }
            }

            @Override
            public void onCoreInitFinished() {
            }
        };
        //x5内核初始化接口
        QbSdk.initX5Environment(getApplicationContext(), cb);

        // 在调用TBS初始化、创建WebView之前进行如下配置
        HashMap map = new HashMap();
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_SPEEDY_CLASSLOADER, true);
        map.put(TbsCoreSettings.TBS_SETTINGS_USE_DEXLOADER_SERVICE, true);
        QbSdk.initTbsSettings(map);
    }

    /**
     * 方法数超64k 解决 https://developer.android.com/studio/build/multidex?hl=zh-cn
     * 继承 MultiDexApplication 或 实现此方法。
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        initWebView();
        MultiDex.install(this);
    }

    /**
     * Android P针对 WebView在不同进程下无法访问非自己进程中的webview目录
     * fix Using WebView from more than one process at once with the same data directory is not supported
     */
    private void initWebView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            String processName = getProcessName();
            String packageName = this.getPackageName();
            if (!packageName.equals(processName)) {
                WebView.setDataDirectorySuffix(processName);
            }
        }
    }
}
