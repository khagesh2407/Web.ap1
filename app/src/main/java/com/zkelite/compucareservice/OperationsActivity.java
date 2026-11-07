package com.zkelite.compucareservice;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.*;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/** Native, role-scoped read view for operational modules. Mutations remain in their dedicated workflows. */
public final class OperationsActivity extends Activity {
    private static final String BASE = SyncWorker.BASE;
    private final OkHttpClient http = new OkHttpClient.Builder().callTimeout(35, TimeUnit.SECONDS).build();
    private LinearLayout root, list;
    private TextView status;
    private JSONObject data = new JSONObject();
    private String role = "customer";
    private static final String[][] MODULES = {
        {"Customers","customers","Customers","customers"},
        {"Engineers","engineers","Engineers","engineers"},
        {"Schedule","appointments","Appointments","appointments"},
        {"Estimates","estimates","Estimates","estimates"},
        {"Invoices & payments","invoices","Invoices","invoices"},
        {"Payments","payments","Payments","payments"},
        {"Inventory","parts","Parts catalogue","parts"},
        {"Procurement","purchaseOrders","Purchase orders","purchaseOrders"},
        {"B2B accounts","organizations","Organizations","organizations"},
        {"AMC contracts","amcContracts","AMC contracts","amcContracts"},
        {"SLA policies","slaPolicies","SLA policies","slaPolicies"},
        {"Partner network","partners","Partners","partners"},
        {"Marketplace","listings","Marketplace listings","listings"},
        {"Marketplace bids","bids","Bids","bids"},
        {"Warranty claims","warranties","Warranty claims","warranties"},
        {"Service regions","areas","Service regions","areas"},
        {"B2B agreements","agreements","Agreements","agreements"},
        {"Email notifications","emailDeliveries","Email delivery log","emailDeliveries"}
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        role = getSharedPreferences("profile_cache", MODE_PRIVATE).getString("role", "customer");
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(245,248,252));
        LinearLayout header = new LinearLayout(this); header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(18),dp(18),dp(18),dp(14)); header.setBackgroundColor(Color.rgb(12,34,61));
        TextView title = text("Business modules",22,Color.WHITE,true); header.addView(title);
        status = text("Operational data is online-only in this build.",13,Color.rgb(200,215,235),false); header.addView(status);
        Button back = new Button(this); back.setText("Back to tickets"); back.setOnClickListener(v -> finish()); header.addView(back);
        root.addView(header);
        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(dp(14),dp(14),dp(14),dp(22));
        scroll.addView(list); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        load();
    }

    private void load() {
        String token = SecureTokenStore.read(this);
        if (token == null) { status.setText("Please sign in again."); return; }
        status.setText("Loading role-authorized modules…");
        new Thread(() -> {
            try {
                Request req = new Request.Builder().url(BASE + "/api/operations")
                    .header("Authorization","Bearer " + token).get().build();
                try (Response response = http.newCall(req).execute()) {
                    String raw = response.body() == null ? "{}" : response.body().string();
                    if (!response.isSuccessful()) throw new Exception("Server returned " + response.code() + ". Your role may not have access to operational modules.");
                    data = new JSONObject(raw);
                }
                runOnUiThread(this::render);
            } catch (Exception e) {
                runOnUiThread(() -> status.setText(e.getMessage() == null ? "Could not load operational data." : e.getMessage()));
            }
        }).start();
    }

    private void render() {
        list.removeAllViews();
        status.setText("Live data · " + role.replace('_',' ') + " · Changes require an online connection");
        JSONObject metrics = data.optJSONObject("metrics");
        if (metrics != null) {
            LinearLayout summary = card();
            summary.addView(text("SERVICE METRICS",12,Color.GRAY,true));
            summary.addView(text("Tickets: " + metrics.optInt("tickets") + "   Open: " + metrics.optInt("open"),18,Color.rgb(15,35,60),true));
            summary.addView(text("Customers: " + metrics.optInt("customers") + "   Verified partners: " + metrics.optInt("verifiedPartners"),14,Color.DKGRAY,false));
            list.addView(summary);
            list.addView(space());
        }
        boolean admin = role.equals("super_admin") || role.equals("administrator");
        boolean finance = admin || role.equals("finance_user");
        boolean inventory = admin || role.equals("inventory_manager");
        boolean dispatch = admin || role.equals("dispatcher");
        boolean support = admin || role.equals("support_agent");
        for (String[] module : MODULES) {
            String key = module[1];
            if (!allowed(key, admin, finance, inventory, dispatch, support)) continue;
            JSONArray rows = data.optJSONArray(key);
            if (rows == null) continue; // Backend deliberately omits data outside the user's role.
            LinearLayout card = card();
            card.addView(text(module[0],17,Color.rgb(15,35,60),true));
            card.addView(text(rows.length() + " records",12,Color.GRAY,false));
            int shown = Math.min(rows.length(), 8);
            for (int i=0;i<shown;i++) {
                JSONObject row = rows.optJSONObject(i);
                if (row == null) continue;
                TextView line = text(summarize(row),14,Color.rgb(55,68,85),false);
                line.setPadding(0,dp(7),0,dp(7));
                card.addView(line);
                if (i < shown-1) {
                    android.view.View divider = new android.view.View(this);
                    divider.setBackgroundColor(Color.rgb(230,235,241));
                    card.addView(divider,new LinearLayout.LayoutParams(-1,dp(1)));
                }
            }
            if (rows.length() == 0) card.addView(text("No records available.",13,Color.GRAY,false));
            list.addView(card); list.addView(space());
        }
        if (list.getChildCount() <= (metrics == null ? 0 : 2)) {
            list.addView(text("No operational modules are available for this account role.",15,Color.GRAY,false));
        }
    }

    private boolean allowed(String key, boolean admin, boolean finance, boolean inventory, boolean dispatch, boolean support) {
        switch (key) {
            case "customers": return admin || dispatch || support || finance;
            case "engineers": case "appointments": case "partners": case "listings": case "bids": case "areas": return dispatch;
            case "estimates": case "invoices": case "payments": case "amcContracts": return finance;
            case "parts": return inventory || role.equals("engineer");
            case "purchaseOrders": return inventory;
            case "organizations": case "slaPolicies": case "agreements": return admin;
            case "warranties": return support;
            case "emailDeliveries": return admin || !role.equals("customer");
            default: return false;
        }
    }

    private String summarize(JSONObject row) {
        String[] preferred = {"ticket_number","invoice_number","contract_number","full_name","name","legal_name","title","listing_title","supplier_name","customer_name","engineer_name","partner_name","event_type","recipient","sku","code","claim_reference","status","payment_status","verification_status","city","amount","total_paise","starts_at","created_at"};
        StringBuilder out = new StringBuilder();
        for (String key : preferred) {
            Object value = row.opt(key);
            if (value == null || value == JSONObject.NULL || value.toString().isEmpty()) continue;
            if (out.length() > 0) out.append("  ·  ");
            out.append(key.replace('_',' ')).append(": ").append(value);
            if (out.length() > 230) break;
        }
        return out.length() == 0 ? "Record " + row.optString("id","") : out.toString();
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this); c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(15),dp(12),dp(15),dp(12));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(14)); c.setBackground(bg); return c;
    }
    private TextView text(String s,int size,int color,boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(null,android.graphics.Typeface.BOLD); return t;
    }
    private android.view.View space() { android.view.View v = new android.view.View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(10))); return v; }
    private int dp(int n) { return (int)(n*getResources().getDisplayMetrics().density+0.5f); }
}
