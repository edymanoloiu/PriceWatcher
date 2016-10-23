package com.soft.edi.pricewatcher;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Pair;

import com.example.edi.myapplication.backend.myApi.MyApi;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;

import java.io.IOException;
import java.util.LinkedList;

/**
 * Created by Edi on 24.01.2016.
 */
public class EndpointsAsyncTask extends AsyncTask<Pair<Context, String>, Void, String> {
    private static MyApi myApiService = null;
    private Context context;

    @Override
    protected String doInBackground(Pair<Context, String>... params) {
        if (myApiService == null) {  // Only do this once
            MyApi.Builder builder = new MyApi.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null)
                    .setRootUrl("https://pricewatcher-1189.appspot.com/_ah/api/");

            // end options for devappserver
            myApiService = builder.build();
        }

        context = params[0].first;
        String name = params[0].second;

        try {
            return myApiService.getJson(name).execute().getData() + "test";
        } catch (IOException e) {
            return e.getMessage();
        }
    }
}
