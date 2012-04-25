/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.ldap.commons;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

/**
 * An implementation of {@link Attributes} which delegates to a given
 * {@code Attributes} instance while adding some
 * attributes to the list if already not present, or replacing the existing
 * values if such attributes are present.
 */
public abstract class AppendingAttributes implements Attributes {

    private static final long serialVersionUID = 1L;

    protected final Attributes delegate;

    public AppendingAttributes(Attributes delegate) {
        this.delegate = delegate;
    }

    protected abstract Set<String> getAttributeIDsToAppend();

    protected abstract Attribute getAttributeToAppend(String attrID);

    @Override
    public abstract Object clone();

    @Override
    public final Attribute get(String attrID) {
        String attrIDToAppend = getNormalizedAttributeIDToAppend(attrID);
        if (attrIDToAppend != null) {
            return getAttributeToAppend(attrIDToAppend);
        } else {
            return delegate.get(attrID);
        }
    }

    private String getNormalizedAttributeIDToAppend(String attrID) {
        if (delegate.isCaseIgnored()) {
            for (String attributeIDToAppend : getAttributeIDsToAppend()) {
                if (attributeIDToAppend.equalsIgnoreCase(attrID)) {
                    return attributeIDToAppend;
                }
            }
        } else {
            if (getAttributeIDsToAppend().contains(attrID)) {
                return attrID;
            }
        }
        return null;
    }

    @Override
    public final NamingEnumeration<? extends Attribute> getAll() {
        return new AttributeAppendingEnumeration(delegate.getAll());
    }

    @Override
    public final NamingEnumeration<String> getIDs() {
        return new AttributeIDAppendingEnumeration(delegate.getIDs());
    }

    @Override
    public final boolean isCaseIgnored() {
        return delegate.isCaseIgnored();
    }

    @Override
    public final Attribute put(Attribute attr) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Attribute put(String attrID, Object val) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final Attribute remove(String attrID) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final int size() {
        int size = delegate.size();
        for (String attributeIDToAppend : getAttributeIDsToAppend()) {
            if (delegate.get(attributeIDToAppend) == null) {
                size++;
            }
        }
        return size;
    }

    private abstract class AppendingEnumeration<T> implements NamingEnumeration<T> {

        private final NamingEnumeration<? extends T> delegate;

        private Enumeration<T> remainingValues;

        public AppendingEnumeration(NamingEnumeration<? extends T> delegate) {
            this.delegate = delegate;
        }

        protected abstract T getReplacementValue(T value);

        protected abstract Enumeration<T> getRemainingValues();

        @Override
        public void close()
                throws NamingException {
            delegate.close();
        }

        @Override
        public boolean hasMore()
                throws NamingException {
            return delegate.hasMore() || hasMoreRemainingValues();
        }

        @Override
        public T next()
                throws NamingException {
            if (delegate.hasMore()) {
                T next = delegate.next();
                // Watch out for null values in the enumeration.
                if (next != null) {
                    T replacement = getReplacementValue(next);
                    if (replacement != null) {
                        next = replacement;
                    }
                }
                return next;
            } else if (hasMoreRemainingValues()) {
                return getNextRemainingValue();
            } else {
                return delegate.next(); // Throws NoSuchElementException.
            }
        }

        @Override
        public boolean hasMoreElements() {
            return delegate.hasMoreElements() || hasMoreRemainingValues();
        }

        @Override
        public T nextElement() {
            if (delegate.hasMoreElements()) {
                T next = delegate.nextElement();
                // Watch out for null values in the enumeration.
                if (next != null) {
                    T replacement = getReplacementValue(next);
                    if (replacement != null) {
                        next = replacement;
                    }
                }
                return next;
            } else if (hasMoreRemainingValues()) {
                return getNextRemainingValue();
            } else {
                return delegate.nextElement(); // Throws NoSuchElementException.
            }
        }

        private boolean hasMoreRemainingValues() {
            if (remainingValues == null) {
                remainingValues = getRemainingValues();
            }
            return remainingValues.hasMoreElements();
        }

        private T getNextRemainingValue() {
            assert remainingValues != null;
            return remainingValues.nextElement();
        }
    }

    private final class AttributeAppendingEnumeration extends AppendingEnumeration<Attribute> {

        private Set<String> replaced = new HashSet<String>();

        public AttributeAppendingEnumeration(NamingEnumeration<? extends Attribute> delegate) {
            super(delegate);
        }

        @Override
        protected Attribute getReplacementValue(Attribute value) {
            String attrID = value.getID();
            if (getAttributeIDsToAppend().contains(attrID)) {
                replaced.add(attrID);
                return getAttributeToAppend(attrID);
            } else {
                return null;
            }
        }

        @Override
        protected Enumeration<Attribute> getRemainingValues() {
            final Set<String> remaining = getAttributeIDsToAppend();
            remaining.removeAll(replaced);

            return new Enumeration<Attribute>() {

                private final Iterator<String> iterator = remaining.iterator();

                @Override
                public boolean hasMoreElements() {
                    return iterator.hasNext();
                }

                @Override
                public Attribute nextElement() {
                    return getAttributeToAppend(iterator.next());
                }
            };
        }
    }

    private final class AttributeIDAppendingEnumeration extends AppendingEnumeration<String> {

        private Set<String> replaced = new HashSet<String>();

        public AttributeIDAppendingEnumeration(NamingEnumeration<String> delegate) {
            super(delegate);
        }

        @Override
        protected String getReplacementValue(String value) {
            if (getAttributeIDsToAppend().contains(value)) {
                replaced.add(value);
                return value;
            } else {
                return null;
            }
        }

        @Override
        protected Enumeration<String> getRemainingValues() {
            final Set<String> remaining = getAttributeIDsToAppend();
            remaining.removeAll(replaced);

            return new Enumeration<String>() {

                private final Iterator<String> iterator = remaining.iterator();

                @Override
                public boolean hasMoreElements() {
                    return iterator.hasNext();
                }

                @Override
                public String nextElement() {
                    return iterator.next();
                }
            };
        }
    }
}
