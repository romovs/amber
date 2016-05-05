package haven;

import java.awt.Desktop;
import java.awt.event.KeyEvent;
import java.net.URI;

public class UpdateWnd extends Window {
    private static final String dwnurl = "https://github.com/puruscor/purus-pasta/releases/download/";

    public UpdateWnd(final String version) {
        super(Coord.z, "Update");
        add(new Label("New update is available - v" + version), new Coord(20, 40));
        add(new Button(200, "Download Update") {
            public void click() {
                Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                    try {
                        desktop.browse(new URI(dwnurl + version + "/update.zip"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, new Coord(0, 80));
        add(new Button(200, "Full Client Download") {
            public void click() {
                Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                    try {
                        desktop.browse(new URI(dwnurl + version + "/build.zip"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, new Coord(0, 112));
        add(new Button(200, "Automatic Launcher Download") {
            public void click() {
                Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
                if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                    try {
                        desktop.browse(new URI("https://github.com/puruscor/Pasta-Launcher/releases/latest"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }, new Coord(0, 145));
        pack();
        this.c = new Coord(HavenPanel.w/2-sz.x/2, HavenPanel.h/2-sz.y/2);
    }

    @Override
    public void wdgmsg(Widget sender, String msg, Object... args) {
        if (sender == cbtn) {
            ui.destroy(this);
        } else {
            super.wdgmsg(sender, msg, args);
        }
    }

    @Override
    public boolean type(char key, KeyEvent ev) {
        if(key == KeyEvent.VK_ESCAPE) {
            wdgmsg(cbtn, "click");
            return(true);
        }
        return(super.type(key, ev));
    }
}