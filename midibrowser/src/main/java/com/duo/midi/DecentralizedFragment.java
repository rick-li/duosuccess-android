package com.duo.midi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.duo.midi.alarm.WakefulIntentService;
import com.duo.midi.music.MusicRepeatListener;
import com.duosuccess.midi.R;
import com.google.common.collect.ImmutableMap;
import com.markupartist.android.widget.ActionBar;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringBufferInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by kl68884 on 6/24/18.
 */

public class DecentralizedFragment extends Fragment implements Handler.Callback{
    private static final String TAG = "midi-browser";
    private static final String KEY_CONTENT = "DecentralizedFragment:Content";

    private static final int CLICK_ON_WEBVIEW = 1;

    private static final String homeUrl = "http://127.0.0.1:43110/1AUHC6wpgF676cEd8uZX6cU8BucGU4KAP7/";
    // private static final String homeUrl =
    // "http://rick-li.github.io/android-midi/index.html";
    // private static final String homeUrl = "http://www.baidu.com";

    private final String tmpMidiFile = "duo-music.mp3";
    public static WebView webView;
    private Timer musicTimer;
    private Timer waitTimer;
    private volatile boolean needRepeat = false;
    private volatile boolean fullscreenLocked = false;
    private String mContent = "music";

    private RelativeLayout quitFullScreenBar;
    private ImageView quitFullScreenBtn;

    public static int waitInterval = 1 * 60 * 60 * 1000;
    public static int musicDuration = 1 * 60 * 60 * 1000;
    public static int waitAddition = 10 * 60 * 1000;
    OkHttpClient client = new OkHttpClient();
    @Override
    public boolean handleMessage(Message message) {
        return false;
    }

    // public static int waitInterval = 10 * 1000;
    // public static int waitAddition = 6 * 1000;
    // public static int musicDuration = 10 * 1000;

    enum STATE {
        stop {
            @Override
            public String toString() {
                return "停止";
            }
        },
        wait {
            @Override
            public String toString() {
                return "等待";
            }
        },
        play {
            @Override
            public String toString() {
                return "播放";
            }
        }
    }

    private ActionBar actionBar;
    private ActionBar footer;
    private Handler handler;
    private ProgressDialog pd;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((savedInstanceState != null)
                && savedInstanceState.containsKey(KEY_CONTENT)) {
            mContent = savedInstanceState.getString(KEY_CONTENT);
        }
        handler = new Handler(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decentralized, null);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        actionBar = (ActionBar) this.getView().findViewById(R.id.actionbar);

        footer = (ActionBar) this.getView().findViewById(R.id.bottombar);
        actionBar.setHomeAction(new ActionBar.Action() {

            @Override
            public int getDrawable() {
                return R.drawable.ic_title_home_default;
            }

            @Override
            public void performAction(View view) {
                webView.loadUrl(homeUrl);
            }

        });

        // refresh button
        actionBar.addAction(new ActionBar.Action() {

            @Override
            public int getDrawable() {
                return R.drawable.ic_menu_refresh;
            }

            @Override
            public void performAction(View view) {
                actionBar.setProgressBarVisibility(View.VISIBLE);
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        webView.reload();

                    }
                });
            }
        });

        // close button
        actionBar.addAction(new ActionBar.Action() {

            @Override
            public int getDrawable() {
                return R.drawable.ic_menu_close_clear_cancel;
            }

            @Override
            public void performAction(View view) {
                stopMedia();
                clearCache();
                DecentralizedFragment.this.getActivity().finish();
                return;
            }

        });

        webView = (WebView) this.getView().findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        // WebView.enablePlatformNotifications();
        webView.requestFocusFromTouch();
        // settings.setPluginsEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDefaultZoom(WebSettings.ZoomDensity.CLOSE);
        settings.setBuiltInZoomControls(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);

        // auto clear cache.
        clearCache();
        webView.setWebChromeClient(new WebChromeClient(){

        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.i(TAG, "Should overrid " + url);
                // handler.sendEmptyMessage(CLICK_ON_URL);
                stopMedia();
                if (url.contains("/shop/") && url.startsWith("http:")) {
//                    url = url.replace("http:", "https:");
//                    url = url.replace("69.195.73.224", "www.duosuccess.com");

                    Log.i(TAG, "new url is " + url);
                    view.loadUrl(url);
                    return true;
                }

                return false;
            }

            Boolean openInParent = false;
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                openInParent = false;

                actionBar.setProgressBarVisibility(View.INVISIBLE);
                // pd.dismiss();
            }

            @SuppressLint("NewApi")
            @Override
            public WebResourceResponse shouldInterceptRequest(final WebView view, String url) {

                WebResourceResponse resultResp = null;
                String result = "";
                if(url.endsWith(".mp3")){
                    try {
                        playMusic(url);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if( url.contains("1AUHC6wpgF676cEd8uZX6cU8BucGU4KAP7") && url.contains(".html")){

                    try {

                            Request request = new Request.Builder()

                                    .url(url)
                                    .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36")
                                    .build();

                            Response response = client.newCall(request).execute();


                        result = response.body().string();

                        result = result.replaceAll("sandbox=\".*\"",  "");
                        result = result.replaceAll("document\\.getElementById\\('playBtn'\\)\\.style\\.display='block';", "");
//                        Log.i(TAG, result);
                        resultResp = new WebResourceResponse("text/html", "UTF-8", new ByteArrayInputStream(result.getBytes("UTF-8")));
                    }catch (Exception e){
                        e.printStackTrace();
                        return null;
                    }
                    return resultResp;
                }

                return null;
            }

        });

        webView.loadUrl(homeUrl);
        super.onViewCreated(view, savedInstanceState);
    }


    private void playMusic(String url) throws Exception {

        Intent i = new Intent(this.getActivity(), MusicService.class);
        i.putExtra("midiFile", url);
        this.getActivity().startService(i);
        final Date startDate = new Date();

        final SimpleDateFormat startSdf = new SimpleDateFormat("MM/dd HH:mm:ss");

        Log.i(TAG, "music start " + new Date());
        // stop after 1 hour
        if (musicTimer != null) {
            musicTimer.cancel();
        }
        musicTimer = new Timer();
        final long startTimeAccr = System.currentTimeMillis();
        final TimeCounter timeCounter = new TimeCounter();
        musicTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timeCounter.increaseOneSec();

                setFooterText("开始时间 "
                        + startSdf.format(startDate)
                        + " 已播放 "
                        + new SimpleDateFormat("mm:ss").format(new Date(
                        timeCounter.getStartMillSec())));
                long nowAccr = System.currentTimeMillis();
                if ((nowAccr - startTimeAccr) >= musicDuration) {
                    stopMusicService();

                    webView.post(new Runnable() {

                            @Override
                            public void run() {
                                webView.loadUrl(homeUrl); }

                            });

                    Log.i(TAG, "music stop " + new Date().toString());

                    musicTimer.cancel();
                    musicTimer = null;
                }

            }
        }, 0, 1000);
    }

    private void setFooterText(final String text) {
        if (handler == null) {
            Log.e(TAG, "handler is null in setFooterText.");
            return;
        }
        handler.post(new Runnable() {

            @Override
            public void run() {
                footer.setTitle(text);
            }
        });
    }

    private void clearCache() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                webView.clearCache(true);
            }
        });
        String[] fileAry = this.getActivity().fileList();
        List<String> fileList = Arrays.asList(fileAry);
        if (fileList.contains(tmpMidiFile)) {
            if (this.getActivity().deleteFile(tmpMidiFile)) {
                Log.i(TAG, "Successfully cleared cache.");
                // ParseAnalytics.trackEvent("Cache clear successful.");
                Toast.makeText(this.getActivity(), "已经清除缓存音乐", 2000).show();
            }
        }
    }

    private void stopMusicService() {
        Log.i(TAG, "Stopping service");
        try {
            Intent i = new Intent(this.getActivity(), MusicService.class);
            this.getActivity().stopService(i);
        } catch (Exception e) {

        }
    }

    private void stopMedia() {
        stopMusicService();

        setFooterText(MusicFragment.STATE.stop.toString());
        if (musicTimer != null) {
            musicTimer.cancel();
        }
        stopWaitTimer();
        clearCache();
    }

    public boolean onBackPressed() {
        Log.i(TAG, "music fragment back pressed");
        boolean handled = false;
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            handled = true;
            stopMedia();
        }
        return handled;
    }

    public void stopWaitTimer() {
        if (waitTimer != null) {
            Log.d(TAG, "Stopping WaitTimer.");
            waitTimer.cancel();
            waitTimer = null;
        }

    }

    boolean isInFullScreen = false;

    private void quitFullScreen() {
        actionBar.setVisibility(View.VISIBLE);
        footer.setVisibility(View.VISIBLE);
        quitFullScreenBar.setVisibility(View.GONE);

        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(0, 45, 0, 45);
        webView.setLayoutParams(lp);
        isInFullScreen = false;
    }

    private void goFullScreen() {
        if (fullscreenLocked) {
            return;
        }
        actionBar.setVisibility(View.GONE);
        footer.setVisibility(View.GONE);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT);
        lp.setMargins(0, 0, 0, 0);
        webView.setLayoutParams(lp);
        quitFullScreenBar.setVisibility(View.VISIBLE);
        isInFullScreen = true;

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CONTENT, mContent);
    }

}
