package com.zkelite.compucareservice;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends Activity {
    private static final String BASE = SyncWorker.BASE;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient http = new OkHttpClient();
    private OfflineStore store;
    private LinearLayout root, content;
    private TextView statusText, titleText;
    private String token, role = "customer", displayName = "COMPUCARE user", email = "";
    private boolean busy = false;
    private boolean exchanging = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new OfflineStore(this);
        token = SecureTokenStore.read(this);
        android.content.SharedPreferences cachedProfile = getSharedPreferences("profile_cache", MODE_PRIVATE);
        role = cachedProfile.getString("role", "customer");
        displayName = cachedProfile.getString("name", "COMPUCARE user");
        email = cachedProfile.getString("email", "");
        handleAuthIntent(getIntent());
        schedulePeriodicSync();
        if (token != null) showWorkspace(); else if (!exchanging) showLogin();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAuthIntent(intent);
        if (token != null) showWorkspace();
    }

    private void handleAuthIntent(Intent intent) {
        Uri data = intent == null ? null : intent.getData();
        if (data == null || !"compucare".equals(data.getScheme()) || !"auth".equals(data.getHost())) return;
        String code = data.getQueryParameter("code");
        if (code == null || code.length() < 20) return;
        exchanging = true;
        showExchange();
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject(); body.put("code", code);
                Request request = new Request.Builder().url(BASE + "/api/native-auth/exchange")
                        .post(RequestBody.create(body.toString(), JSON))
                        .header("Accept", "application/json").build();
                try (Response response = http.newCall(request).execute()) {
                    String raw = response.body() == null ? "{}" : response.body().string();
                    if (!response.isSuccessful()) throw new IOException("Sign-in code expired. Please sign in again.");
                    String accessToken = new JSONObject(raw).optString("accessToken", "");
                    if (accessToken.length() < 40) throw new IOException("The sign-in response was incomplete.");
                    SecureTokenStore.save(this, accessToken);
                    token = accessToken;
                }
                runOnUiThread(() -> { exchanging = false; showWorkspace(); });
            } catch (Exception e) {
                runOnUiThread(() -> { exchanging = false; showLogin(); toast(e.getMessage() == null ? "Sign-in could not be completed." : e.getMessage()); });
            }
        }).start();
    }

    private void showExchange() {
        root = baseLayout();
        LinearLayout panel = column();
        panel.setGravity(Gravity.CENTER);
        panel.addView(text("Completing secure sign-in…", 19, Color.rgb(15, 35, 60), true));
        root.addView(panel, new LinearLayout.LayoutParams(-1, -1));
        setContentView(root);
    }

    private void showLogin() {
        root = baseLayout();
        LinearLayout panel = column();
        panel.setPadding(dp(26), dp(38), dp(26), dp(30));
        panel.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark = text("CC", 30, Color.WHITE, true);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(round(Color.rgb(18, 52, 91), 18));
        LinearLayout.LayoutParams markParams = new LinearLayout.LayoutParams(dp(76), dp(76));
        panel.addView(mark, markParams);
        panel.addView(space(22));
        panel.addView(text("COMPUCARE SERVICE", 27, Color.rgb(15, 35, 60), true));
        panel.addView(text("Native service app · Online and offline", 15, Color.GRAY, false));
        panel.addView(space(30));
        TextView info = text("Sign in securely using your existing COMPUCARE account. Your account and staff permissions stay connected to the service platform.", 16, Color.rgb(70, 84, 102), false);
        panel.addView(info);
        panel.addView(space(22));
        Button signIn = button("Sign in securely");
        signIn.setOnClickListener(v -> openLogin());
        panel.addView(signIn);
        panel.addView(space(12));
        panel.addView(text("First sign-in needs internet. After sign-in, your last synced tickets remain available offline.", 13, Color.GRAY, false));
        root.addView(panel, new LinearLayout.LayoutParams(-1, -1));
        setContentView(root);
    }

    private void openLogin() {
        Uri uri = Uri.parse(BASE + "/?native=1");
        try { new CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, uri); }
        catch (Exception e) { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
    }

    private void showWorkspace() {
        root = baseLayout();
        LinearLayout header = column();
        header.setPadding(dp(20), dp(24), dp(20), dp(18));
        header.setBackground(round(Color.rgb(12, 34, 61), 0));
        TextView brand = text("COMPUCARE SERVICE", 19, Color.WHITE, true);
        header.addView(brand);
        titleText = text("Your service workspace", 23, Color.WHITE, true);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
        tp.topMargin = dp(12); header.addView(titleText, tp);
        statusText = text("Checking connection…", 13, Color.rgb(196, 214, 235), false);
        header.addView(statusText);
        root.addView(header);
        ScrollView scroll = new ScrollView(this);
        content = column();
        content.setPadding(dp(16), dp(16), dp(16), dp(28));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout nav = new LinearLayout(this);
        nav.setPadding(dp(12), dp(8), dp(12), dp(8));
        nav.setBackgroundColor(Color.WHITE);
        Button home = smallButton("Refresh");
        home.setOnClickListener(v -> refreshOnline());
        Button create = smallButton("New request");
        create.setOnClickListener(v -> newTicketDialog());
        Button sync = smallButton("Sync now");
        sync.setOnClickListener(v -> syncNow());
        Button logout = smallButton("Sign out");
        logout.setOnClickListener(v -> signOut());
        nav.addView(home, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(create, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(sync, new LinearLayout.LayoutParams(0, dp(48), 1));
        nav.addView(logout, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(nav);
        setContentView(root);
        renderCached();
        scheduleImmediateSync();
        refreshOnline();
    }

    private void refreshOnline() {
        if (busy || token == null) return;
        busy = true;
        statusText.setText("Syncing with COMPUCARE…");
        new Thread(() -> {
            try {
                JSONObject profile = get("/api/bootstrap");
                JSONArray tickets = get("/api/tickets").optJSONArray("tickets");
                if (tickets == null) tickets = new JSONArray();
                store.cacheTickets(tickets);
                role = profile.optString("role", "customer");
                displayName = profile.optString("name", profile.optString("email", "COMPUCARE user"));
                email = profile.optString("email", "");
                getSharedPreferences("profile_cache", MODE_PRIVATE).edit()
                        .putString("role", role).putString("name", displayName).putString("email", email).apply();
                JSONArray finalTickets = tickets;
                runOnUiThread(() -> {
                    busy = false;
                    titleText.setText("Hello, " + displayName);
                    statusText.setText("Online · " + role.replace('_', ' ') + " · " + store.pendingCount() + " pending sync");
                    renderTickets(finalTickets);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    busy = false;
                    statusText.setText("Offline · Showing saved data · " + store.pendingCount() + " pending");
                    renderCached();
                    if (e instanceof ApiException && ((ApiException)e).code == 401) {
                        statusText.setText("Session expired · Sign in again when online");
                    }
                });
            }
        }).start();
    }

    private void renderCached() {
        renderTickets(store.allTickets());
        if (statusText != null) statusText.setText("Offline-ready · " + store.pendingCount() + " pending sync");
    }

    private void renderTickets(JSONArray tickets) {
        content.removeAllViews();
        LinearLayout summary = card();
        summary.addView(text("SERVICE OVERVIEW", 12, Color.GRAY, true));
        summary.addView(text(tickets.length() + " saved service tickets", 21, Color.rgb(15, 35, 60), true));
        summary.addView(text("Your tickets are stored on this device and refresh when online.", 13, Color.GRAY, false));
        content.addView(summary);
        content.addView(space(14));
        TextView section = text("Recent service requests", 19, Color.rgb(15, 35, 60), true);
        content.addView(section);
        for (int i = 0; i < tickets.length(); i++) {
            JSONObject item = tickets.optJSONObject(i);
            if (item == null) continue;
            LinearLayout ticket = card();
            String number = item.optString("ticket_number", "Pending sync");
            String service = item.optString("service_type", "Service request");
            String status = item.optString("status", "new").replace('_', ' ');
            ticket.addView(text(number + "  ·  " + status.toUpperCase(), 13, Color.rgb(25, 90, 145), true));
            ticket.addView(text(service + " · " + item.optString("device_type", ""), 17, Color.rgb(20, 38, 60), true));
            ticket.addView(text(item.optString("issue", ""), 14, Color.DKGRAY, false));
            if (!"customer".equals(role) && item.has("id") && !item.optString("id").startsWith("local-")) {
                Button update = smallButton("Update status");
                update.setOnClickListener(v -> updateStatus(item));
                ticket.addView(update);
            }
            content.addView(ticket);
            content.addView(space(10));
        }
        if (tickets.length() == 0) {
            LinearLayout empty = card();
            empty.addView(text("No tickets saved yet", 17, Color.rgb(20, 38, 60), true));
            empty.addView(text("Create a service request. It will be saved now and submitted when internet is available.", 14, Color.GRAY, false));
            content.addView(empty);
        }
        LinearLayout profile = card();
        profile.addView(text("ACCOUNT", 12, Color.GRAY, true));
        profile.addView(text(displayName, 16, Color.rgb(20, 38, 60), true));
        profile.addView(text(email + " · " + role.replace('_', ' '), 13, Color.GRAY, false));
        content.addView(space(8)); content.addView(profile);
    }

    private void newTicketDialog() {
        LinearLayout form = column();
        form.setPadding(dp(8), dp(4), dp(8), dp(4));
        EditText service = field("Service category", "Laptop Repair");
        EditText device = field("Device type", "Laptop");
        EditText brand = field("Brand / model", "");
        EditText phone = field("Contact phone", "");
        EditText address = field("Street address", "");
        EditText locality = field("Area / locality", "");
        EditText city = field("City", "");
        EditText state = field("State", "Madhya Pradesh");
        EditText pin = field("6-digit PIN code", "");
        EditText issue = field("Describe the issue (at least 8 characters)", "");
        issue.setMinLines(3);
        issue.setInputType( android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        for (EditText f : new EditText[]{service,device,brand,phone,address,locality,city,state,pin,issue}) form.addView(f);
        ScrollView scroller = new ScrollView(this); scroller.addView(form);
        new AlertDialog.Builder(this).setTitle("New service request").setView(scroller)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save request", (d, w) -> {
                    if (issue.getText().toString().trim().length() < 8 || phone.getText().toString().replaceAll("\\D","").length() < 10
                            || address.getText().toString().trim().isEmpty() || city.getText().toString().trim().isEmpty()
                            || pin.getText().toString().trim().length() != 6) {
                        toast("Enter a valid issue, 10-digit phone, address, city and 6-digit PIN.");
                        return;
                    }
                    try {
                        JSONObject body = new JSONObject();
                        body.put("serviceType", service.getText().toString().trim());
                        body.put("deviceType", device.getText().toString().trim());
                        body.put("brandModel", brand.getText().toString().trim());
                        body.put("priority", "normal");
                        body.put("phone", phone.getText().toString().trim());
                        body.put("addressLine1", address.getText().toString().trim());
                        body.put("locality", locality.getText().toString().trim());
                        body.put("city", city.getText().toString().trim());
                        body.put("state", state.getText().toString().trim());
                        body.put("postalCode", pin.getText().toString().trim());
                        body.put("issue", issue.getText().toString().trim());
                        String localId = "local-" + java.util.UUID.randomUUID();
                        store.addPendingTicket(localId, body);
                        scheduleImmediateSync();
                        toast("Request saved on this device. It will sync when online.");
                        renderCached();
                    } catch (Exception e) { toast("Could not save this request."); }
                }).show();
    }

    private void updateStatus(JSONObject ticket) {
        String[] statuses = {"triaged","assigned","accepted","scheduled","en_route","on_site","diagnosing","awaiting_customer_approval","in_progress","resolved","completed","cancelled","closed"};
        new AlertDialog.Builder(this).setTitle("Update " + ticket.optString("ticket_number"))
                .setItems(statuses, (d, which) -> {
                    try {
                        JSONObject body = new JSONObject(); body.put("status", statuses[which]);
                        store.queueStatus(ticket.optString("id"), statuses[which], java.util.UUID.randomUUID().toString());
                        scheduleImmediateSync(); toast("Status change queued for sync."); renderCached();
                    } catch (Exception ignored) { }
                }).setNegativeButton("Cancel", null).show();
    }

    private void syncNow() {
        scheduleImmediateSync();
        refreshOnline();
        toast("Sync requested. Pending changes will send when connected.");
    }

    private void scheduleImmediateSync() {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class).setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueue(request);
    }

    private void schedulePeriodicSync() {
        Constraints constraints = new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("compucare-background-sync",
                ExistingPeriodicWorkPolicy.KEEP, request);
    }

    private JSONObject get(String path) throws Exception {
        Request request = new Request.Builder().url(BASE + path).header("Authorization", "Bearer " + token).get().build();
        try (Response response = http.newCall(request).execute()) {
            String raw = response.body() == null ? "{}" : response.body().string();
            if (!response.isSuccessful()) throw new ApiException(response.code(), raw);
            return new JSONObject(raw);
        }
    }

    private static class ApiException extends Exception {
        final int code;
        ApiException(int code, String message) { super(message); this.code = code; }
    }

    private void signOut() {
        SecureTokenStore.clear(this); token = null; role = "customer"; showLogin();
    }

    private LinearLayout baseLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.rgb(245, 248, 252));
        return layout;
    }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout card() {
        LinearLayout c = column(); c.setPadding(dp(16), dp(14), dp(16), dp(14));
        c.setBackground(round(Color.WHITE, 16)); return c;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(4), 0, dp(4)); return t;
    }
    private Button button(String value) {
        Button b = new Button(this); b.setText(value); b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(16, 75, 130)));
        return b;
    }
    private Button smallButton(String value) {
        Button b = new Button(this); b.setText(value); b.setTextSize(11);
        b.setPadding(dp(2), 0, dp(2), 0); return b;
    }
    private EditText field(String hint, String value) {
        EditText e = new EditText(this); e.setSingleLine(false); e.setText(value); e.setHint(hint);
        e.setTextSize(15); e.setPadding(dp(10), dp(7), dp(10), dp(7)); return e;
    }
    private View space(int height) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height))); return v; }
    private android.graphics.drawable.GradientDrawable round(int color, int radius) {
        android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    @Override protected void onDestroy() { if (store != null) store.close(); super.onDestroy(); }
}
