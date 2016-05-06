package purus;

import java.awt.Color;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import haven.Button;
import haven.Coord;
import haven.Inventory;
import haven.TextEntry;
import haven.UI;
import haven.Widget;
import haven.Window;

public class KoordMaker extends Thread {
    
	private BotUtils BotUtils;
    private Widget window;  

	private BufferedWriter out;
	
    private String filename;
    private Coord startCoord;
    private ArrayList<Coord> route = new ArrayList<Coord>();

	public KoordMaker(UI ui, Widget w, Inventory i) {
		BotUtils = new BotUtils(ui, w, i);
	}
	
	public void run() {
		BotUtils.sysMsg("Started koordmaker", Color.WHITE);
		window = BotUtils.gui().add(new StatusWindow(), 300, 500);
		startCoord = new Coord(getX(), getY());
	}
	
	public void addWaypoint() {
		int xDiff = startCoord.x - getX();
		int yDiff = startCoord.y - getY();
		route.add(new Coord(xDiff, yDiff));
	}
	
	public void saveRoute() {
		try {
			new File("scripts").mkdir();
			FileWriter fw = new FileWriter(new File("scripts/" + filename + ".pbot"));
			out = new BufferedWriter(fw);
		for(int i = 0; i < route.size(); i++) {
			Coord c = route.get(i);
			out.write(c.x + " " + c.y);
			out.newLine();
		}
		out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public int getX() {
		return (int)BotUtils.player().rc.x;
	}
	
	public int getY() {
		return (int)BotUtils.player().rc.y;
	}
	
	private class StatusWindow extends Window {
        public StatusWindow() {
            super(Coord.z, "KoordMaker");
            setLocal(true);
            int y = 0;
            add(new TextEntry(120, "Enter Filename") {
                {dshow = true;}
                public void activate(String text) {
                    filename = text;
                }
            }, new Coord(0, y));
            y += 25;
            add(new Button(120, "Set Coord") {
                public void click() {
                		KoordMaker.this.addWaypoint();
                    	gameui().msg("Added current position into the route", Color.WHITE); 	
                }
            }, new Coord(0, y));
            y += 35;
            add(new Button(120, "Save") {
                public void click() {
                   window.destroy();
                   KoordMaker.this.saveRoute();
                   gameui().msg("Coordinates saved as " + filename + ".pbot", Color.WHITE);
                }
            }, new Coord(0, y));
            pack();
        }
        public void wdgmsg(Widget sender, String msg, Object... args) {
            if (sender == this && msg.equals("close")) {
            	gameui().msg("Koord Maker cancelled", Color.WHITE);
            	window.destroy();
            }
            super.wdgmsg(sender, msg, args);
        }
	}
}