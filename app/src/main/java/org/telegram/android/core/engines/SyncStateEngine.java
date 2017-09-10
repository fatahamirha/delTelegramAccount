package org.telegram.android.core.engines;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.android.TelegramApplication;

/**
 * Created by ex3ndr on 15.01.14.
 */
public class SyncStateEngine {

    SharedPreferences syncPrefs;

    public SyncStateEngine(TelegramApplication application) {
        syncPrefs = application.getSharedPreferences("org.telegram.android.Sync.prefs", Context.MODE_PRIVATE);
    }

    public synchronized boolean isContactSynced() {
        return syncPrefs.getBoolean("contacts_synced", false);
    }

    public synchronized void setSynced(boolean isSynced) {
        syncPrefs.edit().putBoolean("contacts_synced", isSynced).commit();
    }

    public synchronized int getMessagesSyncState(int peerType, int peerId, int def) {
        return syncPrefs.getInt("msg_" + (peerType + peerId * 10L), def);
    }

    public synchronized void setMessagesSyncState(int peerType, int peerId, int state) {
        syncPrefs.edit().putInt("msg_" + (peerType + peerId * 10L), state).commit();
    }

    public synchronized int getMediaSyncState(int peerType, int peerId, int def) {
        return syncPrefs.getInt("media_" + (peerType + peerId * 10L), def);
    }

    public synchronized void setMediaSyncState(int peerType, int peerId, int state) {
        syncPrefs.edit().putInt("media_" + (peerType + peerId * 10L), state).commit();
    }

    public synchronized int getDialogsSyncState(int def) {
        return syncPrefs.getInt("dialog_state", def);
    }

    public synchronized void setDialogsSyncState(int state) {
        syncPrefs.edit().putInt("dialog_state", state).commit();
    }

    public synchronized void clear() {
        syncPrefs.edit().clear().commit();
    }
}
