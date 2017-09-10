package org.telegram.android.core.model.media;

import org.telegram.mtproto.secure.CryptoUtils;
import org.telegram.tl.TLContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 15.10.13
 * Time: 2:40
 */
public class TLLocalEncryptedFileLocation extends TLAbsLocalFileLocation {

    public static final int CLASS_ID = 0x80172c27;

    private long id;
    private long accessHash;
    private int size;
    private int dcId;
    private byte[] key;
    private byte[] iv;
    private String uniqKey;

    public TLLocalEncryptedFileLocation(long id, long accessHash, int size, int dcId, byte[] key, byte[] iv) {
        this.id = id;
        this.accessHash = accessHash;
        this.size = size;
        this.dcId = dcId;
        this.key = key;
        this.iv = iv;
        this.uniqKey = dcId + "_" + id;
    }

    public TLLocalEncryptedFileLocation() {

    }

    public long getId() {
        return id;
    }

    public long getAccessHash() {
        return accessHash;
    }

    public int getSize() {
        return size;
    }

    public int getDcId() {
        return dcId;
    }

    public byte[] getKey() {
        return key;
    }

    public byte[] getIv() {
        return iv;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeLong(id, stream);
        writeLong(accessHash, stream);
        writeInt(size, stream);
        writeInt(dcId, stream);
        writeTLBytes(key, stream);
        writeTLBytes(iv, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        id = readLong(stream);
        accessHash = readLong(stream);
        size = readInt(stream);
        dcId = readInt(stream);
        key = readTLBytes(stream);
        iv = readTLBytes(stream);
        this.uniqKey = dcId + "_" + id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TLLocalEncryptedFileLocation)) {
            return false;
        }
        return super.equals(o);
    }

    public boolean equals(TLLocalEncryptedFileLocation fileLocation) {
        return fileLocation.id == id &&
                fileLocation.accessHash == accessHash &&
                fileLocation.size == size &&
                fileLocation.dcId == dcId &&
                CryptoUtils.arrayEq(fileLocation.key, key) &&
                CryptoUtils.arrayEq(fileLocation.iv, iv);
    }

    @Override
    public String getUniqKey() {
        return uniqKey;
    }
}
