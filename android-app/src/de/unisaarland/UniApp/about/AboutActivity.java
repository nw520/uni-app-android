package de.unisaarland.UniApp.about;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import de.unisaarland.UniApp.BuildConfig;
import de.unisaarland.UniApp.R;
import de.unisaarland.UniApp.utils.Util;

public class AboutActivity extends ActionBarActivity {

    private static final String TAG = AboutActivity.class.getSimpleName();

    /*
    * Will be called when activity created first time e.g. from scratch
    * */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.aboutText);

        setContentView(R.layout.about_layout);

        final String versionName = BuildConfig.VERSION_NAME;
        final int versionCode = BuildConfig.VERSION_CODE;
        TextView version = (TextView) findViewById(R.id.version_number);
        version.setText("Version " + versionName + " (" + versionCode + ")");
        Button contact = (Button) findViewById(R.id.contact_btn);
        contact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.email_subject) + " Version " + versionName + " (" + versionCode + ")");
                sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{Util.SUPPORT_MAIL});
                String emailBody = "";
                sendIntent.putExtra(Intent.EXTRA_TEXT, emailBody);
                sendIntent.setType("message/rfc822");
                AboutActivity.this.startActivity(Intent.createChooser(sendIntent, "Send Email"));
            }
        });
    }
}