package com.jcraft.jorbis;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;

public class InternSet<T> {
    private final HashMap<Ref<T>, Ref<T>> bk = new HashMap<Ref<T>, Ref<T>>();
    private final ReferenceQueue<T> queue = new ReferenceQueue<T>();

    private static class Ref<T> extends WeakReference<T> {
	private final int hash;

	Ref(T ob, ReferenceQueue<T> queue) {
	    super(ob, queue);
	    hash = ob.hashCode();
	}

	public boolean equals(Object o) {
	    if(o == this)
		return(true);
	    if(!(o instanceof Ref))
		return(false);
	    Ref<?> r = (Ref<?>)o;
	    if(hash != r.hash)
		return(false);
	    T a = get();
	    Object b = r.get();
	    if((a == null) && (b == null))
		return(true);
	    else if((a == null) || (b == null))
		return(false);
	    else
		return(a.equals(b));
	}

	public int hashCode() {
	    return(hash);
	}
    }

    private void clean() {
	Ref<?> old;
	while((old = (Ref<?>)queue.poll()) != null)
	    bk.remove(old);
    }

    public T intern(T ob) {
	if(ob == null)
	    return(null);
	clean();
	Ref<T> ref = new Ref<T>(ob, queue);
	Ref<T> cref = bk.get(ref);
	T cur = (cref == null)?null:cref.get();
	if(cur == null) {
	    bk.put(ref, ref);
	    return(ob);
	} else {
	    return(cur);
	}
    }

    public int size() {
	return(bk.size());
    }
}
