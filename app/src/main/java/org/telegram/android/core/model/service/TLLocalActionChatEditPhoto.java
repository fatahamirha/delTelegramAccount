package org.telegram.android.core.model.service;

import org.telegram.android.core.model.media.TLAbsLocalAvatarPhoto;
import org.telegram.android.core.model.media.TLLocalAvatarPhoto;
import org.telegram.tl.TLContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.telegram.tl.StreamingUtils.*;

/**
 * Created with IntelliJ IDEA.
 * User: ex3ndr
 * Date: 17.10.13
 * Time: 19:53
 */
public class TLLocalActionChatEditPhoto extends TLAbsLocalAction {

    public static final int CLASS_ID = 0x3bb4a31d;

    private TLAbsLocalAvatarPhoto photo;

    public TLAbsLocalAvatarPhoto getPhoto() {
        return photo;
    }

    public void setPhoto(TLAbsLocalAvatarPhoto photo) {
        this.photo = photo;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void serializeBody(OutputStream stream) throws IOException {
        writeTLObject(photo, stream);
    }

    @Override
    public void deserializeBody(InputStream stream, TLContext context) throws IOException {
        photo = (TLAbsLocalAvatarPhoto) readTLObject(stream, context);
    }
}
