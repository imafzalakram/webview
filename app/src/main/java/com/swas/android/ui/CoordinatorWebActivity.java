package com.swas.android.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.TextView;
import android.widget.Toast;

import com.swas.android.MainActivity;
import com.swas.android.R;
import com.swas.android.config.MyJavascriptInterface;
import com.swas.android.utils.StatusBarUtil;
import com.swas.android.utils.WebTools;
import com.swas.android.view.NestedScrollWebView;
import com.google.android.material.appbar.AppBarLayout;

import me.jingbin.web.ByWebTools;
import me.jingbin.web.ByWebView;
import me.jingbin.web.OnByWebClientCallback;
import me.jingbin.web.OnTitleProgressCallback;

/**
 * @author jingbin
 * 与ToolBar联动的WebView,自定义WebView
 * link to https://github.com/youlookwhat/ByWebView
 */
public class CoordinatorWebActivity extends AppCompatActivity {

    // 网页链接
    private int mState;
    private String mUrl;
    private String mTitle;
    private WebView webView;
    private ByWebView byWebView;
    private TextView tvGunTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coordinator_web);
        getIntentData();
        initTitle();
        getDataFromBrowser(getIntent());
    }

    private void getIntentData() {
        mUrl = getIntent().getStringExtra("url");
        mTitle = getIntent().getStringExtra("title");
        mState = getIntent().getIntExtra("state", 0);
    }

    private void initTitle() {
        StatusBarUtil.setColor(this, ContextCompat.getColor(this, R.color.colorPrimary), 0);
        initToolBar();
        CoordinatorLayout container = findViewById(R.id.coordinatorLayout);
        CoordinatorLayout.LayoutParams lp = new CoordinatorLayout.LayoutParams(-1, -1);
        // 设置behavior属性 和 ToolBar属性里设置 layout_scrollFlags="scroll|enterAlways"
        lp.setBehavior(new AppBarLayout.ScrollingViewBehavior());

        NestedScrollWebView nestedScrollWebView = new NestedScrollWebView(this);

        byWebView = ByWebView
                .with(this)
                .setWebParent(container, 1, lp)
                .setCustomWebView(nestedScrollWebView)// 设置自定义WebView
                .useWebProgress(ContextCompat.getColor(this, R.color.coloRed))
                .setOnTitleProgressCallback(onTitleProgressCallback)
                .setOnByWebClientCallback(onByWebClientCallback)
                .addJavascriptInterface("injectedObject", new MyJavascriptInterface(this))
                .loadUrl(mUrl);
        webView = byWebView.getWebView();
    }

    private void initToolBar() {
        // 可滚动的title 使用简单 没有渐变效果，文字两旁有阴影
        Toolbar mTitleToolBar = findViewById(R.id.title_tool_bar);
        tvGunTitle = findViewById(R.id.tv_gun_title);
        setSupportActionBar(mTitleToolBar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            //去除默认Title显示
            actionBar.setDisplayShowTitleEnabled(false);
        }
        mTitleToolBar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.actionbar_more));
        tvGunTitle.postDelayed(new Runnable() {
            @Override
            public void run() {
                tvGunTitle.setSelected(true);
            }
        }, 1900);
        tvGunTitle.setText(mTitle);
    }

    private OnTitleProgressCallback onTitleProgressCallback = new OnTitleProgressCallback() {
        @Override
        public void onReceivedTitle(String title) {
            Log.e("---title", title);
            tvGunTitle.setText(title);
        }
    };

    private OnByWebClientCallback onByWebClientCallback = new OnByWebClientCallback() {
        @Override
        public void onPageFinished(WebView view, String url) {
            // 网页加载完成后的回调
            if (mState == 1) {
                loadImageClickJs();
                loadTextClickJs();
                loadWebsiteSourceCodeJs();
            } else if (mState == 2) {
                loadCallJs();
            }
        }

        @Override
        public boolean isOpenThirdApp(String url) {
            // 处理三方链接
            Log.e("---url", url);
            return ByWebTools.handleThirdApp(CoordinatorWebActivity.this, url);
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_webview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:// 返回键
                handleFinish();
                break;
            case R.id.actionbar_share:// 分享到
                String shareText = webView.getTitle() + webView.getUrl();
                WebTools.share(CoordinatorWebActivity.this, shareText);
                break;
            case R.id.actionbar_cope:// 复制链接
                WebTools.copy(webView.getUrl());
                Toast.makeText(this, "复制成功", Toast.LENGTH_LONG).show();
                break;
            case R.id.actionbar_open:// 打开链接
                WebTools.openLink(CoordinatorWebActivity.this, webView.getUrl());
                break;
            case R.id.actionbar_webview_refresh:// 刷新页面
                byWebView.reload();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 前端注入JS：
     * 这段js函数的功能就是，遍历所有的img节点，并添加onclick函数，函数的功能是在图片点击的时候调用本地java接口并传递url过去
     */
    private void loadImageClickJs() {
        byWebView.getLoadJsHolder().loadJs("javascript:(function(){" +
                "var objs = document.getElementsByTagName(\"img\");" +
                "for(var i=0;i<objs.length;i++)" +
                "{" +
                "objs[i].onclick=function(){window.injectedObject.imageClick(this.getAttribute(\"src\"));}" +
                "}" +
                "})()");
    }

    /**
     * 前端注入JS：
     * 遍历所有的<li>节点,将节点里的属性传递过去(属性自定义,用于页面跳转)
     */
    private void loadTextClickJs() {
        byWebView.getLoadJsHolder().loadJs("javascript:(function(){" +
                "var objs =document.getElementsByTagName(\"li\");" +
                "for(var i=0;i<objs.length;i++)" +
                "{" +
                "objs[i].onclick=function(){" +
                "window.injectedObject.textClick(this.getAttribute(\"type\"),this.getAttribute(\"item_pk\"));}" +
                "}" +
                "})()");
    }

    /**
     * 传应用内的数据给html，方便html处理
     */
    private void loadCallJs() {
        // 无参数调用
        byWebView.getLoadJsHolder().quickCallJs("javacalljs");
        // 传递参数调用
        byWebView.getLoadJsHolder().quickCallJs("javacalljswithargs", "android传入到网页里的数据，有参");
    }

    /**
     * get website source code
     * 获取网页源码
     */
    private void loadWebsiteSourceCodeJs() {
        byWebView.getLoadJsHolder().loadJs("javascript:window.injectedObject.showSource(document.getElementsByTagName('html')[0].innerHTML);");
    }

    /**
     * 上传图片之后的回调
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        byWebView.handleFileChooser(requestCode, resultCode, intent);
    }

    /**
     * 使用singleTask启动模式的Activity在系统中只会存在一个实例。
     * 如果这个实例已经存在，intent就会通过onNewIntent传递到这个Activity。
     * 否则新的Activity实例被创建。
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getDataFromBrowser(intent);
    }

    /**
     * 作为三方浏览器打开传过来的值
     * Scheme: https
     * host: www.jianshu.com
     * path: /p/1cbaf784c29c
     * url = scheme + "://" + host + path;
     */
    private void getDataFromBrowser(Intent intent) {
        Uri data = intent.getData();
        if (data != null) {
            try {
                String scheme = data.getScheme();
                String host = data.getHost();
                String path = data.getPath();
                String text = "Scheme: " + scheme + "\n" + "host: " + host + "\n" + "path: " + path;
                Log.e("data", text);
                String url = scheme + "://" + host + path;
                webView.loadUrl(url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 直接通过三方浏览器打开时，回退到首页
     */
    public void handleFinish() {
        supportFinishAfterTransition();
        if (!MainActivity.isLaunch) {
            MainActivity.start(this);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (byWebView.handleKeyEvent(keyCode, event)) {
            return true;
        } else {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                handleFinish();
            }
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        byWebView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        byWebView.onResume();
    }

    @Override
    protected void onDestroy() {
        byWebView.onDestroy();
        super.onDestroy();
    }

    /**
     * 打开网页:
     *
     * @param mContext 上下文
     * @param url      要加载的网页url
     * @param title    标题
     * @param state    类型
     */
    public static void loadUrl(Context mContext, String url, String title, int state) {
        Intent intent = new Intent(mContext, CoordinatorWebActivity.class);
        intent.putExtra("url", url);
        intent.putExtra("state", state);
        intent.putExtra("title", title == null ? "加载中..." : title);
        mContext.startActivity(intent);
    }
}
