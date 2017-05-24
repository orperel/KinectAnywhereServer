package org.kinectanywhereandroid.util;

/**
 * Utility class for a tuple of 2 items
 * @param <X> First item
 * @param <Y> Second item
 */
public class Pair<X, Y> { 
    public final X first; 
    public final Y second; 
    public Pair(X first, Y second) { 
        this.first = first; 
        this.second = second; 
    }

    @Override
    public String toString() {
        return "(" + first + "," + second + ")";
    }

    @SuppressWarnings("unchecked")
	@Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof Pair)){
            return false;
        }

        Pair<X,Y> _other = (Pair<X,Y>) other;

        // This may cause NPE if nulls are valid values for x or y.
        // The logic may be improved to handle nulls properly, if needed.
        return _other.first.equals(this.first) && _other.second.equals(this.second);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((first == null) ? 0 : first.hashCode());
        result = prime * result + ((second == null) ? 0 : second.hashCode());
        return result;
    }
}