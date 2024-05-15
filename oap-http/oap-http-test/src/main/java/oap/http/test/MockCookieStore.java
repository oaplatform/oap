package oap.http.test;

import org.apache.http.annotation.Contract;
import org.apache.http.annotation.ThreadingBehavior;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieIdentityComparator;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.joda.time.DateTimeZone.UTC;

@Contract( threading = ThreadingBehavior.SAFE )
public class MockCookieStore implements CookieStore {


    private final TreeSet<Cookie> cookies;
    private transient ReadWriteLock lock;

    public MockCookieStore() {
        super();
        this.cookies = new TreeSet<>( new CookieIdentityComparator() );
        this.lock = new ReentrantReadWriteLock();
    }

    private void readObject( final ObjectInputStream stream ) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();

        /* Reinstantiate transient fields. */
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Adds an {@link Cookie HTTP cookie}, replacing any existing equivalent cookies.
     * If the given cookie has already expired it will not be added, but existing
     * values will still be removed.
     *
     * @param cookie the {@link Cookie cookie} to be added
     * @see #addCookies(Cookie[])
     */
    @Override
    public void addCookie( final Cookie cookie ) {
        if( cookie != null ) {
            lock.writeLock().lock();
            try {
                // first remove any old cookie that is equivalent
                cookies.remove( cookie );
                if( !cookie.isExpired( new DateTime( UTC ).toDate() ) ) {
                    cookies.add( cookie );
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }

    /**
     * Adds an array of {@link Cookie HTTP cookies}. Cookies are added individually and
     * in the given array order. If any of the given cookies has already expired it will
     * not be added, but existing values will still be removed.
     *
     * @param cookies the {@link Cookie cookies} to be added
     * @see #addCookie(Cookie)
     */
    public void addCookies( final Cookie[] cookies ) {
        if( cookies != null ) {
            for( final Cookie cookie : cookies ) {
                this.addCookie( cookie );
            }
        }
    }

    /**
     * Returns an immutable array of {@link Cookie cookies} that this HTTP
     * state currently contains.
     *
     * @return an array of {@link Cookie cookies}.
     */
    @Override
    public List<Cookie> getCookies() {
        lock.readLock().lock();
        try {
            //create defensive copy so it won't be concurrently modified
            return new ArrayList<Cookie>( cookies );
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Removes all of {@link Cookie cookies} in this HTTP state
     * that have expired by the specified {@link java.util.Date date}.
     *
     * @return true if any cookies were purged.
     * @see Cookie#isExpired(Date)
     */
    @Override
    public boolean clearExpired( final Date ignored ) {
        if( ignored == null ) {
            return false;
        }

        Date date = DateTime.now( UTC ).toDate();

        lock.writeLock().lock();
        try {
            boolean removed = false;
            for( final Iterator<Cookie> it = cookies.iterator(); it.hasNext(); ) {
                if( it.next().isExpired( date ) ) {
                    it.remove();
                    removed = true;
                }
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears all cookies.
     */
    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            cookies.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return cookies.toString();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Nullable
    public Cookie getCookie( String name ) {
        for( Cookie cookie : getCookies() ) {
            if( name.equals( cookie.getName() ) ) {
                return cookie;
            }
        }

        return null;
    }
}
