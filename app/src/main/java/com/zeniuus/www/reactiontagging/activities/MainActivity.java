package com.zeniuus.www.reactiontagging.activities;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.iid.FirebaseInstanceId;
import com.zeniuus.www.reactiontagging.R;
import com.zeniuus.www.reactiontagging.networks.HttpRequestHandler;
import com.zeniuus.www.reactiontagging.services.MyFirebaseInstanceIDService;
import com.zeniuus.www.reactiontagging.services.MyFirebaseMessagingService;

import org.json.JSONArray;
import org.w3c.dom.Text;

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

import static java.lang.Thread.sleep;

/**
 * Created by zeniuus on 2017. 7. 4..
 */

public class MainActivity extends AppCompatActivity {
    ListView listView;
    ArrayAdapter listViewAdapter;
    LinearLayout downloadingLayout;
    TextView downloadingText;
    BroadcastReceiver onComplete;
    AdapterView.OnItemClickListener onItemClickListener;

    String downloadingVideoName;

    static final int MY_PERMISSIONS_REQUEST_INTERNET = 0;
    static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2;
    public static final String SERVER_URL = "http://emma.kaist.ac.kr:3000";
//    public static final String SERVER_URL = "http://192.168.0.149:3000";
//    public static final String SERVER_URL = "http://143.248.197.87:3000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissionsForApp();

        final String userId = getIntent().getStringExtra("userId");

        listView = (ListView) findViewById(R.id.video_listview);
        onItemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String videoName = listView.getItemAtPosition(position).toString();
                if (isFileExisting(videoName)) {
                    Log.d("flow", "flow if");
                    Intent intent = new Intent(MainActivity.this, VideoHorizontalActivity.class);
                    intent.putExtra("videoName", videoName);
                    intent.putExtra("userId", userId);
                    intent.putExtra("from", "MainActivity");
                    startActivity(intent);
                } else {
                    Log.d("flow", "flow else / videoName: " + videoName);
                    new DownloadVideo().execute(videoName);
                }
            }
        };
        listView.setOnItemClickListener(onItemClickListener);

        downloadingLayout = (LinearLayout) findViewById(R.id.downloading_layout);

        onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                downloadingLayout.setVisibility(View.INVISIBLE);
                Intent videoIntent = new Intent(MainActivity.this, VideoHorizontalActivity.class);
                videoIntent.putExtra("videoName", downloadingVideoName);
                videoIntent.putExtra("userId", userId);
                videoIntent.putExtra("from", "MainActivity");
                startActivity(videoIntent);
            }
        };
        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

        downloadingText = (TextView) findViewById(R.id.downloading_text);

        Intent tokenService = new Intent(MainActivity.this, MyFirebaseInstanceIDService.class);
        Intent messagingService = new Intent(MainActivity.this, MyFirebaseMessagingService.class);
        startService(tokenService);
        startService(messagingService);

        String token = FirebaseInstanceId.getInstance().getToken();
        String result = new HttpRequestHandler(
                "POST", MainActivity.SERVER_URL + "/token_refreshed/" + userId, token).doHttpRequest();
        Log.d("MainActivity", result);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onComplete);
    }

    public void requestPermissionsForApp() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {
                            Manifest.permission.INTERNET,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    },
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
            case MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
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
            strUrl = SERVER_URL + "/videos";
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
        boolean isDownloading = false;
        int downloadRepeater = 0;

        @Override
        protected void onPreExecute() {
            downloadingLayout.setVisibility(View.VISIBLE);
            isDownloading = true;
            listView.setOnItemClickListener(null);
        }

        @Override
        protected Void doInBackground(String... params) {
            videoName = params[0];
            downloadingVideoName = params[0];
            strUrl = SERVER_URL + "/videos/" + videoName;

            if (! new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "Video").exists())
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "Video").mkdir();

            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse(strUrl);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationUri(Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + File.separator + "Video", videoName)));
            Long reference = downloadManager.enqueue(request);

            do {
                publishProgress();
                try {
                    sleep(300);
                } catch (Exception e) {
                    Log.d("exception", e.toString());
                }
            } while (isDownloading);

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... params) {
            String newText = "Downloading.";
            for (int i = 0; i < downloadRepeater; i++) {
                newText += ".";
            }
            downloadingText.setText(newText);

            downloadRepeater = (downloadRepeater + 1) % 3;

            if (downloadingLayout.getVisibility() == View.INVISIBLE) {
                isDownloading = false;
                listView.setOnItemClickListener(onItemClickListener);
            }
        }
    }
}
