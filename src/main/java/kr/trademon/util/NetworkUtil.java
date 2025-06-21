package kr.trademon.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@Slf4j
public class NetworkUtil {

    // GET 요청 (헤더 없음)
    public static String get(String apiUrl) {
        return get(apiUrl, null);
    }

    // GET 요청 (헤더 있음)
    public static String get(String apiUrl, @Nullable Map<String, String> requestHeaders) {
        HttpURLConnection con = connect(apiUrl);

        try {
            con.setRequestMethod("GET");
            setHeaders(con, requestHeaders);
            return handleResponse(con);
        } catch (IOException e) {
            throw new RuntimeException("GET 요청 실패: " + apiUrl, e);
        } finally {
            con.disconnect();
        }
    }

    // POST 요청
    public static String post(String apiUrl, @Nullable Map<String, String> requestHeaders, String postParams) {
        HttpURLConnection con = connect(apiUrl);

        try {
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            setHeaders(con, requestHeaders);

            try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                wr.write(postParams.getBytes());
                wr.flush();
            }

            return handleResponse(con);
        } catch (IOException e) {
            throw new RuntimeException("POST 요청 실패: " + apiUrl, e);
        } finally {
            con.disconnect();
        }
    }

    // 연결 생성
    private static HttpURLConnection connect(String apiUrl) {
        try {
            return (HttpURLConnection) new URL(apiUrl).openConnection();
        } catch (IOException e) {
            throw new RuntimeException("URL 연결 실패: " + apiUrl, e);
        }
    }

    // 헤더 설정
    private static void setHeaders(HttpURLConnection con, @Nullable Map<String, String> headers) {
        if (headers != null) {
            headers.forEach(con::setRequestProperty);
        }
    }

    // 응답 처리
    private static String handleResponse(HttpURLConnection con) throws IOException {
        int responseCode = con.getResponseCode();
        InputStream stream = (responseCode == HttpURLConnection.HTTP_OK) ? con.getInputStream() : con.getErrorStream();
        return readBody(stream);
    }

    // 응답 본문 읽기
    private static String readBody(InputStream body) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (IOException e) {
            throw new RuntimeException("응답 읽기 실패", e);
        }
    }
}
