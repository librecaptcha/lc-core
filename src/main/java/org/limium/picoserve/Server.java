// Distributed under Apache 2 license
// Copyright 2021 github.com/hrj

package org.limium.picoserve;

import java.net.InetSocketAddress;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public final class Server {
  private final HttpServer server;

  public static interface Response {
    public int getCode();
    public byte[] getBytes();
    public Map<String, List<String>> getResponseHeaders();
  }

  public static class ByteResponse implements Response {
    private final int code;
    private final byte[] bytes;
    private final Map<String, List<String>> responseHeaders;

    public ByteResponse(final int code, final byte[] bytes) {
      this.code = code;
      this.bytes = bytes;
      this.responseHeaders = null;
    }

    public ByteResponse(final int code, final byte[] bytes, final Map<String, List<String>> responseHeaders) {
      this.code = code;
      this.bytes = bytes;
      this.responseHeaders = responseHeaders;
    }

    public int getCode() { return this.code; }
    public byte[] getBytes() { return this.bytes; }
    public Map<String, List<String>> getResponseHeaders() {
      return this.responseHeaders;
    }
  }

  public static class StringResponse extends ByteResponse {
    public StringResponse(final int code, final String msg) {
      super(code, msg.getBytes());
    }

    public StringResponse(final int code, final String msg, final Map<String, List<String>> responseHeaders) {
      super(code, msg.getBytes(), responseHeaders);
    }
  }

  public final class Request {
    final HttpExchange exchange;
    Request(final HttpExchange exchange) {
      this.exchange = exchange;
    }
    public String getMethod() {
      return exchange.getRequestMethod();
    }
    public Map<String, List<String>> getQueryParams() {
      final var query = exchange.getRequestURI().getQuery();
      final var params = parseParams(query);
      return params;
    }
    public byte[] getBody() {
      try(final var bodyIS = exchange.getRequestBody()) {
        final var bytes = bodyIS.readAllBytes();
        bodyIS.close();
        return bytes;
      } catch (IOException ioe) {
        return null;
      }
    }
    public String getBodyString() {
      return new String(getBody());
    }
  }

  @FunctionalInterface
  public static interface Processor {
    public Response process(final Request request);
  }

  public static class Handler {
    public final String path;
    public final Processor processor;
    public final String[] methods;
    public Handler(final String path, final Processor processor) {
      this.path = path;
      this.processor = processor;
      this.methods = new String[] {};
    }
    public Handler(final String path, final String methods, final Processor processor) {
      this.path = path;
      this.processor = processor;
      this.methods = methods.split(",");
    }
  }

  public Server(final InetSocketAddress addr, final int backlog, final List<Handler> handlers, final Executor executor) throws IOException {
    this.server = HttpServer.create(addr, backlog);
    this.server.setExecutor(executor);
    for (final var handler: handlers) {
      // System.out.println("Registering handler for " + handler.path);
      this.server.createContext(handler.path, new HttpHandler() {
        public void handle(final HttpExchange exchange) {
          final var method = exchange.getRequestMethod();
          final Response errorResponse = checkMethods(handler.methods, method);
          try(final var os = exchange.getResponseBody()) {
            Response response;
            if (errorResponse != null) {
              response = errorResponse;
            } else {
              try {
                response = handler.processor.process(new Request(exchange));
              } catch (final Exception e) {
                e.printStackTrace();
                response = new StringResponse(500, "Error: " + e);
              }
            }
            final var headersToSend = response.getResponseHeaders();
            if (headersToSend != null) {
              final var responseHeaders = exchange.getResponseHeaders();
              responseHeaders.putAll(headersToSend);
            }
            final var bytes = response.getBytes();
            final var code = response.getCode();
            exchange.sendResponseHeaders(code, bytes.length);
            os.write(bytes);
            os.close();
          } catch (IOException ioe) {
            System.out.println("Error: " + ioe);
          }
        }
      });
    }
  }

  public static Response checkMethods(final String[] methods, final String method) {
    if (methods.length > 0) {
      var found = false;
      for (var m: methods) {
        if (m.equals(method)) {
          found = true;
          break;
        }
      }
      if (!found) {
        return new StringResponse(404, "Method Not Accepted");
      }
    }
    return null;
  }

  public void start() {
    this.server.start();
  }

  public void stop(int delay) {
    this.server.stop(delay);
  }

  public static ServerBuilder builder() {
    return new ServerBuilder();
  }

  // Adapted from https://stackoverflow.com/a/37368660
  private final static Pattern ampersandPattern = Pattern.compile("&");
  private final static Pattern equalPattern = Pattern.compile("=");
  private final static Map<String, List<String>> emptyMap = Map.of();
  private static Map<String, List<String>> parseParams(final String query) {
    if (query == null) {
      return emptyMap;
    }
    final var params = ampersandPattern
      .splitAsStream(query)
      .map(s -> Arrays.copyOf(equalPattern.split(s, 2), 2))
      .collect(Collectors.groupingBy(s -> decode(s[0]), Collectors.mapping(s -> decode(s[1]), Collectors.toList())));
    return params;
  }

  private static String decode(final String encoded) {
    return Optional.ofNullable(encoded)
                   .map(e -> URLDecoder.decode(e, StandardCharsets.UTF_8))
                   .orElse(null);
  }

  public static class ServerBuilder {
    private InetSocketAddress mAddress = new InetSocketAddress(9000);
    private int backlog = 5;
    private List<Handler> handlers = new LinkedList<Handler>();
    private Executor executor = null;

    public ServerBuilder port(final int port) {
      mAddress = new InetSocketAddress(port);
      return this;
    }
    public ServerBuilder backlog(final int backlog) {
      this.backlog = backlog;
      return this;
    }
    public ServerBuilder address(final InetSocketAddress addr) {
      mAddress = addr;
      return this;
    }
    public ServerBuilder handle(final Handler handler) {
      handlers.add(handler);
      return this;
    }
    public ServerBuilder GET(final String path, final Processor processor) {
      handlers.add(new Handler(path, "GET", request -> processor.process(request)));
      return this;
    }
    public ServerBuilder POST(final String path, final Processor processor) {
      handlers.add(new Handler(path, "POST", request -> processor.process(request)));
      return this;
    }
    public ServerBuilder PUT(final String path, final Processor processor) {
      handlers.add(new Handler(path, "PUT", request -> processor.process(request)));
      return this;
    }
    public ServerBuilder DELETE(final String path, final Processor processor) {
      handlers.add(new Handler(path, "DELETE", request -> processor.process(request)));
      return this;
    }
    public ServerBuilder HEAD(final String path, final Processor processor) {
      handlers.add(new Handler(path, "HEAD", request -> processor.process(request)));
      return this;
    }
    public ServerBuilder executor(final Executor executor) {
      this.executor = executor;
      return this;
    }
    public Server build() throws IOException {
      return new Server(mAddress, backlog, handlers, executor);
    }
  }
}
