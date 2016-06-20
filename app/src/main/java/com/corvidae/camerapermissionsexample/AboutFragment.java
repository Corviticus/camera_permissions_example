package com.corvidae.camerapermissionsexample;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * The only Fragment for this app
 */
public class AboutFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View aboutView = inflater.inflate(R.layout.layout_fragment_about, container, false);

        // webview to display app changelog
        WebView _infoWebView = (WebView) aboutView.findViewById(R.id.versionWebView);
        _infoWebView.loadUrl("file:///android_asset/changelog.html");
        _infoWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        _infoWebView.setScrollbarFadingEnabled(false);
        _infoWebView.setBackgroundColor(Color.WHITE);

        // webview to display app license credits
        WebView _changeListWebView = (WebView) aboutView.findViewById(R.id.changeNoticeWebView);
        _changeListWebView.loadUrl("file:///android_asset/licenses_credits.html");
        _changeListWebView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        _changeListWebView.setScrollbarFadingEnabled(false);
        _changeListWebView.setBackgroundColor(Color.WHITE);

        return aboutView;
    }
}
