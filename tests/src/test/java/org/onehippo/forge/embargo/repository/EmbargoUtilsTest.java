/*
 * Copyright 2024 Bloomreach B.V. (http://www.bloomreach.com)
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
package org.onehippo.forge.embargo.repository;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.NodeIterator;

import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.commons.testing.jcr.MockNode;
import org.apache.sling.commons.testing.jcr.MockNodeIterator;
import org.apache.sling.commons.testing.jcr.MockProperty;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onehippo.forge.embargo.repository.EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME;
import static org.onehippo.forge.embargo.repository.EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT;
import static org.onehippo.forge.embargo.repository.EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME;

/**
 * @author Jeroen Reijn
 */
public class EmbargoUtilsTest {

    @Test
    public void testEmbargoGetExpirationDateAsNull() throws Exception {
        Node mockRootNode = createMock(Node.class);
        final Calendar embargoExpirationDate = EmbargoUtils.getEmbargoExpirationDate(mockRootNode);
        assertEquals(null, embargoExpirationDate);
    }

    @Test
    public void testEmbargoGetExpirationDateAsNotNull() throws Exception {
        final Calendar instance = Calendar.getInstance();

        Node mockHandleNode = createMock(Node.class);
        Node requestNode = createMock(Node.class);
        Node triggersNode = createMock(Node.class);
        MockProperty triggerFireTimeProperty = new MockProperty(HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME);
        triggerFireTimeProperty.setValue(instance);

        expect(mockHandleNode.isNodeType(HippoNodeType.NT_HANDLE)).andReturn(true);
        expect(mockHandleNode.hasNode(EMBARGO_SCHEDULE_REQUEST_NODE_NAME)).andReturn(true);
        expect(mockHandleNode.getNode(EMBARGO_SCHEDULE_REQUEST_NODE_NAME)).andReturn(requestNode);
        expect(requestNode.hasNode(HIPPOSCHED_TRIGGERS_DEFAULT)).andReturn(true);
        expect(requestNode.getNode(HIPPOSCHED_TRIGGERS_DEFAULT)).andReturn(triggersNode);

        expect(triggersNode.hasProperty(HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME)).andReturn(true);
        expect(triggersNode.getProperty(HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME)).andReturn(triggerFireTimeProperty);

        replay(mockHandleNode, requestNode, triggersNode);

        final Calendar embargoExpirationDate = EmbargoUtils.getEmbargoExpirationDate(mockHandleNode);
        assertEquals(instance, embargoExpirationDate);
    }

    @Test
    public void testIsVisibleInPreview() throws Exception {
        Node mockedNode = new MockNode("/content");
        mockedNode.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"preview", "live"});

        final boolean visibleInPreview = EmbargoUtils.isVisibleInPreview(mockedNode);
        assertTrue(visibleInPreview);
    }

    @Test
    public void testIsNotVisibleInPreview() throws Exception {
        Node mockedNode = new MockNode("/content");
        mockedNode.setProperty(HippoNodeType.HIPPO_AVAILABILITY, new String[]{"live"});

        final boolean visibleInPreview = EmbargoUtils.isVisibleInPreview(mockedNode);
        assertFalse(visibleInPreview);
    }

    @Test
    public void testGetEmptyArrayForDocumentVariants() throws Exception {
        Node mockedeNode = new MockNode("/content");
        final Node[] documentVariants = EmbargoUtils.getDocumentVariants(mockedeNode);
        assertEquals(documentVariants.length, 0);
    }

    @Test
    public void testGetNonEmptyArrayForDocumentVariants() throws Exception {
        Node mockHandleNode = createMock(Node.class);
        Node mockedChildNode = createMock(Node.class);
        NodeIterator mockNodeIterator = new MockNodeIterator(new Node[]{mockedChildNode});

        expect(mockHandleNode.getName()).andReturn("testNode");
        expect(mockHandleNode.getNodes()).andReturn(mockNodeIterator);
        expect(mockedChildNode.isNodeType(JcrConstants.MIX_REFERENCEABLE)).andReturn(true);
        expect(mockedChildNode.getName()).andReturn("testNode");


        replay(mockHandleNode, mockedChildNode);

        final Node[] documentVariants = EmbargoUtils.getDocumentVariants(mockHandleNode);

        assertEquals(documentVariants.length, 1);
    }

}
