package eu.fbk.st.pullprinting.Activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import eu.fbk.st.pullprinting.R;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;


import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceDiscovery;

import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import eu.fbk.st.pullprinting.AuthModule.AuthStateManager;
import eu.fbk.st.pullprinting.Configuration.Configuration;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class QrReader extends AppCompatActivity {

    SurfaceView cameraPreview;
    CameraSource cameraSource;
    TextView textView;
    BarcodeDetector barcodeDetector;
    final int RequestPermissionID=1001;
    private ExecutorService mExecutor;
    private static final String TAG = "QRreader";
    private String idPrinterQR;
    private int REQUEST_CODE=700;
    private boolean qrLetto= false;

    private CountDownTimer countDownTimer;
    private long timeleftinmilliseconds = 10000; //30 secondi per inquadrare il QR
    private boolean timeRunning;


    private AuthStateManager mStateManager;
    private Configuration mConfiguration;
    private final AtomicReference<JSONObject> mUserInfoJson = new AtomicReference<>();
    private AuthorizationService mAuthService;


    /**
     * controlla i permessi per accedere alla fotocamera dell'utente
     * se corretti allora avvia la camera per la lettura del QRCode
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RequestPermissionID:
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                try {
                    cameraSource.start(cameraPreview.getHolder());
                    //CIE instance
                    //initializeAppAuth();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                onBackPressed();
            }
            break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_reader);
        findViewById(R.id.loading_container_qr).setVisibility(View.GONE);

        mStateManager = AuthStateManager.getInstance(this);
        mConfiguration = Configuration.getInstance(this);
        mAuthService = new AuthorizationService(
                this,
                new AppAuthConfiguration.Builder()
                        .setConnectionBuilder(mConfiguration.getConnectionBuilder())
                        .build());

        startTimer();

        cameraPreview = (SurfaceView) findViewById(R.id.camerapreview);
        textView = (TextView) findViewById(R.id.textview);
        barcodeDetector = new BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build();
        cameraSource = new CameraSource.Builder(this, barcodeDetector)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(24)
                .setAutoFocusEnabled(true)
                .setRequestedPreviewSize(640, 480).setAutoFocusEnabled(true)
                .build();
        cameraPreview.setZOrderMediaOverlay(true);
        mExecutor = Executors.newSingleThreadExecutor();



        cameraPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(),Manifest.permission.READ_CONTACTS)  != PackageManager.PERMISSION_GRANTED) {

                    /*if (ActivityCompat.shouldShowRequestPermissionRationale(QrReader.this,
                            Manifest.permission.CAMERA)) {
                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.
                    } else {
                        ActivityCompat.requestPermissions(QrReader.this,
                                new String[]{Manifest.permission.CAMERA}, RequestPermissionID);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }*/

                    ActivityCompat.requestPermissions(QrReader.this,
                            new String[]{Manifest.permission.CAMERA}, RequestPermissionID);

                } else {
                    //if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    //ActivityCompat.requestPermissions(QrReader.this,
                    //      new String[]{Manifest.permission.CAMERA},RequestPermissionID);
                    //return;
                    //}
                    ActivityCompat.requestPermissions(QrReader.this,
                            new String[]{Manifest.permission.CAMERA}, RequestPermissionID);

                    try {
                        cameraSource.start(holder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                cameraSource.stop();
            }
        });

        /**
         * legge il codice QR e chiama una activity che si occuperà di autenticare l'utente attraverso un secondo fattore, la CIE
         */
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>() {
            @Override
            public void release() {

            }
            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                SparseArray<Barcode> qrCodes = detections.getDetectedItems();
                if (qrCodes.size()!=0) textView.post(new Runnable() {
                    @Override
                    public void run() {

                        if (qrLetto==false) {
                            qrLetto=true;

                            Vibrator vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                            vibrator.vibrate(0);
                            //textView.setText(qrCodes.valueAt(0).displayValue);
                            //commentati ora nell'ulitma versione con dialog di conferma stampante
                            //textView.setText("Richiesta di autenticazione in corso...");
                            //displayLoading("Richiesta di autenticazione...");
                            //textView.setText("Mando i job in stampa...");
                            idPrinterQR = qrCodes.valueAt(0).displayValue;
                            //test
                            //String accessTokenResponse = getIntent().getExtras().getString("accessToken");


                            //System.out.println(idPrinterQR);
                            validateQRreader(idPrinterQR);


                            //rimettere per stampare
                            /*Intent intent = new Intent(QrReader.this, Stampa.class);
                            intent.putExtra("stampance_scelta_attraverso", "QR");
                            intent.putExtra("barcode",idPrinterQR );
                            //intent.putExtra("accessTokenResponse", accessTokenResponse);
                            //System.out.println("idPrinterQR:"+idPrinterQR);
                            //System.out.println("QR letto");
                            setResult(RESULT_OK, intent);
                            finish();
                            //Intent intentCie = new Intent(QrReader.this, CieAuth.class);
                            //startActivityForResult(intentCie, REQUEST_CODE);*/
                        }
                    }
                });
            }
        });
    }


    public void validateQRreader(String valueQR){
        countDownTimer.cancel();
        /*System.out.println("QR value: "+valueQR);
        System.out.println("check1:"+valueQR.length());
        System.out.println("check2:"+valueQR.charAt(8));
        System.out.println("check3:"+valueQR.charAt(13));
        System.out.println("check4:"+valueQR.charAt(18));
        System.out.println("check5:"+valueQR.charAt(23));
        System.out.println("chekc6:"+(valueQR.charAt(8) == '-'));*/


        if (valueQR.length()!=36 || !(valueQR.charAt(8) == '-') || !(valueQR.charAt(13) == '-') || !(valueQR.charAt(18) == '-') || !(valueQR.charAt(23) == '-')) {
            //System.out.println("formattazione contenuto QR errata");
            //Intent intent = new Intent(QrReader.this, Printer_list.class);
            //startActivityForResult(intent, REQUEST_CODE);
            printerList();
        }
        else {
            Intent intent = new Intent(QrReader.this, Stampa.class);
            intent.putExtra("stampance_scelta_attraverso", "QR");
            intent.putExtra("barcode",idPrinterQR );
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    public void startTimer(){
        //System.out.println("Start timer");
        countDownTimer = new CountDownTimer(timeleftinmilliseconds,1000) {
            @Override
            public void onTick(long l) {
                timeleftinmilliseconds = l;
            }

            @Override
            public void onFinish() {
                //Intent intent = new Intent(QrReader.this, Printer_list.class);
                //startActivityForResult(intent, REQUEST_CODE);
                printerList();
            }
        }.start();
    }

    @MainThread
    private void displayLoading(String message) {
        findViewById(R.id.loading_container_qr).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.loading_description_qr)).setText(message);
        findViewById(R.id.camerapreview).setVisibility(View.GONE);
        findViewById(R.id.textview).setVisibility(View.GONE);


    }


    //non prender il token per fare il fetching
    private void printerList() {
        displayLoading("Fetching Printer List");
        mStateManager.getCurrent().performActionWithFreshTokens(mAuthService, this::printerList);
    }


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
                    Intent intent = new Intent(QrReader.this, Printer_list.class);
                    intent.putExtra("list_printer", response);
                    //System.out.println("requestPrinter_QRactivity: "+response);
                    //startActivity(intent);
                    startActivityForResult(intent, REQUEST_CODE);
                    //finish();
                } else {
                    String errorMessage = responseQueuePrinter.body().string();
                }
            } catch (IOException ioEx) {
                Log.e(TAG, "Network error when querying Printer List endpoint", ioEx);
            }
            //runOnUiThread(this::displayAuthorized);
        });
    }


    private Response requestQueuePrinter(String authToken) throws IOException {
        System.err.println("StartRequestPrinterList:"+ new Timestamp(System.currentTimeMillis()));
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        //OkHttpClient client = new OkHttpClient();
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();        Request request = new Request.Builder()
                //                .url("https://"+ getString(R.string.base_url) + "/list_printer?access_token=" + authToken) //prima era printer se non stampanti admin
                .url("https://"+ getString(R.string.base_url) + "/list_printer") //prima era printer se non stampanti admin
                .get()
                .addHeader("cache-control", "no-cache")
                .build();
        //Response response = client.newCall(request).execute();

        return client.newCall(request).execute();
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
        idPrinterQR = data.getExtras().getString("printer_list_result");
        Intent intent = new Intent(QrReader.this, Stampa.class);
        intent.putExtra("stampance_scelta_attraverso", "QR");
        intent.putExtra("barcode",idPrinterQR );
        setResult(RESULT_OK, intent);
        finish();
        //}
    }


}
