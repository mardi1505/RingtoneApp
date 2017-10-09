package com.example.panos.ringtoneapppublishversion;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.List;

public class ViewActivity extends AppCompatActivity {

    ListView ringtonesLV;
    String[] ringtoneNames = null;
    String[] ringtoneNamesFormatted = null;




    public static String toTitleCase(String givenString) {
        String[] arr = givenString.split(" ");
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < arr.length; i++) {
            sb.append(Character.toUpperCase(arr[i].charAt(0)))
                    .append(arr[i].substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        ringtonesLV = (ListView) findViewById(R.id.ringtoneLV);

        //region Get Ringtone titles with assetmanager. Stored in ringtoneNames string array
        AssetManager assetManager = getApplicationContext().getResources().getAssets();

        try{
            ringtoneNames = assetManager.list("ringtones");
            ringtoneNamesFormatted = assetManager.list("ringtones");
        }
        catch(Exception e){
            e.printStackTrace();
        }
        //endregion

        //region Format string names and populate list view with their names
        for(int i = 0; i < ringtoneNamesFormatted.length; ++i){
            ringtoneNamesFormatted[i] = ringtoneNamesFormatted[i].replaceAll("_", " ");
            ringtoneNamesFormatted[i] = ringtoneNamesFormatted[i].substring(0, ringtoneNamesFormatted[i].indexOf("."));
            ringtoneNamesFormatted[i] = toTitleCase(ringtoneNamesFormatted[i]);
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(ViewActivity.this,
                android.R.layout.simple_list_item_1,ringtoneNamesFormatted);
        ringtonesLV.setAdapter(arrayAdapter);
        //endregion

        ringtonesLV.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent ringtoneActivityIntent= new Intent(ViewActivity.this, RingtoneActivity.class);
                ringtoneActivityIntent.putExtra("ringtoneName", ringtoneNames[position]);
                ringtoneActivityIntent.putExtra("ringtoneNameFormatted", ringtoneNamesFormatted[position]);
                startActivity(ringtoneActivityIntent);
            }
        });
    }
}
