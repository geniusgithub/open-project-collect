package org.xutils.http.loader;

import android.text.TextUtils;

import org.xutils.cache.DiskCacheEntity;
import org.xutils.common.util.IOUtil;
import org.xutils.http.RequestParams;
import org.xutils.http.annotation.HttpResponse;
import org.xutils.http.app.RequestTracker;
import org.xutils.http.app.ResponseParser;
import org.xutils.http.request.UriRequest;

import java.io.InputStream;

/**
 * Created by lei.jiao on 2014/6/27.
 * 其他对象的下载转换.
 * 使用类型上的@HttpResponse注解信息进行数据转换.
 */
/*package*/ class ObjectLoader extends Loader<Object> {

    private String charset = "UTF-8";
    private String resultStr = null;

    private final Class<?> objectType;
    private final ResponseParser parser;

    public ObjectLoader(Class<?> objectType) {
        this.objectType = objectType;
        HttpResponse response = objectType.getAnnotation(HttpResponse.class);
        if (response != null) {
            try {
                this.parser = response.parser().newInstance();
            } catch (Throwable ex) {
                throw new RuntimeException("create parser error", ex);
            }
        } else {
            throw new IllegalArgumentException("not found @HttpResponse from " + objectType.getName());
        }
    }

    @Override
    public Loader<Object> newInstance() {
        throw new IllegalAccessError("use constructor create ObjectLoader.");
    }

    @Override
    public void setParams(final RequestParams params) {
        if (params != null) {
            String charset = params.getCharset();
            if (charset != null) {
                this.charset = charset;
            }
        }
    }

    @Override
    public Object load(final InputStream in) throws Throwable {
        resultStr = IOUtil.readStr(in, charset);
        return parser.parse(objectType, resultStr);
    }

    @Override
    public Object load(final UriRequest request) throws Throwable {
        request.sendRequest();
        parser.checkResponse(request);
        return this.load(request.getInputStream());
    }

    @Override
    public Object loadFromCache(final DiskCacheEntity cacheEntity) throws Throwable {
        if (cacheEntity != null) {
            String text = cacheEntity.getTextContent();
            if (!TextUtils.isEmpty(text)) {
                return parser.parse(objectType, text);
            }
        }

        return null;
    }

    @Override
    public void save2Cache(UriRequest request) {
        saveStringCache(request, resultStr);
    }

    @Override
    public RequestTracker getResponseTracker() {
        if (this.parser instanceof RequestTracker) {
            return (RequestTracker) parser;
        } else {
            return tracker;
        }
    }
}