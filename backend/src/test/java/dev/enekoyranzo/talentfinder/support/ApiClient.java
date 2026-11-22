package dev.enekoyranzo.talentfinder.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Cliente HTTP de tests. Se comporta como un navegador: guarda las cookies que recibe y,
 * en las peticiones que modifican datos, copia la cookie XSRF-TOKEN a la cabecera X-XSRF-TOKEN.
 * Si el servidor borra una cookie (Max-Age=0 o fecha pasada), el cliente también la borra.
 */
public final class ApiClient {

    /** AUTO = como el frontend (cabecera correcta). NONE = sin cabecera. FAKE = cabecera falsa. */
    public enum Csrf { AUTO, NONE, FAKE }

    public record Response(int status, byte[] bytes, HttpHeaders headers) {

        public String body() {
            return new String(bytes, StandardCharsets.UTF_8);
        }

        public String header(String name) {
            return headers.firstValue(name).orElse(null);
        }

        public List<String> setCookies() {
            return headers.allValues("set-cookie");
        }

        /** Primer valor de un campo JSON simple (texto o número). null si no existe o es null. */
        public String field(String name) {
            Matcher m = Pattern.compile(
                            "\"" + Pattern.quote(name) + "\"\\s*:\\s*(\"((?:[^\"\\\\]|\\\\.)*)\"|[^,}\\]\\s]+)")
                    .matcher(body());
            if (!m.find()) {
                return null;
            }
            if (m.group(2) != null) {
                return m.group(2);
            }
            return "null".equals(m.group(1)) ? null : m.group(1);
        }

        public long id() {
            return Long.parseLong(field("id"));
        }

        /** Todos los valores numéricos de campos "id" en el cuerpo, en orden de aparición. */
        public List<Long> ids() {
            Matcher m = Pattern.compile("\"id\"\\s*:\\s*(\\d+)").matcher(body());
            List<Long> ids = new ArrayList<>();
            while (m.find()) {
                ids.add(Long.parseLong(m.group(1)));
            }
            return ids;
        }
    }

    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient http = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String base;
    private final Map<String, String> cookies = new LinkedHashMap<>();

    public ApiClient(String base) {
        this.base = base;
    }

    // -------------------------------------------------------------- cookies
    public String cookie(String name) {
        return cookies.get(name);
    }

    public void setCookie(String name, String value) {
        cookies.put(name, value);
    }

    // ------------------------------------------------------------- peticiones
    public Response get(String path) {
        return request("GET", path, null, null, Csrf.NONE, Map.of());
    }

    public Response post(String path) {
        return request("POST", path, null, null, Csrf.AUTO, Map.of());
    }

    public Response post(String path, Csrf csrf) {
        return request("POST", path, null, null, csrf, Map.of());
    }

    public Response postJson(String path, String json) {
        return postJson(path, json, Csrf.AUTO);
    }

    public Response postJson(String path, String json, Csrf csrf) {
        return request("POST", path, utf8(json), "application/json", csrf, Map.of());
    }

    public Response patchJson(String path, String json) {
        return patchJson(path, json, Csrf.AUTO);
    }

    public Response patchJson(String path, String json, Csrf csrf) {
        return request("PATCH", path, utf8(json), "application/json", csrf, Map.of());
    }

    public Response delete(String path) {
        return delete(path, Csrf.AUTO);
    }

    public Response delete(String path, Csrf csrf) {
        return request("DELETE", path, null, null, csrf, Map.of());
    }

    public Response options(String path, String origin, String method) {
        return request("OPTIONS", path, null, null, Csrf.NONE,
                Map.of("Origin", origin, "Access-Control-Request-Method", method));
    }

    public Response upload(String path, String filename, byte[] content, Long folderId) {
        return upload(path, filename, content, folderId, Csrf.AUTO);
    }

    public Response upload(String path, String filename, byte[] content, Long folderId, Csrf csrf) {
        String boundary = "----tf" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream out = new ByteArrayOutputStream(content.length + 512);
        try {
            out.write(utf8("--" + boundary + "\r\n"
                    + "Content-Disposition: form-data; name=\"file\"; filename=\""
                    + filename.replace("\"", "%22") + "\"\r\n"
                    + "Content-Type: application/pdf\r\n\r\n"));
            out.write(content);
            out.write(utf8("\r\n"));
            if (folderId != null) {
                out.write(utf8("--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"folderId\"\r\n\r\n"
                        + folderId + "\r\n"));
            }
            out.write(utf8("--" + boundary + "--\r\n"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return request("POST", path, out.toByteArray(),
                "multipart/form-data; boundary=" + boundary, csrf, Map.of());
    }

    public Response request(String method, String path, byte[] body, String contentType,
                            Csrf csrf, Map<String, String> headers) {
        boolean unsafe = !method.equals("GET") && !method.equals("HEAD") && !method.equals("OPTIONS");

        if (unsafe && csrf == Csrf.AUTO && !cookies.containsKey("XSRF-TOKEN")) {
            // Como el frontend: la primera respuesta del servidor entrega la cookie CSRF.
            get("/auth/me");
        }

        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).timeout(TIMEOUT);
        if (unsafe) {
            if (csrf == Csrf.AUTO && cookies.containsKey("XSRF-TOKEN")) {
                b.header("X-XSRF-TOKEN", cookies.get("XSRF-TOKEN"));
            } else if (csrf == Csrf.FAKE) {
                b.header("X-XSRF-TOKEN", "falso");
            }
        }
        if (!cookies.isEmpty()) {
            b.header("Cookie", cookies.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("; ")));
        }
        if (contentType != null) {
            b.header("Content-Type", contentType);
        }
        headers.forEach(b::header);
        b.method(method, body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofByteArray(body));

        try {
            HttpResponse<byte[]> r = http.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
            absorbCookies(r.headers());
            return new Response(r.statusCode(), r.body(), r.headers());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private void absorbCookies(HttpHeaders headers) {
        for (String setCookie : headers.allValues("set-cookie")) {
            String[] kv = setCookie.split(";")[0].trim().split("=", 2);
            String value = kv.length > 1 ? kv[1] : "";
            String lower = setCookie.toLowerCase();
            boolean expired = value.isEmpty() || lower.contains("max-age=0") || lower.contains("1970");
            if (expired) {
                cookies.remove(kv[0]);
            } else {
                cookies.put(kv[0], value);
            }
        }
    }

    // ------------------------------------------------------------------ JSON
    /** Construye un objeto JSON: obj("email", "a@b.c", "edad", 3, "activo", true, "x", null). */
    public static String obj(Object... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < kv.length; i += 2) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(quote((String) kv[i])).append(':');
            Object v = kv[i + 1];
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append(quote(v.toString()));
            }
        }
        return sb.append('}').toString();
    }

    private static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append('"').toString();
    }

    private static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}