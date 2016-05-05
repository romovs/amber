package haven.resutil;

import haven.Config;
import haven.Indir;
import haven.Loading;
import haven.Resource;
import haven.Utils;

public class CompilerClassLoader extends ClassLoader {
    private Indir<Resource>[] useres;

    static {
        Config.nopreload = true;
    }

    @SuppressWarnings("unchecked")
    public CompilerClassLoader(ClassLoader parent) {
        super(parent);
        String[] useresnm = Utils.getprop("haven.resutil.classloader.useres", null).split(":");
        this.useres = new Indir[useresnm.length];
        for (int i = 0; i < useresnm.length; i++)
            this.useres[i] = Resource.local().load(useresnm[i]);
    }

    public Class<?> findClass(String name) throws ClassNotFoundException {
        for (Indir<Resource> res : useres) {
            try {
                return (Loading.waitfor(res).layer(Resource.CodeEntry.class).loader(true).loadClass(name));
            } catch (ClassNotFoundException e) {
            }
        }
        throw (new ClassNotFoundException(name + " was not found in any of the requested resources."));
    }
}
