package com.meik2333;

import java.util.HashMap;

public class ProxyClass {
    private String status;
    private String body;
    private byte[] byteBody;

    public byte[] getByteBody() {
        return byteBody;
    }

    public void setByteBody(byte[] byteBody) {
        this.byteBody = byteBody;
    }

    private HashMap<String, String> header;

    public ProxyClass() {
        header = new HashMap<String, String>();
        body = "";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getBody() {
        return body;
    }

    public HashMap<String, String> getHeader() {
        return header;
    }

    public String getStatus() {
        return status;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setHeader(HashMap<String, String> header) {
        this.header = header;
    }

    public void putHeader(String key, String value) {
        this.header.put(key, value);
    }

}
