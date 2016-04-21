package haven;

import javax.net.ssl.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatusWdg extends Widget {

    public static String username;
    public static String pass;

    public static boolean AlreadyExecuted = false;
    
    private static ThreadGroup tg = new ThreadGroup("StatusUpdaterThreadGroup");
    private static final String statusupdaterthreadname = "StatusUpdater";

    private static final Tex hearthlingsplayingdef = Text.render(String.format(Resource.getLocString(Resource.l10nLabel, "Players: %s"), "?"), Color.WHITE).tex();
    private static final Tex pingtimedef = Text.render(String.format(Resource.getLocString(Resource.l10nLabel, "Ping: %s ms"), "?"), Color.WHITE).tex();
    private static final Tex accountstatusdef = Text.render("Account: ?", Color.WHITE).tex();

    private Tex hearthlingsplaying = hearthlingsplayingdef;
    private Tex pingtime = pingtimedef;
    private Tex accountstatus = accountstatusdef;

    private static final int RETRY_COUNT = 6;
    private int retries;

    private static SSLSocketFactory sslfactory;

    static {
        InputStream crt = null;
        try {
            KeyStore keystore  = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(null);
            crt = Resource.class.getResourceAsStream("websrv.crt");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            keystore.setCertificateEntry("havenandhearth", cf.generateCertificate(crt));
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(keystore);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);
            sslfactory = ctx.getSocketFactory();
        } catch (CertificateException ce) {
            ce.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        } catch (KeyStoreException kse) {
            kse.printStackTrace();
        } catch (KeyManagementException kme) {
            kme.printStackTrace();
        } finally {
            try {
                if (crt != null)
                    crt.close();
            } catch (IOException ioe) {
            }
        }
    }

    public StatusWdg() {
        retries = 0;
        synchronized (StatusWdg.class) {
            tg.interrupt();
            startupdaterthread();
        }
    }

    private String removehtmltags(String input) {
        return input.replaceAll("\\<[^>]*>", "");
    }

    private void updatehearthlingscount() {
        String hearthlingscount = "?";

        String mainpagecontent = geturlcontent("https://www.havenandhearth.com/portal/");
        if (!mainpagecontent.isEmpty())
            hearthlingscount = getstringbetween(mainpagecontent, "There are", "hearthlings playing").trim();

        if (hearthlingscount.isEmpty())
            hearthlingscount = "?";

        synchronized (StatusWdg.class) {
            hearthlingsplaying = Text.render(String.format(Resource.getLocString(Resource.l10nLabel, "Players: %s"), hearthlingscount), Color.WHITE).tex();
        }
    }

    private void updatepingtime() {
        String ping = "?";

        java.util.List<String> command = new ArrayList<>();
        command.add("ping");
        command.add(Config.iswindows ? "-n" : "-c");
        command.add("1");
        command.add("game.havenandhearth.com");

        BufferedReader standardOutput = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            standardOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String output = "";
            String line;
            while ((line = standardOutput.readLine()) != null) {
                output += line;
            }

            Pattern pattern;
            if (Config.iswindows) {
                // Reply from 87.245.198.59: bytes=32 time=2ms TTL=53
                pattern = Pattern.compile(".+?=32 .+?=(\\d+).*? TTL=.+");
            } else {
                // 64 bytes from ansgar.seatribe.se (213.239.201.139): icmp_seq=1 ttl=47 time=71.4 ms
                pattern = Pattern.compile(".+?time=(\\d+\\.?\\d*) ms");
            }
            Matcher matcher = pattern.matcher(output);
            while (matcher.find()) {
                ping = matcher.group(1);
            }
        } catch (IOException ex) {
            // NOP
        } finally {
            if (standardOutput != null)
                try {
                    standardOutput.close();
                } catch (IOException e) { // ignored
                }
        }

        if (ping.isEmpty()) {
            ping = "?";
        }

        synchronized (this) {
            pingtime = Text.render(String.format(Resource.getLocString(Resource.l10nLabel, "Ping: %s ms"), ping), Color.WHITE).tex();
        }
    }

    private boolean mklogin() {
        if (sslfactory == null || username == null || pass == null || "".equals(username))
            return false;
        if (retries++ > RETRY_COUNT)
            return false;

        try {
            URL url = new URL("https://www.havenandhearth.com/portal/sec/login");
            HttpsURLConnection conn = (HttpsURLConnection)url.openConnection();
            conn.setSSLSocketFactory(sslfactory);

            Map<String,Object> params = new LinkedHashMap<>();
            params.put("username", username);
            params.put("password", pass);

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
                return false;
        } catch (IOException ex) {
            return false;
        }

        return true;
    }

    private void updateaccountstatus() {
        String status = "?";

        int retriescount = 0;
        while (retriescount < 2) {
            String profilepagecontent = geturlcontent("https://www.havenandhearth.com/portal/profile");
            status = removehtmltags(getstringbetween(profilepagecontent, "Account status:", "(All times")).trim();
            if (status.isEmpty()) {
                mklogin();
                ++retriescount;
                continue;
            }

            break;
        }

        if (status.isEmpty())
            status = "?";

        synchronized (StatusWdg.class) {
            accountstatus = Text.render(String.format("Account: %s", status), Color.WHITE).tex();
        }
    }

    private void startupdaterthread() {
        Thread statusupdaterthread = new Thread(tg, new Runnable() {
            public void run() {
                CookieHandler.setDefault(new CookieManager());

                /*if (visible) {
                    mklogin();
                    if (Thread.interrupted())
                        return;
                }*/

                while (true) {
                    if (visible) {
                        updatehearthlingscount();
                        if (Thread.interrupted())
                            return;

                        updatepingtime();
                        if (Thread.interrupted())
                            return;

                        updateaccountstatus();
                        if (Thread.interrupted())
                            return;
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
            }
        }, statusupdaterthreadname);
        statusupdaterthread.start();
    }

    private static String getstringbetween(String input, String leftdelimiter, String rightdelimiter) {
        int leftdelimiterposition = input.indexOf(leftdelimiter);
        if (leftdelimiterposition == -1)
            return "";

        int rightdelimiterposition = input.indexOf(rightdelimiter);
        if (rightdelimiterposition == -1)
            return "";

        return input.substring(leftdelimiterposition + leftdelimiter.length(), rightdelimiterposition);
    }

    private String geturlcontent(String url) {
        if (sslfactory == null)
            return "";
        URL url_;
        BufferedReader br = null;
        String urlcontent = "";

        try {
            url_ = new URL(url);
            HttpsURLConnection conn = (HttpsURLConnection)url_.openConnection();
            conn.setSSLSocketFactory(sslfactory);
            InputStream is = conn.getInputStream();
            br = new BufferedReader(new InputStreamReader(is));

            String line;
            while ((line = br.readLine()) != null) {
                urlcontent += line;
            }
        } catch (SocketException se) {
            // don't print socket exceptions when network is unreachable to prevent console spamming on bad connections
            if (!se.getMessage().equals("Network is unreachable"))
                se.printStackTrace();
            return "";
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
            return "";
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return "";
        } finally {
            try {
                if (br != null)
                    br.close();
            } catch (IOException ioe) {
                // NOP
            }
        }

        return urlcontent;
    }

    @Override
    public void draw(GOut g) {
        synchronized (StatusWdg.class) {
            java.util.List<Tex> texturesToDisplay = new ArrayList<>(3);
            texturesToDisplay.add(this.hearthlingsplaying);
            texturesToDisplay.add(this.pingtime);
            texturesToDisplay.add(this.accountstatus);

            int requiredwidth = 0;
            int requiredheight = 0;
            int y = 0;
            for (Tex textodisplay : texturesToDisplay) {
                g.image(textodisplay, new Coord(0, y));

                if (textodisplay.sz().x > requiredwidth)
                    requiredwidth = textodisplay.sz().x;
                y += textodisplay.sz().y;
            }

            requiredheight = y;
            this.sz = new Coord(requiredwidth, requiredheight);
        }
    }

    @Override
    public void hide() {
        hearthlingsplaying = hearthlingsplayingdef;
        pingtime = pingtimedef;
        accountstatus = accountstatusdef;
        super.hide();
    }
}
