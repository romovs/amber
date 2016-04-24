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

package haven.glsl;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Struct extends Type {
    public final Symbol name;
    public final List<Field> fields;

    public static class Field {
        public final Type type;
        public final String name;

        public Field(Type type, String name) {
            this.type = type;
            this.name = name;
        }
    }

    private Struct(Symbol name, List<Field> fields) {
        this.name = name;
        this.fields = fields;
    }

    public Struct(Symbol name, Field... fields) {
        this(name, Arrays.asList(fields));
    }

    public Struct(Symbol name) {
        this(name, new LinkedList<Field>());
    }

    public static Struct make(Symbol name, Object... args) {
        Field[] fs = new Field[args.length / 2];
        for (int f = 0, a = 0; a < args.length; ) {
            Type ft = (Type) args[a++];
            String fn = (String) args[a++];
            fs[f++] = new Field(ft, fn);
        }
        return (new Struct(name, fs));
    }

    public String name(Context ctx) {
        return ("struct " + name.name(ctx));
    }

    public static final Struct gl_LightSourceParameters = make(new Symbol.Fix("gl_LightSourceParameters"),
            Type.VEC4, "ambient",
            Type.VEC4, "diffuse",
            Type.VEC4, "specular",
            Type.VEC4, "position",
            Type.VEC4, "halfVector",
            Type.VEC3, "spotDirection",
            Type.FLOAT, "spotExponent",
            Type.FLOAT, "spotCutoff",
            Type.FLOAT, "spotCosCutoff",
            Type.FLOAT, "constantAttenuation",
            Type.FLOAT, "linearAttenuation",
            Type.FLOAT, "quadraticAttenuation");
    public static final Struct gl_MaterialParameters = make(new Symbol.Fix("gl_MaterialParameters"),
            Type.VEC4, "emission",
            Type.VEC4, "ambient",
            Type.VEC4, "diffuse",
            Type.VEC4, "specular",
            Type.FLOAT, "shininess");
}
