package com.meik2333;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/*")
public class ProxyPass extends HttpServlet {
    private final String proxy_addr = "127.0.0.1";
    //private final String proxy_addr = "172.7.32.35";
    private final Integer proxy_port = 8000;

    public ProxyPass() {
        super();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ProxyClass proxy = ProxyUtils.proxyRequest(request, proxy_addr, proxy_port, request.getRequestURI());
        request.setCharacterEncoding("utf-8");
        response.setContentType("text/html;charset=utf-8");

        Set<Entry<String, String>> entrys = proxy.getHeader().entrySet();
        entrys.forEach(entry -> {
            response.setHeader(entry.getKey(), entry.getValue());
        });
        response.setHeader("Server", "Servlet");
        response.setStatus(Integer.parseInt(proxy.getStatus()));

        if (proxy.getStatus().equals("304")) {
            return;
        }
        else if (ProxyUtils.isDoc(proxy.getHeader().get("Content-Type"))) {
            response.getWriter().write(proxy.getBody());
        } else {
            response.getOutputStream().write(proxy.getByteBody());
        }
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

}
