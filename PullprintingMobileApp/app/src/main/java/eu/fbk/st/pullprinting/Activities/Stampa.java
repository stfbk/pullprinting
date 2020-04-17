package eu.fbk.st.pullprinting.Activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import eu.fbk.st.pullprinting.R;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceDiscovery;
import net.openid.appauth.browser.AnyBrowserMatcher;
import net.openid.appauth.browser.BrowserMatcher;

import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import eu.fbk.st.pullprinting.AuthModule.AuthStateManager;
import eu.fbk.st.pullprinting.Configuration.Configuration;
import eu.fbk.st.pullprinting.Configuration.ConfigurationCIE;
import eu.fbk.st.pullprinting.Utilities.Commons;
import eu.fbk.st.pullprinting.Utilities.ReadAPI;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Pagina nella quale l'utente, dopo aver già scelto i job, può selezionare la modalità di stampa che preferisce
 * QRcode o attraverso lista
 */

public class Stampa extends AppCompatActivity {

    public static boolean checked = true;
    public static String PrinterID;
    public static String accessTokenResponse;
    public static ArrayList jobResult;

    private ExecutorService mExecutor;
    private static final String TAG = "Stampa";
    Button scanbtn, listbtn;
    CheckBox checkBox;
    TextView result;
    public static final int REQUEST_CODE = 100;
    public static final int AUTH_SC_CODE = 900;
    public static final int PERMISSION_REQUEST = 200;

    private final AtomicReference<JSONObject> mUserInfoJson = new AtomicReference<>();
    private AuthStateManager mStateManager;
    private Configuration mConfiguration;
    private ConfigurationCIE mConfigurationApp;
    private AuthorizationService mAuthService;
    private final AtomicReference<String> mClientId = new AtomicReference<>();
    private final AtomicReference<String> mClientSecret = new AtomicReference<>();
    Context context;
    private final AtomicReference<AuthorizationRequest> mAuthRequest = new AtomicReference<>();
    private final AtomicReference<CustomTabsIntent> mAuthIntent = new AtomicReference<>();
    private CountDownLatch mAuthIntentLatch = new CountDownLatch(1);
    @NonNull
    private BrowserMatcher mBrowserMatcher = AnyBrowserMatcher.INSTANCE;
    //aggiungere anche qui il controllo sulla validità del token (refresh if not valid)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stampa);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //checked=false;
        findViewById(R.id.loading_container_stampa).setVisibility(View.GONE);
        context = this;
        mStateManager = AuthStateManager.getInstance(this);
        //state = mStateManager.getCurrent();
        mConfiguration = Configuration.getInstance(this);
        mConfigurationApp = ConfigurationCIE.getInstance(this);
        mAuthService = new AuthorizationService(
                this,
                new AppAuthConfiguration.Builder()
                        .setConnectionBuilder(mConfiguration.getConnectionBuilder())
                        .build());
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        checkBox = (CheckBox) findViewById(R.id.CIEcheckbox);
        checkBox.setChecked(checked);
        scanbtn = (Button) findViewById(R.id.qr_button);
        listbtn = (Button) findViewById(R.id.list_printer_button);
        result = (TextView) findViewById(R.id.result);
        mExecutor = Executors.newSingleThreadExecutor();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST);
        }
        scanbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Stampa.this, QrReader.class);
                startActivityForResult(intent, REQUEST_CODE);
                //finish();
            }
        });
        listbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printerList();
            }
        });
    }

    private void printerList() {
        displayLoading("Fetching Printer List");
        mStateManager.getCurrent().performActionWithFreshTokens(mAuthService, this::printerList);
    }


    /**
     * dopo aver selezionato una modalità ed aver eseguito i vari passi successivi l'applicazione ritorna su questa pagina,
     * e sarà questa pagina ad eseguire effettivamente la chiamata di stampa verso Google
     *
     * @param requestCode codice richiesta
     * @param resultCode  risultato del response
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                result.post(new Runnable() {
                    @Override
                    public void run() {
                        String mod_stampa = data.getExtras().getString("stampance_scelta_attraverso");
                        //System.out.println("modStampa: "+mod_stampa);
                        //System.out.println("EQUALS :"+mod_stampa.equals("QR"));
                        if (mod_stampa.equals("QR")) {
                            //final Barcode barcode = data.getParcelableExtra("barcode");
                            final String barcode = data.getExtras().getString("barcode");
                            result.setText(barcode);
                            PrinterID = barcode;//getIntent().getExtras().getString("barcode");
                            //System.out.println("ID by QR: "+barcode);
                            //createDialog("Sei sicuro di voler stampare qui?"+PrinterID); //mettere il nome e un immagine
                        } else {
                            PrinterID = data.getExtras().getString("printer_list_result");
                            //nascondo la loading bar
                            findViewById(R.id.loading_container_stampa).setVisibility(View.GONE);
                            findViewById(R.id.design_scelta_stampante).setVisibility(View.VISIBLE);
                            ((TextView) findViewById(R.id.loading_description)).setText("");
                            //System.out.println("PrinterID LISTA: "+PrinterID);
                            //System.out.println("ID by LIST "+data.getExtras().getString("printer_list_result"));
                        }
                        jobResult = getIntent().getExtras().getStringArrayList("jobToDo");
                        accessTokenResponse = getIntent().getExtras().getString("accessToken");
                        //System.out.println("AccessTokenResponse: " + accessTokenResponse + " barcode:" + PrinterID + " jobToDo: " + jobResult.toString());
                        //System.out.println(PrinterID + ", " + accessTokenResponse + ", " + jobResult);
                        dialogConfermaStampante(PrinterID);
                    }
                });
            }
        }
    }

    public void dialogConfermaStampante(String stampante) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Conferma la stampante selezionata");
        alertDialogBuilder.setIcon(R.drawable.unknown_user_48dp);
        alertDialogBuilder.setMessage("vuoi stampare su questa stampante :" + stampante);
        alertDialogBuilder.setCancelable(false);
        //per aggiungere l'immagine
        LayoutInflater factory = LayoutInflater.from(this);
        View view = null;
        //example to extends to all the printers
        if (stampante.equals(Commons.spoolerID)) {
            view = factory.inflate(R.layout.sample, null);
        }
        else if(stampante.equals("21afafed-aaee-696a-9efe-16cdfd1c0acc")){
            view = factory.inflate(R.layout.nord_isp_colorcopier, null);
        }
        else if(stampante.equals("0550ac86-93e7-61f2-465c-da1ea38d2181")){
            view = factory.inflate(R.layout.nord_pp_colorcopier, null);
        }
        else if(stampante.equals("10565f14-1847-799c-8f2d-7ee61b40cd45")){
            view = factory.inflate(R.layout.nord_amm_colorcopier, null);
        }
        else if(stampante.equals("106241bf-bcfb-4dc5-694b-ea13f5b52d2b")){
            view = factory.inflate(R.layout.nord_sp_colorict, null);
        }
        alertDialogBuilder.setView(view);
        //System.out.println("PrinterID" + PrinterID);
        //System.out.println("accessTokenResponse" + accessTokenResponse);
        //System.out.println("jobResult" + jobResult);
        alertDialogBuilder.setPositiveButton("SI", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                Toast.makeText(getApplicationContext(), "Perfetto", Toast.LENGTH_SHORT).show();
                //se giusta allora prosegui
                if (checkBox.isChecked() == true) {
                    checked=true;
                    Intent intent = new Intent(Stampa.this, CieAuth.class);
                    intent.putExtra("PrinterID", PrinterID);
                    intent.putExtra("accessTokenResponse", accessTokenResponse);
                    intent.putExtra("jobResult", jobResult);
                    startActivity(intent);
                    finish();
                } else {
                    print(accessTokenResponse, jobResult, PrinterID);
                }

            }
        }).create();
        alertDialogBuilder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                Toast.makeText(getApplicationContext(), "Okay, seleziona un'altra stampante", Toast.LENGTH_SHORT).show();
            }
        }).create();
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.coordinator),
                message,
                Snackbar.LENGTH_SHORT)
                .show();
    }

    /**
     * lista stampanti che verranno passate all'activity successiva se selzionata la modalità di stampa attraverso "Lista"
     */
    private Response requestQueuePrinter(String authToken) throws IOException {
        System.err.println("StartRequestPrinterList:"+ new Timestamp(System.currentTimeMillis()));
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();        Request request = new Request.Builder()
                .url("https://"+getString(R.string.base_url) + "/list_printer") //prima era printer se non stampanti admin
                .get()
                .addHeader("cache-control", "no-cache")
                .build();

        Log.d(TAG, ReadAPI.bodyToString(request));
        //Response response = client.newCall(request).execute();
        return client.newCall(request).execute();
    }

    /**
     * metodo che esegue effettivamente la chiamata HTTP per ottenere le stampanti dell'edificio (di possesso dell'amministratore che le condivide con gli utenti del servizio)
     */

    private void printerList(String accessToken, String s, AuthorizationException ex) {
        //controllo se il token è valido altrimenti lo refresho
        //AuthState state = mStateManager.getCurrent();
        /*if(state.getRefreshToken() == null || state.getAccessToken() == null || state.getAccessTokenExpirationTime() < System.currentTimeMillis()){
            refreshAccessToken();
        }*/
        AuthorizationServiceDiscovery discovery =
                mStateManager.getCurrent()
                        .getAuthorizationServiceConfiguration()
                        .discoveryDoc;
        URL joburl;
        try {
            joburl =
                    mConfiguration.getUserInfoEndpointUri() != null
                            ? new URL(mConfiguration.getmCloudList().toString())
                            : new URL(discovery.getUserinfoEndpoint().toString());
        } catch (MalformedURLException urlEx) {
            Log.e(TAG, "Failed to construct user info endpoint URL", urlEx);
            mUserInfoJson.set(null);
            //runOnUiThread(this::displayAuthorized);
            return;
        }
        mExecutor.submit(() -> {
            try {
                Response responseQueuePrinter = requestQueuePrinter(accessToken);
                System.err.println("EndRequestPrinterList:"+ new Timestamp(System.currentTimeMillis()));
                int code = responseQueuePrinter.code();
                if (responseQueuePrinter.isSuccessful() == true) {
                    // continue
                    String response = responseQueuePrinter.body().string();
                    //Log.e("requestPrinter", response);
                    Intent intent = new Intent(Stampa.this, Printer_list.class);
                    String accessTokenResponse = getIntent().getExtras().getString("accessToken");
                    intent.putExtra("accessToken", accessTokenResponse);
                    intent.putExtra("list_printer", response);
                    //startActivity(intent);
                    startActivityForResult(intent, REQUEST_CODE);
                    //finish();
                } else {
                    String errorMessage = responseQueuePrinter.body().string();
                    handleError(code, errorMessage);
                }
            } catch (IOException ioEx) {
                Log.e(TAG, "Network error when querying Printer List endpoint", ioEx);
                showSnackbar("Fetching Printer List failed");
            }
            //runOnUiThread(this::displayAuthorized);
        });
    }

    /**
     * metodo per la gestionde degli errori
     * in base al codice risponde con un messaggio/comportamente differente
     */
    private void handleError(int code, String errorMessage) {
        switch (code) {
            case 200:
                //System.out.println("code 200");
                Intent intent_200 = new Intent(Stampa.this, TokenActivity.class);
                intent_200.putExtra("response", errorMessage);
                startActivity(intent_200);
                finish();
                break;
            case 401:
                //System.out.println("Error code 401");
                Intent intent = new Intent(Stampa.this, TokenActivity.class);
                intent.putExtra("response", errorMessage);
                startActivity(intent);
                finish();
                break;
            default:
                Intent intent_deafult = new Intent(Stampa.this, TokenActivity.class);
                intent_deafult.putExtra("response", errorMessage);
                startActivity(intent_deafult);
                finish();
        }
    }
    /**
     * barra di caricamento - progress bar
     */
    @MainThread
    private void displayLoading(String message) {
        findViewById(R.id.loading_container_stampa).setVisibility(View.VISIBLE);
        findViewById(R.id.design_scelta_stampante).setVisibility(View.GONE);
        //findViewById(R.id.toolbar).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.loading_description)).setText(message);
    }

    public void onCIECheckboxClicked(View view) {
        checked = ((CheckBox) view).isChecked();
        //System.out.println("checked: " + checked);
    }

    private Response requestPrint(String accessToken, String jobID, String printerID) throws IOException {
        System.err.println("StartRequestPrintNoCie:"+ new Timestamp(System.currentTimeMillis()));
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
        MediaType mediaType = MediaType.parse("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");
        RequestBody body = RequestBody.create(mediaType, "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"access_token\"\r\n\r\n"+accessToken+"\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"JobID\"\r\n\r\n"+jobID+"\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"printerDestinationId\"\r\n\r\n"+printerID+"\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW--");
        Request request = new Request.Builder()
                .url("https://"+getString(R.string.base_url)+"/print_token_easy")
                .post(body)
                .addHeader("Content-Type", "multipart/form-data; boundary=--------------------------860918798237058699725212")
                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Connection", "keep-alive")
                .build();

        Log.d(TAG, ReadAPI.bodyToString(request));
        return client.newCall(request).execute();
    }

    //usa quella in activity
    private void print(String accessToken, ArrayList jobList, String printerDestinationId) {
        displayLoading("Mando i job in stampa...");
        mExecutor.submit(() -> {
            Iterator iterator;
            int printCode = 400;
            String response_message = "";
            try {
                Response responsePrint=null;
                iterator = jobList.iterator();
                while (iterator.hasNext()) {
                    String jobID = iterator.next().toString();
                    responsePrint = requestPrint(accessToken, jobID, printerDestinationId);
                    System.err.println("EndRequestPrintNoCie:"+ new Timestamp(System.currentTimeMillis()));
                    printCode = responsePrint.code();
                    //print(accessTokenSC, accessTokenResponse, jobID, PrinterID);
                }
                //System.out.println("printCode: "+printCode);
                if (jobList.size() > 0) {
                    if (printCode == 200) {
                        response_message = "I lavori sono stati mandati correttamente nella stampante selezionata.";
                    } else {
                        response_message = responsePrint.body().string();
                    }
                }
            } catch (Exception e) {
                //Log.e(TAG, "Errore nella stampa dei Jobs", e);
                response_message = "Stampa fallita, riprovare." + e.getMessage();
            }
            Intent intent = new Intent(this, TokenActivity.class);
            intent.putExtra("response", response_message);
            startActivity(intent);
            finish();
        });
    }
}
