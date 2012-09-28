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
package org.connid.bundles.db.common;

import org.connid.bundles.db.common.SQLParam;
import org.connid.bundles.db.common.DatabaseConnection;
import org.connid.bundles.db.common.ExpectProxy;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * DatabaseConnection test class
 * @version $Revision 1.0$
 * @since 1.0
 */
public class DatabaseConnectionTest {

    private static final String LOGIN = "login";
    private static final String NAME = "name";
    private static final String TEST_SQL_STATEMENT = "SELECT * FROM dummy";
    private static final String SELECT_SQL_STATEMENT = "SELECT * FROM dummy WHERE login = ? and name = ?";
    
    private List<SQLParam> values;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        values = new ArrayList<SQLParam>();
        values.add(new SQLParam(LOGIN, LOGIN)); 
        values.add(new SQLParam(NAME, NAME)); 
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        // not used yet 
    }

    /**
     * Test method for {@link DatabaseConnection#DatabaseConnection(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
     */
    @Test
    public void testDatabaseConnection() {
        ExpectProxy<Connection> tp = new ExpectProxy<Connection>();
        DatabaseConnection dbc = new DatabaseConnection(tp.getProxy(Connection.class));
        assertNotNull(dbc);         
        assertNotNull(dbc.getConnection());          
    }

    /**
     * Test method for {@link DatabaseConnection#dispose()}.
     */
    @Test
    public void testDispose() {
        ExpectProxy<Connection> tp = new ExpectProxy<Connection>();
        tp.expectAndReturn("isClosed", Boolean.FALSE);
        tp.expect("close");
        Connection xc = tp.getProxy(Connection.class);
        DatabaseConnection dbc = new DatabaseConnection(xc);
        dbc.dispose();
        assertTrue("close called", tp.isDone());
        
        tp = new ExpectProxy<Connection>();
        xc = tp.getProxy(Connection.class);
        tp.expectAndReturn("isClosed", Boolean.TRUE);
        dbc = new DatabaseConnection(xc);
        dbc.dispose();
        assertTrue("close called", tp.isDone());         
    }

    /**
     * Test method for {@link DatabaseConnection#test()}.
     */
    @Test
    public void testTest() {
        ExpectProxy<Connection> tp = new ExpectProxy<Connection>();
        tp.expectAndReturn("getAutoCommit", Boolean.FALSE);
        tp.expect("setAutoCommit");
        tp.expectAndReturn("getAutoCommit", Boolean.TRUE);
        tp.expect("setAutoCommit");
        tp.expect("commit");
        DatabaseConnection dbc = new DatabaseConnection(tp.getProxy(Connection.class));
        dbc.test();
        assertTrue("test called", tp.isDone());
        
    }

    /**
     * Test method for {@link DatabaseConnection#getConnection()}.
     */
    @Test
    public void testGetSetConnection() {
        ExpectProxy<Connection> tp = new ExpectProxy<Connection>();
        final Connection xc = tp.getProxy(Connection.class);
        DatabaseConnection dbc = new DatabaseConnection(xc);
        dbc.getConnection();
        assertTrue("close called", tp.isDone());
        assertNotNull(dbc.getConnection());
        assertSame("connection", xc, dbc.getConnection());
        assertTrue("test called", tp.isDone());
    }

    /**
     * Test method for {@link DatabaseConnection#prepareStatement(java.lang.String, java.util.List)}.
     * @throws Exception 
     */
    @Test
    public void testPrepareStatementNullValues() throws Exception{
        final ExpectProxy<Connection> tpc = new ExpectProxy<Connection>();
        final ExpectProxy<PreparedStatement> tps = new ExpectProxy<PreparedStatement>();
        final PreparedStatement xps = tps.getProxy(PreparedStatement.class);
        tpc.expectAndReturn("prepareStatement", xps);

        DatabaseConnection dbc = new DatabaseConnection(tpc.getProxy(Connection.class));
        dbc.prepareStatement(TEST_SQL_STATEMENT, null);

        assertTrue("statement created", tpc.isDone());
        assertTrue("value binded", tps.isDone());
    }
    
    /**
     * Test method for {@link DatabaseConnection#prepareStatement(java.lang.String, java.util.List)}.
     * @throws Exception 
     */
    @Test
    public void testPrepareStatementEmptyValues() throws Exception{
        final ExpectProxy<Connection> tpc = new ExpectProxy<Connection>();
        final ExpectProxy<PreparedStatement> tps = new ExpectProxy<PreparedStatement>();
        final PreparedStatement xps = tps.getProxy(PreparedStatement.class);
        tpc.expectAndReturn("prepareStatement", xps);

        DatabaseConnection dbc = new DatabaseConnection(tpc.getProxy(Connection.class));
        dbc.prepareStatement(TEST_SQL_STATEMENT, new ArrayList<SQLParam>());
        assertTrue("statement created", tpc.isDone());
        assertTrue("value binded", tps.isDone());
    }    

    /**
     * Test method for {@link DatabaseConnection#prepareStatement(java.lang.String, java.util.List)}.
     * @throws Exception 
     */
    @Test
    public void testPrepareStatement() throws Exception{
        final ExpectProxy<Connection> tpc = new ExpectProxy<Connection>();
        final ExpectProxy<PreparedStatement> tps = new ExpectProxy<PreparedStatement>();
        final PreparedStatement xps = tps.getProxy(PreparedStatement.class);
        tpc.expectAndReturn("prepareStatement", xps);
        tps.expectAndReturn("setObject", LOGIN);
        tps.expectAndReturn("setObject", NAME);
        tps.expectAndReturn("execute", true);

        DatabaseConnection dbc = new DatabaseConnection(tpc.getProxy(Connection.class));
        final PreparedStatement ps = dbc.prepareStatement(SELECT_SQL_STATEMENT, values);
        ps.execute();
       
        assertTrue("statement created", tpc.isDone());
        assertTrue("value binded", tps.isDone());
    }       
    
    
    /**
     * Test method for {@link DatabaseConnection#prepareStatement(java.lang.String, java.util.List)}.
     * @throws Exception 
     */
    @Test
    public void testPrepareCall() throws Exception{
        final ExpectProxy<Connection> tpc = new ExpectProxy<Connection>();
        final ExpectProxy<CallableStatement> tps = new ExpectProxy<CallableStatement>();
        final CallableStatement cs = tps.getProxy(CallableStatement.class);
        tpc.expectAndReturn("prepareStatement", cs);
        tps.expectAndReturn("setObject", LOGIN);
        tps.expectAndReturn("setObject", NAME);
        tps.expectAndReturn("execute", true);

        DatabaseConnection dbc = new DatabaseConnection(tpc.getProxy(Connection.class));
        final PreparedStatement ps = dbc.prepareStatement(SELECT_SQL_STATEMENT, values);
        ps.execute();
       
        assertTrue("statement created", tpc.isDone());
        assertTrue("value binded", tps.isDone());
    }         
    /**
     * Test method for {@link DatabaseConnection#commit(org.identityconnectors.common.logging.Log)}.
     */
    @Test
    public void testCommit() {
        ExpectProxy<Connection> tp = new ExpectProxy<Connection>();
        tp.expect("commit");
        DatabaseConnection dbc = new DatabaseConnection(tp.getProxy(Connection.class));
        dbc.commit();
        assertTrue("commit called", tp.isDone());
    }
}
