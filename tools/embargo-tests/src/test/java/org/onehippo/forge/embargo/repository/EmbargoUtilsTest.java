package org.onehippo.forge.embargo.repository;

import javax.jcr.Node;

import org.apache.sling.commons.testing.jcr.MockNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Jeroen Reijn
 */

@RunWith(PowerMockRunner.class)
public class EmbargoUtilsTest {

    @Test
    public void testIsVisibleInPreview() throws Exception {
        Node mockedeNode = new MockNode("/content");
        mockedeNode.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"preview", "live"});

        final boolean visibleInPreview = EmbargoUtils.isVisibleInPreview(mockedeNode);
        assertTrue(visibleInPreview);
    }

    @Test
    public void testIsNotVisibleInPreview() throws Exception {
        Node mockedeNode = new MockNode("/content");
        mockedeNode.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});

        final boolean visibleInPreview = EmbargoUtils.isVisibleInPreview(mockedeNode);
        assertFalse(visibleInPreview);
    }

}
