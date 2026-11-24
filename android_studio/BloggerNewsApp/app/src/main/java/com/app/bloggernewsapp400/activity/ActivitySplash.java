package com.app.bloggernewsapp400.activity;

import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.app.bloggernewsapp400.Config;
import com.app.bloggernewsapp400.R;
import com.app.bloggernewsapp400.callback.CallbackConfig;
import com.app.bloggernewsapp400.callback.CallbackLabel;
import com.app.bloggernewsapp400.database.prefs.AdsPref;
import com.app.bloggernewsapp400.database.prefs.SharedPref;
import com.app.bloggernewsapp400.database.sqlite.DbLabel;
import com.app.bloggernewsapp400.model.Ads;
import com.app.bloggernewsapp400.model.App;
import com.app.bloggernewsapp400.model.Blog;
import com.app.bloggernewsapp400.model.CustomCategory;
import com.app.bloggernewsapp400.rest.RestAdapter;
import com.app.bloggernewsapp400.util.AdsManager;
import com.app.bloggernewsapp400.util.Tools;
import com.solodroidx.ads.appopen.AppOpenAd;


import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.gson.Gson;

public class ActivitySplash extends AppCompatActivity {

    public static final String TAG = "SplashActivity";
    Call<CallbackConfig> callbackConfigCall = null;
    Call<CallbackLabel> callbackLabelCall = null;
    ImageView imgSplash;
    AdsManager adsManager;
    SharedPref sharedPref;
    AdsPref adsPref;
    App app;
    CustomCategory customCategory;
    Blog blog;
    Ads ads;
    DbLabel dbLabel;
    boolean isForceOpenAds;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        Tools.transparentStatusBarNavigation(this);
        isForceOpenAds = Config.FORCE_TO_SHOW_APP_OPEN_AD_ON_START;
        dbLabel = new DbLabel(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        adsManager = new AdsManager(this);
        adsManager.initializeAd();

        imgSplash = findViewById(R.id.img_splash);
        if (sharedPref.getIsDarkTheme()) {
            imgSplash.setImageResource(R.drawable.bg_splash_dark);
        } else {
            imgSplash.setImageResource(R.drawable.bg_splash_default);
        }

        Tools.postDelayed(this::requestConfig, Config.DELAY_SPLASH);
    }

    private void requestConfig() {
        String source = Config.REMOTE_CONFIG_SOURCE == null ? "" : Config.REMOTE_CONFIG_SOURCE.trim();

        // A remote config source is optional. If it is unavailable, use the last
        // successfully downloaded configuration rather than blocking app startup.
        if (source.isEmpty()) {
            loadCachedConfigOrContinue();
            return;
        }

        requestAPI(source);
        Log.d(TAG, "Started optional remote config request");
    }

    private void requestAPI(String remoteUrl) {
        try {
            if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
                if (remoteUrl.contains("drive.google.com")) {
                    String googleDriveFileId = extractGoogleDriveFileId(remoteUrl);
                    if (googleDriveFileId.isEmpty()) {
                        loadCachedConfigOrContinue();
                        return;
                    }
                    callbackConfigCall = RestAdapter.createApiGoogleDrive().getDriveJsonFileId(googleDriveFileId);
                    Log.d(TAG, "Requesting configuration from Google Drive");
                } else {
                    callbackConfigCall = RestAdapter.createApiJsonUrl().getJsonUrl(remoteUrl);
                    Log.d(TAG, "Requesting configuration from JSON URL");
                }
            } else {
                callbackConfigCall = RestAdapter.createApiGoogleDrive().getDriveJsonFileId(remoteUrl);
                Log.d(TAG, "Requesting configuration using Google Drive file ID");
            }

            callbackConfigCall.enqueue(new Callback<CallbackConfig>() {
                @Override
                public void onResponse(@NonNull Call<CallbackConfig> call, @NonNull Response<CallbackConfig> response) {
                    CallbackConfig resp = response.isSuccessful() ? response.body() : null;
                    if (isValidConfig(resp)) {
                        sharedPref.saveLastKnownConfig(new Gson().toJson(resp));
                        displayApiResults(resp);
                    } else {
                        Log.w(TAG, "Remote config was empty or invalid; trying cached config");
                        loadCachedConfigOrContinue();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<CallbackConfig> call, @NonNull Throwable th) {
                    Log.w(TAG, "Remote config unavailable; trying cached config: " + th.getMessage());
                    loadCachedConfigOrContinue();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Unable to request remote config", e);
            loadCachedConfigOrContinue();
        }
    }

    private String extractGoogleDriveFileId(String url) {
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern
                    .compile("(?:/d/|id=)([a-zA-Z0-9_-]+)")
                    .matcher(url);
            if (matcher.find()) return matcher.group(1);
        } catch (Exception ignored) {
        }
        return "";
    }

    private boolean isValidConfig(CallbackConfig config) {
        return config != null
                && config.blog != null
                && config.blog.blogger_id != null
                && !config.blog.blogger_id.trim().isEmpty()
                && config.blog.api_key != null
                && !config.blog.api_key.trim().isEmpty()
                && config.app != null
                && config.ads != null;
    }

    private void loadCachedConfigOrContinue() {
        String cachedJson = sharedPref.getLastKnownConfig();
        if (cachedJson != null && !cachedJson.trim().isEmpty()) {
            try {
                CallbackConfig cached = new Gson().fromJson(cachedJson, CallbackConfig.class);
                if (isValidConfig(cached)) {
                    Log.d(TAG, "Using last known valid configuration");
                    displayApiResults(cached, true);
                    return;
                }
            } catch (Exception e) {
                Log.e(TAG, "Cached configuration is invalid", e);
            }
        }

        // First installation with no configured URL and no saved config:
        // do not crash or block the user behind a license/config dialog.
        // Main screen can open; Blogger feed requires a valid config source.
        Log.w(TAG, "No cached configuration is available. Starting app without remote config.");
        showAppOpenAdIfAvailable();
    }

    private void displayApiResults(CallbackConfig resp) {
        displayApiResults(resp, false);
    }

    private void displayApiResults(CallbackConfig resp, boolean usingCachedConfig) {
        if (resp != null) {
            app = resp.app;
            ads = resp.ads;
            blog = resp.blog;
            customCategory = resp.custom_category;

            sharedPref.saveBlogCredentials(blog.blogger_id, blog.api_key);
            adsManager.saveConfig(sharedPref, app);
            adsManager.saveAds(adsPref, ads);
            adsManager.saveAdsPlacement(adsPref, ads.placement);

            if (!app.status) {
                startActivity(new Intent(getApplicationContext(), ActivityRedirect.class));
                finish();
                Log.d(TAG, "App status is suspended");
            } else {
                if (customCategory != null && customCategory.status && customCategory.categories != null) {
                    dbLabel.truncateTableCategory(DbLabel.TABLE_LABEL);
                    dbLabel.addListCategory(customCategory.categories, DbLabel.TABLE_LABEL);
                    showAppOpenAdIfAvailable();
                } else if (usingCachedConfig) {
                    // Keep previously saved labels when offline; do not hold startup
                    // waiting for a network request that cannot succeed.
                    showAppOpenAdIfAvailable();
                } else {
                    requestLabel();
                }
                Log.d(TAG, "App status is live");
            }
            Log.d(TAG, "initialize success");
        } else {
            Log.d(TAG, "initialize failed");
            showAppOpenAdIfAvailable();
        }
    }

    private void requestLabel() {
        this.callbackLabelCall = RestAdapter.createApiCategory(sharedPref.getBloggerId()).getLabel();
        this.callbackLabelCall.enqueue(new Callback<>() {
            public void onResponse(@NonNull Call<CallbackLabel> call, @NonNull Response<CallbackLabel> response) {
                CallbackLabel resp = response.body();
                if (resp == null) {
                    showAppOpenAdIfAvailable();
                    return;
                }
                dbLabel.truncateTableCategory(DbLabel.TABLE_LABEL);
                dbLabel.addListCategory(resp.feed.category, DbLabel.TABLE_LABEL);
                showAppOpenAdIfAvailable();
                Log.d(TAG, "Success initialize label with count " + resp.feed.category.size() + " items");
            }

            public void onFailure(@NonNull Call<CallbackLabel> call, @NonNull Throwable th) {
                if (!call.isCanceled()) {
                    showAppOpenAdIfAvailable();
                }
            }
        });
    }

    private void showAppOpenAdIfAvailable() {
        if (isForceOpenAds) {
            if (adsPref.getIsAppOpenAdOnStart()) {
                adsManager.loadAppOpenAd(adsPref.getIsAppOpenAdOnStart(), true, () -> {
                    startMainActivity();
                    AppOpenAd.isAppOpenAdLoaded = false;
                    Log.d(TAG, "showAppOpenAdIfAvailable");
                });
            } else {
                startMainActivity();
                Log.d(TAG, "app open on start disabled");
            }
        } else {
            if (adsPref.getAdStatus() && adsPref.getIsAppOpenAdOnStart()) {
                Application application = getApplication();
                ((MyApplication) application).showAdIfAvailable(this, this::startMainActivity);
            } else {
                startMainActivity();
                Log.d(TAG, "startMainActivity");
            }
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

}
