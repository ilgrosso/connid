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
package org.connid.ad;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.connid.ad.authentication.ADAuthenticate;
import org.connid.ad.crud.ADCreate;
import org.connid.ad.crud.ADDelete;
import org.connid.ad.crud.ADUpdate;
import org.connid.ad.search.ADSearch;
import org.connid.ad.sync.ADSyncStrategy;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.ldap.LdapConnector;
import org.identityconnectors.ldap.LdapConstants;
import org.identityconnectors.ldap.search.LdapFilter;

/**
 * All-java, agent-less Active Directory connector, extending LDAP connector.
 *
 * @see org.identityconnectors.ldap.LdapConnector
 */
@ConnectorClass(configurationClass = ADConfiguration.class,
displayNameKey = "ADConnector")
public class ADConnector extends LdapConnector {

    private static final Log LOG = Log.getLog(ADConnector.class);

    public static final String UACCONTROL_ATTR = "userAccountControl";

    //some useful constants from lmaccess.h
    public static final int UF_ACCOUNTDISABLE = 0x0002;

    public static final int UF_PASSWD_NOTREQD = 0x0020;

    public static final int UF_PASSWD_CANT_CHANGE = 0x0040;

    public static final int UF_NORMAL_ACCOUNT = 0x0200;

    public static final int UF_DONT_EXPIRE_PASSWD = 0x10000;

    public static final int UF_PASSWORD_EXPIRED = 0x800000;

    /**
     * The configuration for this connector instance.
     */
    private transient ADConfiguration config;

    /**
     * The relative DirSyncSyncStrategy instance which sync-related operations are delegated to.
     */
    private transient ADSyncStrategy syncStrategy;

    /**
     * The connection to the AD server.
     */
    private transient ADConnection conn;

    @Override
    public Configuration getConfiguration() {
        return config;
    }

    @Override
    public void init(final Configuration cfg) {

        config = (ADConfiguration) cfg;

        // TODO: easier and more efficient if conn was protected in superclass
        conn = new ADConnection(config);

        syncStrategy = new ADSyncStrategy(conn);
        super.init(cfg);
    }

    @Override
    public void dispose() {
        conn.close();
        super.dispose();
    }

    @Override
    public void executeQuery(
            final ObjectClass oclass,
            final LdapFilter query,
            final ResultsHandler handler,
            final OperationOptions options) {
        new ADSearch(conn, oclass, query, options).executeADQuery(handler);
    }

    @Override
    public SyncToken getLatestSyncToken(final ObjectClass oclass) {
        return syncStrategy.getLatestSyncToken();
    }

    @Override
    public void sync(final ObjectClass oclass, final SyncToken token,
            final SyncResultsHandler handler, final OperationOptions options) {

        syncStrategy.sync(token, handler, options, oclass);
    }

    @Override
    public Uid create(
            final ObjectClass oclass,
            final Set<Attribute> attrs,
            final OperationOptions options) {

        final Set<Attribute> attributes = new HashSet<Attribute>(attrs);

        final Attribute ldapGroups = AttributeUtil.find(LdapConstants.LDAP_GROUPS_NAME, attributes);

        final Set<String> ldapGroupsToBeAdded = new HashSet<String>();

        if (ldapGroups != null) {
            attributes.remove(ldapGroups);
            ldapGroupsToBeAdded.addAll(ldapGroups.getValue() == null ? Collections.EMPTY_LIST : ldapGroups.getValue());
        }

        ldapGroupsToBeAdded.addAll(
                config.getMemberships() == null ? Collections.EMPTY_LIST : Arrays.asList(config.getMemberships()));

        // add groups
        attributes.add(AttributeBuilder.build("ldapGroups", ldapGroupsToBeAdded));

        return new ADCreate(conn, oclass, attributes, options).create();
    }

    @Override
    public Uid update(
            final ObjectClass oclass,
            final Uid uid,
            Set<Attribute> attrs,
            final OperationOptions options) {

        final Set<Attribute> attributes = new HashSet<Attribute>(attrs);

        final Attribute ldapGroups = AttributeUtil.find(LdapConstants.LDAP_GROUPS_NAME, attributes);

        final Set<String> ldapGroupsToBeAdded = new HashSet<String>();

        if (ldapGroups != null) {
            attributes.remove(ldapGroups);
            ldapGroupsToBeAdded.addAll(ldapGroups.getValue() == null ? Collections.EMPTY_LIST : ldapGroups.getValue());
        }

        ldapGroupsToBeAdded.addAll(
                config.getMemberships() == null ? Collections.EMPTY_LIST : Arrays.asList(config.getMemberships()));

        // add groups
        attributes.add(AttributeBuilder.build("ldapGroups", ldapGroupsToBeAdded));

        return new ADUpdate(conn, oclass, uid).update(attributes);
    }

    @Override
    public void delete(
            final ObjectClass oclass,
            final Uid uid,
            final OperationOptions options) {

        new ADDelete(conn, oclass, uid).delete();
    }

    @Override
    public Schema schema() {
        return conn.getADSchema().getSchema();
    }

    @Override
    public Uid authenticate(
            final ObjectClass objectClass,
            final String username,
            final GuardedString password,
            final OperationOptions options) {

        return new ADAuthenticate(conn, objectClass, username, options).authenticate(password);
    }

    @Override
    public Uid resolveUsername(
            final ObjectClass objectClass,
            final String username,
            final OperationOptions options) {

        return new ADAuthenticate(conn, objectClass, username, options).resolveUsername();
    }

    @Override
    public void test() {
        conn.test();
    }

    @Override
    public void checkAlive() {
        conn.checkAlive();
    }
}
