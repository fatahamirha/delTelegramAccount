package org.telegram.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

import org.telegram.dao.SecretChat;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table SECRET_CHAT.
*/
public class SecretChatDao extends AbstractDao<SecretChat, Long> {

    public static final String TABLENAME = "SECRET_CHAT";

    /**
     * Properties of entity SecretChat.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Id = new Property(0, long.class, "id", true, "_id");
        public final static Property AccessHash = new Property(1, long.class, "accessHash", false, "ACCESS_HASH");
        public final static Property Uid = new Property(2, int.class, "uid", false, "UID");
        public final static Property State = new Property(3, int.class, "state", false, "STATE");
        public final static Property IsOut = new Property(4, boolean.class, "isOut", false, "IS_OUT");
        public final static Property Key = new Property(5, byte[].class, "key", false, "KEY");
        public final static Property SelfDestruct = new Property(6, Integer.class, "selfDestruct", false, "SELF_DESTRUCT");
    };


    public SecretChatDao(DaoConfig config) {
        super(config);
    }
    
    public SecretChatDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "'SECRET_CHAT' (" + //
                "'_id' INTEGER PRIMARY KEY NOT NULL ," + // 0: id
                "'ACCESS_HASH' INTEGER NOT NULL ," + // 1: accessHash
                "'UID' INTEGER NOT NULL ," + // 2: uid
                "'STATE' INTEGER NOT NULL ," + // 3: state
                "'IS_OUT' INTEGER NOT NULL ," + // 4: isOut
                "'KEY' BLOB," + // 5: key
                "'SELF_DESTRUCT' INTEGER);"); // 6: selfDestruct
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'SECRET_CHAT'";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, SecretChat entity) {
        stmt.clearBindings();
        stmt.bindLong(1, entity.getId());
        stmt.bindLong(2, entity.getAccessHash());
        stmt.bindLong(3, entity.getUid());
        stmt.bindLong(4, entity.getState());
        stmt.bindLong(5, entity.getIsOut() ? 1l: 0l);
 
        byte[] key = entity.getKey();
        if (key != null) {
            stmt.bindBlob(6, key);
        }
 
        Integer selfDestruct = entity.getSelfDestruct();
        if (selfDestruct != null) {
            stmt.bindLong(7, selfDestruct);
        }
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.getLong(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public SecretChat readEntity(Cursor cursor, int offset) {
        SecretChat entity = new SecretChat( //
            cursor.getLong(offset + 0), // id
            cursor.getLong(offset + 1), // accessHash
            cursor.getInt(offset + 2), // uid
            cursor.getInt(offset + 3), // state
            cursor.getShort(offset + 4) != 0, // isOut
            cursor.isNull(offset + 5) ? null : cursor.getBlob(offset + 5), // key
            cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6) // selfDestruct
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, SecretChat entity, int offset) {
        entity.setId(cursor.getLong(offset + 0));
        entity.setAccessHash(cursor.getLong(offset + 1));
        entity.setUid(cursor.getInt(offset + 2));
        entity.setState(cursor.getInt(offset + 3));
        entity.setIsOut(cursor.getShort(offset + 4) != 0);
        entity.setKey(cursor.isNull(offset + 5) ? null : cursor.getBlob(offset + 5));
        entity.setSelfDestruct(cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6));
     }
    
    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(SecretChat entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    /** @inheritdoc */
    @Override
    public Long getKey(SecretChat entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    /** @inheritdoc */
    @Override    
    protected boolean isEntityUpdateable() {
        return true;
    }
    
}
