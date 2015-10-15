package haven;

import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class Translator {
    public enum Language {
        ENGLISH("en"),
        RUSSIAN("ru");

        private final String text;

        private Language(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    public static class Response {
        public String translatedtext = "";
        public int code = -1;
        public String message = "Unknown reason";
    }

    private static String makepostrequest(String url, Map<String, Object> params) throws IOException {
        StringBuilder responsebody = new StringBuilder();
        InputStream is = null;

        try {
            URL url_ = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection)url_.openConnection();

            StringBuilder postdata = new StringBuilder();
            for (Map.Entry<String,Object> param : params.entrySet()) {
                if (postdata.length() != 0)
                    postdata.append('&');
                postdata.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postdata.append('=');
                postdata.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postdata.toString().length()));

            conn.setDoOutput(true);
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(postdata.toString());
            wr.flush();
            wr.close();

            // Hack to force HttpsURLConnection to run the request
            // Otherwise getErrorStream always returns null
            conn.getResponseCode();
            is = conn.getErrorStream();
            if (is == null) {
                is = conn.getInputStream();
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            String line;
            while ((line = br.readLine()) != null) {
                responsebody.append(line);
            }

            return responsebody.toString();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ioe) {
            }
        }
    }

    public static Response translate(String apikey, String text, String destinationlanguage) {
        Response yandexresponse = new Response();

        try {
            Map<String,Object> params = new LinkedHashMap<>();
            params.put("key", apikey);
            params.put("lang", destinationlanguage);
            params.put("text", text);
            params.put("options", 1);
            String response = makepostrequest("https://translate.yandex.net/api/v1.5/tr.json/translate", params);
            if (response.isEmpty()) {
                return yandexresponse;
            }

            JSONObject responsejson = new JSONObject(response);
            yandexresponse.code = responsejson.getInt("code");
            if (yandexresponse.code == 200) {
                yandexresponse.translatedtext = responsejson.getJSONArray("text").getString(0);
            } else {
                yandexresponse.message = responsejson.getString("message");
            }
        } catch (Exception ex) {
            yandexresponse.message = ex.getMessage();
        }

        return yandexresponse;
    }
}
