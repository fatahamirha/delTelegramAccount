package org.telegram.dao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table USER.
 */
public class User {

    private long id;
    private Long accessHash;
    /** Not-null value. */
    private String firstName;
    /** Not-null value. */
    private String lastName;
    private String phone;
    private int linkType;
    private byte[] status;
    private byte[] avatar;

    public User() {
    }

    public User(long id) {
        this.id = id;
    }

    public User(long id, Long accessHash, String firstName, String lastName, String phone, int linkType, byte[] status, byte[] avatar) {
        this.id = id;
        this.accessHash = accessHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.linkType = linkType;
        this.status = status;
        this.avatar = avatar;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getAccessHash() {
        return accessHash;
    }

    public void setAccessHash(Long accessHash) {
        this.accessHash = accessHash;
    }

    /** Not-null value. */
    public String getFirstName() {
        return firstName;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /** Not-null value. */
    public String getLastName() {
        return lastName;
    }

    /** Not-null value; ensure this value is available before it is saved to the database. */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public int getLinkType() {
        return linkType;
    }

    public void setLinkType(int linkType) {
        this.linkType = linkType;
    }

    public byte[] getStatus() {
        return status;
    }

    public void setStatus(byte[] status) {
        this.status = status;
    }

    public byte[] getAvatar() {
        return avatar;
    }

    public void setAvatar(byte[] avatar) {
        this.avatar = avatar;
    }

}
