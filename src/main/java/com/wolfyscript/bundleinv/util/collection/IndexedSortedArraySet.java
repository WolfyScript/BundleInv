package com.wolfyscript.bundleinv.util.collection;

import it.unimi.dsi.fastutil.objects.ObjectArrays;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.jetbrains.annotations.Nullable;

/**
 * This class is a modified version of the {@link net.minecraft.util.collection.SortedArraySet}.<br>
 * It provides access to some index based methods that would otherwise be not accessible.<br>
 * Note that only methods that do not break the Set when invoked are made public.
 * Inserting objects at a specified index is therefor not support, as this Set is sorted!
 *
 * @param <T>
 */
public class IndexedSortedArraySet<T> extends AbstractSet<T> {

    private static final int DEFAULT_CAPACITY = 10;
    private final Comparator<T> comparator;
    T[] elements;
    int size;

    private IndexedSortedArraySet(int initialCapacity, Comparator<T> comparator) {
        this.comparator = comparator;
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Initial capacity (" + initialCapacity + ") is negative");
        }
        this.elements = IndexedSortedArraySet.cast(new Object[initialCapacity]);
    }

    public static <T extends Comparable<T>> IndexedSortedArraySet<T> create() {
        return IndexedSortedArraySet.create(10);
    }

    public static <T extends Comparable<T>> IndexedSortedArraySet<T> create(int initialCapacity) {
        return new IndexedSortedArraySet<T>(initialCapacity, Comparator.naturalOrder());
    }

    public static <T> IndexedSortedArraySet<T> create(Comparator<T> comparator) {
        return IndexedSortedArraySet.create(comparator, 10);
    }

    public static <T> IndexedSortedArraySet<T> create(Comparator<T> comparator, int initialCapacity) {
        return new IndexedSortedArraySet<T>(initialCapacity, comparator);
    }

    private static <T> T[] cast(Object[] array) {
        return (T[]) array;
    }

    private int binarySearch(T object) {
        return Arrays.binarySearch(this.elements, 0, this.size, object, this.comparator);
    }

    private static int insertionPoint(int binarySearchResult) {
        return -binarySearchResult - 1;
    }

    public int indexOf(T object) {
        int index = binarySearch(object);
        return index >= 0 ? index : -1;
    }

    @Override
    public boolean add(T object) {
        int i = this.binarySearch(object);
        if (i >= 0) {
            return false;
        }
        int j = IndexedSortedArraySet.insertionPoint(i);
        this.add(object, j);
        return true;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= this.elements.length) {
            return;
        }
        if (this.elements != ObjectArrays.DEFAULT_EMPTY_ARRAY) {
            minCapacity = (int)Math.max(Math.min((long)this.elements.length + (long)(this.elements.length >> 1), 0x7FFFFFF7L), (long)minCapacity);
        } else if (minCapacity < 10) {
            minCapacity = 10;
        }
        Object[] objects = new Object[minCapacity];
        System.arraycopy(this.elements, 0, objects, 0, this.size);
        this.elements = IndexedSortedArraySet.cast(objects);
    }

    /**
     * This must not be public as the array is sorted and inserting at random indexes will break this whole structure!
     */
    private void add(T object, int index) {
        this.ensureCapacity(this.size + 1);
        if (index != this.size) {
            System.arraycopy(this.elements, index, this.elements, index + 1, this.size - index);
        }
        this.elements[index] = object;
        ++this.size;
    }

    public void remove(int index) {
        --this.size;
        if (index != this.size) {
            System.arraycopy(this.elements, index + 1, this.elements, index, this.size - index);
        }
        this.elements[this.size] = null;
    }

    /**
     * Gets the element at the specified index.
     * Note that the index is not in insertion order, but in sorted order.
     *
     * @param index The index to get the object from.
     * @return The object at the specified index.
     * @throws IndexOutOfBoundsException If the index is out of bounds.
     */
    public T get(int index) {
        return this.elements[index];
    }

    public T addAndGet(T object) {
        int i = this.binarySearch(object);
        if (i >= 0) {
            return this.get(i);
        }
        this.add(object, IndexedSortedArraySet.insertionPoint(i));
        return object;
    }

    @Override
    public boolean remove(Object object) {
        int i = this.binarySearch((T) object);
        if (i >= 0) {
            this.remove(i);
            return true;
        }
        return false;
    }

    @Nullable
    public T getIfContains(T object) {
        int i = this.binarySearch(object);
        if (i >= 0) {
            return this.get(i);
        }
        return null;
    }

    public T first() {
        return this.get(0);
    }

    public T last() {
        return this.get(this.size - 1);
    }

    @Override
    public boolean contains(Object object) {
        int i = this.binarySearch((T) object);
        return i >= 0;
    }

    @Override
    public Iterator<T> iterator() {
        return new IndexedSortedArraySet.SetIterator();
    }

    @Override
    public int size() {
        return this.size;
    }

    @Override
    public Object[] toArray() {
        return (Object[])this.elements.clone();
    }

    @Override
    public <U> U[] toArray(U[] objects) {
        if (objects.length < this.size) {
            return (U[]) Arrays.copyOf(this.elements, this.size, objects.getClass());
        }
        System.arraycopy(this.elements, 0, objects, 0, this.size);
        if (objects.length > this.size) {
            objects[this.size] = null;
        }
        return objects;
    }

    @Override
    public void clear() {
        Arrays.fill(this.elements, 0, this.size, null);
        this.size = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof IndexedSortedArraySet<?> sortedArraySet) {
            if (this.comparator.equals(sortedArraySet.comparator)) {
                return this.size == sortedArraySet.size && Arrays.equals(this.elements, sortedArraySet.elements);
            }
        }
        return super.equals(o);
    }

    class SetIterator
            implements Iterator<T> {
        private int nextIndex;
        private int lastIndex = -1;

        SetIterator() {
        }

        @Override
        public boolean hasNext() {
            return this.nextIndex < IndexedSortedArraySet.this.size;
        }

        @Override
        public T next() {
            if (this.nextIndex >= IndexedSortedArraySet.this.size) {
                throw new NoSuchElementException();
            }
            this.lastIndex = this.nextIndex++;
            return IndexedSortedArraySet.this.elements[this.lastIndex];
        }

        @Override
        public void remove() {
            if (this.lastIndex == -1) {
                throw new IllegalStateException();
            }
            IndexedSortedArraySet.this.remove(this.lastIndex);
            --this.nextIndex;
            this.lastIndex = -1;
        }
    }

}
