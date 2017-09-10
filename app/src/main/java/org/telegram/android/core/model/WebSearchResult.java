package org.telegram.android.core.model;

import org.telegram.android.R;
import org.telegram.android.core.model.web.TLSearchResult;

/**
 * Created by ex3ndr on 14.03.14.
 */
public class WebSearchResult {
    private int index;
    private String key;
    private int size;
    private int w;
    private int h;
    private String fullUrl;
    private int thumbW;
    private int thumbH;
    private String thumbUrl;
    private String contentType;

    public WebSearchResult(int index, TLSearchResult result) {
        this.index = index;
        key = result.getKey();
        size = result.getSize();
        w = result.getW();
        h = result.getH();
        fullUrl = result.getFullUrl();
        thumbH = result.getThumbH();
        thumbW = result.getThumbW();
        thumbUrl = result.getThumbUrl();
        contentType = result.getContentType();
    }

    public WebSearchResult(String key, int index, int size, int w, int h, String fullUrl, int thumbW, int thumbH, String thumbUrl,
                           String contentType) {
        this.key = key;
        this.index = index;
        this.size = size;
        this.w = w;
        this.h = h;
        this.fullUrl = fullUrl;
        this.thumbW = thumbW;
        this.thumbH = thumbH;
        this.thumbUrl = thumbUrl;
        this.contentType = contentType;
    }

    public String getKey() {
        return key;
    }

    public String getContentType() {
        return contentType;
    }

    public int getIndex() {
        return index;
    }

    public int getSize() {
        return size;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    public String getFullUrl() {
        return fullUrl;
    }

    public int getThumbW() {
        return thumbW;
    }

    public int getThumbH() {
        return thumbH;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public TLSearchResult toTlResult() {
        return new TLSearchResult(key, size, w, h, fullUrl, thumbH, thumbW, thumbUrl, contentType);
    }
}
