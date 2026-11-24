package com.app.bloggernewsapp400.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.bloggernewsapp400.R;
import com.app.bloggernewsapp400.activity.ActivityPageDetail;
import com.app.bloggernewsapp400.database.prefs.SharedPref;
import com.app.bloggernewsapp400.model.Post;
import com.app.bloggernewsapp400.callback.CallbackPage;
import com.app.bloggernewsapp400.rest.RestAdapter;
import com.app.bloggernewsapp400.util.Tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Native Ratnagiri district information dashboard. Blogger is used only as the content API. */
public class FragmentCategory extends Fragment {
    private Activity activity;
    private SharedPref sharedPref;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipe;
    private final Map<String, Post> pageBySlug = new LinkedHashMap<>();
    private final List<PageTile> tiles = new ArrayList<>();
    private boolean pagesLoaded = false;
    private boolean loadingPages = false;
    private PageTile pendingTile = null;

    private static class PageTile {
        final String title, description, slug, icon;
        PageTile(String title, String description, String slug, String icon) {
            this.title = title; this.description = description; this.slug = slug; this.icon = icon;
        }
    }

    @Override public void onAttach(@NonNull Context context) { super.onAttach(context); activity = (Activity) context; }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        View root = inflater.inflate(R.layout.fragment_category, container, false);
        sharedPref = new SharedPref(activity);
        recyclerView = root.findViewById(R.id.recycler_view);
        swipe = root.findViewById(R.id.swipe_refresh_layout);
        swipe.setColorSchemeResources(R.color.color_light_primary);
        recyclerView.setLayoutManager(new GridLayoutManager(activity, 2));
        buildTiles();
        recyclerView.setAdapter(new RecyclerView.Adapter<TileHolder>() {
            @NonNull @Override public TileHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
                CardView card = new CardView(activity);
                card.setRadius(dp(14)); card.setCardElevation(dp(2));
                card.setUseCompatPadding(true);
                TextView text = new TextView(activity);
                text.setPadding(dp(14), dp(16), dp(14), dp(16));
                text.setGravity(Gravity.START); text.setTextColor(Color.rgb(35, 48, 65));
                text.setTextSize(14); text.setBackgroundColor(Color.WHITE);
                card.addView(text, new CardView.LayoutParams(-1, dp(116)));
                return new TileHolder(card, text);
            }
            @Override public void onBindViewHolder(@NonNull TileHolder holder, int position) {
                PageTile tile = tiles.get(position);
                holder.text.setText(tile.icon + "  " + tile.title + "\n\n" + tile.description);
                holder.text.setTypeface(null, Typeface.NORMAL);
                holder.itemView.setOnClickListener(v -> openTile(tile));
            }
            @Override public int getItemCount() { return tiles.size(); }
        });
        swipe.setOnRefreshListener(this::loadPages);
        loadPages();
        return root;
    }

    private void buildTiles() {
        tiles.clear();
        tiles.add(new PageTile("जिल्हा परिचय", "रत्नागिरी जिल्ह्याची मूलभूत माहिती", "ratnagiri-district-information", "🏛️"));
        tiles.add(new PageTile("तालुके", "जिल्ह्यातील तालुके आणि त्यांची माहिती", "ratnagiri-district-talukas", "📍"));
        tiles.add(new PageTile("गाव निर्देशिका", "गावे आणि स्थानिक संदर्भ", "ratnagiri-village-directory", "🏘️"));
        tiles.add(new PageTile("पर्यटन स्थळे", "समुद्रकिनारे, किल्ले आणि निसर्गस्थळे", "ratnagiri-tourist-places", "🏝️"));
        tiles.add(new PageTile("शासकीय कार्यालये", "नागरिक सेवा आणि कार्यालयीन माहिती", "ratnagiri-government-offices-citizen-services", "🏢"));
        tiles.add(new PageTile("शिक्षण व आरोग्य", "शैक्षणिक आणि आरोग्य सुविधा", "ratnagiri-education-health-facilities", "🏥"));
        tiles.add(new PageTile("शेती व मत्स्यव्यवसाय", "शेती, मासेमारी आणि उद्योग", "ratnagiri-agriculture-fisheries-industries", "🌾"));
        tiles.add(new PageTile("वाहतूक", "रेल्वे, बस आणि महामार्ग", "ratnagiri-transport-railway-bus-highway", "🚌"));
        tiles.add(new PageTile("संस्कृती व खाद्यसंस्कृती", "सण, खाद्यपदार्थ आणि लोककला", "ratnagiri-culture-festivals-food-folk-art", "🎭"));
        tiles.add(new PageTile("इतिहास व किल्ले", "ऐतिहासिक वारसा आणि व्यक्तिमत्त्वे", "ratnagiri-history-forts-great-personalities", "🏰"));
        tiles.add(new PageTile("शासकीय योजना", "योजना, अनुदाने आणि लाभ", "ratnagiri-government-schemes-subsidies", "📋"));
        tiles.add(new PageTile("नोकरी व रोजगार", "नोकरी, रोजगार आणि करिअर", "ratnagiri-jobs-employment-career", "💼"));
        tiles.add(new PageTile("बँक व पोस्ट", "आर्थिक आणि टपाल सेवा", "ratnagiri-banks-post-office-financial-services", "🏦"));
        tiles.add(new PageTile("आपत्कालीन संपर्क", "पोलीस, अग्निशमन आणि रुग्णालये", "ratnagiri-emergency-contacts-police-fire-hospital", "🚨"));
        tiles.add(new PageTile("स्थानिक स्वराज्य संस्था", "ग्रामपंचायत आणि नगरपरिषद", "ratnagiri-gram-panchayat-municipal-councils", "🏛️"));
        tiles.add(new PageTile("जमीन अभिलेख", "७/१२, फेरफार आणि मालमत्ता", "ratnagiri-land-records-7-12-ferfar-property", "📑"));
        tiles.add(new PageTile("घरकुल व बांधकाम", "घरकुल योजना, परवानग्या आणि कर", "ratnagiri-housing-schemes-building-permission-property-tax", "🏠"));
        tiles.add(new PageTile("हवामान व आपत्ती", "पाऊस, हवामान आणि आपत्ती व्यवस्थापन", "ratnagiri-weather-rainfall-disaster-management", "🌦️"));
        tiles.add(new PageTile("पर्यावरण", "जैवविविधता आणि वन्यजीव", "ratnagiri-environment-biodiversity-wildlife", "🌿"));
        tiles.add(new PageTile("उद्योग व व्यवसाय", "MIDC आणि व्यवसाय संधी", "ratnagiri-industries-midc-business-opportunities", "🏭"));
        tiles.add(new PageTile("पर्यटन व्यवसाय", "होमस्टे आणि कृषी पर्यटन", "ratnagiri-tourism-business-homestay-agritourism", "🛏️"));
        tiles.add(new PageTile("महिला बचत गट", "स्वयं-सहायता गट आणि व्यवसाय", "ratnagiri-women-self-help-groups-business", "👩‍👩‍👧"));
        tiles.add(new PageTile("युवा व कौशल्य", "विद्यार्थी, करिअर आणि कौशल्य विकास", "ratnagiri-youth-student-career-skill-development", "🎓"));
        tiles.add(new PageTile("आरोग्य सेवा", "आरोग्य सुविधा आणि सरकारी योजना", "ratnagiri-health-services-government-health-schemes", "🩺"));
        tiles.add(new PageTile("पोलीस व नागरिक हक्क", "पोलीस ठाणे आणि ऑनलाइन तक्रारी", "ratnagiri-police-stations-citizen-rights-online-complaints", "👮"));
        tiles.add(new PageTile("न्यायालय व कायदेशीर मदत", "न्यायालये, लोकअदालत आणि विधी सेवा", "ratnagiri-courts-legal-aid-lok-adalat", "⚖️"));
        tiles.add(new PageTile("ग्रामविकास", "पाणी, रस्ते आणि स्वच्छता", "ratnagiri-village-development-water-roads-sanitation", "🚰"));
    }

    private void loadPages() {
        if (loadingPages || pagesLoaded) {
            swipe.setRefreshing(false);
            return;
        }
        if (!Tools.isConnect(activity)) {
            swipe.setRefreshing(false);
            showPendingLoadError();
            return;
        }
        try {
            String bloggerId = sharedPref.getBloggerId();
            String configuredKeys = sharedPref.getAPIKey();
            if (bloggerId == null || bloggerId.trim().isEmpty() || configuredKeys == null || configuredKeys.trim().isEmpty()) {
                swipe.setRefreshing(false);
                showPendingLoadError();
                return;
            }
            List<String> keys = Arrays.asList(configuredKeys.replace(", ", ",").split(","));
            String apiKey = keys.get(0).trim();
            if (apiKey.isEmpty()) {
                swipe.setRefreshing(false);
                showPendingLoadError();
                return;
            }
            loadingPages = true;
            RestAdapter.createApiPosts(bloggerId).getPages(apiKey, null).enqueue(new Callback<CallbackPage>() {
                @Override public void onResponse(@NonNull Call<CallbackPage> call, @NonNull Response<CallbackPage> response) {
                    loadingPages = false;
                    CallbackPage body = response.isSuccessful() ? response.body() : null;
                    if (body != null && body.items != null) {
                        pageBySlug.clear();
                        for (Post page : body.items) {
                            String slug = slugOf(page.url);
                            if (slug != null && page.id != null) pageBySlug.put(slug, page);
                        }
                        pagesLoaded = true;
                    }
                    swipe.setRefreshing(false);
                    if (pendingTile != null) {
                        PageTile requested = pendingTile;
                        pendingTile = null;
                        if (pagesLoaded) openTile(requested);
                        else showPendingLoadError();
                    }
                }
                @Override public void onFailure(@NonNull Call<CallbackPage> call, @NonNull Throwable t) {
                    loadingPages = false;
                    swipe.setRefreshing(false);
                    showPendingLoadError();
                }
            });
        } catch (Exception e) {
            loadingPages = false;
            swipe.setRefreshing(false);
            showPendingLoadError();
        }
    }

    private void showPendingLoadError() {
        if (pendingTile != null && isAdded()) {
            Toast.makeText(activity, "माहिती सध्या मिळत नाही. कृपया थोड्या वेळाने पुन्हा प्रयत्न करा.", Toast.LENGTH_SHORT).show();
        }
    }

    private String slugOf(String url) {
        try { String path = Uri.parse(url).getLastPathSegment(); return path == null ? null : path.replace(".html", ""); }
        catch (Exception e) { return null; }
    }

    private void openTile(PageTile tile) {
        if (!pagesLoaded) {
            pendingTile = tile;
            Toast.makeText(activity, "माहिती लोड होत आहे. कृपया थोडा वेळ थांबा.", Toast.LENGTH_SHORT).show();
            loadPages();
            return;
        }
        Post page = pageBySlug.get(tile.slug);
        if (page == null || page.id == null) {
            Toast.makeText(activity, "हे माहिती पान सध्या उपलब्ध नाही.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(activity, ActivityPageDetail.class);
        intent.putExtra("id", page.id);
        intent.putExtra("title", page.title == null ? tile.title : page.title);
        startActivity(intent);
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }
    private static class TileHolder extends RecyclerView.ViewHolder {
        final TextView text;
        TileHolder(@NonNull View itemView, TextView text) { super(itemView); this.text = text; }
    }
}
