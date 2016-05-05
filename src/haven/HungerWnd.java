package haven;

import java.awt.event.KeyEvent;

public class HungerWnd extends Window {

    public HungerWnd() {
        super(Coord.z, "You are hungry!");  
        add(new Label("You hear your stomach rumbling, gnawing pain in your guts reminding that you haven't had anything to eat for a while."), new Coord(20,10));
        add(new Label("Burning through energy when it’s below 1000% will result in starvation damage to your health, eventually death."), new Coord(20,30));
        add(new Button(60, "Close") {
            @Override
            public void click() {
                parent.reqdestroy();
            }
        }, new Coord(629/2-60, 50));
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