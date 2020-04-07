/*
 * Copyright 2015 The AppAuth Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.myapplication;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceDiscovery;
import net.openid.appauth.TokenRequest;
import net.openid.appauth.TokenResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Handles token exchange after user authorization.
 */
public class CieTokenActivity extends AppCompatActivity {
    private static final String TAG = "CieTokenActivity";
    private static final String KEY_AUTH_STATE = "authState";
    private static final String KEY_USER_INFO = "userInfo";
    private static final String EXTRA_AUTH_SERVICE_DISCOVERY = "authServiceDiscovery";
    private static final String EXTRA_CLIENT_SECRET = "clientSecret";
    private static final int BUFFER_SIZE = 1024;

    private static AuthState mAuthState;
    private AuthorizationService mAuthService;
    private JSONObject mUserInfoJson;
    private ConfigurationCIE mConfigurationApp;
    private ExecutorService mExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cie_activity_token);
        //System.out.println("CieTokenActivity onCreate");
        mAuthService = new AuthorizationService(this);
        mConfigurationApp = ConfigurationCIE.getInstance(this);
        mExecutor = Executors.newSingleThreadExecutor();
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_AUTH_STATE)) {
                try {
                    mAuthState = AuthState.jsonDeserialize(
                            savedInstanceState.getString(KEY_AUTH_STATE));
                } catch (JSONException ex) {
                    Log.e(TAG, "Malformed authorization JSON saved", ex);
                }
            }
            if (savedInstanceState.containsKey(KEY_USER_INFO)) {
                try {
                    mUserInfoJson = new JSONObject(savedInstanceState.getString(KEY_USER_INFO));
                } catch (JSONException ex) {
                    Log.e(TAG, "Failed to parse saved user info JSON", ex);
                }
            }
        }
        //in questo modo richiede la CIE ogni volta, altrimenti (senza) usa il token finchè valido
        mAuthState=null;
        if (mAuthState == null) {
            AuthorizationResponse response = AuthorizationResponse.fromIntent(getIntent());
            AuthorizationException ex = AuthorizationException.fromIntent(getIntent());
            mAuthState = new AuthState(response, ex);
            if (response != null) {
                Log.d(TAG, "Received AuthorizationResponse.");
                showSnackbar(R.string.exchange_notification);
                exchangeAuthorizationCode(response);
            } else {
                Log.i(TAG, "Authorization failed: " + ex);
                showSnackbar(R.string.authorization_failed);
            }
        }
        refreshUi();
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        if (mAuthState != null) {
            state.putString(KEY_AUTH_STATE, mAuthState.jsonSerializeString());
        }
        if (mUserInfoJson != null) {
            state.putString(KEY_USER_INFO, mUserInfoJson.toString());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAuthService.dispose();
    }

    private void receivedTokenResponse(
            @Nullable TokenResponse tokenResponse,
            @Nullable AuthorizationException authException) {
        Log.d(TAG, "Token request complete");
        mAuthState.update(tokenResponse, authException);
        //System.out.println("ARRIVA QUALCOSA??:"+mAuthState.getIdToken());
        showSnackbar((tokenResponse != null)
                ? R.string.exchange_complete
                : R.string.refresh_failed);
        refreshUi();
    }

    @MainThread
    private void displayLoading(String message) {
        ((TextView) findViewById(R.id.loading_description_cie)).setText(message);
    }

    private void refreshUi() {
        displayLoading("sending jobs to the printer...");
    }

    private void refreshAccessToken() {
        HashMap<String, String> additionalParams = new HashMap<>();
        /*if (getClientSecretFromIntent(getIntent()) != null) {
            additionalParams.put("client_secret", getClientSecretFromIntent(getIntent()));
        }*/
        additionalParams.put("client_secret", mConfigurationApp.getClientSecret());
        performTokenRequest(mAuthState.createTokenRefreshRequest(additionalParams));
    }

    private void exchangeAuthorizationCode(AuthorizationResponse authorizationResponse) {
        //System.out.println(authorizationResponse.authorizationCode+",state: "+authorizationResponse.state);
        mExecutor.submit(() -> {
            try {
                Response responseTokenExchange = requestExchangeCode(authorizationResponse.authorizationCode,authorizationResponse.state);
                System.err.println("EndRequestLetturaCie:"+ new Timestamp(System.currentTimeMillis()));
                int code = responseTokenExchange.code();
                if (responseTokenExchange.isSuccessful()==true) {
                    // continue
                    String response = responseTokenExchange.body().string();
                    //Log.e("ExchangeCodeResponse", response);
                    JSONObject jsonObjectResponse = null;
                    try {
                        String response_message = "";
                        jsonObjectResponse = new JSONObject(response);
                        //System.out.println("jsonObjectResponse: "+jsonObjectResponse);
                        //passo tutti i valori utili alla funzione di stampa
                        String PrinterID = Stampa.PrinterID;
                        String accessTokenResponse = Stampa.accessTokenResponse;
                        ArrayList jobResult = Stampa.jobResult;
                        String accessTokenSC = jsonObjectResponse.getString("access_token");
                        System.out.println("accessTokenSC: "+accessTokenSC);
                        /*System.out.println("arraylist: "+jobResult);
                        System.out.println("checkvalue: \n"+"PrinterID: "+PrinterID +"\n accessTokenResponse: " +accessTokenResponse+"\n jobResult: "+jobResult);
                        System.out.println("jsonObjectResponse_accessToken: "+accessTokenSC);*/
                        int printCode=400;
                        Response responsePrint=null;
                        Iterator iterator;
                        try {
                            //  showSnackbar("Trying to print your job");
                            iterator = jobResult.iterator();
                            while (iterator.hasNext()) {
                                String jobID = iterator.next().toString();
                                System.out.println("accessTokenSC passed: "+accessTokenSC);
                                responsePrint = requestPrintCIE(accessTokenSC,accessTokenResponse,PrinterID,jobID);
                                System.err.println("EndRequestCIE:"+ new Timestamp(System.currentTimeMillis()));
                                printCode = responsePrint.code();
                                //print(accessTokenSC, accessTokenResponse, jobID, PrinterID);
                            }
                            //System.out.println("printCode: "+printCode);
                            if(jobResult.size()>0){
                                if(printCode==200) {
                                    response_message = "I lavori sono stati mandati correttamente nella stampante selezionata.";
                                }
                                else if(printCode!=200)
                                {
                                    response_message = responsePrint.body().string();
                                }
                            }
                            else{
                                response_message = "Non hai selezionato nessun lavoro";
                            }
                        } catch (Exception e) {
                            //Log.e(TAG, "Errore nella stampa dei Jobs", e);
                            response_message = "Stampa fallita, riprovare." + e.getMessage();
                        }
                        Intent intent = new Intent(this, TokenActivity.class);
                        intent.putExtra("response", response_message);
                        startActivity(intent);
                        finish();
                    }catch (JSONException err){
                        Log.d("Error", err.toString());
                    }
                } else {
                    String errorMessage = responseTokenExchange.body().string();
                }
            } catch (IOException ioEx) {
                Log.e(TAG, "Network error when querying Printer List endpoint", ioEx);
            }
        });
    }

    private Response requestPrintCIE(String accessTokenSC, String accessToken, String printerID, String jobID ) throws IOException {
        OkHttpClient client = new OkHttpClient();
        System.out.println("accessTokenSC: "+accessTokenSC);
        /*RequestBody formBody = new FormBody.Builder()
                .add("", "")
                .build();*/
        MediaType mediaType = MediaType.parse("multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW");
        RequestBody body = RequestBody.create(mediaType, "------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"access_token\"\r\n\r\n"+accessToken+"\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"JobID\"\r\n\r\n"+jobID+"\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"printerDestinationId\"\r\n\r\n"+printerID+"\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW\r\nContent-Disposition: form-data; name=\"token\"\r\n\r\n"+accessTokenSC+"\r\n------WebKitFormBoundary7MA4YWxkTrZu0gW--");
        System.err.println("StartRequestCIE:"+ new Timestamp(System.currentTimeMillis()));
        Request request = new Request.Builder()
                .url("https://"+Commons.ipaddress+"/print_token_SC")
                .post(body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Connection", "keep-alive")
                .build();
        return client.newCall(request).execute();
    }

    private Response requestExchangeCode(String code,String state) throws IOException {
        OkHttpClient client = new OkHttpClient();
        System.out.println("code: "+code);
        System.out.println("state: "+state);
        RequestBody formBody = new FormBody.Builder()
                .add("", "")
                .build();
        Request request = new Request.Builder()
                .url("https://am-test.smartcommunitylab.it/aac/oauth/token?client_id=e9610874-1548-4311-a663-472ba9c1ce33&client_secret=383ef7c1-7718-4265-8108-f098b769c5e9&grant_type=authorization_code&code="+code+"&state="+state+"&redirect_uri=com.googleusercontent.apps.641468808636-roej63drmm2vaude7n444oj21afbphel:/oauth2redirect")
                .post(formBody)
                .addHeader("cache-control", "no-cache")
                //.addHeader("Postman-Token", "1b20ce77-6b5f-42a5-a1b4-877bd279de02")
                .build();
        return client.newCall(request).execute();
    }

        private void performTokenRequest(TokenRequest request) {
        mAuthService.performTokenRequest(
                request,
                new AuthorizationService.TokenResponseCallback() {
                    @Override
                    public void onTokenRequestCompleted(
                            @Nullable TokenResponse tokenResponse,
                            @Nullable AuthorizationException ex) {
                        receivedTokenResponse(tokenResponse, ex);
                    }
                });
    }

    private void updateUserInfo(final JSONObject jsonObject) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mUserInfoJson = jsonObject;
                refreshUi();
            }
        });
    }

    @MainThread
    private void showSnackbar(@StringRes int messageId) {
        Snackbar.make(findViewById(R.id.coordinator),
                getResources().getString(messageId),
                Snackbar.LENGTH_SHORT)
                .show();
    }
      static PendingIntent createPostAuthorizationIntent(
             @NonNull Context context,
             @NonNull AuthorizationRequest request,
             @Nullable AuthorizationServiceDiscovery discoveryDoc,
             @Nullable String clientSecret) {


        Intent intent = new Intent(context, CieTokenActivity.class); //TokenActivity.class -->torna alla token activty ma solo dopo aver passato le stampanti e i tutti i prametri necessari per chiamare (qui) la funzione stampa
        if (discoveryDoc != null) {
            intent.putExtra(EXTRA_AUTH_SERVICE_DISCOVERY, discoveryDoc.docJson.toString());
        }

        if (clientSecret != null) {
            intent.putExtra(EXTRA_CLIENT_SECRET, clientSecret);
        }

        return PendingIntent.getActivity(context, request.hashCode(), intent, 0);
    }
}