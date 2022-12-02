package oap.util;

import org.openjdk.jol.info.GraphLayout;

/**
 * Replaced with JOL, which is 2 times faster and supported.
 */
public class ObjectSizeCalculator {

    /**
     * Given an object, returns the total allocated size, in bytes, of the object
     * and all other objects reachable from it.
     *
     * @param obj the object; can be null. Passing in a {@link java.lang.Class} object doesn't do
     *            anything special, it measures the size of all objects
     *            reachable through it (which will include its class loader, and by
     *            extension, all other Class objects loaded by
     *            the same loader, and all the parent class loaders). It doesn't provide the
     *            size of the static fields in the JVM class that the Class object
     *            represents.
     * @return the total allocated size of the object and all other objects it
     * retains.
     */
    public synchronized long calculateObjectSize( Object obj ) {
        return GraphLayout.parseInstance( obj ).totalSize();
    }
}
