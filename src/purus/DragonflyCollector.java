package purus;

import java.awt.Color;

import haven.Button;
import haven.Coord;
import haven.FlowerMenu;
import haven.Gob;
import haven.Inventory;
import haven.UI;
import haven.Widget;
import haven.Window;

public class DragonflyCollector {
	/* This script collects dragonfly with crawlin speed around swamp
	 * How it works plan:
	 * 1. Right click nearest dragonfly
	 * 2. Check if player is not moving or dragonfly is out of camera/collected to inventory
	 * 3. Check if inv is on hand (something in hand) if not then repeat
	 */

	private final UI ui;
    private haven.Widget w;
    private Inventory i;
    private Widget window;  
    
	BotUtils BotUtils;

	public DragonflyCollector (UI ui, Widget w, Inventory i) {
		this.ui = ui;
		this.w = w;
		this.i = i;
		BotUtils = new BotUtils(ui, w, i);
	}
	
	public void Run () {
		t.start();
		}
		Thread t = new Thread(new Runnable() {
		public void run()  {
			window = BotUtils.gui().add(new StatusWindow(), 300, 200);
			ui.root.findchild(FlowerMenu.class);
				BotUtils.setSpeed(0);
				while(BotUtils.getItemAtHand() == null) {
					if (!BotUtils.isMoving()) {
						Gob gob = BotUtils.findObjectByNames(1000, "gfx/kritter/dragonfly/dragonfly");
						if (gob != null)
						BotUtils.doClick(gob, 3, 0); 
						
					}
						sleep(1000);
				}
		        window.destroy();
		       t.stop();
			}
		});
		
	private void sleep(int t){
		try {
			Thread.sleep(t);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		}
	}

		// This thingy makes that stupid window with cancel button, TODO: make it better
		private class StatusWindow extends Window {
	        public StatusWindow() {
	            super(Coord.z, "Dragonfly Collector");
	            setLocal(true);
	            add(new Button(120, "Cancel") {
	                public void click() {
	                    window.destroy();
	                    if(t != null) {
	                    	gameui().msg("Dragonfly Collector Cancelled", Color.WHITE);
	                    	t.stop();
	                    }
	                }
	            });
	            pack();
	        }
	        public void wdgmsg(Widget sender, String msg, Object... args) {
	            if (sender == this && msg.equals("close")) {
	                t.stop();
	            }
	            super.wdgmsg(sender, msg, args);
	        }
		}
	}