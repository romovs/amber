package haven;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class TabStrip extends Widget {
    private static final int MAX_BUTTON_TEXT_LENGTH = 12;

    private final List<Button> buttons = new ArrayList<Button>();
    private Button selected;

    protected void selected(Button button) {}

    public int getSelectedButtonIndex() {
        return buttons.indexOf(selected);
    }

    public int getButtonCount() {
        return buttons.size();
    }

    public Button insert(int index, String text) {
        String tooltip = null;
        if (text.length() > MAX_BUTTON_TEXT_LENGTH) {
            tooltip = text; // preserve full text in tooltip
            text = text.substring(0, MAX_BUTTON_TEXT_LENGTH - 2) + "..";
        }
        final Button button = add(new Button(text) {
            public void click() {
                select(this);
            }
        });
        button.tooltip = tooltip;
        buttons.add(index, button);
        alignButtons();
        return button;
    }

    public void select(int buttonIndex) {
        select(buttons.get(buttonIndex));
    }

    public void select(Button button) {
        if (selected != button) {
            for (Button b : buttons) {
                b.setActive(b == button);
            }
            selected = button;
            selected(button);
        }
    }

    public Button remove(int buttonIndex) {
        Button button = buttons.remove(buttonIndex);
        button.destroy();
        alignButtons();
        return button;
    }

    public void remove(Button button) {
        if (buttons.remove(button)) {
            button.destroy();
            alignButtons();
        }
    }

    private void alignButtons() {
        int x = 0;
        for (Button button : buttons) {
            button.c = new Coord(x, 0);
            x += button.sz.x - 1;
        }
        pack();
    }

    public abstract static class Button extends Widget {
        public static final Coord padding = new Coord(5, 2);
        public static final Text.Foundry font = new Text.Foundry(Text.sans, 14).aa(true);
        private static final IBox frame = new IBox("hud/tab", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb");
        private static final Color bg = new Color(0, 0, 0, 128);
        private Text text;
        private boolean active;

        Button(String text) {
            this.text = font.render(text);
            resize(this.text.sz().add(padding.mul(2)));
        }

        public abstract void click();

        @Override
        public void draw(GOut g) {
            if (active) {
                g.chcolor(bg);
                g.frect(Coord.z, sz);
                g.chcolor();
            }
            frame.draw(g, Coord.z, sz);
            g.image(text.tex(), padding);
        }

        @Override
        public boolean mousedown(Coord c, int button) {
            if (button == 1) {
                click();
                return true;
            }
            return false;
        }

        void setActive(boolean value) {
            this.active = value;
        }
    }
}