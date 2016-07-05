package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ItemsContract;
import com.squareup.picasso.Picasso;

/**
 * Created by Derek on 5/28/2016.
 *
 * Splash screen taken from: https://www.bignerdranch.com/blog/splash-screens-the-right-way/
 *
 */
public class SplashActivity extends AppCompatActivity {

    //public static final String LOG_TAG = SplashActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // request all articles from the database
        Cursor articleCursor = getApplicationContext().getContentResolver().query(
                ItemsContract.Items.buildDirUri(),
                null,
                null,
                null,
                null
        );

        String iUrl;
        if (null != articleCursor) {
            articleCursor.moveToFirst();
            for (int i = 0; i < articleCursor.getCount(); i++) {

                // pre-fetch the image thumbnails
                iUrl = articleCursor.getString(articleCursor.getColumnIndex(ItemsContract.ItemsColumns.THUMB_URL));
                //Log.d(LOG_TAG, " pre-fetch thumb: " + iUrl);

                Picasso
                        .with(getApplicationContext())
                        .load(iUrl)
                        .fetch();

                // pre-fetch the real images to allow for the fancy transition
                iUrl = articleCursor.getString(articleCursor.getColumnIndex(ItemsContract.ItemsColumns.PHOTO_URL));
                //Log.d(LOG_TAG, " >> pre-fetch Photo: " + iUrl);

                Picasso
                        .with(getApplicationContext())
                        .load(iUrl)
                        .fetch();

                articleCursor.moveToNext();
            }
            articleCursor.close();
        }

        // leave the splash screen up for a small amount of time (like 1.5 seconds)
        int delay = getResources().getInteger(R.integer.splash_screen_time);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, ArticleListActivity.class);
                startActivity(intent);

                finish();
            }
        }, delay);
    }
}
