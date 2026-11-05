package com.miniserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RequestHandler implements Runnable {

    /** 静态资源根目录：绝对化并归一化，作为所有请求的访问边界。 */
    private static final Path STATIC_ROOT = Paths.get("static").toAbsolutePath().normalize();

    /** 请求根路径时返回的默认首页。 */
    private static final String INDEX_FILE = "index.html";

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

            // 3. 解析并校验目标路径，确保最终文件位于 static 根目录内
            Path target = resolveStaticFile(path);
            if (target == null) {
                // 路径非法（越界、编码损坏、空字节等），明确拒绝
                HttpResponse.sendError(output, 403, "Forbidden");
                return;
            }

            // 4. 读取文件并返回
            if (!Files.isRegularFile(target)) {
                HttpResponse.sendError(output, 404, "Not Found");
                return;
            }
            if (!isInsideStaticRoot(target)) {
                // 通过符号链接等方式逃逸出根目录
                HttpResponse.sendError(output, 403, "Forbidden");
                return;
            }

            byte[] content = Files.readAllBytes(target);
            String contentType = target.getFileName().toString().toLowerCase().endsWith(".html")
                    ? "text/html" : "text/plain";
            HttpResponse.sendSuccess(output, content, contentType);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * 把请求路径解析成 static 根目录内的文件路径。
     *
     * @return 归一化后的目标路径；返回 {@code null} 表示路径非法或试图越出根目录，调用方必须拒绝该请求。
     */
    private static Path resolveStaticFile(String rawPath) {
        String decoded;
        try {
            // 先转义 '+'，避免 URLDecoder 把它解码成空格（路径中的 '+' 是字面量）
            decoded = URLDecoder.decode(rawPath.replace("+", "%2B"), "UTF-8");
        } catch (UnsupportedEncodingException | IllegalArgumentException e) {
            // 非法的百分号编码
            return null;
        }

        if (decoded.indexOf('\0') >= 0) {
            return null; // 空字节截断，直接拒绝
        }

        // 去掉查询串/片段
        int cut = decoded.indexOf('?');
        int hash = decoded.indexOf('#');
        if (cut < 0 || (hash >= 0 && hash < cut)) cut = hash;
        String relative = cut >= 0 ? decoded.substring(0, cut) : decoded;

        // Windows 下 '\' 同样是路径分隔符，先统一成 '/'
        relative = relative.replace('\\', '/');

        if (relative.isEmpty() || "/".equals(relative)) {
            relative = "/" + INDEX_FILE;
        }

        // 去掉前导 '/'，保证按相对路径解析到 STATIC_ROOT 之下
        int start = 0;
        while (start < relative.length() && relative.charAt(start) == '/') start++;
        relative = relative.substring(start);
        if (relative.isEmpty()) relative = INDEX_FILE;

        try {
            Path candidate = STATIC_ROOT.resolve(relative).normalize();
            // 归一化后仍必须落在 static 根目录内，否则视为目录穿越
            return candidate.startsWith(STATIC_ROOT) ? candidate : null;
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /** 跟随符号链接后仍位于 static 根目录内，才允许读取。 */
    private static boolean isInsideStaticRoot(Path candidate) {
        try {
            Path root = STATIC_ROOT.toRealPath();
            return candidate.toRealPath().startsWith(root);
        } catch (IOException e) {
            return false;
        }
    }
}
