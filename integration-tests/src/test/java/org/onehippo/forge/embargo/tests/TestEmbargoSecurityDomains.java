/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.forge.embargo.tests;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.hippoecm.repository.api.HippoQuery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.forge.embargo.tests.helpers.RepositorySessionBuilder;
import org.onehippo.forge.embargo.tests.helpers.TestConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assume.assumeTrue;

/**
 * @version $Id$
 */
public class TestEmbargoSecurityDomains {

    private static Logger log = LoggerFactory.getLogger(TestEmbargoSecurityDomains.class);
    private RepositorySessionBuilder sessionBuilder;


    @Before
    public void setUp() throws Exception {
        sessionBuilder = new RepositorySessionBuilder();

        //Skip all tests if we can't reach the repository.
        //These tests need to run on an existing repository (integration tests)
        assumeTrue(sessionBuilder.getRepository() != null);
    }

    @Test
    public void testConnectToRepository() {
        Session adminSession = sessionBuilder.build(TestConstants.ADMIN_CREDENTIALS);
        Assert.assertNotNull(adminSession);
        sessionBuilder.destroy();
    }

    @Test
    public void testDeniedAccess() {
        //Assert.assertFalse(queryReturnMultipleNodes("//formdata", TestConstants.AUTHOR_CREDENTIALS));

    }

    @Test
    public void testCssFolder_ViewAsAdmin_FolderShown() {
        //Assert.assertTrue(queryReturnMultipleNodes("//formdata", TestConstants.ADMIN_CREDENTIALS));

    }

    private boolean queryReturnMultipleNodes(String xpathQuery, SimpleCredentials credentials) {
        boolean returnsMultipleNodes = false;
        HippoQuery query = null;
        Session session = sessionBuilder.build(credentials);
        try {
            if (session != null) {
                query = (HippoQuery) session.getWorkspace().getQueryManager().createQuery(xpathQuery, Query.XPATH);
                QueryResult queryResult = query.execute();
                NodeIterator nodes = queryResult.getNodes();
                returnsMultipleNodes = nodes.hasNext();
            } else {
                Assert.fail("Could not open session");
            }

        } catch (RepositoryException e) {
            Assert.fail();
            log.warn("Could not execute query", e);

        } finally {
            sessionBuilder.destroy();
        }

        return returnsMultipleNodes;
    }

}
