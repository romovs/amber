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

    private static String apikey = "trnsl.1.1.20151011T100257Z.56e6087f23957f5d.309d4ea2aae34ee07f33dde22949c8c3cff5c34a";

    private static String makepostrequest(String url, Map<String, Object> params) {
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

            int responsecode = conn.getResponseCode();
            if (responsecode != HttpsURLConnection.HTTP_OK)
                return "";

            is = conn.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            String line;
            while ((line = br.readLine()) != null) {
                responsebody.append(line);
            }

            return responsebody.toString();
        } catch (Exception ex) {
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException ioe) {
            }
        }

        return "";
    }

    public static String translate(String text, String destinationlanguage) {
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("key", apikey);
        params.put("lang", destinationlanguage);
        params.put("text", text);
        params.put("options", 1);
        String response = makepostrequest("https://translate.yandex.net/api/v1.5/tr.json/translate", params);
        if (response.isEmpty()) {
            return "";
        }

        try {
            JSONObject responsejson = new JSONObject(response);
            int code = responsejson.getInt("code");
            if (code != 200) {
                return "";
            }

            return responsejson.getJSONArray("text").getString(0);
        } catch (Exception ex) {
        }

        return "";
    }
}
