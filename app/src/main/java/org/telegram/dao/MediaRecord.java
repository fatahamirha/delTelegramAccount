package org.telegram.dao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table MEDIA_RECORD.
 */
public class MediaRecord {

    private Long id;
    private Integer mid;
    private long peerUniq;
    private int date;
    private int senderId;
    /** Not-null value. */
    private byte[] preview;

    public MediaRecord() {
    }

    public MediaRecord(Long id) {
        this.id = id;
    }

    public MediaRecord(Long id, Integer mid, long peerUniq, int date, int senderId, byte[] preview) {
        this.id = id;
        this.mid = mid;
        this.peerUniq = peerUniq;
        this.date = date;
        this.senderId = senderId;
        this.preview = preview;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getMid() {
        return mid;
    }

    public void setMid(Integer mid) {
        this.mid = mid;
    }

    public long getPeerUniq() {
        return peerUniq;
    }

    public void setPeerUniq(long peerUniq) {
        this.peerUniq = peerUniq;
    }

    public int getDate() {
        return date;
    }

    public void setDate(int date) {
        this.date = date;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    /** Not-null value. */
    public byte[] getPreview() {
        return preview;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setPreview(byte[] preview) {
        this.preview = preview;
    }

}
