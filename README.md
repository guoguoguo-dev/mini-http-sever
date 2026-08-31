# - 手写迷你HTTP服务器

## 📖 项目简介
纯Java实现的轻量级Web服务器，不依赖Spring/Tomcat等任何框架，仅用原生Socket和IO流完成HTTP协议解析与响应。

## 🛠️ 技术栈
- Java 8+ (OOP、多线程)
- 原生 java.net.Socket
- 线程池 (ExecutorService)

## 🚀 快速启动
```bash
javac -d . src/com/miniserver/*.java
java com.miniserver.Server
# 浏览器访问 http://localhost:8080
