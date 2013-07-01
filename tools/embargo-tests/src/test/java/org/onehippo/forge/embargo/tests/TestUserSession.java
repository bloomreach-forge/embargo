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

import javax.jcr.Session;

import org.hippoecm.frontend.session.UserSession;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.Assert;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replay;

/**
 * @version "$Id$"
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({org.apache.wicket.Session.class})
public class TestUserSession {

    private static Logger log = LoggerFactory.getLogger(TestUserSession.class);

    @Test
    public void testUserSession() throws Exception {
        Session jcrSession = createNiceMock(Session.class);
        UserSession userSession = createNiceMock(UserSession.class);
        mockStatic(org.apache.wicket.Session.class);
        expect(org.apache.wicket.Session.get()).andReturn(userSession).anyTimes();
        expect(userSession.getJcrSession()).andReturn(jcrSession).anyTimes();
        replay(org.apache.wicket.Session.class, userSession);
        final org.apache.wicket.Session session1 = org.apache.wicket.Session.get();
        Assert.assertNotNull(session1);
        UserSession userSession1 = (UserSession) session1;
        final Session jcrSession1 = userSession1.getJcrSession();
        Assert.assertNotNull(jcrSession1);
    }

}
