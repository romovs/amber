package haven;

import java.io.*;

import org.jsoup.*;
import org.jsoup.nodes.*;
import org.jsoup.select.*;

public class StudyTimes {
    private static final int CONN_RETRIES = 6;

    public static void updatestudytimes() {
        Document doc = null;
        for (int i = 0; i < CONN_RETRIES; i++) {
            try {
                doc = Jsoup.connect("http://ringofbrodgar.com/wiki/Curiosity").get();
                break;
            } catch (IOException ioe) {
                try {
                    // Give some time to fix possible internet connection's errors
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                    return;
                }
            }
        }

        if (doc != null) {
            try {
                Element allcuriostable = doc.select("table").get(0);
                Elements rows = allcuriostable.select("tr");
                for (int i = 1; i < rows.size(); ++i) {
                    try {
                        Element row = rows.get(i);
                        Elements cols = row.select("td");

                        String name = cols.get(0).text();
                        String studytime = cols.get(3).text();
                        if (name.isEmpty() || studytime.isEmpty()) {
                            continue;
                        }

                        Utils.setprefd("curio." + name.toLowerCase(), Double.valueOf(studytime));
                    } catch (Exception ex) {
                        continue;
                    }
                }
            } catch (Exception ex) {
                // Invalid page received
                return;
            }
        }
    }

    public static double getstudytime(String curioname) {
        return Utils.getprefd("curio." + curioname.toLowerCase(), 0.0);
    }
}
