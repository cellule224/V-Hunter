package com.cellule.hunter;

import android.app.MediaRouteButton;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class ANSSActivity extends AppCompatActivity {

    private WebView mWebView;
    private LinearLayout llProgressView;
    private ProgressBar progressBar;;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anss);

        mWebView = (WebView) findViewById(R.id.help_webView);
        progressBar = findViewById(R.id.webView_progress_bar);
        llProgressView = findViewById(R.id.ll_progress);

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress < 100 && llProgressView.getVisibility() == LinearLayout.GONE) {
                    llProgressView.setVisibility(View.VISIBLE);
                }

                progressBar.setProgress(progress);
                if (progress == 100) {
                    llProgressView.setVisibility(View.GONE);
                }
            }
        });


        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.loadUrl("https://anss-guinee.org/");

    }
}
