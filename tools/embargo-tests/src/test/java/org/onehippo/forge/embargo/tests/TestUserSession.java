package org.onehippo.forge.embargo.tests;

import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.Calendar;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.MappingException;
import org.hippoecm.repository.api.WorkflowException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflow;
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
