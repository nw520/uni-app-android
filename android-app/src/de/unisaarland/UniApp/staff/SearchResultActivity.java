package de.unisaarland.UniApp.staff;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;

import de.unisaarland.UniApp.R;
import de.unisaarland.UniApp.staff.uihelper.SearchResultAdapter;
import de.unisaarland.UniApp.utils.Util;


public class SearchResultActivity extends ActionBarActivity {
    private String url = null;
    private ArrayList<String> namesArray;
    private ArrayList<String> linksArray;
    private ListView body;
    private ProgressBar pBar;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        savedInstanceState = getIntent().getExtras();
        url = savedInstanceState.getString("url");
        namesArray = new ArrayList<String>();
        linksArray = new ArrayList<String>();
    }

    @Override
    public void onBackPressed() {
        namesArray = null;
        linksArray = null;
        body = null;
        pBar = null;
        deleteTempFile();
        super.onBackPressed();
    }

    private void deleteTempFile() {
        if(tempSearchFileExist()){
            File f = new File(getFilesDir().getAbsolutePath()+ Util.TEMP_STAFF_SEARCH_FILE);
            boolean delete = f.delete();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setActionBar();
        setContentView(R.layout.search_result_layout);
        body = (ListView) findViewById(R.id.body);
        pBar = (ProgressBar) findViewById(R.id.web_view_progress_bar);
        pBar.setVisibility(View.VISIBLE);
        body.setVisibility(View.GONE);
        if(tempSearchFileExist()){
            loadSearchResultFromFile();
            showSearchResults();
        }else{
            getTask(url).execute();
        }
    }

    private void loadSearchResultFromFile() {
        try{
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(new File(getFilesDir().getAbsolutePath()+ Util.TEMP_STAFF_SEARCH_FILE)));
            namesArray = (ArrayList<String>) ois.readObject();
            linksArray = (ArrayList<String>) ois.readObject();
            ois.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean tempSearchFileExist() {
        File f = new File(getFilesDir().getAbsolutePath()+ Util.TEMP_STAFF_SEARCH_FILE);
        return f.exists();
    }

    private AsyncTask<Void,Void,Integer> getTask(final String url){
        return new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                Document doc = null;
                try {
                    doc = Jsoup.connect(url).timeout(15*1000).get();
                    Elements divElements = doc.getElementsByTag("div");
                    for(Element divElement: divElements){
                        if(divElement.className().equals("erg_list_entry")){
                            Elements ergListLabelElements = divElement.getElementsByAttributeValueContaining("class", "erg_list_label");
                            if(ergListLabelElements.size()>0){
                                Element timeElement = ergListLabelElements.get(0);
                                if(timeElement.ownText().equals("Name:")){
                                    Elements aElements = divElement.getElementsByTag("a");
                                    Element nameElement = aElements.get(0);
                                    String rawName = nameElement.text();
                                    String[] nameArray = rawName.split(" ");
                                    // filter out all leading "Prof.", "Dr.", "rer." ...
                                    StringBuilder name = new StringBuilder();
                                    boolean titlePart = true;
                                    for (String namePart : nameArray) {
                                        if (!namePart.endsWith(".") &&
                                                !namePart.endsWith(".-"))
                                            titlePart = false;
                                        if (!titlePart)
                                            name.append(" ").append(namePart);
                                    }
                                    String url = nameElement.attr("href");
                                    //safety check in case user press the back button of device
                                    if (namesArray != null && linksArray != null) {
                                        namesArray.add(name.substring(1));
                                        linksArray.add(url);
                                    }
                                }
                            }

                        }
                    }
                    return 1;
                } catch (IOException e) {
                    return 0;
                }
            }

            @Override
            protected void onPostExecute(Integer i) {
                //if data fetching was successfull
                if (i == 1)
                    showSearchResults();
                    //else show error message and dismiss view
                else{
                    new AlertDialog.Builder(SearchResultActivity.this)
                            .setMessage(getString(R.string.not_connected))
                            .setCancelable(true)
                            .setPositiveButton(getString(R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                        dialog.cancel();
                                    }
                                })
                            .create().show();
                }
            }
        };
    }

    private void showSearchResults() {
        if (pBar != null) {
            pBar.setVisibility(View.INVISIBLE);
            if (namesArray.size() == 0) {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.no_staff_member_title))
                        .setMessage(getString(R.string.no_staff_member_found_description))
                        .setCancelable(true)
                        .setPositiveButton(getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                    onBackPressed();
                                }
                            })
                        .create().show();
            } else {
                if (body != null) {
                    body.setVisibility(View.VISIBLE);
                    SearchResultAdapter adapter = new SearchResultAdapter(this,namesArray,linksArray);
                    body.setAdapter(adapter);
                }
            }
        }
    }

    private void setActionBar() {
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.search_results);

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