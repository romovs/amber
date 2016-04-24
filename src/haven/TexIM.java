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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

public class TexIM extends TexI {
    WritableRaster buf;
    Graphics2D cg = null;
    Throwable cgc;

    public TexIM(Coord sz) {
        super(sz);
        clear();
    }

    public Graphics2D graphics() {
        if (cg != null)
            throw (new RuntimeException("Multiple TexIM Graphics created (" + Thread.currentThread().getName() + ")", cgc));
        cgc = new Throwable("Current Graphics created (on " + Thread.currentThread().getName() + ")");
        return (cg = back.createGraphics());
    }

    public void update() {
        cg.dispose();
        cg = null;
        dispose();
    }

    public void clear() {
        buf = Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, tdim.x, tdim.y, 4, null);
        back = new BufferedImage(glcm, buf, false, null);
    }
}
