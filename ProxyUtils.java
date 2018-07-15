package com.meik2333;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;

public class ProxyUtils {
    static private final String GET_CONTENT_FORMAT = "GET %s HTTP/1.1\r\nHost: %s\r\n%s\r\n";
    static private final String POST_CONTENT_FORMAT = "POST %s HTTP/1.1\r\nHost: %s\r\n%s\r\n%s";

    /**
     * 拼接 HTTP 请求的正文
     * 
     * @param addr
     * @param uri
     * @param query
     * @param header
     * @return
     */
    static private String makeRequestContent(String method, String addr, Integer port, String uri, String query,
            String header, String body) {
        if (port != 80) {
            addr = addr + ":" + port.toString();
        }
        if (method.equalsIgnoreCase("GET")) {
            return String.format(GET_CONTENT_FORMAT, uri + query, addr, header);
        } else {
            return String.format(POST_CONTENT_FORMAT, uri + query, addr, header, body);
        }
    }

    static public String makeRequestSocket(String addr, String uri, String query, String header, String post) {
        return String.format(GET_CONTENT_FORMAT, addr, uri + query, addr, header, "");
    }

    static public String readInputStreamLine(InputStream in) {
        String str = "";
        char ch = 0;
        do {
            try {
                ch = (char) in.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (ch == '\n') {
                break;
            }
            str += ch;
        } while (ch != -1);
        int len = str.length();
        if (len >= 1 && str.charAt(len - 1) == '\r') {
            str = str.substring(0, len - 1);
        }
        return str;
    }

    static public Boolean isDoc(String type) {
        return (type.equalsIgnoreCase("text/html")
                || type.equalsIgnoreCase("application/javascript")
                || type.equalsIgnoreCase("text/css"));
    }
    
    static public ProxyClass readResponse(Socket socket) {
        ProxyClass proxy = new ProxyClass();

        InputStream in;
        String str = "";
        Integer contentLength = 0;
        try {

            in = socket.getInputStream();
            str = readInputStreamLine(in);
            String[] args = str.split(" ");
            proxy.setStatus(args[1]);
            // 读取 headers
            while ((str = readInputStreamLine(in)) != null) {
                if (str.indexOf(':') >= 0) {
                    String[] k_v = str.split(": ");
                    proxy.putHeader(k_v[0], k_v[1]);

                    // 读取 body 长度
                    if (k_v[0].equalsIgnoreCase("Content-Length")) {
                        contentLength = Integer.parseInt(k_v[1]);
                    }
                } else {
                    break;
                }
            }

            if (proxy.getStatus().equals("304")) {
                return proxy;
            }

            // 读取 body
            int ch;
            String body = "";
            byte[] buffer = null;
            // 文档类型
            if (isDoc(proxy.getHeader().get("Content-Type"))) {
                while ((ch = in.read()) != -1) {
                    if (body.length() < contentLength) {
                        body += (char) ch;
                        if (body.length() >= contentLength) {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                proxy.setBody(body);
            } else { // 文件类型
                buffer = new byte[contentLength];
                in.read(buffer);
                proxy.setByteBody(buffer);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return proxy;
    }

    static private HashMap<String, String> getHeadersInfo(HttpServletRequest request) {

        HashMap<String, String> map = new HashMap<String, String>();

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            map.put(key, value);
        }
        return map;
    }

    /**
     * 拼接 Headers 字符串
     * 
     * @param request
     * @return
     */
    static private String makeHeaderContent(HttpServletRequest request) {
        HashMap<String, String> map = getHeadersInfo(request);
        String headerContent = "";

        for (String key : map.keySet()) {
            if (key.equalsIgnoreCase("host")) {
                continue;
            }
            headerContent += key + ": " + map.get(key) + "\r\n";
        }

        return headerContent;
    }

    /**
     * 获得 URL 请求参数
     * 
     * @param request
     * @return String
     */
    static private String makeGetContent(HttpServletRequest request) {
        String getContent = request.getQueryString();
        if (getContent != null) {
            getContent = "?" + getContent;
        } else {
            getContent = "";
        }
        return getContent;
    }

    /**
     * 获得 POST 的请求参数 只有在第一次读取的时候有效, 如果再次调用会导致程序崩溃
     * 
     * @param request
     * @return
     */
    static private String makePostContent(HttpServletRequest request) {
        String contentLength = request.getHeader("Content-Length");
        String postContent = "";
        if (contentLength == null) {
            return postContent;
        }
        Integer postLength = Integer.parseInt(request.getHeader("Content-Length"));
        int ch;

        try {
            while ((ch = request.getInputStream().read()) != -1) {
                postContent += (char) ch;
                if (postContent.length() >= postLength) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            return new String(postContent.getBytes("ISO-8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return "";
        }
    }

    static public ProxyClass proxyRequest(HttpServletRequest request, String proxy_addr, Integer proxy_port,
            String uri) {
        Socket socket = null;
        try {
            socket = new Socket(proxy_addr, proxy_port);
            // 解析 HTTP 请求并转发
            OutputStream out = socket.getOutputStream();
            String requestContent = ProxyUtils.makeRequestContent(request.getMethod(), proxy_addr, proxy_port, uri,
                    makeGetContent(request), makeHeaderContent(request), makePostContent(request));
            out.write(requestContent.getBytes());

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ProxyUtils.readResponse(socket);
    }
}
