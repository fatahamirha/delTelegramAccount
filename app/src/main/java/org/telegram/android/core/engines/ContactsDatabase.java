package org.telegram.android.core.engines;

import org.telegram.android.core.model.Contact;
import org.telegram.dao.ContactDao;
import org.telegram.dao.UserDao;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ex3ndr on 14.01.14.
 */
public class ContactsDatabase {
    private ContactDao contactDao;
    private ArrayList<Contact> cache = null;

    public ContactsDatabase(ModelEngine engine) {
        contactDao = engine.getDaoSession().getContactDao();
    }

    public Contact[] getContactsForLocalId(long localId) {
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        for (Contact cached : readAllContacts()) {
            if (cached.getLocalId() == localId) {
                contacts.add(cached);
            }
        }

        return contacts.toArray(new Contact[contacts.size()]);
    }

    public int[] getUidsForLocalId(long localId) {
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        for (Contact cached : readAllContacts()) {
            if (cached.getLocalId() == localId) {
                contacts.add(cached);
            }
        }

        int[] res = new int[contacts.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = contacts.get(i).getUid();
        }
        return res;
    }

    public long[] getContactsForUid(int uid) {
        ArrayList<Contact> contacts = new ArrayList<Contact>();
        for (Contact cached : readAllContacts()) {
            if (cached.getUid() == uid) {
                contacts.add(cached);
            }
        }

        long[] res = new long[contacts.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = contacts.get(i).getLocalId();
        }
        return res;
    }

    public synchronized Contact[] readAllContacts() {
        if (cache == null) {
            ArrayList<Contact> res = new ArrayList<Contact>();
            for (org.telegram.dao.Contact contact : contactDao.queryBuilder().list()) {
                res.add(new Contact(contact.getUid(), contact.getLocalId()));
            }
            cache = res;
        }
        return cache.toArray(new Contact[0]);
    }

    public synchronized void deleteContactsForLocalId(long localId) {
        contactDao.queryBuilder()
                .where(ContactDao.Properties.LocalId.eq(localId))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
        for (Contact cached : readAllContacts()) {
            if (cached.getLocalId() == localId) {
                cache.remove(cached);
            }
        }
    }

    public synchronized void deleteContactsForUid(int uid) {
        contactDao.queryBuilder()
                .where(ContactDao.Properties.Uid.eq(uid))
                .buildDelete()
                .executeDeleteWithoutDetachingEntities();
        for (Contact cached : readAllContacts()) {
            if (cached.getUid() == uid) {
                cache.remove(cached);
            }
        }
    }

    public synchronized void writeContacts(Contact[] contacts) {
        List<org.telegram.dao.Contact> saved = contactDao.loadAll();
        ArrayList<org.telegram.dao.Contact> toAdd = new ArrayList<org.telegram.dao.Contact>();
        ArrayList<org.telegram.dao.Contact> toRemove = new ArrayList<org.telegram.dao.Contact>();
        outer:
        for (Contact c : contacts) {
            for (org.telegram.dao.Contact s : saved) {
                if (c.getUid() == s.getUid() && c.getLocalId() == s.getLocalId()) {
                    continue outer;
                }
            }
            toAdd.add(new org.telegram.dao.Contact(null, c.getUid(), c.getLocalId()));
            if (cache != null) {
                cache.add(c);
            }
        }

        outer:
        for (org.telegram.dao.Contact c : saved) {
            for (Contact s : contacts) {
                if (c.getUid() == s.getUid() && c.getLocalId() == s.getLocalId()) {
                    continue outer;
                }
            }
            toRemove.add(c);
        }

        if (toAdd.size() > 0) {
            contactDao.insertInTx(toAdd, true);
        }

        if (toRemove.size() > 0) {
            contactDao.deleteInTx(toRemove);
        }
    }

    public synchronized void clear() {
        contactDao.deleteAll();
        cache = null;
    }
}
