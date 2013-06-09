package com.mcxiaoke.fanfouapp.fragment;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemLongClickListener;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener2;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.mcxiaoke.fanfouapp.adapter.BaseCursorAdapter;
import com.mcxiaoke.fanfouapp.AppContext;
import com.mcxiaoke.fanfouapp.controller.PopupController;
import com.mcxiaoke.fanfouapp.dao.model.StatusModel;
import com.mcxiaoke.fanfouapp.preference.PreferenceHelper;
import com.mcxiaoke.fanfouapp.service.Constants;
import com.mcxiaoke.fanfouapp.ui.UIHelper;
import com.mcxiaoke.fanfouapp.util.NetworkHelper;
import com.mcxiaoke.fanfouapp.util.Utils;
import com.mcxiaoke.fanfouapp.R;

/**
 * @author mcxiaoke
 * @version 1.8 2012.03.19
 */
public abstract class PullToRefreshListFragment extends AbstractListFragment
        implements OnRefreshListener2<ListView>, OnItemLongClickListener,
        LoaderCallbacks<Cursor> {

    protected static final int LOADER_ID = 1;

    private static final String TAG = PullToRefreshListFragment.class
            .getSimpleName();

    private PullToRefreshListView mPullToRefreshView;
    private ListView mListView;

    private Parcelable mParcelable;

    private BaseCursorAdapter mAdapter;

    boolean mDataLoaded;

    volatile boolean busy;

    public PullToRefreshListFragment() {
        super();
        if (AppContext.DEBUG) {
            Log.v(TAG, "PullToRefreshListFragment() id=" + this);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (AppContext.DEBUG) {
            Log.v(TAG, "onAttach() isVisible=" + isVisible());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (AppContext.DEBUG) {
            Log.v(TAG, "onCreate() isVisible=" + isVisible());
        }

        Bundle args = getArguments();
        if (args != null) {
            parseArguments(args);
        }

        mDataLoaded = false;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (AppContext.DEBUG) {
            Log.v(TAG, "onCreateView() isVisible=" + isVisible());
        }
        View v = inflater.inflate(R.layout.fm_pull_list, container, false);
        setLayout(v);
        return v;
    }

    private void setLayout(View root) {
        mPullToRefreshView = (PullToRefreshListView) root.findViewById(R.id.pull_list);
        mPullToRefreshView.setOnRefreshListener(this);
        mPullToRefreshView.setPullToRefreshOverScrollEnabled(false);
        mPullToRefreshView.setShowIndicator(false);
        mPullToRefreshView.setMode(Mode.BOTH);
        mListView = mPullToRefreshView.getRefreshableView();
        mListView.setOnItemClickListener(this);
        mListView.setLongClickable(false);
        UIHelper.setListView(mListView);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (AppContext.DEBUG) {
            Log.v(TAG, "onActivityCreated() isVisible=" + isVisible());
        }

        parseArguments(getArguments());

        if (savedInstanceState != null) {
            mParcelable = savedInstanceState.getParcelable("state");
        }

        mAdapter = (BaseCursorAdapter) onCreateAdapter();
        mListView.setAdapter(mAdapter);
        mListView.setOnScrollListener(mAdapter);
        getLoaderManager().initLoader(LOADER_ID, null, this);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (AppContext.DEBUG) {
            Log.v(TAG, "onHiddenChanged() hidden=" + hidden + " isVisible="
                    + isVisible());
        }
    }

    protected abstract void parseArguments(Bundle args);

    protected abstract CursorAdapter onCreateAdapter();

    protected abstract void doFetch(boolean doGetMore);

    protected abstract int getType();

    public void onPullDownToRefresh(
            final PullToRefreshBase<ListView> refreshView) {
        if (NetworkHelper.isNotConnected(getActivity())) {
            mPullToRefreshView.onRefreshComplete();
            return;
        }
        doFetch(false);
        getBaseSupport().showProgressIndicator();
    }

    public void onPullUpToRefresh(final PullToRefreshBase<ListView> refreshView) {
        if (NetworkHelper.isNotConnected(getActivity())) {
            mPullToRefreshView.onRefreshComplete();
            return;
        }
        doFetch(true);
        getBaseSupport().showProgressIndicator();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view,
                                   int position, long id) {
        return true;
    }

    protected void doRefresh() {
        if (AppContext.DEBUG) {
            Log.v(TAG, "doRefresh()");
        }
        doFetch(false);
    }

    protected void doGetMore() {
        if (AppContext.DEBUG) {
            Log.v(TAG, "doGetMore()");
        }
        doFetch(true);
    }

    public Cursor getCursor() {
        if (mAdapter != null) {
            return mAdapter.getCursor();
        }
        return null;
    }

    @Override
    public CursorAdapter getAdapter() {
        return mAdapter;
    }

    @Override
    public ListView getListView() {
        return mListView;
    }

    public void setSelection(int position) {
        mListView.setSelection(position);
    }

    public void setEmptyView(View emptyView) {
        mListView.setEmptyView(emptyView);
    }

    public void setEmptyText(CharSequence text) {
        final TextView tv = new TextView(getActivity());
        tv.setText(text);
        mListView.setEmptyView(tv);
    }

    public void goTop() {
        mListView.setSelection(0);
    }

    @Override
    public void startRefresh() {
        if (AppContext.DEBUG) {
            Log.v(TAG, "startRefresh() busy=" + busy);
        }
        if (NetworkHelper.isNotConnected(getActivity())) {
            return;
        }
        if (!busy) {
            busy = true;
            doRefresh();
            getBaseSupport().showProgressIndicator();
        }
    }

    private void onSuccess(Bundle data) {
        int count = data.getInt("count");
        if (AppContext.DEBUG) {
            Log.v(TAG, "onSuccess(data) count=" + count);
        }
    }

    private void onError(Bundle data) {
        if (AppContext.DEBUG) {
            Log.v(TAG, "onSuccess()");
        }
        String errorMessage = data.getString("error_message");
        int errorCode = data.getInt("error_code");
        Utils.notify(getActivity(), errorMessage);
        Utils.checkAuthorization(getActivity(), errorCode);
    }

    private void onRefreshComplete() {
        if (mPullToRefreshView != null) {
            mPullToRefreshView.onRefreshComplete();
        }
    }

    protected static void showPopup(Activity context, final View view,
                                    final Cursor c) {
        if (c != null) {
            final StatusModel s = StatusModel.from(c);
            if (s != null) {
                PopupController.showPopup(view, s, c);
            }
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (AppContext.DEBUG) {
            Log.v(TAG, "onViewCreated() isVisible=" + isVisible());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (AppContext.DEBUG) {
            Log.v(TAG, "onStart() isVisible=" + isVisible());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mListView != null) {
            mParcelable = mListView.onSaveInstanceState();
            outState.putParcelable("state", mParcelable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mParcelable != null && mListView != null) {
            mListView.onRestoreInstanceState(mParcelable);
            mParcelable = null;
        }
        if (AppContext.DEBUG) {
            Log.v(TAG, "onResume() isVisible=" + isVisible());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (AppContext.DEBUG) {
            Log.v(TAG, "onPause() isVisible=" + isVisible());
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (AppContext.DEBUG) {
            Log.v(TAG, "onStop() isVisible=" + isVisible());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (AppContext.DEBUG) {
            Log.v(TAG, "onDestroyView()");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDataLoaded = false;
        if (AppContext.DEBUG) {
            Log.v(TAG, "onDestroy()");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (AppContext.DEBUG) {
            Log.v(TAG, "onDetach() isVisible=" + isVisible());
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor newCursor) {
        getAdapter().swapCursor(newCursor);
        if (AppContext.DEBUG) {
            Log.v(TAG, "onLoadFinished() adapter=" + mAdapter.getCount());
        }
        checkRefresh();
    }

    protected void checkRefresh() {
        boolean refreshOnStart = PreferenceHelper.getInstance(getActivity()).isRefreshOnStart();
        if (AppContext.DEBUG) {
            Log.v(TAG, "checkRefresh() mDataLoaded=" + mDataLoaded
                    + " refreshOnStart=" + refreshOnStart + " adapter.count=" + mAdapter.getCount());
        }
        if (!mDataLoaded && (refreshOnStart || mAdapter.isEmpty())) {
            startRefresh();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (AppContext.DEBUG) {
            Log.v(TAG, "onLoaderReset()");
        }
        getAdapter().swapCursor(null);
    }

    /**
     * FetchService返回数据处理 根据resultData里面的type信息分别处理
     */
    protected static class ResultHandler extends Handler {
        private PullToRefreshListFragment mFragment;

        public ResultHandler(PullToRefreshListFragment fragment) {
            this.mFragment = fragment;
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            if (AppContext.DEBUG) {
                Log.v(TAG, "handleMessage() data=" + data + " msg=" + msg);
            }
            mFragment.mDataLoaded = true;
            mFragment.busy = false;
            switch (msg.what) {
                case Constants.RESULT_SUCCESS:
                    mFragment.onSuccess(data);
                    break;
                case Constants.RESULT_ERROR:
                    mFragment.onError(data);
                    break;
                default:
                    break;
            }
            mFragment.getBaseSupport().hideProgressIndicator();
            mFragment.onRefreshComplete();
        }

    }

}
