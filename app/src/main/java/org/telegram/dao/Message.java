package org.telegram.dao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table MESSAGE.
 */
public class Message {

    private Long id;
    private long peerUniqId;
    private int mid;
    private long rid;
    private int date;
    private int state;
    private Integer senderId;
    private int contentType;
    /** Not-null value. */
    private String message;
    private byte[] extras;
    private Boolean isOut;
    private Integer forwardDate;
    private Integer forwardSenderId;
    private Integer forwardMid;
    private boolean deletedLocal;
    private boolean deletedServer;
    private Integer messageTimeout;
    private Integer messageDieTime;

    public Message() {
    }

    public Message(Long id) {
        this.id = id;
    }

    public Message(Long id, long peerUniqId, int mid, long rid, int date, int state, Integer senderId, int contentType, String message, byte[] extras, Boolean isOut, Integer forwardDate, Integer forwardSenderId, Integer forwardMid, boolean deletedLocal, boolean deletedServer, Integer messageTimeout, Integer messageDieTime) {
        this.id = id;
        this.peerUniqId = peerUniqId;
        this.mid = mid;
        this.rid = rid;
        this.date = date;
        this.state = state;
        this.senderId = senderId;
        this.contentType = contentType;
        this.message = message;
        this.extras = extras;
        this.isOut = isOut;
        this.forwardDate = forwardDate;
        this.forwardSenderId = forwardSenderId;
        this.forwardMid = forwardMid;
        this.deletedLocal = deletedLocal;
        this.deletedServer = deletedServer;
        this.messageTimeout = messageTimeout;
        this.messageDieTime = messageDieTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getPeerUniqId() {
        return peerUniqId;
    }

    public void setPeerUniqId(long peerUniqId) {
        this.peerUniqId = peerUniqId;
    }

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public long getRid() {
        return rid;
    }

    public void setRid(long rid) {
        this.rid = rid;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }

    public int getContentType() {
        return contentType;
    }

    public void setContentType(int contentType) {
        this.contentType = contentType;
    }

    /** Not-null value. */
    public String getMessage() {
        return message;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setMessage(String message) {
        this.message = message;
    }

    public byte[] getExtras() {
        return extras;
    }

    public void setExtras(byte[] extras) {
        this.extras = extras;
    }

    public Boolean getIsOut() {
        return isOut;
    }

    public void setIsOut(Boolean isOut) {
        this.isOut = isOut;
    }

    public Integer getForwardDate() {
        return forwardDate;
    }

    public void setForwardDate(Integer forwardDate) {
        this.forwardDate = forwardDate;
    }

    public Integer getForwardSenderId() {
        return forwardSenderId;
    }

    public void setForwardSenderId(Integer forwardSenderId) {
        this.forwardSenderId = forwardSenderId;
    }

    public Integer getForwardMid() {
        return forwardMid;
    }

    public void setForwardMid(Integer forwardMid) {
        this.forwardMid = forwardMid;
    }

    public boolean getDeletedLocal() {
        return deletedLocal;
    }

    public void setDeletedLocal(boolean deletedLocal) {
        this.deletedLocal = deletedLocal;
    }

    public boolean getDeletedServer() {
        return deletedServer;
    }

    public void setDeletedServer(boolean deletedServer) {
        this.deletedServer = deletedServer;
    }

    public Integer getMessageTimeout() {
        return messageTimeout;
    }

    public void setMessageTimeout(Integer messageTimeout) {
        this.messageTimeout = messageTimeout;
    }

    public Integer getMessageDieTime() {
        return messageDieTime;
    }

    public void setMessageDieTime(Integer messageDieTime) {
        this.messageDieTime = messageDieTime;
    }

}
