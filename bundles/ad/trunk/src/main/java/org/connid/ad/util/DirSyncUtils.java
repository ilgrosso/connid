/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011 Tirasa. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 */
package org.connid.ad.util;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapContext;
import org.connid.ad.ADConfiguration;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.ldap.search.LdapInternalSearch;

public class DirSyncUtils {

    public static String createLdapFilter(final AbstractConfiguration conf) {

        final String[] memberships =
                ((ADConfiguration) conf).getMemberships();

        final String isDeleted = String.valueOf(
                ((ADConfiguration) conf).isRetrieveDeletedUser()).toUpperCase();

        final StringBuilder filter = new StringBuilder();
        final StringBuilder mfilter = new StringBuilder();
        final StringBuilder ufilter = new StringBuilder();

        if (memberships != null && memberships.length > 0) {
            mfilter.append("(&(objectClass=group)(|");
            ufilter.append("(&");

            for (String group : memberships) {
                mfilter.append("(distinguishedName=").append(group).append(")");
                ufilter.append("(memberOf=").append(group).append(")");
            }

            ufilter.append(")");
            mfilter.append("))");
        }

        ufilter.insert(0, "(&(objectClass=user)").append(")");

        filter.append("(|").append(ufilter).append(mfilter).
                append("(&(isDeleted=").
                append(isDeleted).
                append(")(objectClass=user)))");

        return filter.toString();
    }

    private static String AddLeadingZero(int k) {
        return (k <= 0xF)
                ? "0" + Integer.toHexString(k) : Integer.toHexString(k);
    }

    public static String getGuidAsString(byte[] GUID) {
        String strGUID = "";
        String byteGUID = "";

        for (int c = 0; c < GUID.length; c++) {
            byteGUID = byteGUID + "\\" + AddLeadingZero((int) GUID[c] & 0xFF);
        }

        //convert the GUID into string format
        strGUID = strGUID + AddLeadingZero((int) GUID[3] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[2] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[1] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[0] & 0xFF);
        strGUID = strGUID + "-";
        strGUID = strGUID + AddLeadingZero((int) GUID[5] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[4] & 0xFF);
        strGUID = strGUID + "-";
        strGUID = strGUID + AddLeadingZero((int) GUID[7] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[6] & 0xFF);
        strGUID = strGUID + "-";
        strGUID = strGUID + AddLeadingZero((int) GUID[8] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[9] & 0xFF);
        strGUID = strGUID + "-";
        strGUID = strGUID + AddLeadingZero((int) GUID[10] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[11] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[12] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[13] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[14] & 0xFF);
        strGUID = strGUID + AddLeadingZero((int) GUID[15] & 0xFF);

        return strGUID;
    }

    public static boolean verifyFilter(
            final LdapContext ctx,
            final String dn,
            final AbstractConfiguration conf) {

        final StringBuilder filter = new StringBuilder();
        filter.append("(&(").append(createLdapFilter(conf)).append(")");

        filter.append(getFilter(conf) != null ? getFilter(conf) : "").append(")");

        final SearchControls searchCtls =
                LdapInternalSearch.createDefaultSearchControls();

        searchCtls.setSearchScope(SearchControls.OBJECT_SCOPE);
        searchCtls.setReturningAttributes(new String[]{});

        boolean found = true;

        if (StringUtil.isNotBlank(filter.toString())) {
            try {
                final NamingEnumeration res =
                        ctx.search(dn, filter.toString(), searchCtls);

                found = res != null && res.hasMoreElements();
            } catch (NamingException ex) {
                found = false;
            }
        }

        return found;
    }

    private static String getFilter(final AbstractConfiguration conf) {
        return ((ADConfiguration) conf).getAccountSearchFilter();
    }
}
