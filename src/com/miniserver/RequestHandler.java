package com.miniserver;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RequestHandler implements Runnable {
    private final Socket socket;

    public RequestHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (InputStream input = socket.getInputStream();
             OutputStream output = socket.getOutputStream()) {
            // 1. 解析请求行 (GET /index.html HTTP/1.1)
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] parts = requestLine.split(" ");
            if (parts.length < 2) return;

            String method = parts[0];
            String path = parts[1];

            // 2. 只支持GET，其他返回405
            if (!"GET".equalsIgnoreCase(method)) {
                HttpResponse.sendError(output, 405, "Method Not Allowed");
                return;
            }

            // 3. 处理路径（默认首页）
            if (path.equals("/")) path = "/index.html";
            String filePath = "./static" + path;

            // 4. 读取文件并返回
            File file = new File(filePath);
            if (file.exists() && !file.isDirectory()) {
                byte[] content = Files.readAllBytes(Paths.get(filePath));
                String contentType = path.endsWith(".html") ? "text/html" : "text/plain";
                HttpResponse.sendSuccess(output, content, contentType);
            } else {
                HttpResponse.sendError(output, 404, "Not Found");
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
