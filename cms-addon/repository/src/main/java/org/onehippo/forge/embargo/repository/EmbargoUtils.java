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
package org.onehippo.forge.embargo.repository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class EmbargoUtils {

    private static Logger log = LoggerFactory.getLogger(EmbargoUtils.class);

    public static String[] getCurrentUserEmbargoEnabledGroups(Session session, String userIdentity) throws RepositoryException {

        Query selectGroupsQuery = session.getWorkspace().getQueryManager().createQuery(
                EmbargoConstants.SELECT_GROUPS_QUERY.replace("{}", userIdentity),
                Query.SQL);
        NodeIterator groupNodes = selectGroupsQuery.execute().getNodes();
        if (!groupNodes.hasNext()) {
            return new String[]{};
        }

        List<String> groupNames = new ArrayList<String>();
        List<String> embargoEnabledGroupNames = getAllEmbargoEnabledGroups(session);

        while (groupNodes.hasNext()) {
            String groupName = groupNodes.nextNode().getName();
            if (embargoEnabledGroupNames.contains(groupName)) {
                groupNames.add(groupName);
            }
        }

        return groupNames.toArray(new String[groupNames.size()]);
    }

    public static List<String> getAllEmbargoEnabledGroups(Session session) throws RepositoryException {
        NodeIterator embargoGroupMappingNodes = session.getRootNode().getNode(EmbargoConstants.EMBARGO_GROUPS_MAPPING_NODE_PATH)
                .getNodes(EmbargoConstants.EMBARGO_GROUPS_MAPPING_NODE_NAMES);

        List<String> embargoGroupNames = new ArrayList<String>();
        while (embargoGroupMappingNodes.hasNext()) {
            Node embargoGroupMappingNode = embargoGroupMappingNodes.nextNode();
            if (embargoGroupMappingNode.hasProperty(HippoNodeType.HIPPO_GROUPS)) {
                Value[] embargoGroups = embargoGroupMappingNode.getProperty(HippoNodeType.HIPPO_GROUPS).getValues();
                for (final Value embargoGroup : embargoGroups) {
                    embargoGroupNames.add(embargoGroup.getString());
                }
            }
        }
        return embargoGroupNames;
    }

    public static Calendar getEmbargoExpirationDate(Node hippoHandleNode) throws RepositoryException {
        if (hippoHandleNode.isNodeType(HippoNodeType.NT_HANDLE) && hippoHandleNode.hasNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME)) {
            Node requestNode = hippoHandleNode.getNode(EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME);
            if (requestNode.hasNode(EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT)) {
                Node defaultTriggerNode = requestNode.getNode(EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT);
                if (defaultTriggerNode.hasProperty(EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME)) {
                    return defaultTriggerNode.getProperty(EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME).getDate();
                }
            }
        }
        return null;
    }

    /**
     * We only want to show the menu for the 'preview' variant of a document
     *
     * @param documentNode
     * @return
     */
    public static boolean isVisibleInPreview(Node documentNode) {
        try {
            if (documentNode.hasProperty(HippoNodeType.HIPPO_AVAILABILITY)) {
                for (Value availability : documentNode.getProperty(HippoNodeType.HIPPO_AVAILABILITY).getValues()) {
                    if ("preview".equals(availability.getString())) {
                        return true;
                    }
                }
            } else {
                log.warn("Document '{}' does not contain the property '{}'. No 'View' menu items will be shown.",
                        documentNode.getPath(), HippoNodeType.HIPPO_AVAILABILITY);
            }
        } catch (RepositoryException e) {
            log.error("Error getting " + HippoNodeType.HIPPO_AVAILABILITY + " property from document", e);
        }
        return false;
    }

    public static Node[] getDocumentVariants(Node documentHandleNode) throws RepositoryException {
        NodeIterator nodeIterator = documentHandleNode.getNodes();
        List<Node> documentNodes = new ArrayList<Node>();
        while (nodeIterator.hasNext()) {
            Node documentNode = nodeIterator.nextNode();
            if (documentNode.isNodeType(HippoNodeType.NT_HARDDOCUMENT)) {
                documentNodes.add(documentNode);
            }
        }
        return documentNodes.toArray(new Node[documentNodes.size()]);
    }
}
