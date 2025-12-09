package org.eclipse.openvsx.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

public class HttpHeadersUtil {
    private HttpHeadersUtil() {
    }

    public static HttpHeaders getForwardedHeaders() {
        var headers = new HttpHeaders();
        try {
            var requestAttrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            var request = requestAttrs.getRequest();

            var it = request.getHeaderNames();
            while (it.hasMoreElements()) {
                var header = it.nextElement();
                headers.add(header, request.getHeader(header));
            }

        } catch (IllegalStateException _) {}
        headers.remove(HttpHeaders.HOST);
        return headers;
    }

    public static HttpHeaders getAcceptJsonHeaders() {
        var headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }
}
