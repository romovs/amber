package haven.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ObservableCollection<T> implements Iterable<T> {
    private final Collection<T> base;
    private final Set<CollectionListener<T>> listeners;

    public ObservableCollection(Collection<T> base) {
        this.base = base;
        this.listeners = new HashSet<CollectionListener<T>>();
    }

    public boolean add(T t) {
        if (base.add(t)) {
            for (CollectionListener<T> listener : listeners)
                listener.onItemAdded(t);
            return true;
        }
        return false;
    }

    public boolean remove(T item) {
        if (base.remove(item)) {
            for (CollectionListener<T> listener : listeners)
                listener.onItemRemoved(item);
            return true;
        }
        return false;
    }

    public boolean addListener(CollectionListener<T> listener) {
        return listeners.add(listener);
    }

    public boolean removeListener(CollectionListener<T> listener) {
        return listeners.remove(listener);
    }

    @Override
    public Iterator<T> iterator() {
        return base.iterator();
    }
}