package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;

import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.text.SimpleDateFormat;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * contained in a {@link ArticleDetailActivity} on handsets and tablets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mDefaultMutedColor;
    private int mMutedColor;
    private LinearLayout mMetaBar;

    private View mPhotoContainerView;
    private ImageView mPhotoView;
//    private boolean mIsCard = false;
//    private boolean mIsLandscape = false;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (null != getArguments() && getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        //mIsCard = getResources().getBoolean(R.bool.detail_is_card);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        mPhotoContainerView = mRootView.findViewById(R.id.photo_container);
        mMetaBar = (LinearLayout)mRootView.findViewById(R.id.meta_bar);
        mDefaultMutedColor = ContextCompat.getColor(getContext(), R.color.color_muted_background);

        // update the Transition name of this photo to match the article list by appending the item id
        String transName = mPhotoView.getTransitionName();
        mPhotoView.setTransitionName(transName + String.valueOf(mItemId));

        // set the scale type of the detail image based on the resource value for each display size
        int scaleType = getResources().getInteger(R.integer.detail_photo_scale_type);
        if (0 == scaleType) {
            mPhotoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            mPhotoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        return mRootView;
    }

    // make sure to set Target as strong reference
    private Target loadtarget;
    private Bitmap mThumbBitmap = null;

    public void loadBitmap(String url) {
        if (loadtarget == null) loadtarget = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                mThumbBitmap = bitmap;
            }
            @Override
            public void onPrepareLoad(Drawable drawable) { } // nothing to do
            @Override
            public void onBitmapFailed(Drawable drawable) { } // nothing to do
        };
        Picasso.with(getActivity().getApplicationContext()).load(url).into(loadtarget);
    }

    private void bindViews() {
        if (null == mRootView) {
            return;
        }

        TextView titleView = (TextView) mRootView.findViewById(R.id.article_title);
        TextView bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);

        // sanity checks
        if (null == titleView || null == bylineView || null == bodyView) {
            Log.e(TAG, getResources().getString(R.string.textview_missing));
            return;
        }

        bylineView.setMovementMethod(LinkMovementMethod.getInstance());

        // the default Roboto typeface is better because it is crisp and easy to read
//        titleView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));
//        bylineView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));
//        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (null == mCursor){
            mRootView.setVisibility(View.GONE);
            titleView.setText("N/A");
            bylineView.setText("N/A" );
            bodyView.setText("N/A");
            return;
        }

        // get the full image loaded asap
        String iUrl =  mCursor.getString(ArticleLoader.Query.PHOTO_URL);
        Picasso
                .with(getActivity().getApplicationContext())
                .load(iUrl)
                .placeholder(R.drawable.image_loading)
                .into(mPhotoView);

        // get the thumb image loaded (it's most likely cached in memory / storage)
        String thumbUrl =  mCursor.getString(ArticleLoader.Query.THUMB_URL);
        loadBitmap(thumbUrl);

        titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

        // the byline is a combination of the article date and author
        SimpleDateFormat sdf = new SimpleDateFormat(
                getContext().getString(R.string.article_date_format),
                java.util.Locale.getDefault());
        long pDate = mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE);
        String author = mCursor.getString(ArticleLoader.Query.AUTHOR);
        String publishDate = sdf.format(pDate);
        String byLine = publishDate + getContext().getString(R.string.byline_word_by) + author;
        bylineView.setText(byLine);

        // convert the article text into a SpannableString to enable web links to work properly
        setTextViewHTML(bodyView, mCursor.getString(ArticleLoader.Query.BODY));
        bodyView.setMovementMethod(LinkMovementMethod.getInstance());

        // use the palette of the image to generate the meta bar background color
        createMetaBarColor();
    }

    /**
     * Method that builds a SpannableString from HTML and puts into a TextView
     * @param text The TextView that will display the HTML
     * @param html The HTML to convert into a SpannableString
     *
     *   the following code was taken from stackoverflow:
     *
     *      http://stackoverflow.com/questions/30328875/multiple-clickable-link-in-textview
     *
     *             setTextViewHTML
     *             makeLinkClickable
     */
    protected void setTextViewHTML(TextView text, String html) {
        CharSequence sequence = Html.fromHtml(html);
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(),
                URLSpan.class);
        for (URLSpan span : urls) {
            makeLinkClickable(strBuilder, span);
        }
        text.setText(strBuilder);
    }

    protected void makeLinkClickable(SpannableStringBuilder strBuilder,
                                     final URLSpan span) {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);
        // the onClick method was modified to use an Intent to launch a browser and visit the URL
        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {

                //Toast.makeText(getActivityCast().getApplicationContext(), span.getURL(), Toast.LENGTH_LONG).show();

                String url = span.getURL();
                Intent urlIntent = new Intent(Intent.ACTION_VIEW);
                urlIntent.setData(Uri.parse(url));
                startActivity(urlIntent);
            }
        };
        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }

    // generates a palette from the thumbnail version of the image displayed on the detail screen
    // the Dark Muted Color is then used as the background color of the meta bar
    private void createMetaBarColor() {

        // this is the fallback color of the meta bar
        mMutedColor = mDefaultMutedColor;

        // use the thumbnail because it is very likely already loaded
        if (null != mThumbBitmap) {
            Palette.from(mThumbBitmap).generate(
                    new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            // try to get a muted color from the image and assign it to the meta bar background
                            mMutedColor = palette.getDarkMutedColor(mDefaultMutedColor);
                            setMutedColorItems();
                        }
                    }
            );
        } else {
            setMutedColorItems();
        }
    }

    private void setMutedColorItems(){
        mMetaBar.setBackgroundColor(mMutedColor);
        mPhotoContainerView.setBackgroundColor(mMutedColor);
    }

    private void scheduleStartPostponedTransition(final View sharedElement) {
        sharedElement.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        sharedElement.getViewTreeObserver().removeOnPreDrawListener(this);
                        //getActivityCast().supportStartPostponedEnterTransition(); // startPostponedEnterTransition();
                        getActivity().supportStartPostponedEnterTransition();
                        return true;
                    }
                });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        // this cursor contains data for one article
        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();

        // finish the transition animation now
        if (Globals.getInstance().getSelectedItemId() == mItemId) {
            scheduleStartPostponedTransition(mPhotoView);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
    }
}
