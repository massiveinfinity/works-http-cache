package com.mobilesolutionworks.android.httpcache;

import android.content.ContentValues;
import android.os.Bundle;
import android.text.TextUtils;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.commons.lang3.SerializationUtils;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.client.HttpResponseException;

/**
 * Created by yunarta on 29/10/14.
 */
public class LoopJHttpCacheService extends HttpCacheService {

    @Override
    protected void executeRequest(final String local, final String remote, String method, Bundle params, final int cache, int timeout) {
        RequestParams rp = new RequestParams();
        if (params != null) {
            for (String key : params.keySet()) {
                String value = params.getString(key);
                if (!TextUtils.isEmpty(value)) {
                    rp.add(key, value);
                }
            }
        }

        TextHttpResponseHandler handler = new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                if (throwable instanceof HttpResponseException) {
                    ContentValues values = new ContentValues();
                    values.put("local", local);
                    values.put("remote", remote);
                    values.put("data", "");
                    values.put("time", System.currentTimeMillis() + cache);

                    if (onValidateStatus(statusCode)) {
                        values.put("error", CacheErrorCode.createNet(statusCode));
                        values.putNull("trace");
                    } else {
                        values.put("error", CacheErrorCode.PROCESS_ERROR);
                        values.put("trace", SerializationUtils.serialize(throwable));
                    }

                    values.put("status", statusCode);

                    insert(values, local);
                } else {
                    ContentValues values = new ContentValues();
                    values.put("local", local);
                    values.put("remote", remote);
                    values.put("data", "");
                    values.put("time", System.currentTimeMillis() + cache);
                    values.put("error", CacheErrorCode.PROCESS_ERROR);

                    values.put("trace", SerializationUtils.serialize(throwable));
                    values.put("status", statusCode);

                    insert(values, local);
                }
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                ContentValues values = new ContentValues();

                values.put("local", local);
                values.put("remote", remote);
                values.put("data", responseString);
                values.put("time", System.currentTimeMillis() + cache);
                values.put("error", CacheErrorCode.OK);

                values.putNull("trace");
                values.put("status", statusCode);
                insert(values, local);
            }
        };


        Header[] headers = null;
        String contentType = null;

        AsyncHttpClient client = new AsyncHttpClient();
        client.setCookieStore(mCookieStore);

        RequestHandle handle;
        if ("POST".equals(method)) {
            handle = client.post(this, remote, headers, rp, contentType, handler);
        } else {
            handle = client.get(this, remote, headers, rp, handler);
        }
    }

    protected boolean onValidateStatus(int statusCode) {
        return true;
    }
}
