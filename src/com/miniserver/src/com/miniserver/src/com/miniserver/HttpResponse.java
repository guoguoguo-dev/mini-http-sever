package com.miniserver;

import java.io.IOException;
import java.io.OutputStream;

public class HttpResponse {
    public static void sendSuccess(OutputStream out, byte[] content, String contentType) throws IOException {
        String statusLine = "HTTP/1.1 200 OK\r\n";
        String headers = "Content-Type: " + contentType + "\r\n" +
                         "Content-Length: " + content.length + "\r\n" +
                         "Connection: close\r\n\r\n";
        out.write(statusLine.getBytes());
        out.write(headers.getBytes());
        out.write(content);
        out.flush();
    }

    public static void sendError(OutputStream out, int code, String message) throws IOException {
        String body = "<h1>" + code + " " + message + "</h1><hr><p>MiniWebServer</p>";
        String response = "HTTP/1.1 " + code + " " + message + "\r\n" +
                          "Content-Type: text/html\r\n" +
                          "Content-Length: " + body.length() + "\r\n" +
                          "Connection: close\r\n\r\n" + body;
        out.write(response.getBytes());
        out.flush();
    }
}
