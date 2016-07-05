package com.example.xyzreader.data;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.ParseException;
import android.net.Uri;
import android.os.RemoteException;
import android.text.format.Time;
import android.util.Log;

import com.example.xyzreader.R;
import com.example.xyzreader.remote.RemoteEndpointUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class UpdaterService extends IntentService {
    private static final String TAG = "UpdaterService";

    public static final String BROADCAST_ACTION_STATE_CHANGE
            = "com.example.xyzreader.intent.action.STATE_CHANGE";
    public static final String EXTRA_REFRESHING
            = "com.example.xyzreader.intent.extra.REFRESHING";
    public static final String EXTRA_NO_CONNECTION
            = "com.example.xyzreader.intent.extra.NO_CONNECTION";

    public UpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Log.w(TAG, "Not online, not refreshing.");
            sendBroadcast(
                    new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_NO_CONNECTION, true));
            return;
        }

        // Don't even inspect the intent, we only do one thing, and that's fetch content.
        ArrayList<ContentProviderOperation> cpo = new ArrayList<ContentProviderOperation>();

        Uri dirUri = ItemsContract.Items.buildDirUri();

        // Delete all items
        cpo.add(ContentProviderOperation.newDelete(dirUri).build());

        try {
            JSONArray array = RemoteEndpointUtil.fetchJsonArray();
            if (array == null) {
                throw new JSONException("Invalid parsed item array" );
            }

            for (int i = 0; i < array.length(); i++) {
                ContentValues values = new ContentValues();
                JSONObject object = array.getJSONObject(i);
                values.put(ItemsContract.Items.SERVER_ID, object.getString("id" ));
                values.put(ItemsContract.Items.AUTHOR, object.getString("author" ));
                values.put(ItemsContract.Items.TITLE, object.getString("title" ));
                values.put(ItemsContract.Items.BODY, object.getString("body" ));

                //Log.d(TAG, "Article Body: " + object.getString("body"));

                values.put(ItemsContract.Items.THUMB_URL, object.getString("thumb" ));
                values.put(ItemsContract.Items.PHOTO_URL, object.getString("photo" ));
                values.put(ItemsContract.Items.ASPECT_RATIO, object.getString("aspect_ratio" ));

                //Log.d(TAG, "Published Date: " + object.getString("published_date"));
                // 2013-06-20T00:00:00.000Z

                // clean the Published Date string and convert it into milliseconds
                String dateString = object.getString("published_date");
                dateString = dateString.replace("T", " ");
                dateString = dateString.replace("Z", "");
                //Log.d(TAG, "Cleaned Publish Date: " + dateString);

                long timeInMilliseconds = 0;
                SimpleDateFormat sdf = new SimpleDateFormat(getString(R.string.server_date_format));
                try {
                    Date mDate = sdf.parse(dateString);
                    timeInMilliseconds = mDate.getTime();
                    //Log.d(TAG, "Published Date in Milli: " + String.valueOf(timeInMilliseconds));
                    //System.out.println("Date in milli :: " + timeInMilliseconds);
                } catch (java.text.ParseException e) {
                    e.printStackTrace();
                }

                //time.parse3339(object.getString("published_date"));

                // sdf = new SimpleDateFormat("MMM dd, yyyy");
                //Log.d(TAG, ">>>Published Date as String: " + sdf.format(timeInMilliseconds));


                values.put(ItemsContract.Items.PUBLISHED_DATE, timeInMilliseconds);

                cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
            }

            getContentResolver().applyBatch(ItemsContract.CONTENT_AUTHORITY, cpo);

        } catch (JSONException | RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Error updating content.", e);
        }

        sendBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, false));
    }
}
