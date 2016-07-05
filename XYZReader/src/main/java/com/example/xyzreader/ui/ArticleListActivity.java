package com.example.xyzreader.ui;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SwipeRefreshLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.transition.Transition;
//import android.view.Display;
import android.util.Log;
import android.view.Gravity;
//import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
//import android.view.WindowManager;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;

import java.text.SimpleDateFormat;


/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor>,
        AppBarLayout.OnOffsetChangedListener {

    public static final String LOG_TAG = ArticleListActivity.class.getSimpleName();

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private Adapter mAdapter;
    private AppCompatActivity mActionBarActivity;
    private boolean mIsRefreshing = false;
    private boolean mIsLandscape = false;
    private float mRangeFraction;

    private CollapsingToolbarLayout mCollapsingToolbar;
    private AppBarLayout mAppbar;
    private Toolbar mToolbar;

    private ImageView mBigLogo;
    private Bitmap mBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (util.isLandscape(this)){
            mIsLandscape = true;
        }

        setContentView(R.layout.activity_article_list);

        // set up the SwipeRefreshLayout container to cause a database refresh
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        mActionBarActivity = this;

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        mAdapter = new Adapter(null);
        mRecyclerView.setAdapter(mAdapter);

        // set the number of columns according to the size specific resource file
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

        getSupportLoaderManager().initLoader(0,null,this);

        if (savedInstanceState == null) {
            // this only happens when the app is first started
            refresh();

            // show the spinning arrow
            mSwipeRefreshLayout.setRefreshing(true);
        }

        // monitor the collapsing toolbar size and scale the size of the logo according to the collapse amount
        mCollapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.main_collapsing_toolbar_layout);
        mAppbar = (AppBarLayout) findViewById(R.id.main_app_bar_layout);
        mToolbar = (Toolbar) findViewById(R.id.app_bar);

        // get info about the Big Logo to use when the article list is scrolled (Listener below)
        mBigLogo = (ImageView) findViewById(R.id.big_logo);
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.logo_big);
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset){

        int appBarBottom = appBarLayout.getBottom();
        int appBarHeight = appBarLayout.getHeight();
        int delta = appBarHeight - appBarBottom;

        int range = mAppbar.getTotalScrollRange();

        // ensure that there is a default range to avoid divide by zero
        if (0 == range) {
            range = 500;
        }

        // the intention of this formula is to scale between 1.0 to 0.5
        // 1.0 corresponds to the AppBar expanded and 0.5 to the collapsed position
        // as the article list is scrolled up and down the logo is smoothly scaled
        // from it's normal size (at the bottom) to about half it's size (at the top)

        mRangeFraction = ((range - (delta)) * 1.0f) / (range * 1.0f);
        float imageScale = 0.5f + 0.5f * mRangeFraction;

        int newH = Math.round(mBitmap.getHeight() * imageScale);
        int newW = Math.round(mBitmap.getWidth() * imageScale);

        //Log.d(LOG_TAG, " new height: " + newH + ", new width: " + newW);

        // resize the logo bitmap according to the current scroll position
        Bitmap bMapScaled = Bitmap.createScaledBitmap(mBitmap, newW, newH, true);
        mBigLogo.setImageBitmap(bMapScaled);

        // only allow the swipe to refresh to work if the app bar is very close to or fully expanded
        if (0.98f > imageScale) {
            mSwipeRefreshLayout.setEnabled(false);
        } else {
            mSwipeRefreshLayout.setEnabled(true);
        }
    }


    private void refresh() {
        // send the intent to the UpdaterService and it will reload the database from the server
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();

        // must setup the broadcast receiver when this activity is started
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));

        mAppbar.addOnOffsetChangedListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // must remove the broadcast receiver when this activity is stopped
        unregisterReceiver(mRefreshingReceiver);

        mAppbar.removeOnOffsetChangedListener(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        //outState.putFloat(getString(R.string.range_fraction), mRangeFraction);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
    }

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {

                // report 'no connection' to user
                if (intent.hasExtra(UpdaterService.EXTRA_NO_CONNECTION)) {
                    Toast.makeText(context, R.string.no_internet_connection, Toast.LENGTH_LONG).show();
                }

                // the update has completed or there is no data connection
                // either way the spinner can stop now
                if (mSwipeRefreshLayout.isRefreshing()) {
                    mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
                }
            }
        }
    };

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // get all available articles from the database
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.swapCursor(cursor);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //mRecyclerView.setAdapter(null);
        mAdapter.swapCursor(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;
        private boolean mDataIsValid;
        private DataSetObserver mDataSetObserver;

        public Adapter(Cursor cursor) {

            mCursor = cursor;
            mDataIsValid = cursor != null;

            mDataSetObserver = new NotifyingDataSetObserver();
            if (mDataIsValid){
                mCursor.registerDataSetObserver(mDataSetObserver);
            }
        }

        @Override
        public int getItemCount(){
            if(mDataIsValid && mCursor != null){
                return mCursor.getCount();
            }
            return 0;
        }

        @Override
        public long getItemId(int position) {
            long itemId = 0;
            if (mDataIsValid && mCursor != null && mCursor.moveToPosition(position)) {
                itemId = mCursor.getLong(ArticleLoader.Query._ID);
            }
            return itemId;
        }


        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view;

            // deal with the size of the article list item
            int listItemSize = getResources().getInteger(R.integer.list_item_size);

            if (0 != listItemSize){
                view = getLayoutInflater().inflate(R.layout.list_item_article_big, parent, false);
            } else {
                view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            }

            // set up the onClick to launch the details view using a shared transition on the image
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ImageView sharedView = (ImageView) view.findViewById(R.id.thumbnail);

                    // create the transition name by appending the item number to make it unique
                    // across all the detail pages in the ViewPager
                    long itemId = getItemId(vh.getAdapterPosition());
                    String transitionImageName = sharedView.getTransitionName() + String.valueOf(itemId);

                    // pass in the image name as part of the transition animation
                    Bundle bundle = ActivityOptions
                            .makeSceneTransitionAnimation(
                                    ArticleListActivity.this,
                                    sharedView,
                                    transitionImageName)
                            .toBundle();

                    startActivity(new Intent(Intent.ACTION_VIEW,
                                    ItemsContract.Items.buildItemUri(itemId)),
                            bundle);
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            //mCursor.moveToPosition(position);
            if(!mDataIsValid){
                throw new IllegalStateException("This should only be called when Cursor is valid");
            }
            if(!mCursor.moveToPosition(position)){
                throw new IllegalStateException("Could not move cursor to position " + position);
            }

            // some layouts may not show the title and subtitle
            if (null != holder.titleView) {
                holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            }
            if (null != holder.subtitleView) {
                SimpleDateFormat sdf = new SimpleDateFormat(getString(R.string.article_date_format), java.util.Locale.getDefault());
                long pDate = mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE);
                String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
                String subTitle = sdf.format(pDate) + getString(R.string.byline_word_by) + author;
                holder.subtitleView.setText(subTitle);
            }

            String iUrl =  mCursor.getString(ArticleLoader.Query.THUMB_URL);
            //Log.d(LOG_TAG, " thumb: " + iUrl);

            Picasso
                    .with(getApplicationContext())
                    .load(iUrl)
                    .placeholder(R.drawable.image_loading)
                    .into(holder.thumbnailView);
        }

        public Cursor swapCursor(Cursor newCursor) {
            if (newCursor == mCursor) {
                return null;
            }
            final Cursor oldCursor = mCursor;
            if (oldCursor != null && mDataSetObserver != null) {
                oldCursor.unregisterDataSetObserver(mDataSetObserver);
            }
            mCursor = newCursor;
            if(mCursor != null){
                if(mDataSetObserver != null){
                    mCursor.registerDataSetObserver(mDataSetObserver);
                }
                mDataIsValid = true;
                notifyDataSetChanged();
            }else{
                mDataIsValid = false;
                notifyDataSetChanged();
            }
            return oldCursor;
        }

        private class NotifyingDataSetObserver extends DataSetObserver {
            @Override
            public void onChanged(){
                super.onChanged();
                mDataIsValid = true;
                notifyDataSetChanged();
            }

            @Override
            public void onInvalidated(){
                super.onInvalidated();
                mDataIsValid = false;
                notifyDataSetChanged();
            }
        }

    }

    // set up the ViewHolder to reference important views and speed up RecyclerView
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);

            thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }
}
