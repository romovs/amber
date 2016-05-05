package haven;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONObject;

public class UpdateChecker extends Thread {
    private final String url = "https://api.github.com/repos/puruscor/purus-pasta/releases/latest";

    public void run() { 
    	
        try {
            JSONObject json = getjson();
            String latestver = json.getString("tag_name");
            if (isnewer(Config.version, latestver) && HavenPanel.lui != null && HavenPanel.lui.root != null) {
            	System.out.println("Client is not up to date, current client version: " + Config.version + " Newest client version: " + latestver);
                Window updwnd = new UpdateWnd(latestver);
                HavenPanel.lui.root.add(updwnd);
                updwnd.show();
                updwnd.raise();
            }
            else
            	System.out.println("Client is up to date, current client version: " + Config.version + " Newest client version: " + latestver);
        } catch (Exception e) {
            System.err.println("WARNING: error checking for updates");
            e.printStackTrace();
        }
        
    }

    private JSONObject getjson() throws IOException {
        BufferedReader rd = null;

        try {
            InputStream is = new URL(url).openStream();
            rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            return new JSONObject(sb.toString());
        } finally {
            if (rd != null) {
                rd.close();
            }
        }
    }

    private boolean isnewer(String currentver, String latestver) {
        String[] vtokc = currentver.split("[\\.]+");
        int majc = Integer.parseInt(vtokc[0]);
        int minc = Integer.parseInt(vtokc[1]);
        int ptcc = Integer.parseInt(vtokc[2]);
        String[] vtokl = latestver.split("[\\.]+");
        int majl = Integer.parseInt(vtokl[0]);
        int minl = Integer.parseInt(vtokl[1]);
        int ptcl = Integer.parseInt(vtokl[2]);
        return majl > majc || (minl > minc && majl >= majc) || (ptcl > ptcc && majl >= majc && minl >= minc);
    }
}
