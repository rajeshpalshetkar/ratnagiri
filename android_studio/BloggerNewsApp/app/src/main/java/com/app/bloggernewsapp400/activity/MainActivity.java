package com.app.bloggernewsapp400.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.viewpager.widget.ViewPager;

import com.app.bloggernewsapp400.BuildConfig;
import com.app.bloggernewsapp400.Config;
import com.app.bloggernewsapp400.R;
import com.app.bloggernewsapp400.database.prefs.AdsPref;
import com.app.bloggernewsapp400.database.prefs.SharedPref;
import com.app.bloggernewsapp400.util.AdsManager;
import com.app.bloggernewsapp400.util.AppBarLayoutBehavior;
import com.app.bloggernewsapp400.util.Constant;
import com.app.bloggernewsapp400.util.RtlViewPager;
import com.app.bloggernewsapp400.util.Tools;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.solodroid.push.sdk.provider.OneSignalPush;
import com.solodroidx.ads.appopen.AppOpenAd;

public class MainActivity extends AppCompatActivity implements DefaultLifecycleObserver {

    private static final String TAG = "MainActivity";
    private ViewPager viewPager;
    private RtlViewPager viewPagerRTL;
    private AppUpdateManager appUpdateManager;
    private ActivityResultLauncher<IntentSenderRequest> appUpdateResultLauncher;
    private long exitTime = 0;
    BottomNavigationView navigation;
    Toolbar toolbar;
    TextView titleToolbar;
    CardView lytSearchBar;
    LinearLayout searchBar;
    ImageButton btnSearch;
    ImageView btnMoreOptions;
    SharedPref sharedPref;
    AdsPref adsPref;
    CoordinatorLayout parentView;
    AdsManager adsManager;
    Tools tools;
    OneSignalPush.Builder onesignal;
    LinearLayout customDialogLayout;
    View dimBackground;
    FloatingActionButton btnRate;
    FloatingActionButton btnShare;
    Button btnExit;
    Button btnCancel;
    LinearLayout nativeAdView;
    private int screenWidth;
    OnBackPressedDispatcher onBackPressedDispatcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        tools = new Tools(this);
        if (Config.ENABLE_NEW_APP_DESIGN) {
            setContentView(R.layout.activity_main_new);
        } else {
            setContentView(R.layout.activity_main);
        }
        if (Config.FORCE_TO_SHOW_APP_OPEN_AD_ON_START) {
            ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
            adsPref.setIsOpenAd(true);
        }
        Tools.setNavigation(this);

        onesignal = new OneSignalPush.Builder(this);
        onesignal.requestNotificationPermission();

        adsManager = new AdsManager(this);
        Tools.postDelayed(() -> {
            adsManager.initializeAd();
            adsManager.updateConsentStatus();
            adsManager.loadAppOpenAd(adsPref.getIsAppOpenAdOnResume(), false, () -> {
            });
            adsManager.loadBannerAd(adsPref.getIsBannerHome());
            adsManager.loadInterstitialAd(adsPref.getIsInterstitialPostList(), adsPref.getInterstitialAdInterval());
        }, 100);

        sharedPref.resetPostToken();
        sharedPref.resetPageToken();
        sharedPref.setIsFirstTimeLaunch(false);

        initView();

        Tools.notificationOpenHandler(this, getIntent());

        if (!BuildConfig.DEBUG) {
            inAppUpdate();
            inAppReview();
        }

        handleOnBackPressed();

        initExitDialog();
        getScreenDimensions();
        setCustomDialogWidth();

    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
        Tools.postDelayed(() -> {
            if (adsPref.getIsAppOpenAdOnResume()) {
                if (AppOpenAd.isAppOpenAdLoaded) {
                    adsManager.showAppOpenAd(() -> {
                    });
                    Log.d(AppOpenAd.TAG, "lifecycleObserver Show App Open Ad");
                }
            }
        }, 100);
    }

    public void showInterstitialAd() {
        adsManager.showInterstitialAd();
    }

    public void showSnackBar(String msg) {
        Snackbar.make(parentView, msg, Snackbar.LENGTH_SHORT).show();
    }

    public void initView() {
        parentView = findViewById(R.id.parent_view);

        AppBarLayout appBarLayout = findViewById(R.id.appBarLayout);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());
        if (Config.ENABLE_NEW_APP_DESIGN) {
            if (sharedPref.getIsDarkTheme()) {
                appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.color_dark_background));
            } else {
                appBarLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.color_light_background));
            }
        }

        toolbar = findViewById(R.id.toolbar);
        titleToolbar = findViewById(R.id.title_toolbar);
        lytSearchBar = findViewById(R.id.lyt_search_bar);
        searchBar = findViewById(R.id.search_bar);
        btnSearch = findViewById(R.id.btn_search);
        btnMoreOptions = findViewById(R.id.btn_more_options);

        if (Config.ENABLE_NEW_APP_DESIGN) {
            setupNewToolbar();
        } else {
            setupToolbar();
        }

        navigation = findViewById(R.id.navigation);
        navigation.getMenu().clear();
        if (sharedPref.getIsShowPageMenu()) {
            navigation.inflateMenu(R.menu.navigation_default);
        } else {
            navigation.inflateMenu(R.menu.navigation_no_page);
        }
        navigation.setLabelVisibilityMode(BottomNavigationView.LABEL_VISIBILITY_LABELED);
        if (sharedPref.getIsDarkTheme()) {
            navigation.setBackgroundColor(ContextCompat.getColor(this, R.color.color_dark_bottom_navigation));
        } else {
            navigation.setBackgroundColor(ContextCompat.getColor(this, R.color.color_light_bottom_navigation));
        }

        viewPager = findViewById(R.id.viewpager);
        viewPagerRTL = findViewById(R.id.viewpager_rtl);
        if (sharedPref.getIsEnableRtlMode()) {
            tools.setupViewPagerRTL(this, viewPagerRTL, navigation, toolbar, titleToolbar, sharedPref);
        } else {
            tools.setupViewPager(this, viewPager, navigation, toolbar, titleToolbar, sharedPref);
        }

        if (!Tools.isConnect(this)) {
            if (sharedPref.getIsShowPageMenu()) {
                if (sharedPref.getIsEnableRtlMode()) {
                    viewPagerRTL.setCurrentItem(3);
                } else {
                    viewPager.setCurrentItem(3);
                }
            } else {
                if (sharedPref.getIsEnableRtlMode()) {
                    viewPagerRTL.setCurrentItem(2);
                } else {
                    viewPager.setCurrentItem(2);
                }
            }
        }

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_search) {
            Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
            startActivity(intent);
            adsManager.destroyBannerAd();
            return true;
        } else if (menuItem.getItemId() == R.id.action_more) {
            Intent intent;
            if (Config.ENABLE_NEW_APP_DESIGN) {
                intent = new Intent(getApplicationContext(), ActivitySettingsNew.class);
            } else {
                intent = new Intent(getApplicationContext(), ActivitySettings.class);
            }
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    public void handleOnBackPressed() {
        onBackPressedDispatcher = getOnBackPressedDispatcher();
        onBackPressedDispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (sharedPref.getIsEnableRtlMode()) {
                    if (viewPagerRTL.getCurrentItem() != 0) {
                        viewPagerRTL.setCurrentItem((0), true);
                    } else {
                        exitApp();
                    }
                } else {
                    if (viewPager.getCurrentItem() != 0) {
                        viewPager.setCurrentItem((0), true);
                    } else {
                        exitApp();
                    }
                }
            }
        });
    }

    public void exitApp() {
        if (sharedPref.getIsEnableExitDialog()) {
            if (customDialogLayout != null) {
                if (customDialogLayout.getVisibility() == View.VISIBLE) {
                    hideCustomDialog();
                } else {
                    showCustomDialog();
                }
            } else {
                showCustomDialog();
            }
        } else {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                showSnackBar(getString(R.string.press_again_to_exit));
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                destroyBannerAd();
                destroyAppOpenAd();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        adsManager.resumeBannerAd(adsPref.getIsBannerHome());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyBannerAd();
        destroyAppOpenAd();
    }

    public void destroyBannerAd() {
        adsManager.destroyBannerAd();
    }

    public void destroyAppOpenAd() {
        AppOpenAd.isAppOpenAdLoaded = false;
        if (Config.FORCE_TO_SHOW_APP_OPEN_AD_ON_START) {
            adsManager.destroyAppOpenAd();
            ProcessLifecycleOwner.get().getLifecycle().removeObserver(this);
        }
    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    private void inAppReview() {
        if (sharedPref.getInAppReviewToken() <= 3) {
            sharedPref.updateInAppReviewToken(sharedPref.getInAppReviewToken() + 1);
        } else {
            ReviewManager manager = ReviewManagerFactory.create(this);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ReviewInfo reviewInfo = task.getResult();
                    manager.launchReviewFlow(MainActivity.this, reviewInfo).addOnFailureListener(e -> {
                    }).addOnCompleteListener(complete -> {
                                Log.d(TAG, "In-App Review Success");
                            }
                    ).addOnFailureListener(failure -> {
                        Log.d(TAG, "In-App Review Rating Failed");
                    });
                }
            }).addOnFailureListener(failure -> Log.d("In-App Review", "In-App Request Failed " + failure));
        }
        Log.d(TAG, "in app review token : " + sharedPref.getInAppReviewToken());
    }

    private void inAppUpdate() {
        appUpdateManager = AppUpdateManagerFactory.create(getApplicationContext());
        appUpdateResultLauncher = registerForActivityResult(new ActivityResultContracts.StartIntentSenderForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                showSnackBar("Update success!");
            } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                showSnackBar("Update canceled");
                Log.d(TAG, "Update flow cancelled by user.");
            } else {
                showSnackBar("Update Failed!");
                Log.e(TAG, "Update flow failed with result code: " + result.getResultCode());
            }
        });

        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                startUpdateFlow(appUpdateInfo);
            } else if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                startUpdateFlow(appUpdateInfo);
            }
        });
    }

    private void startUpdateFlow(AppUpdateInfo appUpdateInfo) {
        AppUpdateOptions appUpdateOptions = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).setAllowAssetPackDeletion(true).build();
        appUpdateManager.startUpdateFlowForResult(appUpdateInfo, appUpdateResultLauncher, appUpdateOptions);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.IMMEDIATE_APP_UPDATE_REQ_CODE) {
            if (resultCode == RESULT_OK) {
                showSnackBar("Update success!");
            } else if (resultCode == RESULT_CANCELED) {
                showSnackBar("Update canceled");
            } else {
                showSnackBar("Update Failed!");
                inAppUpdate();
            }
        }
    }

    private void setupToolbar() {
        Tools.setupToolbar(this, toolbar, getString(R.string.app_name), false);
    }

    private void setupNewToolbar() {
        if (sharedPref.getIsDarkTheme()) {
            lytSearchBar.setCardBackgroundColor(ContextCompat.getColor(this, R.color.color_dark_search_bar));
            btnSearch.setColorFilter(ContextCompat.getColor(this, R.color.color_dark_icon), PorterDuff.Mode.SRC_IN);
            titleToolbar.setTextColor(ContextCompat.getColor(this, R.color.color_dark_icon));
        } else {
            lytSearchBar.setCardBackgroundColor(ContextCompat.getColor(this, R.color.color_light_search_bar));
            btnSearch.setColorFilter(ContextCompat.getColor(this, R.color.color_light_icon), PorterDuff.Mode.SRC_IN);
            titleToolbar.setTextColor(ContextCompat.getColor(this, R.color.color_light_text));
        }

        searchBar.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), ActivitySearch.class));
            destroyBannerAd();
        });

        btnSearch.setOnClickListener(view -> {
            startActivity(new Intent(getApplicationContext(), ActivitySearch.class));
            destroyBannerAd();
        });

        titleToolbar.setText(getString(R.string.app_name));

        if (Config.SET_LAUNCHER_IMAGE_AS_HOME_TOP_RIGHT_ICON) {
            btnMoreOptions.setImageResource(R.mipmap.ic_launcher);
            int padding = getResources().getDimensionPixelSize(R.dimen.padding_small);
            btnMoreOptions.setPadding(padding, padding, padding, padding);
        } else {
            btnMoreOptions.setImageResource(R.drawable.ic_settings);
            int padding = getResources().getDimensionPixelSize(R.dimen.padding_medium);
            btnMoreOptions.setPadding(padding, padding, padding, padding);
            if (sharedPref.getIsDarkTheme()) {
                btnMoreOptions.setColorFilter(ContextCompat.getColor(this, R.color.color_dark_icon), PorterDuff.Mode.SRC_IN);
            } else {
                btnMoreOptions.setColorFilter(ContextCompat.getColor(this, R.color.color_light_icon), PorterDuff.Mode.SRC_IN);
            }
        }
        btnMoreOptions.setOnClickListener(view -> {
            Intent intent;
            if (Config.ENABLE_NEW_APP_DESIGN) {
                intent = new Intent(getApplicationContext(), ActivitySettingsNew.class);
            } else {
                intent = new Intent(getApplicationContext(), ActivitySettings.class);
            }
            startActivity(intent);
        });
    }

    private void initExitDialog() {
        customDialogLayout = findViewById(R.id.custom_dialog_layout);
        if (sharedPref.getIsDarkTheme()) {
            customDialogLayout.setBackgroundResource(R.drawable.bg_dialog_dark);
        } else {
            customDialogLayout.setBackgroundResource(R.drawable.bg_dialog_light);
        }
        customDialogLayout.setOnClickListener(v -> {
            //do nothing
        });

        dimBackground = findViewById(R.id.dim_background);
        btnRate = findViewById(R.id.btn_rate);
        btnShare = findViewById(R.id.btn_share);
        btnExit = findViewById(R.id.btn_exit);
        btnCancel = findViewById(R.id.btn_cancel);
        nativeAdView = findViewById(R.id.native_ad_view);

        Tools.setNativeAdStyle(MainActivity.this, nativeAdView, adsPref.getNativeAdStyleExitDialog());
        adsManager.loadNativeAd(adsPref.getIsNativeExitDialog(), adsPref.getNativeAdStyleExitDialog());

        btnRate.setOnClickListener(v -> {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID)));
            hideCustomDialog();
        });

        btnShare.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.app_share) + "\n" + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
            intent.setType("text/plain");
            startActivity(intent);
            hideCustomDialog();
        });

        btnExit.setOnClickListener(v -> {
            finish();
            destroyBannerAd();
            destroyAppOpenAd();
            hideCustomDialog();
        });

        dimBackground.setOnClickListener(v -> hideCustomDialog());
        btnCancel.setOnClickListener(v -> hideCustomDialog());
    }

    private void getScreenDimensions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = getWindowManager().getCurrentWindowMetrics();
            screenWidth = windowMetrics.getBounds().width();
        } else {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            screenWidth = displayMetrics.widthPixels;
        }
    }

    private void setCustomDialogWidth() {
        int desiredWidth = (int) (screenWidth * 0.85);
        ViewGroup.LayoutParams params = customDialogLayout.getLayoutParams();
        params.width = desiredWidth;
        customDialogLayout.setLayoutParams(params);
    }

    private void showCustomDialog() {
        customDialogLayout.setAlpha(0f);
        dimBackground.setAlpha(0f);
        customDialogLayout.setVisibility(View.VISIBLE);
        dimBackground.setVisibility(View.VISIBLE);
        customDialogLayout.animate().alpha(1f).setDuration(300).setListener(null);
        dimBackground.animate().alpha(1f).setDuration(300).setListener(null);
        isWindowLightStatusBarNavigation(false);
        showBannerAd(false);
    }

    private void hideCustomDialog() {
        customDialogLayout.animate().alpha(0f).setDuration(300).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                customDialogLayout.setVisibility(View.GONE);
            }
        });
        dimBackground.animate().alpha(0f).setDuration(300).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                dimBackground.setVisibility(View.GONE);
            }
        });
        isWindowLightStatusBarNavigation(Config.ENABLE_NEW_APP_DESIGN);
        showBannerAd(true);
    }

    private void isWindowLightStatusBarNavigation(boolean show) {
        if (sharedPref.getIsDarkTheme()) {
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
            WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(false);
        } else {
            if (show) {
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
            } else {
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(false);
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(false);
            }
        }
    }

    private void showBannerAd(boolean show) {
        if (show) {
            findViewById(R.id.bannerAdView).setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.bannerAdView).setVisibility(View.GONE);
        }
    }

}
