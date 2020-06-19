package eu.fbk.st.pullprinting.Activities;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.WorkerThread;
import android.support.customtabs.CustomTabsIntent;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.common.internal.service.Common;

import eu.fbk.st.pullprinting.Model.MessageFragment;
import eu.fbk.st.pullprinting.Model.Model;
import eu.fbk.st.pullprinting.R;

import org.json.JSONArray;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceDiscovery;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import eu.fbk.st.pullprinting.AuthModule.AuthStateManager;
import eu.fbk.st.pullprinting.Configuration.Configuration;
import eu.fbk.st.pullprinting.Utilities.Commons;
import eu.fbk.st.pullprinting.Utilities.ReadAPI;
import eu.fbk.st.pullprinting.Utilities.RemainderBroadcast;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Okio;

import org.joda.time.format.DateTimeFormat;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class TokenActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "TokenActivity";
    private static final String KEY_USER_INFO = "userInfo";

    private AuthorizationService mAuthService;
    private AuthStateManager mStateManager;
    private final AtomicReference<JSONObject> mUserInfoJson = new AtomicReference<>();
    private Configuration mConfiguration;
    private ExecutorService mExecutor;
    public JSONObject my_json = new JSONObject();
    String accessTokenResponse;
    String response;
    String response_secure;
    boolean nessun_job = false;
    boolean fetching = false;
    private ListView lv;
    private ArrayList<Model> modelArrayList;
    private CustomAdapter customAdapter;
    private Button btnselect, btndeselect, btnnext, btndelete;
    private String[] joblist_array = new String[]{"job1", "job2", "job3", "job4"};
    private ArrayList<String> job_da_stampare = new ArrayList<String>();
    String id = "100";
    private boolean messaggioLetto = false;
    private String response_stampa;
    public static boolean checked = true;
    //public static boolean checkBox;
    public static boolean secure_print_active=false;
    public static final int REQUEST_CODE = 100;
    public static final int AUTH_SC_CODE = 900;
    public static final int PERMISSION_REQUEST = 200;
    public static String PrinterID;
    public static ArrayList jobResult;
    TextView result;
    ArrayList jobToDo;
    public static TimerTask timer;
    private static final String CHANNEL_ID = "simplied_coding";

    private DrawerLayout drawer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mStateManager = AuthStateManager.getInstance(this);
        mConfiguration = Configuration.getInstance(this);
        mExecutor = Executors.newSingleThreadExecutor();
        mAuthService = new AuthorizationService(
                this,
                new AppAuthConfiguration.Builder()
                        .setConnectionBuilder(mConfiguration.getConnectionBuilder())
                        .build());
        setContentView(R.layout.activity_token);
        displayLoading("Restoring state...");
        if (savedInstanceState != null) {
            try {
                mUserInfoJson.set(new JSONObject(savedInstanceState.getString(KEY_USER_INFO)));
            } catch (JSONException ex) {
                Log.e(TAG, "Failed to parse saved user info JSON, discarding", ex);
            }
        }


       /* NavController navController = Navigation.findNavController(this, R.id.nav_view);
        AppBarConfiguration appBarConfiguration =
                new AppBarConfiguration.Builder(navController.getGraph()).build();
        Toolbar toolbar = findViewById(R.id.toolbar);
        NavigationUI.setupWithNavController(
                toolbar, navController, appBarConfiguration);*/


        //sidebar add
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        //sidebar add end

        //create channel for push notification

        /*if(getIntent().getExtras().getBoolean("uscita_confermata")==true){
            //System.out.println("signOut(): "+getIntent().getExtras().getBoolean("uscita_confermata"));
            signOut();
        }*/

        //to fix per avere il messaggio sul response... altrimenti crasha quando l'utente ha già fatto il login!
        try {
            response_stampa = getIntent().getExtras().getString("response_message");
            //System.out.println("response_message to show: "+response_stampa);
            AlertDiStampa(response_stampa);
        } catch (Exception e){
            Log.d("Error", "Exception occurred: "+e);
        }

        /*if (response_stampa != "" && response_stampa != null) {
            if (messaggioLetto == false) {
                AlertDiStampa(response_stampa);
                messaggioLetto = true;
            }
        }*/

        //controllo se è uscito una volta, da non riepresentare in caso di rotazione schermo
        ((TextView) findViewById(R.id.respone_stampa)).setText(response_stampa);


        //checkBox = (CheckBox) findViewById(R.id.CIEcheckbox);
        //checkBox.setChecked(checked);
        result = (TextView) findViewById(R.id.result);


    }

    public void onResponse(Response response) {
        modelArrayList = getModel(false);
        customAdapter = new CustomAdapter(this, modelArrayList);
        //add this setter to your data object if not exist.
        customAdapter.notifyDataSetChanged();
        lv.setAdapter(customAdapter);
        //System.out.println("onResponseMethod");
    }


    //create sidebar for logout
    @Override
    public void onBackPressed() {

        createDialog();

        /*if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }*/
    }


    public void createDialog() {
        AlertDialog.Builder exit = new AlertDialog.Builder(this);
        exit.setMessage("Sicuro di voler uscire?");
        exit.setCancelable(false);

        exit.setPositiveButton("Si", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                TokenActivity.super.onBackPressed();
                signOut();
            }
        });
        exit.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        exit.create().show();
    }

    private ArrayList<Model> getModel(boolean isSelect) {
        ArrayList<Model> list = new ArrayList<>();
        String jsonResult = response;
        int jobCounter = 0;
        try {
            JSONObject json = new JSONObject(jsonResult);
            jobCounter = Integer.parseInt(json.getJSONObject("range").getString("jobsCount"));
        } catch (JSONException e) {
            Log.e("MYAPP", "unexpected JSON exception", e);
        }
        for (int i = 0; i < jobCounter; i++) {
            //System.out.println("jobCounter: "+jobCounter);
            try {
                JSONObject json = new JSONObject(jsonResult);
                String title = json.getJSONArray("jobs").getJSONObject(i).getString("title");
                String JobID = (json.getJSONArray("jobs").getJSONObject(i).getString("id"));
                String stateOfJob = json.getJSONArray("jobs").getJSONObject(i).getJSONObject("uiState").getString("summary");
                String jobDestination = json.getJSONArray("jobs").getJSONObject(i).getString("printerid");
                if (stateOfJob.contentEquals("IN_PROGRESS") && jobDestination.contentEquals(Commons.spoolerID)) {
                    Model model = new Model();
                    model.setSelected(isSelect);
                    model.setJob(title + "\n" + ":_:" + JobID);
                    //test per select all
                    //customAdapter.job_selezionati.add(title+"\n"+"_"+JobID); //why not???
                    list.add(model);
                    if (isSelect == true) {
                        //customAdapter.job_selezionati.add(model);
                        //inserisci tutti gli elementi nella lista c
                    }
                }
            } catch (JSONException e) {
                Log.e("JSON OBJECT", "unexpected JSON exception", e);
            }
        }


        //secure mode
        String jsonResult_secure = response_secure;
        int jobCounter_secure = 0;
        try {
            JSONObject json = new JSONObject(jsonResult_secure);
            jobCounter_secure = Integer.parseInt(json.getJSONObject("range").getString("jobsCount"));
        } catch (JSONException e) {
            Log.e("MYAPP", "unexpected JSON exception", e);
        }
        for (int i = 0; i < jobCounter_secure; i++) {
            //System.out.println("jobCounter: "+jobCounter_secure);
            try {
                JSONObject json = new JSONObject(jsonResult_secure);
                String title = json.getJSONArray("jobs").getJSONObject(i).getString("title");
                String JobID = (json.getJSONArray("jobs").getJSONObject(i).getString("id"));
                String stateOfJob = json.getJSONArray("jobs").getJSONObject(i).getJSONObject("uiState").getString("summary");
                String jobDestination = json.getJSONArray("jobs").getJSONObject(i).getString("printerid");
                if (stateOfJob.contentEquals("IN_PROGRESS") && jobDestination.contentEquals(getString(R.string.SecureSpoolerID))) {
                    Model model = new Model();
                    model.setSelected(isSelect);
                    model.setJob(title + "\n" + ":_:" + JobID + ":_: \t" + " *SECURE*" );
                    //test per select all
                    //customAdapter.job_selezionati.add(title+"\n"+"_"+JobID); //why not???
                    list.add(model);
                    if (isSelect == true) {
                        //customAdapter.job_selezionati.add(model);
                        //inserisci tutti gli elementi nella lista c
                    }
                }
            } catch (JSONException e) {
                Log.e("JSON OBJECT", "unexpected JSON exception", e);
            }
        }

        if (list == null || jobCounter + jobCounter_secure == 0) {
            nessun_job = true;
            Model model = new Model();
            model.setSelected(isSelect);
            model.setJob("NON CI SONO JOB");
            list.add(model);
        } else {
            nessun_job = false;
        }
        return list;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mExecutor.isShutdown()) {
            mExecutor = Executors.newSingleThreadExecutor();
        }
        if (mStateManager.getCurrent().isAuthorized()) {
            displayAuthorized();
            return;
        }
        else{
            displayNotAuthorized("No authorization state retained - reauthorization required");
        }
        AuthorizationResponse response = AuthorizationResponse.fromIntent(getIntent());
        AuthorizationException ex = AuthorizationException.fromIntent(getIntent());
        if (response != null || ex != null) {
            mStateManager.updateAfterAuthorization(response, ex);
        }
        if (response != null && response.authorizationCode != null) {
            // authorization code exchange is required
            mStateManager.updateAfterAuthorization(response, ex);
            exchangeAuthorizationCode(response);
        } else if (ex != null) {
            displayNotAuthorized("Authorization flow failed: " + ex.getMessage());
        } else {
            displayNotAuthorized("No authorization state retained - reauthorization required");
        }

    }


    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mUserInfoJson.get() != null) {
            state.putString(KEY_USER_INFO, mUserInfoJson.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAuthService.dispose();
    }

    /**
     * display/view che verrà stampata se il token/l'utente risulta non autorizzato
     *
     * @param explanation
     */
    @MainThread
    private void displayNotAuthorized(String explanation) {
        findViewById(R.id.not_authorized).setVisibility(View.VISIBLE);
        findViewById(R.id.authorized).setVisibility(View.GONE);
        findViewById(R.id.loading_container).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.explanation)).setText(explanation);
        findViewById(R.id.reauth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });
    }

    /**
     * barra di caricamento
     *
     * @param message che si vuole inserire nella load bar
     */
    @MainThread
    private void displayLoading(String message) {
        findViewById(R.id.loading_container).setVisibility(View.VISIBLE);
        findViewById(R.id.authorized).setVisibility(View.GONE);
        findViewById(R.id.not_authorized).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.loading_description)).setText(message);
    }

    /**
     * view mostrata se l'utente è autorizzato e possiede un token valido
     */
    @MainThread
    private void displayAuthorized() {
        findViewById(R.id.authorized).setVisibility(View.VISIBLE);
        findViewById(R.id.not_authorized).setVisibility(View.GONE);
        findViewById(R.id.loading_container).setVisibility(View.GONE);

        //tolgo tutti gli elementi non più utili terminati i controlli funzionali
        findViewById(R.id.refresh_token).setVisibility(View.GONE);
        findViewById(R.id.view_profile).setVisibility(View.GONE);
        findViewById(R.id.auth_granted).setVisibility(View.GONE);
        findViewById(R.id.select).setVisibility(View.GONE);

        findViewById(R.id.refresh_token_info).setVisibility(View.GONE);
        findViewById(R.id.access_token_info).setVisibility(View.GONE);
        findViewById(R.id.id_token_info).setVisibility(View.GONE);
        findViewById(R.id.userinfo_card).setVisibility(View.GONE);
        findViewById(R.id.next1).setVisibility(View.GONE);

        findViewById(R.id.container_button).setVisibility(View.GONE);
        AuthState state = mStateManager.getCurrent();
        //refresho il token se scaduto o non valido
        if (state.getRefreshToken() == null || state.getAccessToken() == null || state.getAccessTokenExpirationTime() < System.currentTimeMillis()) {
            refreshAccessToken();
        }
        TextView refreshTokenInfoView = (TextView) findViewById(R.id.refresh_token_info);
        /*refreshTokenInfoView.setText((state.getRefreshToken() == null)
                ? "No refresh token returned"
                : "Refresh token returned");*/
        TextView idTokenInfoView = (TextView) findViewById(R.id.id_token_info);
        /*idTokenInfoView.setText((state.getIdToken()) == null
                ? "No ID Token returned"
                : "ID Token returned");*/
        //Log.d("id_token: ", state.getIdToken());
        TextView accessTokenInfoView = (TextView) findViewById(R.id.access_token_info);
        if (state.getAccessToken() == null) {
            accessTokenInfoView.setText("No access token returned");
        } else {
            Long expiresAt = state.getAccessTokenExpirationTime();
            accessTokenResponse = state.getAccessToken();
            if (expiresAt == null) {
                accessTokenInfoView.setText("Access time has no defined expiry");
            } else if (expiresAt < System.currentTimeMillis()) {
                accessTokenInfoView.setText("Access token has expired ");
            } else {
                String template = "Access token expires at: %s";
                accessTokenInfoView.setText(String.format(template,
                        DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss ZZ").print(expiresAt)));
                Log.d(TAG,"accessTokenResponse: " + accessTokenResponse + " - expiresAt: "+String.format(template,
                        DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss ZZ").print(expiresAt)));
            }
        }

        //sidebar config
        Toolbar toolbar = findViewById(R.id.toolbar_navbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        /*if (saveInstanceState==null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MessageFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_message);
        }*/
        //sidebar config


        Button refreshTokenButton = (Button) findViewById(R.id.refresh_token);
        refreshTokenButton.setVisibility(state.getRefreshToken() != null
                ? View.VISIBLE
                : View.GONE);
        refreshTokenButton.setOnClickListener((View view) -> refreshAccessToken());

        Button viewProfileButton = (Button) findViewById(R.id.view_profile);

        AuthorizationServiceDiscovery discoveryDoc =
                state.getAuthorizationServiceConfiguration().discoveryDoc;
        if ((discoveryDoc == null || discoveryDoc.getUserinfoEndpoint() == null)
                && mConfiguration.getUserInfoEndpointUri() == null) {
            viewProfileButton.setVisibility(View.GONE);
        } else {
            viewProfileButton.setVisibility(View.VISIBLE);
            viewProfileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fetchUserInfo();
                }
            });
            //provare se una volta scaduto il token serve riassegnarlo alla variabile
            //accessTokenResponse = state.getAccessToken();
            //System.out.println("token refreshed: " + accessTokenResponse);
        }
        Button Job = (Button) findViewById(R.id.job_list);
        Button Printer = (Button) findViewById(R.id.printer_list);
        Button Print = (Button) findViewById(R.id.submit);
        Button Next = (Button) findViewById(R.id.next);
        Button Delete = (Button) findViewById(R.id.delete);

        Job.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //refresho il token se scaduto o non valido
                if (state.getRefreshToken() == null || state.getAccessToken() == null || state.getAccessTokenExpirationTime() < System.currentTimeMillis()) {
                    refreshAccessToken();
                }

                timer = new TimerTask() {
                    @Override
                    public void run() {
                        if (System.currentTimeMillis() - scheduledExecutionTime() >=8.64e+7)
                            ExtractSpoolerID();
                            ExtractSecureSpoolerID();
                    }
                };

                jobCount();
                jobList();
            }
        });

        /*Printer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printerList();
            }
        });
        Print.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                print();
            }
        });*/

        ((Button) findViewById(R.id.sign_out)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signOut();
            }
        });

        View userInfoCard = findViewById(R.id.userinfo_card);
        JSONObject userInfo = mUserInfoJson.get();
        if (userInfo == null) {
            userInfoCard.setVisibility(View.INVISIBLE);
        } else {
            try {
                String name = "???";
                if (userInfo.has("name")) {
                    name = userInfo.getString("name");
                }
                ((TextView) findViewById(R.id.userinfo_name)).setText(name);

                /*if (userInfo.has("picture")) {
                    GlideApp.with(TokenActivity.this)
                            .load(Uri.parse(userInfo.getString("picture")))
                            .fitCenter()
                            .into((ImageView) findViewById(R.id.userinfo_profile));
                }
*/
                ((TextView) findViewById(R.id.userinfo_json)).setText(mUserInfoJson.toString());
                //userInfoCard.setVisibility(View.VISIBLE);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed to read userinfo JSON", ex);
            }
        }

        if (fetching == false) {
            Job.performClick();
            fetching = true;
        }

        lv = (ListView) findViewById(R.id.lv);
        btnselect = (Button) findViewById(R.id.select);
        btndeselect = (Button) findViewById(R.id.deselect);
        btnnext = (Button) findViewById(R.id.next);
        btndelete = (Button) findViewById(R.id.delete);

        if (nessun_job == true) {
            ((ScrollView) findViewById(R.id.ScrollView)).setVisibility(View.GONE);
            lv.setVisibility(View.GONE);
            btnnext.setEnabled(false);
            btnnext.setVisibility(View.GONE);
            btndelete.setEnabled(false);
            btndelete.setVisibility(View.GONE);
            btndeselect.setVisibility(View.GONE);
            btnselect.setVisibility(View.GONE);
            ((TextView) findViewById(R.id.respone_stampa)).setText("No job in the queue");
            ((TextView) findViewById(R.id.respone_stampa)).setVisibility(View.VISIBLE);

        } else {
            ((ScrollView) findViewById(R.id.ScrollView)).setVisibility(View.VISIBLE);
            lv.setVisibility(View.VISIBLE);
            btnnext.setEnabled(true);
            btnnext.setVisibility(View.VISIBLE);
            btndelete.setEnabled(true);
            btndelete.setVisibility(View.VISIBLE);
            btndeselect.setVisibility(View.VISIBLE);
            btnselect.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.respone_stampa)).setText("");
            ((TextView) findViewById(R.id.respone_stampa)).setVisibility(View.GONE);
        }
        //modelArrayList = getModel(false);
        //customAdapter = new CustomAdapter(this,modelArrayList);
        //lv.setAdapter(customAdapter);

        btnselect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modelArrayList = getModel(true);
                customAdapter = new CustomAdapter(TokenActivity.this, modelArrayList);
                lv.setAdapter(customAdapter);
            }
        });
        btndeselect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modelArrayList = getModel(false);


                customAdapter = new CustomAdapter(TokenActivity.this, modelArrayList);
                lv.setAdapter(customAdapter);
            }
        });
        btnnext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (customAdapter.job_selezionati.size() != 0) {
                    Intent intent = new Intent(TokenActivity.this, QrReader.class);

                    intent.putExtra("json", response);
                    intent.putExtra("accessToken", accessTokenResponse);
                    intent.putExtra("jobToDo", customAdapter.job_selezionati);
                    jobToDo=customAdapter.job_selezionati;
                    //System.out.println("cosa passo come json (tutti gli id dei job?: "+ customAdapter.job_selezionati);
                    //System.out.println("o json?= "+response);

                    //String accessTokenResponse = getIntent().getExtras().getString("accessToken");
                    //intent.putExtra("accessToken",accessTokenResponse);
                    //startActivity(intent);
                    //finish();

                    startActivityForResult(intent, REQUEST_CODE);

                } else {
                    AlertDiStampa("Non hai selezionato nessun lavoro da stampare!");
                }
            }
        });
        btndelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if (customAdapter.job_selezionati.size() != 0) {
                    delete(accessTokenResponse,customAdapter.job_selezionati);

                } else {
                    AlertDiStampa("Non hai selezionato nessun lavoro da stampare!");
                }
            }
        });


        Commons.copy_speed_map.put("TOSHIBA e-STUDIO4555C",50.0);
        Commons.copy_speed_map.put("ECOSYS P6230cdn",30.0);
        Commons.first_copy_map.put("TOSHIBA e-STUDIO4555C",6.1);
        Commons.first_copy_map.put("ECOSYS P6230cdn",7.5);
    }


    @MainThread
    private void refreshAccessToken() {
        displayLoading("Refreshing access token");
        performTokenRequest(
                mStateManager.getCurrent().createTokenRefreshRequest(),
                this::handleAccessTokenResponse);
        accessTokenResponse = mStateManager.getCurrent().getAccessToken();
    }

    @MainThread
    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
        displayLoading("Exchanging authorization code");
        performTokenRequest(
                authorizationResponse.createTokenExchangeRequest(),
                this::handleCodeExchangeResponse);
    }

    @MainThread
    private void performTokenRequest(
            TokenRequest request,
            AuthorizationService.TokenResponseCallback callback) {
        ClientAuthentication clientAuthentication;
        try {
            clientAuthentication = mStateManager.getCurrent().getClientAuthentication();
        } catch (ClientAuthentication.UnsupportedAuthenticationMethod ex) {
            Log.d(TAG, "Token request cannot be made, client authentication for the token "
                    + "endpoint could not be constructed (%s)", ex);
            displayNotAuthorized("Client authentication method is unsupported");
            return;
        }

        mAuthService.performTokenRequest(
                request,
                clientAuthentication,
                callback);
    }

    @WorkerThread
    private void handleAccessTokenResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        mStateManager.updateAfterTokenResponse(tokenResponse, authException);
        runOnUiThread(this::displayAuthorized);
    }

    @WorkerThread
    private void handleCodeExchangeResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {

        mStateManager.updateAfterTokenResponse(tokenResponse, authException);
        if (!mStateManager.getCurrent().isAuthorized()) {
            final String message = "Authorization Code exchange failed"
                    + ((authException != null) ? authException.error : "");

            // WrongThread inference is incorrect for lambdas
            //noinspection WrongThread
            runOnUiThread(() -> displayNotAuthorized(message));
        } else {
            runOnUiThread(this::displayAuthorized);
        }
    }

    @MainThread
    private void fetchUserInfo() {
        displayLoading("Fetching user info");
        mStateManager.getCurrent().performActionWithFreshTokens(mAuthService, this::fetchUserInfo);
    }

    @MainThread
    private void fetchUserInfo(String accessToken, String idToken, AuthorizationException ex) {
        if (ex != null) {
            Log.e(TAG, "Token refresh failed when fetching user info");
            mUserInfoJson.set(null);
            runOnUiThread(this::displayAuthorized);
            return;
        }

        AuthorizationServiceDiscovery discovery =
                mStateManager.getCurrent()
                        .getAuthorizationServiceConfiguration()
                        .discoveryDoc;

        URL userInfoEndpoint;
        try {
            userInfoEndpoint =
                    mConfiguration.getUserInfoEndpointUri() != null
                            ? new URL(mConfiguration.getUserInfoEndpointUri().toString())
                            : new URL(discovery.getUserinfoEndpoint().toString());
        } catch (MalformedURLException urlEx) {
            Log.e(TAG, "Failed to construct user info endpoint URL", urlEx);
            mUserInfoJson.set(null);
            runOnUiThread(this::displayAuthorized);
            return;
        }

        mExecutor.submit(() -> {
            try {
                HttpURLConnection conn =
                        (HttpURLConnection) userInfoEndpoint.openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + accessToken);
                conn.setInstanceFollowRedirects(false);
                String response = Okio.buffer(Okio.source(conn.getInputStream()))
                        .readString(Charset.forName("UTF-8"));


                mUserInfoJson.set(new JSONObject(response));
            } catch (IOException ioEx) {
                Log.e(TAG, "Network error when querying userinfo endpoint", ioEx);
                showSnackbar("Fetching user info failed");
            } catch (JSONException jsonEx) {
                Log.e(TAG, "Failed to parse userinfo response");
                showSnackbar("Failed to parse user info");
            }

            runOnUiThread(this::displayAuthorized);
        });
    }

    @MainThread
    private void showSnackbar(String message) {
        Snackbar.make(findViewById(R.id.coordinator),
                message,
                Snackbar.LENGTH_SHORT)
                .show();
    }
    //private void jobListJSON() {
    //displayLoading("Fetching Job List");
    //mStateManager.getCurrent().performActionWithFreshTokens(mAuthService, this::jobListJSON);
    //}

    private void jobList() {
        displayLoading("Fetching Job List");
        mStateManager.getCurrent().performActionWithFreshTokens(mAuthService, this::jobList);
    }

    private void jobCount() {
        displayLoading("Fetching Job Count");
        mStateManager.getCurrent().performActionWithFreshTokens(mAuthService, this::jobCount);
    }

    private void ExtractSpoolerID() {
        displayLoading("extract Spooler ID");
        mStateManager.getCurrent().performActionWithFreshTokens(mAuthService, this::extractSpoolerID);
    }

    private void ExtractSecureSpoolerID() {
        displayLoading("extract Spooler ID");
        mStateManager.getCurrent().performActionWithFreshTokens(mAuthService, this::extractSecureSpoolerID);
    }

    private void getModelPrinter() {
        displayLoading("extract printer model");
        mStateManager.getCurrent().performActionWithFreshTokens(mAuthService, this::getModelPrinter);
    }


    private Response requestQueueJobs(String authToken, String printerId) throws IOException {
        System.err.println("StartRequestJobList:"+ new Timestamp(System.currentTimeMillis()));
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
        MediaType mediaType = MediaType.parse("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");
        //RequestBody body = RequestBody.create(mediaType, "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"" + printerId + "\"\r\n\r\n4f6b7646-f5de-6950-684d-e393434aff2e\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW--");
        //status in progress added

        RequestBody body = RequestBody.create(mediaType, "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"status\"\r\n\r\nIN_PROGRESS\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"printerid\"\r\n\r\n"+printerId+"\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"limit\"\r\n\r\n"+(R.integer.limit)+"\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW--");

        Request request = new Request.Builder()
                .url("https://www.google.com/cloudprint/jobs")
                .post(body)
                .addHeader("content-type", "multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW")
                .addHeader("X-CloudPrint-Proxy", "node-gcp")
                .addHeader("Authorization", "OAuth " + authToken)
                .addHeader("cache-control", "no-cache")
                .build();
        Log.d(TAG, ReadAPI.bodyToString(request));
        return client.newCall(request).execute();

    }

    /**
     * metodo per il signout e distuzione dell'authtoken google
     */
    @MainThread
    private void signOut() {
        AuthState currentState = mStateManager.getCurrent();
        AuthState clearedState =
                new AuthState(currentState.getAuthorizationServiceConfiguration());
        if (currentState.getLastRegistrationResponse() != null) {
            clearedState.update(currentState.getLastRegistrationResponse());
        }
        mStateManager.replace(clearedState);
        Intent mainIntent = new Intent(this, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(mainIntent);
        finish();
    }

    /**
     * metodo che attraverso un mExecutor esegue la chiamata http per ottenere i dati relativi alla lista dei job
     *
     * @param accessToken token d'autenticazione
     * @param s
     * @param ex
     */
    private void jobList(String accessToken, String s, AuthorizationException ex) {
        //controllo se il token è valido altrimenti lo refresho
        AuthState state = mStateManager.getCurrent();
        if (state.getRefreshToken() == null || state.getAccessToken() == null || state.getAccessTokenExpirationTime() < System.currentTimeMillis()) {
            refreshAccessToken();
        }
        if (ex != null) {
            Log.e(TAG, "Token refresh failed when fetching user info");
            mUserInfoJson.set(null);
            runOnUiThread(this::displayAuthorized);
            return;
        }
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
            runOnUiThread(this::displayAuthorized);
            return;
        }
        mExecutor.submit(() -> {
            try {
                Response responseQueueJob = requestQueueJobs(accessToken, Commons.spoolerID);
                Response responseQueueJob_secure = requestQueueJobs(accessToken, Commons.SecureSpoolerID);

                response = responseQueueJob.body().string();
                response_secure = responseQueueJob_secure.body().string();

                //System.out.println("RESPONSEJOB:"+response);
                onResponse(responseQueueJob);
                onResponse(responseQueueJob_secure);
                mUserInfoJson.set(new JSONObject(response));
            } catch (IOException ioEx) {
                Log.e(TAG, "Network error when querying Job List endpoint", ioEx);
                showSnackbar("Fetching Job List failed");
            } catch (JSONException jsonEx) {
                Log.e(TAG, "Failed to parse Job List response");
                showSnackbar("Failed to parse Job List");
            }
            runOnUiThread(this::displayAuthorized);
        });
    }


    //message dialog
    public void AlertDiStampa(String message) {
        //creo canale per notifiche
        createNotificationChannel();

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        // Setting Alert Dialog Title
        alertDialogBuilder.setTitle("Response di stampa");
        // Icon Of Alert Dialog
        if (message.equals(getString(R.string.messaggioLavoroCompleto)))
        {
            alertDialogBuilder.setIcon(R.drawable.complete);
        }
        else{
            alertDialogBuilder.setIcon(R.drawable.cancel);
        }
        // Setting Alert Dialog Message
        alertDialogBuilder.setMessage(message);
        alertDialogBuilder.setCancelable(false);
        alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                //finish();
                Toast.makeText(getApplicationContext(), "Check your printer", Toast.LENGTH_SHORT).show();
            }
        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();



        //usare il request code per fare switch da errore a corretto in base al msg di risposta del server.
        Intent intent = new Intent(TokenActivity.this, RemainderBroadcast.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(TokenActivity.this,0,intent,0);
        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        long time_start = System.currentTimeMillis();

        //System.out.println("\""+Commons.acutalModel+"\"");

        Double firstCopyTime= Commons.first_copy_map.get(Commons.acutalModel);
        Double speedCopy = Commons.copy_speed_map.get(Commons.acutalModel);

        int numeroPagine=(Commons.acutalPageNumberBeforePrint);

        //System.out.println("numeroPagine: "+numeroPagine);
        //System.out.println("copy_speed_map: "+Commons.copy_speed_map.get(Commons.acutalModel));
        //System.out.println("first_copy_map: "+Commons.first_copy_map.get(Commons.acutalModel));


        long print_time = (long) (1000*60*(numeroPagine/speedCopy) + (1000*firstCopyTime)); //10 secondi per ex
        //System.out.println("print_time: "+print_time/1000.0);
        alarmManager.set(AlarmManager.RTC_WAKEUP,
                time_start+print_time,pendingIntent);
    }


    private Response requestDelete(String accessToken, String jobID) throws IOException {
        {
            System.err.println("StartRequestDelete:"+ new Timestamp(System.currentTimeMillis()));
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();
            RequestBody formBody = new FormBody.Builder()
                    .add("", "")
                    .build();
            Request request = new Request.Builder()
                    .url("https://"+getString(R.string.base_url) + "/deletejob?access_token="+accessToken+"&jobid="+jobID)
                    .post(formBody)
                    .build();
            Log.d(TAG, ReadAPI.bodyToString(request));
            return client.newCall(request).execute();
        }
    }

    private void delete(String accessToken, ArrayList jobList) {
        displayLoading("Elimino i job...");
        mExecutor.submit(() -> {
            Iterator iterator;
            int printCode = 400;
            String response_message = "";
            try {
                //  showSnackbar("Trying to print your job");
                iterator = jobList.iterator();
                while (iterator.hasNext()) {
                    String jobID = iterator.next().toString();
                    Response responseDeResponse = requestDelete(accessToken, jobID);
                    printCode = responseDeResponse.code();
                    //print(accessTokenSC, accessTokenResponse, jobID, PrinterID);
                }

                if (jobList.size() > 0) {
                    if (printCode == 200) {
                        response_message = "I lavori sono stati eliminati correttamente.";
                    } else if (printCode != 200) {
                        //to fix it!!
                        response_message = "Problema di connessione con il server...";
                    }
                } else {
                    response_message = "Non hai selezionato nessun lavoro";
                }
            } catch (Exception e) {
                //Log.e(TAG, "Errore nella stampa dei Jobs", e);
                response_message = "delete fallito, riprovare." + e.getMessage();
            }
            finally {
                Intent intent = new Intent(this, TokenActivity.class);
                //intent.putExtra("response", response_message);
                startActivity(intent);
                finish();
            }

        });
    }


    private void extractSpoolerID(String accessToken, String s, AuthorizationException ex) {
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
            return;
        }
        mExecutor.submit(() -> {
            try {
                Response responseQueuePrinter = requestQueuePrinter(accessToken);
                if (responseQueuePrinter.isSuccessful() == true) {
                    String response = responseQueuePrinter.body().string();
                    JSONObject obj = new JSONObject("{printer : "+response+"}");
                    List<String> list = new ArrayList<String>();
                    JSONArray array = obj.getJSONArray("printer");
                    for(int i = 0 ; i < array.length() ; i++){
                        if(array.getJSONObject(i).getString("name").toString().equals(getString(R.string.spoolerName))) {
                            Commons.spoolerID=array.getJSONObject(i).getString("id");
                            //System.out.println("spoolerID: "+Commons.spoolerID);
                        }
                    }
                }
            } catch (IOException ioEx) {
                Log.e(TAG, "Network error when querying Printer List endpoint", ioEx);
                showSnackbar("Fetching Printer List failed");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void extractSecureSpoolerID(String accessToken, String s, AuthorizationException ex) {
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
            return;
        }
        mExecutor.submit(() -> {
            try {
                Response responseQueuePrinter = requestQueuePrinter(accessToken);
                if (responseQueuePrinter.isSuccessful() == true) {
                    String response = responseQueuePrinter.body().string();
                    JSONObject obj = new JSONObject("{printer : "+response+"}");
                    List<String> list = new ArrayList<String>();
                    JSONArray array = obj.getJSONArray("printer");
                    for(int i = 0 ; i < array.length() ; i++){
                        if(array.getJSONObject(i).getString("name").toString().equals(getString(R.string.SecureSpoolerName))) {
                            Commons.SecureSpoolerID=array.getJSONObject(i).getString("id");
                            //System.out.println("SecureSpoolerID: "+Commons.SecureSpoolerID);
                        }
                    }
                }
            } catch (IOException ioEx) {
                Log.e(TAG, "Network error when querying Printer List endpoint", ioEx);
                showSnackbar("Fetching Printer List failed");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }


    private Response requestQueuePrinter(String authToken) throws IOException {
        System.err.println("StartRequestPrinterList:"+ new Timestamp(System.currentTimeMillis()));
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();

        Request request = new Request.Builder()
                .url("https://"+getString(R.string.base_url) + "/list_printer") //prima era printer se non stampanti admin
                .get()
                .addHeader("cache-control", "no-cache")
                .build();
        Log.d(TAG, ReadAPI.bodyToString(request));
        //Response response = client.newCall(request).execute();
        return client.newCall(request).execute();
    }



    //checkbox funcion
    public void onCIECheckboxClicked(View view) {
        checked = ((CheckBox) view).isChecked();
        //System.out.println("checked: " + checked);
    }


    //add conferma stampante

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
                if (secure_print_active == true || customAdapter.securePrintDocument_number>0) {
                    checked=true;
                    Intent intent = new Intent(TokenActivity.this, CieAuth.class);
                    intent.putExtra("PrinterID", PrinterID);
                    intent.putExtra("accessTokenResponse", accessTokenResponse);
                    intent.putExtra("jobResult", jobResult);
                    startActivity(intent);
                    Commons.acutalPrinter=PrinterID;
                    finish();
                } else {
                    print(accessTokenResponse, jobResult, PrinterID);
                    Commons.acutalPrinter=PrinterID;
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
                        response_message = getString(R.string.messaggioLavoroCompleto);
                    } else {
                        response_message = responsePrint.body().string();
                    }
                }
            } catch (Exception e) {
                //Log.e(TAG, "Errore nella stampa dei Jobs", e);
                response_message = "Stampa fallita, riprovare." + e.getMessage();
            }
            //System.out.println("response_message: "+response_message);


            Intent intent = new Intent(this, TokenActivity.class);
            intent.putExtra("response_message", response_message);
            startActivity(intent);
            finish();
        });
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


                            getModelPrinter();
                            Commons.acutalPrinter=PrinterID;

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
                        jobResult = jobToDo;
                        //System.out.println("AccessTokenResponse: " + accessTokenResponse + " barcode:" + PrinterID + " jobToDo: " + jobResult.toString());
                        //System.out.println(PrinterID + ", " + accessTokenResponse + ", " + jobResult);
                        dialogConfermaStampante(PrinterID);
                    }
                });
            }
        }
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        Toast.makeText(TokenActivity.this,"onNavigationItemSelected",Toast.LENGTH_LONG).show();
        //System.out.println("menuItem.getItemId(): "+menuItem.getItemId());
        switch (menuItem.getItemId()){
            case R.id.nav_feedback:
                Toast.makeText(this, "Feedback", Toast.LENGTH_SHORT).show();
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new MessageFragment()).commit();
                break;
            case R.id.nav_logout:
                Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show();
                signOut();
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }



    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Channel1";
            String description = "Channel1";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    //get model from printers
    private Response requestModelPrinters(String authToken) throws IOException {
        System.err.println("StartRequestModelPrinters:"+ new Timestamp(System.currentTimeMillis()));
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
        Request request = new Request.Builder()
                .url("https://"+ getString(R.string.base_url) + "/printer_model?printer_id=" + Commons.acutalPrinter+"&access_token="+authToken) //prima era printer se non stampanti admin
                .get()
                .build();
        //Response response = client.newCall(request).execute();
        Log.d(TAG, ReadAPI.bodyToString(request));
        return client.newCall(request).execute();
    }



    private void getModelPrinter(String accessToken, String printerID, AuthorizationException ex) {
        AuthorizationServiceDiscovery discovery =
                mStateManager.getCurrent()
                        .getAuthorizationServiceConfiguration()
                        .discoveryDoc;
        mExecutor.submit(() -> {
            try {
                Response responseModelPrinters = requestModelPrinters(accessToken);
                if (responseModelPrinters.isSuccessful() == true) {
                    String response = responseModelPrinters.body().string();
                    Commons.acutalModel=response;

                }
            } catch (IOException ioEx) {
                Log.e(TAG, "Network error when querying Model List endpoint", ioEx);
                showSnackbar("Fetching Model failed");
            }
        });
    }



    private void jobCount(String accessToken, String s, AuthorizationException ex) {
        AuthState state = mStateManager.getCurrent();
        (Commons.acutalPageNumberBeforePrint)=0;

        if (state.getRefreshToken() == null || state.getAccessToken() == null || state.getAccessTokenExpirationTime() < System.currentTimeMillis()) {
            refreshAccessToken();
        }
        if (ex != null) {
            Log.e(TAG, "Token refresh failed when fetching user info");
            mUserInfoJson.set(null);
            runOnUiThread(this::displayAuthorized);
            return;
        }
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
            runOnUiThread(this::displayAuthorized);
            return;
        }
        mExecutor.submit(() -> {
            try {
                Response responseQueueJob = requestQueueJobs(accessToken, Commons.acutalPrinter);
                //System.out.println("Commons.acutalPrinter: "+Commons.acutalPrinter);
                String response = responseQueueJob.body().string();
                int jobCounter=0;

                try {
                    JSONObject json = new JSONObject(response);
                    jobCounter = Integer.parseInt(json.getJSONObject("range").getString("jobsCount"));
                } catch (JSONException e) {
                    Log.e("MYAPP", "unexpected JSON exception", e);
                }
                for (int i = 0; i < jobCounter; i++) {
                    //System.out.println("jobCounter: " + jobCounter);
                    try {
                        JSONObject json = new JSONObject(response);
                        //System.out.println("RESPONSE COUNT JOB FUNCTION1: " + response);
                        if (json.getJSONArray("jobs").getJSONObject(i).getJSONObject("uiState").getString("summary").equals("IN_PROGRESS")) {
                            Commons.acutalPageNumberBeforePrint += Integer.parseInt(json.getJSONArray("jobs").getJSONObject(i).getString("numberOfPages"));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                //System.out.println("acutalPageNumberBeforePrint: "+ Commons.acutalPageNumberBeforePrint);


            } catch (IOException ioEx) {
                Log.e(TAG, "Network error when querying Job List endpoint", ioEx);
                showSnackbar("Fetching Job Count failed");
            }
        });
    }


    public void testCustomTab(View view){
        String url = "https://am-test.smartcommunitylab.it/aac/eauth/authorize/google?redirect_uri=com.googleusercontent.apps.641468808636-roej63drmm2vaude7n444oj21afbphel%3A%2Foauth2redirect&client_id=e9610874-1548-4311-a663-472ba9c1ce33&response_type=code&state=G9naiYshNwNhI5N9fS364A&scope=openid%20operation.confirmed&code_challenge=9KtceW5_78R6e9zhve-H60TX_V84t021SV1b1dLjCOM&code_challenge_method=S256";
        //String url = "https://idp-ipzs.fbk.eu/CustomTab?OpId=4601772763715634&Time=1583488159489&SP=PullPrinting&IdP=Smart%20Community&Text_Op=Inserisci+il+PIN+per+accedere+a+PullPrinting+tramite+Smart+Community&certRequest=true&nextURL=https://am-test.smartcommunitylab.it/aac/mobile2factor-callback/cie?execution=e2s1";
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(url));

    }
}