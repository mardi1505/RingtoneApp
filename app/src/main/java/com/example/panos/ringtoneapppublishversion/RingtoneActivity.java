package com.example.panos.ringtoneapppublishversion;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;

public class RingtoneActivity extends AppCompatActivity {

    Button previewBTN;
    Button downloadBTN;
    Button setRingtoneBTN;
    SeekBar previewSB;
    TextView ringtoneTitleTV;
    ImageView ringtoneImageIV;

    String ringtoneName;
    String ringtoneNameFormatted;
    String ringtoneImageName;

    ProgressDialog mProgressDialog;

    MediaPlayer mediaPlayer = new MediaPlayer();
    boolean isPlaying = false;

    int ringtoneDuration;

    private Handler mHandler = new Handler();

    private static final int REQUEST_EXTERNAL_STORAGE = 1;

    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    public int getResourceId(String pVariableName, String pResourcename, String pPackageName) {
        try {
            return getResources().getIdentifier(pVariableName, pResourcename, pPackageName);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ringtone);

        previewBTN = (Button) findViewById(R.id.previewBTN);
        downloadBTN = (Button) findViewById(R.id.downloadBTN);
        setRingtoneBTN = (Button) findViewById(R.id.setRingtoneBTN);
        previewSB = (SeekBar) findViewById(R.id.previewSB);
        ringtoneTitleTV = (TextView) findViewById(R.id.ringtoneTitleTV);
        ringtoneImageIV = (ImageView) findViewById(R.id.ringtoneImageIV);


        Intent cameFromIntent = getIntent();
        ringtoneName = cameFromIntent.getStringExtra("ringtoneName");
        ringtoneNameFormatted = cameFromIntent.getStringExtra("ringtoneNameFormatted");

        ringtoneTitleTV.setText(ringtoneNameFormatted);


        ringtoneImageName = ringtoneName.substring(0, ringtoneName.indexOf("."));

        ringtoneImageIV.setImageResource(getResourceId(ringtoneImageName, "drawable", getPackageName()));


        //region Ask for Permissions

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(!Settings.System.canWrite(getApplicationContext())){
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
            else{
                Toast.makeText(this, "Can Write!", Toast.LENGTH_SHORT).show();
            }
        }

        int permission = ActivityCompat.checkSelfPermission(RingtoneActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    RingtoneActivity.this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
        //endregion


        //region Do stuff

        previewBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isPlaying) {
                    try {

                        AssetFileDescriptor descriptor = getAssets().openFd("ringtones/" + ringtoneName);
                        mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
                        descriptor.close();

                        mediaPlayer.prepare();

                        ringtoneDuration = mediaPlayer.getDuration();

                        mediaPlayer.setVolume(1f, 1f);
                        mediaPlayer.setLooping(false);
                        mediaPlayer.start();


                        previewSB.setMax(ringtoneDuration / 1000);


                        //Make sure you update Seekbar on UI thread
                        RingtoneActivity.this.runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                if (mediaPlayer != null) {
                                    int mCurrentPosition = mediaPlayer.getCurrentPosition() / 1000;
                                    previewSB.setProgress(mCurrentPosition);
                                }
                                mHandler.postDelayed(this, 1000);
                            }
                        });

                        isPlaying = !isPlaying;
                        previewBTN.setText("STOP");

                        //Need to add completion listener inside the onClick event so as to create a new listener each time ( because i do .release() )
                        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                mediaPlayer.release();
                                mediaPlayer = new MediaPlayer();
                                isPlaying = !isPlaying;
                                previewBTN.setText("PREVIEW");

                            }
                        });


                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                } else {
                    mediaPlayer.release();
                    mediaPlayer = new MediaPlayer();
                    isPlaying = !isPlaying;
                    previewBTN.setText("PREVIEW");

                }
            }
        });

        previewSB.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mediaPlayer != null && fromUser) {
                    mediaPlayer.seekTo(progress * 1000);
                }
            }
        });

        //endregion


        //region Show progress dialogue and start task on button click

        downloadBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // instantiate it within the onCreate method
                mProgressDialog = new ProgressDialog(RingtoneActivity.this);
                mProgressDialog.setMessage("Downloading file...");
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                mProgressDialog.setCancelable(true);

                // execute this when the downloader must be fired
                final DownloadTask downloadTask = new DownloadTask(RingtoneActivity.this);
                downloadTask.execute("https://www.bensound.com/royalty-free-music?download=goinghigher");

                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        downloadTask.cancel(true);
                    }
                });

            }
        });

        //endregion an and start task


        setRingtoneBTN.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                try {
                    //region Once the file is copied externally, Set it as a ringtone
                    String path = Environment.getDataDirectory() + "/Download/Ringtones/";
                    String file = "downloadedShit.mp3";

                    File k = new File(path, file);

                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DATA, k.getAbsolutePath());
                    values.put(MediaStore.MediaColumns.TITLE, "TwiAppclip");
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "audio/*");
                    values.put(MediaStore.Audio.Media.ARTIST, "cssounds ");
                    values.put(MediaStore.Audio.Media.DURATION, 230);
                    values.put(MediaStore.Audio.Media.IS_RINGTONE, true);
                    values.put(MediaStore.Audio.Media.IS_NOTIFICATION, true);
                    values.put(MediaStore.Audio.Media.IS_ALARM, false);
                    values.put(MediaStore.Audio.Media.IS_MUSIC, false);

                    Uri uri = MediaStore.Audio.Media.getContentUriForPath(k.getAbsolutePath());
                    getContentResolver().delete(uri, MediaStore.MediaColumns.DATA + "=\"" + k.getAbsolutePath() + "\"", null);
                    Uri newUri = getContentResolver().insert(uri, values);

                    RingtoneManager.setActualDefaultRingtoneUri(RingtoneActivity.this,
                            RingtoneManager.TYPE_NOTIFICATION, newUri);
                    //endregion

                    //region Delete Internal Files Created by my app
            /*
            for(int i = 0; i <= 6; ++i){
                if(i == 0){
                    File delFile = new File(path, "bensound-goinghigher.mp3");
                    delFile.delete();
                }
                else {
                    File delFile = new File(path, "bensound-goinghigher" + String.valueOf(i) + ".mp3");
                    delFile.delete();
                }
            }
            */
                    //endregion
                }
                catch (Exception err){
                    err.printStackTrace();
                }
            }
        });

    }

    //region Async Task for downloading a file
    private class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();

                File folder = new File (Environment.getDataDirectory() + "/Download/ringtones/");
                folder.mkdir();

                output = new FileOutputStream(Environment.getDataDirectory() + "/Download/Ringtones/downloadedShit.mp3");

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null)
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
        }


    }
    //endregion

}
