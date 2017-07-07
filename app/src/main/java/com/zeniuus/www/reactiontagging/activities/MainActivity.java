package com.zeniuus.www.reactiontagging.activities;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.zeniuus.www.reactiontagging.R;

import org.json.JSONArray;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by zeniuus on 2017. 7. 4..
 */

public class MainActivity extends AppCompatActivity {
    ListView listView;
    ArrayAdapter listViewAdapter;

    static final int MY_PERMISSIONS_REQUEST_INTERNET = 0;
    static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissionsForApp();

        listView = (ListView) findViewById(R.id.video_listview);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String videoName = listView.getItemAtPosition(position).toString();
                if (isFileExisting(videoName)) {
                    Log.d("flow", "flow if");
                    Intent intent = new Intent(MainActivity.this, VideoActivity.class);
                    intent.putExtra("video name", videoName);
                    startActivity(intent);
                } else {
                    Log.d("flow", "flow else / video name: " + videoName);
                    new DownloadVideo().execute(videoName);
                }
            }
        });
    }

    public void requestPermissionsForApp() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE },
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            new GetVideoList().execute();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_INTERNET: {
                requestPermissionsForApp();
            }
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                requestPermissionsForApp();
            }
        }
    }

    private boolean isFileExisting(String fileName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "Video" + File.separator + fileName);
        Log.d("file name", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "Video" + File.separator + fileName);

        return file.exists();
    }

    private class GetVideoList extends AsyncTask<Void, Void, String> {
        String strUrl;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            strUrl = "http://143.248.197.157:3000/videos";
        }

        @Override
        protected String doInBackground(Void... params) {
            String result = "";

            try {
                URL url = new URL(strUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setDoInput(true);

                InputStream is = conn.getInputStream();
                StringBuilder builder = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;

                while ((line = reader.readLine()) != null) {
                    builder.append(line + '\n');
                }

                result = builder.toString();
            } catch (Exception e) {
                Log.d("exception", e.toString());
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.d("video list", result);
            JSONArray jsonArray = null;
            Object[] dataset = {};
            try {
                jsonArray = new JSONArray(result);
                ArrayList<String> list = new ArrayList<>();
                if (jsonArray.length() != 0) {
                    for (int i = 0; i < jsonArray.length(); i++) {
                        list.add(jsonArray.get(i).toString().replace("static/videos/", ""));
                    }
                }
                dataset = list.toArray();
            } catch (Exception e) {
                Log.d("exception", e.toString());
            }
            listViewAdapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, dataset);
            listView.setAdapter(listViewAdapter);
        }
    }

    private class DownloadVideo extends AsyncTask<String, Void, Void> {
        String videoName;
        String strUrl;

        @Override
        protected Void doInBackground(String... params) {
            videoName = params[0];
            strUrl = "http://143.248.197.157:3000/videos/" + videoName;

            if (! new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "Video").exists())
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "Video").mkdir();

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(strUrl);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationUri(Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "Video", videoName)));
            Long reference = downloadManager.enqueue(request);

            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
//            Intent intent = new Intent(MainActivity.this, VideoActivity.class);
//            intent.putExtra("video name", videoName);
//            startActivity(intent);
        }
    }
}
