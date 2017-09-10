package org.telegram.android.core.engines;

import android.os.SystemClock;
import de.greenrobot.dao.query.WhereCondition;
import org.telegram.android.core.model.DialogDescription;
import org.telegram.android.core.model.TLLocalContext;
import org.telegram.android.log.Logger;
import org.telegram.dao.Dialog;
import org.telegram.dao.DialogDao;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ex3ndr on 02.01.14.
 */
public class DialogsDatabase {
    private static final String TAG = "DialogsDatabase";

    private DialogDao dialogsDao;

    private ConcurrentHashMap<Long, DialogDescription> dialogsCache;

    public DialogsDatabase(ModelEngine engine) {
        dialogsDao = engine.getDaoSession().getDialogDao();
        dialogsCache = new ConcurrentHashMap<Long, DialogDescription>();
    }

    public DialogDescription[] getItems(int offset, int limit) {
        try {
            long start = SystemClock.uptimeMillis();
            List<Dialog> dialogDescriptions = dialogsDao.queryBuilder()
                    .where(DialogDao.Properties.Date.gt(0))
                    .orderDesc(DialogDao.Properties.Date)
                    .offset(offset)
                    .limit(limit)
                    .list();
            Logger.d(TAG, "Queried (" + offset + ") in " + (SystemClock.uptimeMillis() - start) + " ms");
            Dialog[] res = dialogDescriptions.toArray(new Dialog[dialogDescriptions.size()]);
            Logger.d(TAG, "Loaded items in " + (SystemClock.uptimeMillis() - start) + " ms");
            DialogDescription[] dRes = new DialogDescription[res.length];
            for (int i = 0; i < dRes.length; i++) {
                dRes[i] = convertCached(res[i]);
            }
            return dRes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DialogDescription[] getAll() {
        try {
            long start = SystemClock.uptimeMillis();
            List<Dialog> dialogs = dialogsDao.queryBuilder().where(DialogDao.Properties.Date.gt(0)).list();
            Logger.d(TAG, "Queried in " + (SystemClock.uptimeMillis() - start) + " ms");
            Dialog[] res = dialogs.toArray(new Dialog[dialogs.size()]);
            Logger.d(TAG, "Loaded items in " + (SystemClock.uptimeMillis() - start) + " ms");
            DialogDescription[] dRes = new DialogDescription[res.length];
            for (int i = 0; i < dRes.length; i++) {
                dRes[i] = convertCached(res[i]);
            }
            return dRes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public DialogDescription[] getUnreadedRemotelyDescriptions() {
        List<Dialog> dbRes = dialogsDao.queryBuilder()
                .where(new WhereCondition.StringCondition(
                        DialogDao.Properties.LastLocalViewedMessage.columnName +
                                " > " +
                                DialogDao.Properties.LastRemoteViewedMessage.columnName))
                .list();
        DialogDescription[] res = new DialogDescription[dbRes.size()];
        for (int i = 0; i < dbRes.size(); i++) {
            res[i] = convertCached(dbRes.get(i));
        }
        return res;
    }

    public Dialog loadDialogDb(int peerType, int peerId) {
        return dialogsDao.load(peerType + peerId * 10L);
    }

    public DialogDescription loadDialog(int peerType, int peerId) {
        long id = uniqId(peerType, peerId);
        DialogDescription description = dialogsCache.get(id);
        if (description != null) {
            return description;
        }
        return convertCached(loadDialogDb(peerType, peerId));
    }

    public DialogDescription[] loadDialogs(Long[] uniqIds) {
        Dialog[] dialogs = dialogsDao.queryBuilder()
                .where(DialogDao.Properties.Id.in((Object[]) uniqIds))
                .list()
                .toArray(new Dialog[0]);
        DialogDescription[] res = new DialogDescription[dialogs.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = convertCached(dialogs[i]);
        }
        return res;
    }

    public void createDialog(DialogDescription description) {
        Dialog dialog = new Dialog();
        applyChanges(description, dialog);
        dialogsDao.insertOrReplace(dialog);
    }

    public void updateOrCreateDialog(DialogDescription description) {
        Dialog dialog = loadDialogDb(description.getPeerType(), description.getPeerId());
        if (dialog == null) {
            dialog = new Dialog();
        }
        applyChanges(description, dialog);
        dialogsDao.insertOrReplace(dialog);
    }

    public void updateOrCreateDialogs(DialogDescription[] descriptions) {
        Dialog[] res = new Dialog[descriptions.length];
        for (int i = 0; i < descriptions.length; i++) {
            res[i] = loadDialogDb(descriptions[i].getPeerType(), descriptions[i].getPeerId());
            if (res[i] == null) {
                res[i] = new Dialog();
            }
            applyChanges(descriptions[i], res[i]);
        }
        dialogsDao.insertOrReplaceInTx(res);
    }

    public void updateDialog(DialogDescription description) {
        Dialog dialog = loadDialogDb(description.getPeerType(), description.getPeerId());
        applyChanges(description, dialog);
        dialogsDao.update(dialog);
    }

    public void deleteDialog(int peerType, int peerId) {
        Dialog dialog = loadDialogDb(peerType, peerId);
        if (dialog != null) {
            dialog.setDate(0);
            dialogsDao.update(dialog);
        }
    }

    public void deleteDialogPermanent(int peerType, int peerId) {
        Dialog dialog = loadDialogDb(peerType, peerId);
        if (dialog != null) {
            dialogsDao.delete(dialog);
        }
    }

    private DialogDescription convertCached(Dialog dialog) {
        if (dialog == null) {
            return null;
        }

        long id = uniqId(dialog);
        synchronized (dialog) {
            DialogDescription res = dialogsCache.get(id);
            if (res == null) {
                res = new DialogDescription();
                res.setPeerType((int) ((10 + dialog.getId() % 10L) % 10));
                res.setPeerId((int) ((dialog.getId() - res.getPeerType()) / 10L));
                dialogsCache.putIfAbsent(id, res);
                res = dialogsCache.get(id);
            }

            res.setContentType(dialog.getContentType());
            res.setDate(dialog.getDate());
            res.setFailure(dialog.getFailure());
            res.setFirstUnreadMessage(dialog.getFirstUnreadMessage());
            res.setLastLocalViewedMessage(dialog.getLastLocalViewedMessage());
            res.setLastRemoteViewedMessage(dialog.getLastRemoteViewedMessage());
            res.setFirstUnreadMessage(dialog.getFirstUnreadMessage());
            if (dialog.getMessage() == null) {
                res.setMessage("");
            } else {
                res.setMessage(dialog.getMessage());
            }
            res.setMessageState(dialog.getMessageState());
            res.setSenderId(dialog.getSenderId());
            res.setTopMessageId(dialog.getTopMessageId());
            res.setFirstUnreadMessage(dialog.getFirstUnreadMessage());
            res.setUnreadCount(dialog.getUnreadCount());
            if (dialog.getExtras() != null) {
                try {
                    res.setExtras(TLLocalContext.getInstance().deserializeMessage(dialog.getExtras()));
                } catch (IOException e) {
                    e.printStackTrace();
                    res.setExtras(null);
                }
            } else {
                res.setExtras(null);
            }
        }

        return dialogsCache.get(id);
    }

    private void applyChanges(DialogDescription src, Dialog dest) {
        dest.setId(uniqId(src));
        dest.setContentType(src.getContentType());
        dest.setDate(src.getDate());
        dest.setFailure(src.isFailure());
        dest.setFirstUnreadMessage(src.getFirstUnreadMessage());
        dest.setLastLocalViewedMessage(src.getLastLocalViewedMessage());
        dest.setLastRemoteViewedMessage(src.getLastRemoteViewedMessage());
        dest.setFirstUnreadMessage(src.getFirstUnreadMessage());
        dest.setMessage(src.getMessage());
        dest.setMessageState(src.getMessageState());
        dest.setSenderId(src.getSenderId());
        dest.setTopMessageId(src.getTopMessageId());
        dest.setFirstUnreadMessage(src.getFirstUnreadMessage());
        dest.setUnreadCount(src.getUnreadCount());
        if (src.getExtras() != null) {
            try {
                dest.setExtras(src.getExtras().serialize());
            } catch (IOException e) {
                e.printStackTrace();
                dest.setExtras(null);
            }
        } else {
            dest.setExtras(null);
        }
    }

    private long uniqId(DialogDescription description) {
        return description.getPeerType() + description.getPeerId() * 10L;
    }

    private long uniqId(Dialog description) {
        return description.getId();
    }

    private long uniqId(int peerType, int peerId) {
        return peerType + peerId * 10L;
    }

    public void clear() {
        dialogsDao.deleteAll();
        dialogsCache.clear();
    }
}
