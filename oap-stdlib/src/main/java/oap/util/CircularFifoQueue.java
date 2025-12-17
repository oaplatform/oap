package oap.util;

import org.apache.commons.lang3.NotImplementedException;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;

public class CircularFifoQueue<T> extends AbstractCollection<T> implements Queue<T>, Serializable {
    @Serial
    private static final long serialVersionUID = 7342311080952645978L;
    private final int maxElements;
    private transient T[] elements;
    private transient int start;
    private transient int end;
    private transient boolean full;

    public CircularFifoQueue( Collection<? extends T> coll ) {
        this( coll.size() );
        this.addAll( coll );
    }

    @SuppressWarnings( "unchecked" )
    public CircularFifoQueue( int size ) {
        if( size <= 0 ) {
            throw new IllegalArgumentException( "The size must be greater than 0" );
        } else {
            this.elements = ( T[] ) ( new Object[size] );
            this.maxElements = this.elements.length;
        }
    }

    public boolean add( T element ) {
        Objects.requireNonNull( element, "element" );

        if( this.isAtFullCapacity() ) {
            this.remove();
        }

        this.elements[this.end++] = element;
        if( this.end >= this.maxElements ) {
            this.end = 0;
        }

        if( this.end == this.start ) {
            this.full = true;
        }

        return true;
    }

    public void clear() {
        this.full = false;
        this.start = 0;
        this.end = 0;
        Arrays.fill( this.elements, ( Object ) null );
    }

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    private int decrement( int index ) {
        --index;
        if( index < 0 ) {
            index = this.maxElements - 1;
        }

        return index;
    }

    public T element() {
        if( this.isEmpty() ) {
            throw new NoSuchElementException( "queue is empty" );
        } else {
            return ( T ) this.peek();
        }
    }

    public T get( int index ) {
        int sz = this.size();
        if( index >= 0 && index < sz ) {
            int idx = ( this.start + index ) % this.maxElements;
            return ( T ) this.elements[idx];
        } else {
            throw new NoSuchElementException( String.format( "The specified index %1$d is outside the available range [0, %2$d)", index, sz ) );
        }
    }

    @SuppressWarnings( "checkstyle:ParameterAssignment" )
    private int increment( int index ) {
        ++index;
        if( index >= this.maxElements ) {
            index = 0;
        }

        return index;
    }

    public boolean isAtFullCapacity() {
        return this.size() == this.maxElements;
    }

    public boolean isEmpty() {
        return this.size() == 0;
    }

    public boolean isFull() {
        return false;
    }

    public int maxSize() {
        return this.maxElements;
    }

    public boolean offer( T element ) {
        return this.add( element );
    }

    public T peek() {
        return ( T ) ( this.isEmpty() ? null : this.elements[this.start] );
    }

    public T poll() {
        return ( T ) ( this.isEmpty() ? null : this.remove() );
    }

    @SuppressWarnings( "unchecked" )
    private void readObject( ObjectInputStream in ) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        this.elements = ( T[] ) ( new Object[this.maxElements] );
        int size = in.readInt();

        for( int i = 0; i < size; ++i ) {
            this.elements[i] = ( T ) in.readObject();
        }

        this.start = 0;
        this.full = size == this.maxElements;
        if( this.full ) {
            this.end = 0;
        } else {
            this.end = size;
        }

    }

    public T remove() {
        if( this.isEmpty() ) {
            throw new NoSuchElementException( "queue is empty" );
        } else {
            T element = this.elements[this.start];
            if( null != element ) {
                this.elements[this.start++] = null;
                if( this.start >= this.maxElements ) {
                    this.start = 0;
                }

                this.full = false;
            }

            return element;
        }
    }

    public int size() {
        int size;
        if( this.end < this.start ) {
            size = this.maxElements - this.start + this.end;
        } else if( this.end == this.start ) {
            size = this.full ? this.maxElements : 0;
        } else {
            size = this.end - this.start;
        }

        return size;
    }

    @Serial
    private void writeObject( ObjectOutputStream out ) throws IOException {
        out.defaultWriteObject();
        out.writeInt( this.size() );

        for( T e : this ) {
            out.writeObject( e );
        }

    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private int index;
            private int lastReturnedIndex;
            private boolean isFirst;

            {
                this.index = start;
                this.lastReturnedIndex = -1;
                this.isFirst = full;
            }

            public boolean hasNext() {
                return this.isFirst || this.index != end;
            }

            public T next() {
                if( !this.hasNext() ) {
                    throw new NoSuchElementException();
                } else {
                    this.isFirst = false;
                    this.lastReturnedIndex = this.index;
                    this.index = increment( this.index );
                    return elements[this.lastReturnedIndex];
                }
            }

            public void remove() {
                if( this.lastReturnedIndex == -1 ) {
                    throw new IllegalStateException();
                } else if( this.lastReturnedIndex == start ) {
                    remove();
                    this.lastReturnedIndex = -1;
                } else {
                    int pos = this.lastReturnedIndex + 1;
                    if( start < this.lastReturnedIndex && pos < end ) {
                        System.arraycopy( elements, pos, elements, this.lastReturnedIndex, end - pos );
                    } else {
                        while( pos != end ) {
                            if( pos >= maxElements ) {
                                elements[pos - 1] = elements[0];
                                pos = 0;
                            } else {
                                elements[decrement( pos )] = elements[pos];
                                pos = increment( pos );
                            }
                        }
                    }

                    this.lastReturnedIndex = -1;
                    end = decrement( end );
                    elements[end] = null;
                    full = false;
                    this.index = decrement( this.index );
                }
            }
        };
    }

    @Nonnull
    public Iterator<T> reverseIterator() {
        return new Iterator<>() {
            private int index;
            private int lastReturnedIndex;
            private boolean isLast;

            {
                this.index = end;
                this.lastReturnedIndex = -1;
                this.isLast = full;
            }

            public boolean hasNext() {
                return this.isLast || this.index != start;
            }

            public T next() {
                if( !this.hasNext() ) {
                    throw new NoSuchElementException();
                } else {
                    this.isLast = false;
                    this.lastReturnedIndex = decrement( this.index );
                    this.index = decrement( this.index );
                    return elements[this.lastReturnedIndex];
                }
            }

            public void remove() {
                throw new NotImplementedException();
            }
        };
    }
}
