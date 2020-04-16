package eu.fbk.st.pullprinting;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import eu.fbk.st.pullprinting.R;
import eu.fbk.st.pullprinting.*;

import net.openid.appauth.AppAuthConfiguration;
import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.browser.AnyBrowserMatcher;
import net.openid.appauth.browser.BrowserMatcher;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import eu.fbk.st.pullprinting.AuthModule.AuthStateManager;
import eu.fbk.st.pullprinting.Configuration.Configuration;
import eu.fbk.st.pullprinting.*;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuthStateManager = AuthStateManager.getInstance(this);
        mConfiguration = Configuration.getInstance(this);
        loginidp = (Button) findViewById(R.id.login_Idp);
        loginidp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAuth();
            }
        });

        if (mAuthStateManager.getCurrent().isAuthorized()
                && !mConfiguration.hasConfigurationChanged()) {
            Log.i(TAG, "User is already authenticated, proceeding to token activity");
            startActivity(new Intent(this, TokenActivity.class));
            finish();
            return;
        }

        initializeAppAuth();

        if (mConfiguration.hasConfigurationChanged()) {
            // discard any existing authorization state due to the change of configuration
            Log.i(TAG, "Configuration change detected, discarding old state");
            mAuthStateManager.replace(new AuthState());
            mConfiguration.acceptConfiguration();
        }

        //startAuth();

    }


    @Override
    protected void onStart() {
        super.onStart();
        AuthorizationServiceConfiguration config = null;
        mAuthStateManager.replace(new AuthState(config));
        initializeAppAuth();
    }

    private static final String TAG = "LoginActivity";
    private static final int RC_AUTH = 100;
    private AuthorizationService mAuthService;
    private AuthStateManager mAuthStateManager;
    private Configuration mConfiguration;
    private final AtomicReference<String> mClientId = new AtomicReference<>();
    private final AtomicReference<AuthorizationRequest> mAuthRequest = new AtomicReference<>();
    private final AtomicReference<CustomTabsIntent> mAuthIntent = new AtomicReference<>();
    private CountDownLatch mAuthIntentLatch = new CountDownLatch(1);
    @NonNull
    private BrowserMatcher mBrowserMatcher = AnyBrowserMatcher.INSTANCE;
    Button loginidp;

    private void createAuthRequest() {
        AuthorizationRequest.Builder authRequestBuilder = new AuthorizationRequest.Builder(
                mAuthStateManager.getCurrent().getAuthorizationServiceConfiguration(),
                mClientId.get(),
                ResponseTypeValues.CODE,
                mConfiguration.getRedirectUri())
                .setScope(mConfiguration.getScope());

        //authRequestBuilder.setAdditionalParameters(additionalParams());
        mAuthRequest.set(authRequestBuilder.build());
    }

    private AuthorizationService createAuthorizationService() {
        Log.i(TAG, "Creating authorization service");
        AppAuthConfiguration.Builder builder = new AppAuthConfiguration.Builder();
        builder.setBrowserMatcher(mBrowserMatcher);
        builder.setConnectionBuilder(mConfiguration.getConnectionBuilder());

        return new AuthorizationService(this, builder.build());
    }

    private void recreateAuthorizationService() {
        if (mAuthService != null) {
            Log.i(TAG, "Discarding existing AuthService instance");
            mAuthService.dispose();
        }
        mAuthService = createAuthorizationService();
        mAuthRequest.set(null);
        mAuthIntent.set(null);
    }

    private void warmUpBrowser() {
        mAuthIntentLatch = new CountDownLatch(1);
        Log.i(TAG, "Warming up browser instance for auth request");
        CustomTabsIntent.Builder intentBuilder =
                mAuthService.createCustomTabsIntentBuilder(mAuthRequest.get().toUri());
        mAuthIntent.set(intentBuilder.build());
        mAuthIntentLatch.countDown();
    }

    private void initializeAuthRequest() {
        createAuthRequest();
        warmUpBrowser();
    }

    private void initializeClient() {
        if (mConfiguration.getClientId() != null) {
            Log.i(TAG, "Using static client ID: " + mConfiguration.getClientId());
            // use a statically configured client ID
            mClientId.set(mConfiguration.getClientId());
            initializeAuthRequest();
        }
    }

    private void handleConfigurationRetrievalResult(
            AuthorizationServiceConfiguration config,
            AuthorizationException ex) {
        if (config == null) {
            Log.i(TAG, "Failed to retrieve discovery document", ex);
            return;
        }

        Log.i(TAG, "Discovery document retrieved");
        mAuthStateManager.replace(new AuthState(config));
        initializeClient();
    }

    private void initializeAppAuth() {
        Log.i(TAG, "Initializing AppAuth");
        recreateAuthorizationService();

        if (mAuthStateManager.getCurrent().getAuthorizationServiceConfiguration() != null) {
            // configuration is already created, skip to client initialization
            Log.i(TAG, "auth config already established");
            initializeClient();
            return;
        }

        // if we are not using discovery, build the authorization service configuration directly
        // from the static configuration values.
        if (mConfiguration.getDiscoveryUri() == null) {
            Log.i(TAG, "Creating auth config from res/raw/auth_config.json");
            AuthorizationServiceConfiguration config = new AuthorizationServiceConfiguration(
                    mConfiguration.getAuthEndpointUri(),
                    mConfiguration.getTokenEndpointUri(),
                    mConfiguration.getRegistrationEndpointUri());
            mAuthStateManager.replace(new AuthState(config));
            initializeClient();
            return;
        }

        // WrongThread inference is incorrect for lambdas
        // noinspection WrongThread
        Log.i(TAG, "Retrieving OpenID discovery doc");
        AuthorizationServiceConfiguration.fetchFromUrl(
                mConfiguration.getDiscoveryUri(),
                this::handleConfigurationRetrievalResult);
    }

    private void doAuth() {
        try {
            mAuthIntentLatch.await();
        } catch (InterruptedException ex) {
            Log.w(TAG, "Interrupted while waiting for auth intent");
        }


        Intent intent = mAuthService.getAuthorizationRequestIntent(
                mAuthRequest.get(),
                mAuthIntent.get());
        startActivityForResult(intent, RC_AUTH);


    }

    void startAuth() {

        doAuth();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Intent intent = new Intent(this, TokenActivity.class);
        intent.putExtras(data.getExtras());
        Bundle bundle = intent.getExtras();
        if(bundle!=null){
            for (String key : bundle.keySet())
                System.out.println(bundle.get(key)!=null ? bundle.get(key) : "NULL");
        }
        //System.out.println("DATA_MAIN ACTIVITY: "+data);
        startActivity(intent);
        finish();
    }

    private HashMap<String,String> additionalParams() {
        HashMap<String, String> addParam = new HashMap<>();
        addParam.put("prompt", "none");
        return addParam;
    }


    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(mAuthService != null){
            mAuthService.dispose();
        }
    }
}
