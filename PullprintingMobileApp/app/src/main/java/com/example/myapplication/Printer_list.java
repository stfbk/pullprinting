package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.internal.service.Common;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationServiceDiscovery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Printer_list extends AppCompatActivity {

    /*
    * carica il json passatogli dalla classe Stampa e lo carica in una lista andoid studio
    */

    ListView lv;
    private ExecutorService mExecutor;
    private static final String TAG = "Lista_Stampanti";
    ArrayList<String> arrayList = new ArrayList<>();
    ArrayList<String> printerLIDist = new ArrayList<>();
    private int REQUEST_CODE=700;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExecutor = Executors.newSingleThreadExecutor();
        /**
         * legge le printer che sono state passate dall'activity precedente
         */
        //String accessTokenResponse = getIntent().getExtras().getString("accessToken");
        String response = getIntent().getExtras().getString("list_printer");
        System.out.println("printerList response: "+response);
        setContentView(R.layout.activity_printer_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        lv = (ListView) findViewById(R.id.listview_printer);
        /**
         * crea un JSONarray con le stampanti
         */
        JSONArray json_array = null;
        try {
            json_array = new JSONArray(response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < json_array.length(); i++) {
            try {
                arrayList.add(json_array.getJSONObject(i).getString("name"));
                printerLIDist.add(json_array.getJSONObject(i).getString("id"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        /**
         * sistema tutte le stampanti in un arrayadpter che le mostrerà all'utente che successivamente potrà selezionarne una per stampare
         */
        super.onPostCreate(savedInstanceState);
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, arrayList);
        lv.setAdapter(arrayAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(Printer_list.this, "clicked item:" + position + " " + printerLIDist.get(position).toString(), Toast.LENGTH_LONG).show();
                //GUARDA I CAMBIAMENTI PER LA CIE!
                Intent intent = new Intent(Printer_list.this, Stampa.class);

                String mod_stampa = "Lista";
                intent.putExtra("stampance_scelta_attraverso", mod_stampa);
                intent.putExtra("printer_list_result",printerLIDist.get(position));
                //System.out.println("elemento selezionato dal toast :"+printerLIDist.get(position));
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        //System.out.println(arrayList);
    }
}


