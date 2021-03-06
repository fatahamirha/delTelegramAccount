package org.telegram.dao;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

import org.telegram.dao.Message;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table MESSAGE.
*/
public class MessageDao extends AbstractDao<Message, Long> {

    public static final String TABLENAME = "MESSAGE";

    /**
     * Properties of entity Message.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property PeerUniqId = new Property(1, long.class, "peerUniqId", false, "PEER_UNIQ_ID");
        public final static Property Mid = new Property(2, int.class, "mid", false, "MID");
        public final static Property Rid = new Property(3, long.class, "rid", false, "RID");
        public final static Property Date = new Property(4, int.class, "date", false, "DATE");
        public final static Property State = new Property(5, int.class, "state", false, "STATE");
        public final static Property SenderId = new Property(6, Integer.class, "senderId", false, "SENDER_ID");
        public final static Property ContentType = new Property(7, int.class, "contentType", false, "CONTENT_TYPE");
        public final static Property Message = new Property(8, String.class, "message", false, "MESSAGE");
        public final static Property Extras = new Property(9, byte[].class, "extras", false, "EXTRAS");
        public final static Property IsOut = new Property(10, Boolean.class, "isOut", false, "IS_OUT");
        public final static Property ForwardDate = new Property(11, Integer.class, "forwardDate", false, "FORWARD_DATE");
        public final static Property ForwardSenderId = new Property(12, Integer.class, "forwardSenderId", false, "FORWARD_SENDER_ID");
        public final static Property ForwardMid = new Property(13, Integer.class, "forwardMid", false, "FORWARD_MID");
        public final static Property DeletedLocal = new Property(14, boolean.class, "deletedLocal", false, "DELETED_LOCAL");
        public final static Property DeletedServer = new Property(15, boolean.class, "deletedServer", false, "DELETED_SERVER");
        public final static Property MessageTimeout = new Property(16, Integer.class, "messageTimeout", false, "MESSAGE_TIMEOUT");
        public final static Property MessageDieTime = new Property(17, Integer.class, "messageDieTime", false, "MESSAGE_DIE_TIME");
    };


    public MessageDao(DaoConfig config) {
        super(config);
    }
    
    public MessageDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "'MESSAGE' (" + //
                "'_id' INTEGER PRIMARY KEY AUTOINCREMENT ," + // 0: id
                "'PEER_UNIQ_ID' INTEGER NOT NULL ," + // 1: peerUniqId
                "'MID' INTEGER NOT NULL UNIQUE ," + // 2: mid
                "'RID' INTEGER NOT NULL ," + // 3: rid
                "'DATE' INTEGER NOT NULL ," + // 4: date
                "'STATE' INTEGER NOT NULL ," + // 5: state
                "'SENDER_ID' INTEGER," + // 6: senderId
                "'CONTENT_TYPE' INTEGER NOT NULL ," + // 7: contentType
                "'MESSAGE' TEXT NOT NULL ," + // 8: message
                "'EXTRAS' BLOB," + // 9: extras
                "'IS_OUT' INTEGER," + // 10: isOut
                "'FORWARD_DATE' INTEGER," + // 11: forwardDate
                "'FORWARD_SENDER_ID' INTEGER," + // 12: forwardSenderId
                "'FORWARD_MID' INTEGER," + // 13: forwardMid
                "'DELETED_LOCAL' INTEGER NOT NULL ," + // 14: deletedLocal
                "'DELETED_SERVER' INTEGER NOT NULL ," + // 15: deletedServer
                "'MESSAGE_TIMEOUT' INTEGER," + // 16: messageTimeout
                "'MESSAGE_DIE_TIME' INTEGER);"); // 17: messageDieTime
        // Add Indexes
        db.execSQL("CREATE INDEX " + constraint + "IDX_MESSAGE_MID ON MESSAGE" +
                " (MID);");
        db.execSQL("CREATE INDEX " + constraint + "IDX_MESSAGE_RID ON MESSAGE" +
                " (RID);");
        db.execSQL("CREATE INDEX " + constraint + "IDX_MESSAGE_IS_OUT ON MESSAGE" +
                " (IS_OUT);");
        db.execSQL("CREATE INDEX " + constraint + "IDX_MESSAGE_MESSAGE_TIMEOUT ON MESSAGE" +
                " (MESSAGE_TIMEOUT);");
        db.execSQL("CREATE INDEX " + constraint + "IDX_MESSAGE_MESSAGE_DIE_TIME ON MESSAGE" +
                " (MESSAGE_DIE_TIME);");
        db.execSQL("CREATE INDEX " + constraint + "IDX_MESSAGE_PEER_UNIQ_ID_MID_DATE_DELETED_LOCAL ON MESSAGE" +
                " (PEER_UNIQ_ID,MID,DATE,DELETED_LOCAL);");
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'MESSAGE'";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, Message entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindLong(2, entity.getPeerUniqId());
        stmt.bindLong(3, entity.getMid());
        stmt.bindLong(4, entity.getRid());
        stmt.bindLong(5, entity.getDate());
        stmt.bindLong(6, entity.getState());
 
        Integer senderId = entity.getSenderId();
        if (senderId != null) {
            stmt.bindLong(7, senderId);
        }
        stmt.bindLong(8, entity.getContentType());
        stmt.bindString(9, entity.getMessage());
 
        byte[] extras = entity.getExtras();
        if (extras != null) {
            stmt.bindBlob(10, extras);
        }
 
        Boolean isOut = entity.getIsOut();
        if (isOut != null) {
            stmt.bindLong(11, isOut ? 1l: 0l);
        }
 
        Integer forwardDate = entity.getForwardDate();
        if (forwardDate != null) {
            stmt.bindLong(12, forwardDate);
        }
 
        Integer forwardSenderId = entity.getForwardSenderId();
        if (forwardSenderId != null) {
            stmt.bindLong(13, forwardSenderId);
        }
 
        Integer forwardMid = entity.getForwardMid();
        if (forwardMid != null) {
            stmt.bindLong(14, forwardMid);
        }
        stmt.bindLong(15, entity.getDeletedLocal() ? 1l: 0l);
        stmt.bindLong(16, entity.getDeletedServer() ? 1l: 0l);
 
        Integer messageTimeout = entity.getMessageTimeout();
        if (messageTimeout != null) {
            stmt.bindLong(17, messageTimeout);
        }
 
        Integer messageDieTime = entity.getMessageDieTime();
        if (messageDieTime != null) {
            stmt.bindLong(18, messageDieTime);
        }
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public Message readEntity(Cursor cursor, int offset) {
        Message entity = new Message( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getLong(offset + 1), // peerUniqId
            cursor.getInt(offset + 2), // mid
            cursor.getLong(offset + 3), // rid
            cursor.getInt(offset + 4), // date
            cursor.getInt(offset + 5), // state
            cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6), // senderId
            cursor.getInt(offset + 7), // contentType
            cursor.getString(offset + 8), // message
            cursor.isNull(offset + 9) ? null : cursor.getBlob(offset + 9), // extras
            cursor.isNull(offset + 10) ? null : cursor.getShort(offset + 10) != 0, // isOut
            cursor.isNull(offset + 11) ? null : cursor.getInt(offset + 11), // forwardDate
            cursor.isNull(offset + 12) ? null : cursor.getInt(offset + 12), // forwardSenderId
            cursor.isNull(offset + 13) ? null : cursor.getInt(offset + 13), // forwardMid
            cursor.getShort(offset + 14) != 0, // deletedLocal
            cursor.getShort(offset + 15) != 0, // deletedServer
            cursor.isNull(offset + 16) ? null : cursor.getInt(offset + 16), // messageTimeout
            cursor.isNull(offset + 17) ? null : cursor.getInt(offset + 17) // messageDieTime
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, Message entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setPeerUniqId(cursor.getLong(offset + 1));
        entity.setMid(cursor.getInt(offset + 2));
        entity.setRid(cursor.getLong(offset + 3));
        entity.setDate(cursor.getInt(offset + 4));
        entity.setState(cursor.getInt(offset + 5));
        entity.setSenderId(cursor.isNull(offset + 6) ? null : cursor.getInt(offset + 6));
        entity.setContentType(cursor.getInt(offset + 7));
        entity.setMessage(cursor.getString(offset + 8));
        entity.setExtras(cursor.isNull(offset + 9) ? null : cursor.getBlob(offset + 9));
        entity.setIsOut(cursor.isNull(offset + 10) ? null : cursor.getShort(offset + 10) != 0);
        entity.setForwardDate(cursor.isNull(offset + 11) ? null : cursor.getInt(offset + 11));
        entity.setForwardSenderId(cursor.isNull(offset + 12) ? null : cursor.getInt(offset + 12));
        entity.setForwardMid(cursor.isNull(offset + 13) ? null : cursor.getInt(offset + 13));
        entity.setDeletedLocal(cursor.getShort(offset + 14) != 0);
        entity.setDeletedServer(cursor.getShort(offset + 15) != 0);
        entity.setMessageTimeout(cursor.isNull(offset + 16) ? null : cursor.getInt(offset + 16));
        entity.setMessageDieTime(cursor.isNull(offset + 17) ? null : cursor.getInt(offset + 17));
     }
    
    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(Message entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    /** @inheritdoc */
    @Override
    public Long getKey(Message entity) {
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
