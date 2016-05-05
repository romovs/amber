package haven.util;

public interface CollectionListener<T> {
    void onItemAdded(T item);
    void onItemRemoved(T item);
}