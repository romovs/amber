/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.image.BufferedImage;

public class Curiosity extends ItemInfo.Tip {
    public final int exp, mw, enc;
    private GItem item;
    private CuriosityInfo customInfo;

    public Curiosity(Owner owner, int exp, int mw, int enc) {
        super(owner);
        this.exp = exp; // LP
        this.mw = mw; // Mental Weight
        this.enc = enc; // Experience Points
        if (owner instanceof GItem) {
        	item = (GItem)owner;
        }
    }

    public BufferedImage tipimg() {
        StringBuilder buf = new StringBuilder();
        buf.append(String.format(Resource.getLocString(Resource.l10nLabel, "Learning points: $col[192,192,255]{%s}\nMental weight: $col[255,192,255]{%d}\n"), Utils.thformat(exp), mw));
        if (enc > 0)
        	buf.append(String.format(Resource.getLocString(Resource.l10nLabel, "Experience cost: $col[255,255,192]{%d}\n"), enc));
        if (item != null && customInfo == null) {
            try { 
                String resName = item.resname();
                customInfo = CuriosityInfo.get(resName);
                if (customInfo == null)
                    customInfo = CuriosityInfo.empty;
            } catch (Loading e) {
                return null;
            }
        }
        if (customInfo != null && customInfo != CuriosityInfo.empty) {
            buf.append(String.format("Time: $col[192,192,255]{%s}\n", customInfo.getFormattedTime()));
            buf.append(String.format("LP/H/Slot: $col[255,192,255]{%.2f}\n", LPH(exp) / customInfo.slots));
            buf.append(String.format("LP/Exp: $col[255,255,192]{%.2f}\n", LPE(exp, enc)));
        }
        return(RichText.render(buf.toString(), 0).img);
    }
    
    public float LPE(int exp, int enc) {
    	// LP per experience point
            if (enc<=0)
    	    	// If no xp cost, dont divide
    	        return exp;
    	    else
            return exp / enc;
        }
    
    public float LPH(int exp){
        if (item != null && customInfo == null) {
            try { 
                String resName = item.resname();
                customInfo = CuriosityInfo.get(resName);
                if (customInfo == null)
                    customInfo = CuriosityInfo.empty;
            } catch (Loading e) {
            }
        }
        if (customInfo.time < 0)
        return 0;
        else 
            	return exp / (customInfo.time / 3600.0f);
         }
}
