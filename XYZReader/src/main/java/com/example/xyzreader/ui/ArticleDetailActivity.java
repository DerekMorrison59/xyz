package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Slide;
import android.transition.Transition;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;

import java.util.HashMap;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private long mStartId;
    private Cursor mCursor;
    private ViewPager mPager;
    private MyPagerAdapter mPagerAdapter;
    private FloatingActionButton mFab;
    private View mBackButton;
    private Toolbar mDetailToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_article_detail);
        supportPostponeEnterTransition();
        setWindowTransitions();

        getSupportLoaderManager().initLoader(0,null,this);

        mDetailToolbar = (Toolbar)findViewById(R.id.detail_toolbar);
        setSupportActionBar(mDetailToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setUpBackButton();

        mPagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mPagerAdapter);

        setUpPageChangeListener();

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mStartId = ItemsContract.Items.getItemId(getIntent().getData());
                Globals.getInstance().setSelectedItemId(mStartId);
            }
        }

        mFab = (FloatingActionButton) findViewById(R.id.share_fab);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(ArticleDetailActivity.this)
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

    }

    private void setUpBackButton() {
        // find the 'up' button inside the toolbar and override it's look and functionality
        mBackButton = mDetailToolbar.getChildAt(0);
        if (null != mBackButton && mBackButton instanceof ImageView) {

            // the scrim makes the back arrow more visible when the image behind is very light
            mBackButton.setBackgroundResource(R.drawable.scrim);

            ViewGroup.MarginLayoutParams lpt = (ViewGroup.MarginLayoutParams) mBackButton.getLayoutParams();
            lpt.setMarginStart((int) getResources().getDimension(R.dimen.keyline_1));

            ViewGroup.LayoutParams lp = mBackButton.getLayoutParams();
            lp.height = (int) getResources().getDimension(R.dimen.small_back_arrow);
            lp.width = (int) getResources().getDimension(R.dimen.small_back_arrow);

            // tapping the back button or the Up button should return to the list of articles
            mBackButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                    supportFinishAfterTransition();
                }
            });
        }
    }

    /**
     * monitor the ViewPager for horizontal swiping
     * while the user is dragging the page left or right - hide the 'up' button and FAB
     * once the swiping stops then show the buttons again
     */
    private void setUpPageChangeListener() {
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);

                // hide the FAB and 'up' button while the user is swiping
                if (ViewPager.SCROLL_STATE_DRAGGING == state) {
                    int fade_out = getResources().getInteger(R.integer.FAB_fade_out_time);
                    mFab.animate().alpha(0f).setDuration(fade_out);
                    if (null != mBackButton) {
                        mBackButton.animate().alpha(0f).setDuration(fade_out);
                    }
                }

                // once the swiping (horiz. page change) has stopped set the new FAB color and
                // show the FAB again by fading in
                if (ViewPager.SCROLL_STATE_IDLE == state) {
                    int fade_in = getResources().getInteger(R.integer.FAB_fade_in_time);
                    if (null != mFab) {
                        mFab.animate().alpha(1f).setDuration(fade_in);
                    }
                    if (null != mBackButton) {
                        mBackButton.animate().alpha(1f).setDuration(fade_in);
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {
                if (mCursor != null) {
                    mCursor.moveToPosition(position);
                    long selectedItemId = mCursor.getLong(ArticleLoader.Query._ID);
                    Globals.getInstance().setSelectedItemId(selectedItemId);
                }
            }
        });
    }

    /**
     * add Enter and Return transitions to this window
     */
    private void setWindowTransitions() {
        Transition returnTrans = new Slide(Gravity.RIGHT);
        returnTrans.setDuration(250);
        getWindow().setReturnTransition(returnTrans);
        getWindow().setEnterTransition(returnTrans);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;
        mPagerAdapter.notifyDataSetChanged();

        // Select the start ID
        if (mStartId > 0) {
            mCursor.moveToFirst();
            while (!mCursor.isAfterLast()) {
                if (mCursor.getLong(ArticleLoader.Query._ID) == mStartId) {
                    //final int position = mCursor.getPosition();
                    mPager.setCurrentItem(mCursor.getPosition(), false);
                    break;
                }
                mCursor.moveToNext();
            }
            mStartId = 0;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        mPagerAdapter.notifyDataSetChanged();
    }

    private class MyPagerAdapter extends FragmentStatePagerAdapter {
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }
        private HashMap<Integer, ArticleDetailFragment> mPageReferenceMap = new HashMap<Integer, ArticleDetailFragment>();

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
        }

        @Override
        public ArticleDetailFragment getItem(int position) {
            mCursor.moveToPosition(position);
            ArticleDetailFragment newFragment = ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
            mPageReferenceMap.put(position, newFragment);
            return newFragment;
        }

        @Override
        public void destroyItem (ViewGroup container, int position, Object object) {
            super.destroyItem(container, position, object);
            mPageReferenceMap.remove(position);
        }

        public ArticleDetailFragment getFragment(int key) {
            return mPageReferenceMap.get(key);
        }
        @Override
        public int getCount() {
            return (mCursor != null) ? mCursor.getCount() : 0;
        }
    }
}
