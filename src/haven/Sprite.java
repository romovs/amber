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

import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public abstract class Sprite implements Rendered {
    public static final int GOB_HEALTH_ID = -1001;
    public static final int GROWTH_STAGE_ID = -1002;
    public final Resource res;
    public final Owner owner;
    public static List<Factory> factories = new LinkedList<Factory>();

    static {
        factories.add(SpriteLink.sfact);
        factories.add(SkelSprite.fact);
        factories.add(AnimSprite.fact);
        factories.add(StaticSprite.fact);
        factories.add(AudioSprite.fact);
    }

    public interface Owner {
        public Random mkrandoom();

        public Resource getres();

        public Glob glob();
    }

    public static class FactMaker implements Resource.PublishedCode.Instancer {
        public Factory make(Class<?> cl) throws InstantiationException, IllegalAccessException {
            if (Factory.class.isAssignableFrom(cl))
                return (cl.asSubclass(Factory.class).newInstance());
            if (Sprite.class.isAssignableFrom(cl))
                return (mkdynfact(cl.asSubclass(Sprite.class)));
            return (null);
        }
    }

    @Resource.PublishedCode(name = "spr", instancer = FactMaker.class)
    public interface Factory {
        public Sprite create(Owner owner, Resource res, Message sdt);
    }

    public static Factory mkdynfact(Class<? extends Sprite> cl) {
        try {
            final Constructor<? extends Sprite> cons = cl.getConstructor(Owner.class, Resource.class);
            return (new Factory() {
                public Sprite create(Owner owner, Resource res, Message sdt) {
                    return (Utils.construct(cons, owner, res));
                }
            });
        } catch (NoSuchMethodException e) {
        }
        try {
            final Constructor<? extends Sprite> cons = cl.getConstructor(Owner.class, Resource.class, Message.class);
            return (new Factory() {
                public Sprite create(Owner owner, Resource res, Message sdt) {
                    return (Utils.construct(cons, owner, res, sdt));
                }
            });
        } catch (NoSuchMethodException e) {
        }
        throw (new RuntimeException("Could not find any suitable constructor for dynamic sprite"));
    }

    @SuppressWarnings("serial")
	public static class ResourceException extends RuntimeException {
        public Resource res;

        public ResourceException(String msg, Resource res) {
            super(msg + " (" + res + ", from " + res.source + ")");
            this.res = res;
        }

        public ResourceException(String msg, Throwable cause, Resource res) {
            super(msg + " (" + res + ", from " + res.source + ")", cause);
            this.res = res;
        }
    }

    protected Sprite(Owner owner, Resource res) {
        this.res = res;
        this.owner = owner;
    }

    public static int decnum(Message sdt) {
        if (sdt == null)
            return (0);
        int ret = 0, off = 0;
        while (!sdt.eom()) {
            ret |= sdt.uint8() << off;
            off += 8;
        }
        return (ret);
    }

    public static Sprite create(Owner owner, Resource res, Message sdt) {
        {
            Factory f = res.getcode(Factory.class, false);
            if (f != null)
                return (f.create(owner, res, sdt));
        }
        for (Factory f : factories) {
            Sprite ret = f.create(owner, res, sdt);
            if (ret != null)
                return (ret);
        }
        throw (new ResourceException("Does not know how to draw resource " + res.name, res));
    }

    public void draw(GOut g) {
    }

    public abstract boolean setup(RenderList d);

    public boolean tick(int dt) {
        return (false);
    }

    public void dispose() {
    }
}
