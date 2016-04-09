import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class Main {
    private static Map<String, String> baseTooltip, basePagina, baseWindow, baseButton, baseFlower, baseMsg, baseLabel, baseAction;
    private static Map<String, String> l10nTooltip, l10nPagina, l10nWindow, l10nButton, l10nFlower, l10nMsg, l10nLabel, l10nAction;
    private static final String BUNDLE_TOOLTIP = "tooltip";
    private static final String BUNDLE_PAGINA = "pagina";
    private static final String BUNDLE_WINDOW = "window";
    private static final String BUNDLE_BUTTON = "button";
    private static final String BUNDLE_FLOWER = "flower";
    private static final String BUNDLE_MSG = "msg";
    private static final String BUNDLE_LABEL = "label";
    private static final String BUNDLE_ACTION = "action";

    private static final String missing = "missing";
    private static final String extra = "extra";

    public static void main(String[] args) {
        if (args.length == 0 || args[0].length() != 2) {
            System.out.println("Usage: java -cp \".;l10n.jar\" Main LANGUAGE_CODE\n");
            System.exit(0);
        }

        System.out.println("Processing...");

        baseTooltip = load(BUNDLE_TOOLTIP, "en");
        basePagina = load(BUNDLE_PAGINA, "en");
        baseWindow = load(BUNDLE_WINDOW, "en");
        baseButton = load(BUNDLE_BUTTON, "en");
        baseFlower = load(BUNDLE_FLOWER, "en");
        baseMsg = load(BUNDLE_MSG, "en");
        baseLabel = load(BUNDLE_LABEL, "en");
        baseAction = load(BUNDLE_ACTION, "en");

        l10nTooltip = load(BUNDLE_TOOLTIP, args[0]);
        l10nPagina = load(BUNDLE_PAGINA, args[0]);
        l10nWindow = load(BUNDLE_WINDOW, args[0]);
        l10nButton = load(BUNDLE_BUTTON, args[0]);
        l10nFlower = load(BUNDLE_FLOWER, args[0]);
        l10nMsg = load(BUNDLE_MSG, args[0]);
        l10nLabel = load(BUNDLE_LABEL, args[0]);
        l10nAction = load(BUNDLE_ACTION, args[0]);

        diff(baseTooltip, l10nTooltip);
        diff(basePagina, l10nPagina);
        diff(baseWindow, l10nWindow);
        diff(baseButton, l10nButton);
        diff(baseFlower, l10nFlower);
        diff(baseMsg, l10nMsg);
        diff(baseLabel, l10nLabel);
        diff(baseAction, l10nAction);

        dump(baseTooltip, BUNDLE_TOOLTIP, missing);
        dump(basePagina, BUNDLE_PAGINA, missing);
        dump(baseWindow, BUNDLE_WINDOW, missing);
        dump(baseButton, BUNDLE_BUTTON, missing);
        dump(baseFlower, BUNDLE_FLOWER, missing);
        dump(baseMsg, BUNDLE_MSG, missing);
        dump(baseLabel, BUNDLE_LABEL, missing);
        dump(baseAction, BUNDLE_ACTION, missing);

        dump(l10nTooltip, BUNDLE_TOOLTIP, extra);
        dump(l10nPagina, BUNDLE_PAGINA, extra);
        dump(l10nWindow, BUNDLE_WINDOW, extra);
        dump(l10nButton, BUNDLE_BUTTON, extra);
        dump(l10nFlower, BUNDLE_FLOWER, extra);
        dump(l10nMsg, BUNDLE_MSG, extra);
        dump(l10nLabel, BUNDLE_LABEL, extra);
        dump(l10nAction, BUNDLE_ACTION, extra);

        System.out.println("Done");
    }

    private static void dump(Map<String, String> base, String bundle, String file) {
        if (base == null || base.size() == 0)
            return;

        BufferedWriter out = null;
        try {
            CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
            encoder.onMalformedInput(CodingErrorAction.REPORT);
            encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(bundle + "_" + file + ".properties", true), encoder));
            for (String key : base.keySet()) {
                String val = base.get(key);
                key = key.replace(" ", "\\ ").replace(":", "\\:").replace("=", "\\=");
                val = val.replace("\\", "\\\\").replace("\n", "\\n").replace("\u0000", "");
                out.write(key + " = " + val);
                out.newLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void diff(Map<String, String> base, Map<String, String> loc) {
        if (base == null || loc == null)
            return;
        for (Iterator<Map.Entry<String, String>> it = base.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<String, String> entry = it.next();
            if (loc.containsKey(entry.getKey())) {
                it.remove();
                loc.remove(entry.getKey());
            }
        }
    }

    private static Map<String, String> load(String bundle, String langcode) {
        Properties props = new Properties();

        InputStream is = Main.class.getClassLoader().getResourceAsStream("l10n/" + bundle + "_" + langcode + ".properties");
        if (is == null)
            return null;

        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(is, "UTF-8");
            props.load(isr);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException e) { // ignored
                }
            }
        }

        return props.size() > 0 ? new HashMap<>((Map) props) : null;
    }
}
