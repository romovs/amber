package haven;

import java.awt.Color;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AttnMeter extends Widget {
    private static final Tex bg = Resource.loadtex("hud/meter/attention");

    private final CharWnd.StudyInfo study;

    public AttnMeter(CharWnd.StudyInfo study) {
        super(IMeter.fsz);
        this.study = study;
    }

    @Override
    public void draw(GOut g) {
        Coord isz = IMeter.msz;
        Coord off = IMeter.off;
        g.chcolor(0, 0, 0, 255);
        g.frect(off, isz);
        g.chcolor();

        study.upd();

        int w = isz.x;
        int c = study.tw;
        int t = ui.sess.glob.cattr.get("int").comp;

        if (c < t) {
            g.chcolor(Color.YELLOW);
        } else if (c == t) {
            g.chcolor(Color.GREEN);
        } else {
            g.chcolor(Color.RED);
        }

        g.frect(off, new Coord((int) Math.floor((float) c / t * w), isz.y));
        g.chcolor();
        g.image(bg, Coord.z);
    }

    static class CRec implements Comparable<Object> {
        public String name;
        public long time;
        public int attn;

        public CRec(String name, Long time, int attn) {
            this.name = name;
            this.time = time;
            this.attn = attn;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof CRec) {
                CRec that = (CRec) o;
                if (time != that.time) {
                    if (this.time < 0)
                        return 1;
                    else if (that.time < 0)
                        return -1;
                    else
                        return Long.compare(time, that.time);
                } else {
                    return name.compareTo(that.name);
                }
            }
            return 0;
        }
    }

    @Override
    public Object tooltip(Coord c, Widget prev) {
        long tt = System.currentTimeMillis();
        String curios = study.study.children(GItem.class).stream()
                .flatMap(gItem -> {
                    try {
                        Curiosity ci = ItemInfo.find(Curiosity.class, gItem.info());
                        ItemInfo.Name nm = ItemInfo.find(ItemInfo.Name.class, gItem.info());
                        if (ci != null && nm != null) {
                            return Stream.of(new CRec(nm.str.text, gItem.finishedTime, ci.mw));
                        }
                    } catch (Loading ignored) {
                    }
                    return Stream.empty();
                })
                .sorted()
                .map(cr -> String.format("%s %s (%d)", cr.time > tt ? Utils.timeLeft(cr.time) : "??:??:??", cr.name, cr.attn))
                .collect(Collectors.joining("\n"));
        return RichText.render(String.format("Attention: %d/%d\nXP Cost: %d\nLP Gain: %d\n\n" + curios, study.tw, ui.sess.glob.cattr.get("int").comp, study.tenc, study.texp), -1).tex();
    }
}