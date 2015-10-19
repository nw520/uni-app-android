package de.unisaarland.UniApp.rssViews;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;

import de.unisaarland.UniApp.R;
import de.unisaarland.UniApp.utils.NetworkRetrieveAndCache;
import de.unisaarland.UniApp.utils.NewsItem;
import de.unisaarland.UniApp.utils.NewsItemExtractor;
import de.unisaarland.UniApp.utils.Util;


/**
 * Activity for showing events or news.
 */
public class RSSDetailActivity extends ActionBarActivity {

    // passed by the intent:
    private String url;
    private int titleId;

    private NetworkRetrieveAndCache<NewsItem> fetcher = null;

    /**
     * Will be called when activity created as this activity is being created from scratch every time when user
     * wants to view a new event details so all work is being done in onCreate no need to separate the work
     * in onResume.
     * It gets the event model object from intent which is saved with name model.
     */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        url = extras.getString("url");
        titleId = extras.getInt("titleId");
        // sets the custom navigation bar according to each activity.
        setActionBar();
        setContentView(R.layout.rss_detail);
        if (fetcher == null) {
            String tag = "rss-"+Integer.toHexString(url.hashCode());
            fetcher = new NetworkRetrieveAndCache<>(url, tag, 15 * 60,
                    Util.getContentCache(this),
                    new NewsItemExtractor(url),
                    new NetworkDelegate(), this);
        }
        fetcher.loadAsynchronously();
    }

    /**
     * Called when back button is pressed either from device or navigation bar.
     */
    @Override
    public void onBackPressed() {
        if (fetcher != null) {
            fetcher.cancel();
            fetcher = null;
        }
        super.onBackPressed();
    }

    private class NetworkDelegate implements NetworkRetrieveAndCache.Delegate<NewsItem> {

        @Override
        public void onUpdate(NewsItem rss) {
            showRSSItem(rss);
        }

        @Override
        public void onStartLoading() {
            ProgressBar pBar = (ProgressBar) findViewById(R.id.web_view_progress_bar);
            pBar.setVisibility(View.VISIBLE);
            WebView body = (WebView) findViewById(R.id.body);
            body.setVisibility(View.GONE);
        }

        @Override
        public void onFailure(String message) {
            new AlertDialog.Builder(RSSDetailActivity.this).
                    setMessage(message).
                    setCancelable(true).
                    setPositiveButton(getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    onBackPressed();
                                }
                            })
                    .create().show();
        }
    }

    /**
     * load the downloaded description of a event and show it as html after setting the necessary html tags.
     */
    private void showRSSItem(NewsItem item) {
        WebView bodyView = (WebView) findViewById(R.id.body);
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head></head><body><h5><center>").append(item.getDate());
        sb.append("</center></h5><h3><center><font color=\"#034A78\">").append(item.getHead());
        sb.append("</font></center></h3>");
        if (item.getSubTitle() != null) {
            sb.append("<h5><center>").append(item.getSubTitle()).append("</center></h5>");
        }
        sb.append("<div style=\"padding-left:10px; padding-right:10px\">");
        sb.append(item.getBody()).append("</div></body>");
        bodyView.loadDataWithBaseURL(item.getBaseHref(), sb.toString(), "text/html", "utf-8", null);
        bodyView.getSettings().setJavaScriptEnabled(true);
        bodyView.setVisibility(View.VISIBLE);
        ProgressBar pBar = (ProgressBar) findViewById(R.id.web_view_progress_bar);
        pBar.setVisibility(View.GONE);
    }

    /**
     * sets the custom navigation bar according to each activity.
     */
    private void setActionBar() {
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        //Enabling Up-Navigation
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(titleId);
    }

    // Handling the Action Bar Buttons
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                onBackPressed();
                NavUtils.navigateUpFromSameTask(this);

                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}