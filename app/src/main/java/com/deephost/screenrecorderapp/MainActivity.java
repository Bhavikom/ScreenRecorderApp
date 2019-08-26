package com.deephost.screenrecorderapp;

import android.annotation.SuppressLint;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat.Action;
import android.support.v4.app.NotificationCompat;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import static android.app.Notification.GROUP_ALERT_SUMMARY;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1000;
    private int mScreenDensity;
    private MediaProjectionManager mediaProjectionManager;
    private static int width = 720;
    private static int height = 1280;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaProjectionCallback mediaProjectionCallback;
    private MediaRecorder mediaRecorder;
    private static final SparseIntArray cor = new SparseIntArray();
    private static final int REQUEST_CODE_RECORD_AUDIO = 10;

    String strDate;
    List<RecordingDatasetList> arraylistRecording;
    ListView listViewRecodingList;
    String strRecordingStatus = "";
    FloatingActionButton floatingActionButton;
    public static List<String> arraylistString;
    File file;
    File clt;
    long recordingTime;
    private static final String YES_ACTION = "YES_ACTION";
    private NotificationManager notificationManager;
    private InterstitialAd mInterstitialAd;
    private AdView mAdView;
    static {
        cor.append(Surface.ROTATION_0, 90);
        cor.append(Surface.ROTATION_90, 0);
        cor.append(Surface.ROTATION_180, 270);
        cor.append(Surface.ROTATION_270, 180);
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MobileAds.initialize(this, getString(R.string.app_id));
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId( getString(R.string.interstitial_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                // Load the next interstitial.
                mInterstitialAd.loadAd(new AdRequest.Builder().build());
            }

        });
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
        arraylistRecording = new ArrayList<>();
        listViewRecodingList = (ListView) findViewById(R.id.cmy1);
        floatingActionButton = (FloatingActionButton) findViewById(R.id.fav);
        strRecordingStatus = "s";
        //creating the adapter
        CustomAdapter adapter = new CustomAdapter(this, R.layout.custom_listview, arraylistRecording);
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "Screen Recording");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
            }
        }
        marshmallowPermission();
        Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        width = display.getWidth();
        height = display.getHeight();
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (strRecordingStatus.equals("s")){
                    checkPermissionAndStartRecording();
                } else if (strRecordingStatus.equals("t")){
                    notificationManager.cancel(1);
                    strRecordingStatus = "s";
                    floatingActionButton.setImageResource(R.drawable.ic_recode);
                    stopMediaRecorder();
                    initLiview();
                    showInterstial();
                }
            }
        });
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mediaRecorder = new MediaRecorder();
        mediaProjectionManager = (MediaProjectionManager) getSystemService
                (Context.MEDIA_PROJECTION_SERVICE);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        jfcfdfh(getIntent());
    }

    private void marshmallowPermission(){
        TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.RECORD_AUDIO)
                .check();
    }
    PermissionListener permissionlistener = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            initLiview();
        }

        @Override
        public void onPermissionDenied(List<String> deniedPermissions) {
            Toast.makeText(MainActivity.this, "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
        }
    };
    public void dateStr(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
        Date now = new Date();
        strDate = formatter.format(now);
    }
    @SuppressLint("DefaultLocale")
    public static String getTimeDate(long seconds) {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(seconds),
                TimeUnit.MILLISECONDS.toMinutes(seconds) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(seconds)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(seconds) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(seconds)));
    }
    public static String getSize(long size) {
        if (size <= 0)
            return "0";
        final String[] units = new String[] { "CustomAdapter", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
    public void initLiview(){
        arraylistRecording.clear();
        arraylistString = new ArrayList<String>();
        File directory = Environment.getExternalStorageDirectory();
        file = new File(directory + "/Screen Recording");
        File list[] = file.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String cf) {
                // TODO Auto-generated method stub
                if (cf.contains(".mp4")) {
                    return true;
                }
                return false;
            }
        });

        if(list.length > 0) {
            for (int i = 0; i < list.length; i++) {
                arraylistString.add(list[i].getName());
                String filepath = Environment.getExternalStorageDirectory() + "/Screen Recording/" + list[i].getName();
                File file = new File(filepath);
                long length = file.length();
                if (length < 1024) {
                    clt = new File(Environment.getExternalStorageDirectory() + "/Screen Recording/" + list[i].getName());
                    clt.delete();
                } else {
                    MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                    retriever.setDataSource(getApplicationContext(), Uri.parse(Environment.getExternalStorageDirectory() + "/Screen Recording/" + list[i].getName()));
                    String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    recordingTime = Long.parseLong(time);
                }
                arraylistRecording.add(new RecordingDatasetList("video.png", list[i].getName(), "" + getTimeDate(recordingTime), "Size : " + getSize(length)));

                CustomAdapter adapter = new CustomAdapter(this, R.layout.custom_listview, arraylistRecording);
                //attaching adapter to the listview
                listViewRecodingList.setAdapter(adapter);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, arraylistString);
        listViewRecodingList.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,int position, long arg3)
            {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = Uri.parse(Environment.getExternalStorageDirectory() + "/Screen Recording/"+ arraylistString.get(position));
                intent.setDataAndType(uri, "video/*");
                startActivity(intent);
            }
        });


    }
    private Intent getMainActivityIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return intent;
    }

    public void showNotification() {
        Intent intent = getMainActivityIntent();
        intent.setAction(YES_ACTION);
        notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = 1;
        String channelId = "channel-01";
        String channelName = "Channel Name";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Screen Recoder App")
                .setContentText("Recoding")
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setOngoing(true)
                .setUsesChronometer(true)
                .setVibrate(null)
                .setGroupAlertBehavior(GROUP_ALERT_SUMMARY)
                .setGroup("My group")
                .setGroupSummary(false)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .addAction(new Action(
                        R.drawable.ic_close,
                        "t",
                        PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)));
        notificationManager.notify(notificationId, mBuilder.build());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        jfcfdfh(intent);
        super.onNewIntent(intent);
    }
    private void jfcfdfh(Intent intent) {
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case YES_ACTION:
                    notificationManager.cancel(1);
                    strRecordingStatus = "s";
                    floatingActionButton.setImageResource(R.drawable.ic_recode);
                    stopMediaRecorder();
                    initLiview();
                    showInterstial();
                    break;
            }
        }
    }
    
    public void checkPermissionAndStartRecording(){
        if (ContextCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) + ContextCompat
                .checkSelfPermission(MainActivity.this,
                        Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
            } else {
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_RECORD_AUDIO);
            }
        } else {
            initRecorder();
            shareScreen();
        }
    }

    public void stopMediaRecorder(){
        mediaRecorder.stop();
        mediaRecorder.reset();
        Log.v(TAG, "Stopping Recording");
        releaseVirtualDisplay();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "Screen Cast Permission Denied", Toast.LENGTH_SHORT).show();
            notificationManager.cancel(1);
            initLiview();
            strRecordingStatus = "s";
            floatingActionButton.setImageResource(R.drawable.ic_recode);

            return;
        }else {
            strRecordingStatus = "t";
            floatingActionButton.setImageResource(R.drawable.ic_clear);
            showNotification();
            startIntentMain();
            loadInterstial();
        }
        mediaProjectionCallback = new MediaProjectionCallback();
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
        mediaProjection.registerCallback(mediaProjectionCallback, null);
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
    }
    /* start home screen */
    public void startIntentMain() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);
    }
    public void loadInterstial(){
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }
    public void showInterstial(){
        if (mInterstitialAd.isLoaded()) {
            mInterstitialAd.show();
        }else {
            mInterstitialAd.loadAd(new AdRequest.Builder().build());
        }

    }
    private void shareScreen() {
        if (mediaProjection == null) {
            startActivityForResult(mediaProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
            return;
        }
        virtualDisplay = createVirtualDisplay();
        mediaRecorder.start();
    }

    private VirtualDisplay createVirtualDisplay() {
        return mediaProjection.createVirtualDisplay("MainActivity",
                width, height, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
    }

    private void initRecorder() {
        dateStr();
        try {
            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory()+"/Screen Recording" + "/Screen Recording "+ strDate +".mp4");
            mediaRecorder.setVideoSize(width, height);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
            mediaRecorder.setCaptureRate(20);
            mediaRecorder.setVideoFrameRate(20);
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = cor.get(rotation + 90);
            mediaRecorder.setOrientationHint(orientation);
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mediaRecorder.stop();
            mediaRecorder.reset();
            Log.v(TAG, "Recording Stopped");

            mediaProjection = null;
            releaseVirtualDisplay();
        }
    }

    private void releaseVirtualDisplay() {
        if (virtualDisplay == null) {
            return;
        }
        virtualDisplay.release();

        stopMediaProjection();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMediaProjection();
    }

    private void stopMediaProjection() {
        if (mediaProjection != null) {
            mediaProjection.unregisterCallback(mediaProjectionCallback);
            mediaProjection.stop();
            mediaProjection = null;
        }
        Log.i(TAG, "MediaProjection Stopped");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_RECORD_AUDIO: {
                if ((grantResults.length > 0) && (grantResults[0] +
                        grantResults[1]) == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.share ){

            try {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
                String sAux = "\n "+getString(R.string.app_name)+" - Download Now\n\n";
                sAux = sAux + "https://play.google.com/store/apps/details?id="+getPackageName()+" \n\n";
                i.putExtra(Intent.EXTRA_TEXT, sAux);
                startActivity(Intent.createChooser(i, "choose one"));
            } catch(Exception e) {

            }
        }

        switch (item.getItemId())
        {
            case R.id.menu_rate:

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id="+getPackageName()));
                startActivity(intent);

                return true;

            case R.id.menu_more:

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/developer?id="+getString(R.string.Devloper_ID))));
                } catch (android.content.ActivityNotFoundException anfe) {
                }

                return true;

            case R.id.menu_privacy:

                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.privacy_policy_url))));
                } catch (android.content.ActivityNotFoundException anfe) {
                }

                return true;

            case R.id.menu_contact_us:
                Intent email = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto",getString(R.string.Email), null));
                startActivity(Intent.createChooser(email, "Choose an Email client :"));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
