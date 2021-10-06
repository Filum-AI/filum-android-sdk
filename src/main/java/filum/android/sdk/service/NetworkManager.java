package filum.android.sdk.service;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.BaseHttpStack;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import filum.android.sdk.util.Logger;
import androidx.annotation.VisibleForTesting;


public class NetworkManager {
    private RequestQueue requestQueue;
    private ConfigManager configManager;

    public NetworkManager(ConfigManager configManager) {
        this.configManager = configManager;

        requestQueue = Volley.newRequestQueue(configManager.getContext());
    }

    @VisibleForTesting
    NetworkManager(ConfigManager configManager, BaseHttpStack stack) {
        this.configManager = configManager;

        requestQueue = Volley.newRequestQueue(configManager.getContext(), stack);
    }

    public static NetworkManager makeInstance(ConfigManager configManager) {
        return new NetworkManager(configManager);
    }

    private void initializeSSLContext(){
        try {
            SSLContext.getInstance("TLSv1.2");
        } catch (NoSuchAlgorithmException e) {
            Logger.error(e);
        }
        try {
            Context context = configManager.getContext().getApplicationContext();
            ProviderInstaller.installIfNeeded(context.getApplicationContext());
        } catch (GooglePlayServicesRepairableException e) {
            Logger.error(e);
        } catch (GooglePlayServicesNotAvailableException e) {
            Logger.error(e);
        }
    }

    /* package */ void request(final int method, String endpoint, Map<String, String> params, @Nullable JSONArray body, Listener<JSONObject> callback, ErrorListener errorCallback) {
        initializeSSLContext();
        Uri.Builder builder = new Uri.Builder();
        String serverUrl = configManager.getServerUrl();
        if (!serverUrl.startsWith("http")) {
            builder.scheme("https");
            builder.encodedAuthority(serverUrl);
        } else {
            builder.encodedPath(serverUrl);
        }
        builder.appendEncodedPath(endpoint);
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                builder.appendQueryParameter(entry.getKey(), entry.getValue());
            }
        }
        String url = builder.build().toString();
        FilumJsonObjectRequest requestJson = new FilumJsonObjectRequest(method, url, body, callback, errorCallback) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + configManager.getToken());
                if (method != Method.GET) {
                    params.put("Content-Type", "application/json");
                }
                return params;
            }
        };
        requestQueue.add(requestJson);
        Logger.log(
                "REQUEST: sent a request"
                        + " - " + requestJson.getMethod()
                        + " " + requestJson.getUrl()
                        + (body != null ? " - " + body.toString() : "")
        );
    }

    public void track(JSONArray eventList, Listener<JSONObject> callback, ErrorListener errorCallback) {
        request(Request.Method.POST, "events", null, eventList, callback, errorCallback);
    }
}
