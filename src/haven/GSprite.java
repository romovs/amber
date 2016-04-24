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
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public abstract class GSprite implements Drawn {
    public final Owner owner;
    public static final List<Factory> factories;

    static {
        factories = Arrays.asList(new Factory[]{
                StaticGSprite.fact,
        });
    }

    public interface Owner {
        public Random mkrandoom();

        public Resource getres();

        public Glob glob();
    }

    public GSprite(Owner owner) {
        this.owner = owner;
    }

    public static class FactMaker implements Resource.PublishedCode.Instancer {
        private static Factory dynfact(Class<? extends GSprite> cl) {
            try {
                final Constructor<? extends GSprite> cons = cl.getConstructor(Owner.class, Resource.class, Message.class);
                return (new Factory() {
                    public GSprite create(Owner owner, Resource res, Message sdt) {
                        return (Utils.construct(cons, owner, res, sdt));
                    }
                });
            } catch (NoSuchMethodException e) {
            }
            throw (new RuntimeException("Could not find any suitable constructor for dynamic sprite"));
        }

        public Factory make(Class<?> cl) throws InstantiationException, IllegalAccessException {
            if (Factory.class.isAssignableFrom(cl))
                return (cl.asSubclass(Factory.class).newInstance());
            if (GSprite.class.isAssignableFrom(cl))
                return (dynfact(cl.asSubclass(GSprite.class)));
            throw (new RuntimeException("Could not find construct sprite factory for dynamic sprite class " + cl));
        }
    }

    @Resource.PublishedCode(name = "ispr", instancer = FactMaker.class)
    public interface Factory {
        public GSprite create(Owner owner, Resource res, Message sdt);
    }

    public static GSprite create(Owner owner, Resource res, Message sdt) {
        {
            Factory f = res.getcode(Factory.class, false);
            if (f != null)
                return (f.create(owner, res, sdt));
        }
        for (Factory f : factories) {
            GSprite ret = f.create(owner, res, sdt);
            if (ret != null)
                return (ret);
        }
        throw (new Sprite.ResourceException("Does not know how to draw resource " + res.name, res));
    }

    public abstract void draw(GOut g);

    public abstract Coord sz();

    public void tick(double dt) {
    }

    public String getname() {
        Class<? extends GSprite> cl = this.getClass();
        try {
            Field name = cl.getDeclaredField("name");
            return (String)name.get(this);
        } catch (NoSuchFieldException nsfe) {
        } catch (ClassCastException cce) {
        } catch (IllegalAccessException iae) {
        }
        return null;
    }
}
