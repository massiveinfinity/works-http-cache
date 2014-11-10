/*
 * Copyright 2014-present Yunarta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobilesolutionworks.android.httpcache;

import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.os.Build;
import android.os.Bundle;

/**
 * Created by yunarta on 24/8/14.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public abstract class HttpCacheLoaderManager implements LoaderManager.LoaderCallbacks<HttpCache> {

    private Context mContext;

    private HttpCacheBuilder mBuilder;

    private HttpCacheLoader mLoader;

    private boolean mLoadFinished;

    public HttpCacheLoaderManager(Context context, HttpCacheBuilder builder) {
        mContext = context;
        mBuilder = builder;
    }

    @Override
    public Loader<HttpCache> onCreateLoader(int id, Bundle args) {
        return onCreateLoader(mContext, mBuilder);
    }

    protected Loader<HttpCache> onCreateLoader(Context context, HttpCacheBuilder builder) {
        return new HttpCacheLoader(context, builder);
    }

    @Override
    public void onLoadFinished(Loader<HttpCache> loader, HttpCache data) {
        if (mLoadFinished) return;

        mLoadFinished = true;
        mLoader = (HttpCacheLoader) loader;

        if (data.loaded) {
            beforeUse(data.error, data.trace, data.content, data.expiry);
        } else {
            onDataLoading();
        }
    }

    @Override
    public void onLoaderReset(Loader<HttpCache> loader) {
        mLoadFinished = false;
    }

    private void beforeUse(int errorCode, Throwable trace, String data, long time) {
        try {
            int generic = CacheErrorCode.getGeneric(errorCode);
            switch (generic) {
                case CacheErrorCode.NET_ERROR: {
                    if (onHandleNetError(errorCode, data)) {
                        return;
                    }
                    break;
                }

                case CacheErrorCode.PROCESS_ERROR: {
                    if (onHandleException(errorCode, data, trace)) {
                        return;
                    }
                    break;
                }

                default: {
                    onDataFinished(errorCode, data, time);
                    return;
                }
            }

            onError(errorCode, data);
        } finally {
            onCompleted();
        }
    }

    protected abstract void onDataLoading();

    protected abstract void onDataFinished(int error, String data, long time);

    protected boolean onHandleException(int error, String data, Throwable trace) {
        return false;
    }

    protected boolean onHandleNetError(int error, String data) {
        return false;
    }

    protected abstract void onError(int error, String data);

    protected void onCompleted() {

    }

    public void stopChangeNotification() {
        if (mLoader != null) {
            mLoader.stopChangeNotificaton();
        }
    }
}
