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

    public HttpCacheLoaderManager(Context context, HttpCacheBuilder builder) {
        mContext = context;
        mBuilder = builder;
    }

    @Override
    public Loader<HttpCache> onCreateLoader(int id, Bundle args) {
        return new HttpCacheLoader(mContext, mBuilder);
    }

    @Override
    public void onLoadFinished(Loader<HttpCache> loader, HttpCache data) {
        mLoader = (HttpCacheLoader) loader;
        if (data.loaded) {
            beforeUse(data.error, data.content, data.expiry);
        } else {
            nodata();
        }
    }

    @Override
    public void onLoaderReset(Loader<HttpCache> loader) {

    }

    private void beforeUse(int errorCode, String data, long time) {
        try {
            CacheErrorCode generic = CacheErrorCode.getGeneric(errorCode);
            switch (generic) {
                case GENERIC_NET_ERROR: {
                    if (netf(errorCode, data)) {
                        return;
                    }
                    break;
                }

                case GENERIC_PROCESS_ERROR: {
                    if (pf(errorCode, data)) {
                        return;
                    }
                    break;
                }

                default: {
                    use(errorCode, data, time);
                    return;
                }
            }

            error(errorCode, data);
        } finally {
            completed();
        }
    }

    protected boolean pf(int error, String data) {
        return false;
    }

    protected boolean netf(int error, String data) {
        return false;
    }

    protected abstract void nodata();

    protected abstract void use(int error, String data, long time);

    protected abstract void error(int error, String data);

    protected void completed() {

    }

    public void stopChangeNotification() {
        if (mLoader != null) {
            mLoader.stopChangeNotificaton();
        }
    }
}
