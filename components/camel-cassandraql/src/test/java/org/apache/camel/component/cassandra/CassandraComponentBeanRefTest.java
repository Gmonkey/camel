/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.cassandra;

import com.datastax.driver.core.Cluster;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.cassandraunit.CassandraCQLUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;

public class CassandraComponentBeanRefTest extends CamelTestSupport {
    public static final String CQL = "insert into camel_user(login, first_name, last_name) values (?, ?, ?)";
    public static final String SESSION_URI = "cql:bean:cassandraSession?cql=#insertCql";
    public static final String CLUSTER_URI = "cql:bean:cassandraCluster/camel_ks?cql=#insertCql";

    @Produce(uri = "direct:input")
    public ProducerTemplate producerTemplate;

    @Rule
    public CassandraCQLUnit cassandra = CassandraUnitUtils.cassandraCQLUnit();

    @BeforeClass
    public static void setUpClass() throws Exception {
        assumeTrue("Skipping test running in CI server - Fails sometimes on CI server with address already in use", System.getenv("BUILD_ID") == null);
        CassandraUnitUtils.startEmbeddedCassandra();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            CassandraUnitUtils.cleanEmbeddedCassandra();
        } catch (Throwable e) {
            // ignore shutdown errors
        }
    }

    @Override
    protected JndiRegistry createRegistry() throws Exception {
        JndiRegistry registry = super.createRegistry();
        Cluster cluster = Cluster.builder()
                .addContactPoint("localhost")
                .build();
        registry.bind("cassandraCluster", cluster);
        registry.bind("cassandraSession", cluster.connect("camel_ks"));
        registry.bind("insertCql", CQL);
        return registry;
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from("direct:inputSession")
                        .to(SESSION_URI);
                from("direct:inputCluster")
                        .to(CLUSTER_URI);
            }
        };
    }

    @Test
    public void testSession() throws Exception {
        CassandraEndpoint endpoint = getMandatoryEndpoint(SESSION_URI, CassandraEndpoint.class);

        assertEquals("camel_ks", endpoint.getKeyspace());
        assertEquals(CQL, endpoint.getCql());
    }

    @Test
    public void testCluster() throws Exception {
        CassandraEndpoint endpoint = getMandatoryEndpoint(CLUSTER_URI, CassandraEndpoint.class);

        assertEquals("camel_ks", endpoint.getKeyspace());
        assertEquals(CQL, endpoint.getCql());
    }

}
