package com.zkelite.compucareservice;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

final class OfflineStore extends SQLiteOpenHelper {
    OfflineStore(Context context) { super(context, "compucare_offline.db", null, 1); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tickets (id TEXT PRIMARY KEY, data TEXT NOT NULL, pending INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE outbox (id TEXT PRIMARY KEY, method TEXT NOT NULL, path TEXT NOT NULL, body TEXT NOT NULL, created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX outbox_created_idx ON outbox(created_at)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }

    synchronized void cacheTickets(JSONArray tickets) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (int i = 0; i < tickets.length(); i++) {
                JSONObject item = tickets.optJSONObject(i);
                if (item == null) continue;
                String id = item.optString("id", "");
                if (id.isEmpty()) continue;
                Cursor existing = db.rawQuery("SELECT pending FROM tickets WHERE id=?", new String[]{id});
                boolean pending = existing.moveToFirst() && existing.getInt(0) == 1;
                existing.close();
                if (!pending) {
                    ContentValues values = new ContentValues();
                    values.put("id", id);
                    values.put("data", item.toString());
                    values.put("pending", 0);
                    values.put("updated_at", System.currentTimeMillis());
                    db.insertWithOnConflict("tickets", null, values, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    synchronized void addPendingTicket(String localId, JSONObject body) {
        JSONObject local = new JSONObject();
        try {
            local.put("id", localId);
            local.put("ticket_number", "Pending sync");
            local.put("service_type", body.optString("serviceType"));
            local.put("device_type", body.optString("deviceType"));
            local.put("brand_model", body.optString("brandModel"));
            local.put("issue", body.optString("issue"));
            local.put("priority", body.optString("priority"));
            local.put("status", "pending_sync");
            local.put("created_at", new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(new java.util.Date()));
        } catch (Exception ignored) { }
        ContentValues ticket = new ContentValues();
        ticket.put("id", localId); ticket.put("data", local.toString());
        ticket.put("pending", 1); ticket.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("tickets", null, ticket, SQLiteDatabase.CONFLICT_REPLACE);
        enqueue("POST", "/api/tickets", body.toString(), localId);
    }

    synchronized void enqueue(String method, String path, String body, String id) {
        ContentValues values = new ContentValues();
        values.put("id", id); values.put("method", method); values.put("path", path);
        values.put("body", body); values.put("created_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict("outbox", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    synchronized List<JSONObject> pendingActions() {
        List<JSONObject> result = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id,method,path,body FROM outbox ORDER BY created_at", null);
        try {
            while (c.moveToNext()) {
                JSONObject row = new JSONObject();
                row.put("id", c.getString(0)); row.put("method", c.getString(1));
                row.put("path", c.getString(2)); row.put("body", c.getString(3));
                result.add(row);
            }
        } catch (Exception ignored) { } finally { c.close(); }
        return result;
    }

    synchronized void queueStatus(String ticketId, String status, String requestId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT data FROM tickets WHERE id=?", new String[]{ticketId});
        try {
            if (cursor.moveToFirst()) {
                JSONObject ticket = new JSONObject(cursor.getString(0));
                ticket.put("status", status);
                ticket.put("sync_pending", true);
                ContentValues update = new ContentValues();
                update.put("data", ticket.toString());
                update.put("pending", 1);
                update.put("updated_at", System.currentTimeMillis());
                db.update("tickets", update, "id=?", new String[]{ticketId});
            }
        } catch (Exception ignored) { } finally { cursor.close(); }
        JSONObject body = new JSONObject();
        try { body.put("status", status); } catch (Exception ignored) { }
        enqueue("PUT", "/api/tickets/" + ticketId + "/status", body.toString(), "status:" + ticketId + ":" + requestId);
    }

    synchronized void actionSynced(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("outbox", "id=?", new String[]{id});
        if (id.startsWith("local-")) {
            db.delete("tickets", "id=? AND pending=1", new String[]{id});
        } else if (id.startsWith("status:")) {
            int separator = id.lastIndexOf(':');
            if (separator > 7) {
                String ticketId = id.substring(7, separator);
                ContentValues update = new ContentValues();
                update.put("pending", 0);
                db.update("tickets", update, "id=?", new String[]{ticketId});
            }
        }
    }

    synchronized JSONArray allTickets() {
        JSONArray result = new JSONArray();
        Cursor c = getReadableDatabase().rawQuery("SELECT data FROM tickets ORDER BY updated_at DESC", null);
        try { while (c.moveToNext()) result.put(new JSONObject(c.getString(0))); }
        catch (Exception ignored) { } finally { c.close(); }
        return result;
    }

    synchronized int pendingCount() {
        Cursor c = getReadableDatabase().rawQuery("SELECT count(*) FROM outbox", null);
        try { return c.moveToFirst() ? c.getInt(0) : 0; } finally { c.close(); }
    }
}
