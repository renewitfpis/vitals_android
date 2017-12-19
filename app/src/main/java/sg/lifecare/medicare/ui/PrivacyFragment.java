package sg.lifecare.medicare.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import sg.lifecare.medicare.R;
import sg.lifecare.medicare.ui.view.CustomToolbar;

import static sg.lifecare.medicare.utils.LifeCareHandler.privacyUrl;

/**
 * Created by janice on 24/06/16.
 */

@SuppressLint("SetJavaScriptEnabled")
public class PrivacyFragment extends Fragment
{
    private ProgressBar progBar;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_privacy, container, false);

        FragmentActivity parent = getActivity();
        if (parent instanceof DashboardActivity) {
            ((DashboardActivity)parent).setToolbar(R.string.title_privacy, R.drawable.ic_toolbar_back);
            ((DashboardActivity)parent).setToolbarListener(mToolbarListener);
        }

        progBar = (ProgressBar) view.findViewById(R.id.misc_loading);
        progBar.getIndeterminateDrawable().setColorFilter(
                ContextCompat.getColor(getActivity(),R.color.progress_bar),
                android.graphics.PorterDuff.Mode.SRC_IN
        );

        WebView webview = (WebView) view.findViewById(R.id.misc_page_view);
        webview.setWebViewClient(new MyWebviewClient());
        webview.getSettings().setJavaScriptEnabled(true);
        webview.setVisibility(View.GONE);
        webview.loadUrl(privacyUrl);

        return view;
    }

    class MyWebviewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            progBar.setVisibility(View.GONE);
            view.loadUrl("javascript:"
                   /* + "document.getElementById(\"footer\").setAttribute(\"style\", \"display:none;\");"*/
                    + "var footer = document.getElementsByClassName(\"footer\")[0];"
                    + "footer.parentNode.removeChild(footer);"
                    + "var top2 = document.getElementsByClassName(\"navbar-header\")[0];"
                    + "top2.parentNode.removeChild(top2);"
                    + "var bar = document.getElementsByClassName(\"two wide column\")[0];"
                    + "bar.parentNode.removeChild(bar);"
                    + "var zopim2 = document.getElementsByClassName(\"zopim\")[1];"
                    + "zopim2.parentNode.removeChild(zopim2);"
                    + "document.getElementsByClassName(\"body-content\")[0].setAttribute(\"style\",\"padding-top:0px;\");");
            view.setVisibility(View.VISIBLE);
        }
    }

    private CustomToolbar.OnToolbarClickListener mToolbarListener = new CustomToolbar.OnToolbarClickListener() {
        @Override
        public void leftButtonClick() {
            getActivity().onBackPressed();
        }

        @Override public void rightButtonClick() {

        }

        @Override public void secondRightButtonClick() {

        }
    };

}
