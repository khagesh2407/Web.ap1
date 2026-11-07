package com.zkelite.compucareservice;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SyncWorker extends Worker {
    static final String BASE = "https://compucare-service-sf3up4.v2.appdeploy.ai";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull @Override public Result doWork() {
        String token = SecureTokenStore.read(getApplicationContext());
        if (token == null || token.isEmpty()) return Result.failure();
        OfflineStore store = new OfflineStore(getApplicationContext());
        try {
            for (JSONObject item : store.pendingActions()) {
                String method = item.optString("method");
                String path = item.optString("path");
                JSONObject body = new JSONObject(item.optString("body", "{}"));
                Request.Builder builder = new Request.Builder().url(BASE + path)
                        .header("Authorization", "Bearer " + token)
                        .header("Accept", "application/json");
                RequestBody requestBody = RequestBody.create(body.toString(), JSON);
                if ("POST".equals(method)) builder.post(requestBody);
                else if ("PUT".equals(method)) builder.put(requestBody);
                else if ("DELETE".equals(method)) builder.delete(requestBody);
                else return Result.failure();
                try (Response response = client.newCall(builder.build()).execute()) {
                    if (response.code() == 401) return Result.failure();
                    if (!response.isSuccessful()) {
                        if (response.code() >= 500 || response.code() == 429) return Result.retry();
                        return Result.failure();
                    }
                    store.actionSynced(item.optString("id"));
                }
            }
            Request request = new Request.Builder().url(BASE + "/api/tickets")
                    .header("Authorization", "Bearer " + token).get().build();
            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    JSONObject payload = new JSONObject(response.body().string());
                    JSONArray tickets = payload.optJSONArray("tickets");
                    if (tickets != null) store.cacheTickets(tickets);
                } else if (response.code() == 401) return Result.failure();
                else if (response.code() >= 500) return Result.retry();
            }
            return Result.success();
        } catch (IOException e) {
            return Result.retry();
        } catch (Exception e) {
            return Result.failure();
        } finally { store.close(); }
    }
}
