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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.forge.embargo.repository.EmbargoConstants.EMBARGO_DOMAIN_PATH;
import static org.onehippo.forge.embargo.repository.EmbargoConstants.EMBARGO_SCHEDULE_REQUEST_NODE_NAME;
import static org.onehippo.forge.embargo.repository.EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT;
import static org.onehippo.forge.embargo.repository.EmbargoConstants.HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME;

/**
 * @version $Id$
 */
public final class EmbargoUtils {

    private static final String HIPPOSYS_AUTHROLE = "hipposys:authrole";

    private EmbargoUtils() {}

    private static Logger log = LoggerFactory.getLogger(EmbargoUtils.class);

    public static String[] getCurrentUserEmbargoEnabledGroups(Session session, String userIdentity) {
        String[] allUserGroups = getAllUserGroups(session, userIdentity);
        List<String> embargoEnabledUserGroups = new ArrayList<String>();
        List<String> allEmbargoEnabledGroups = getAllEmbargoEnabledGroups(session);

        for (final String allUserGroup : allUserGroups) {
            if (allEmbargoEnabledGroups.contains(allUserGroup)) {
                embargoEnabledUserGroups.add(allUserGroup);
            }
        }
        return embargoEnabledUserGroups.toArray(new String[embargoEnabledUserGroups.size()]);
    }

    public static boolean isAdminUser(Session session, String userIdentity) {
        String[] allUserGroups = getAllUserGroups(session, userIdentity);
        for (final String allUserGroup : allUserGroups) {
            if (EmbargoConstants.ADMIN_GROUP_NAME.equals(allUserGroup)) {
                return true;
            }
        }
        return false;
    }


    public static String[] getAllUserGroups(Session session, String userIdentity) {
        try{
            Query selectGroupsQuery = session.getWorkspace().getQueryManager().createQuery(
                    EmbargoConstants.SELECT_GROUPS_QUERY.replace("{}", userIdentity),
                    Query.SQL);
            NodeIterator groupNodes = selectGroupsQuery.execute().getNodes();
            List<String> groupNames = new ArrayList<String>();
            while (groupNodes.hasNext()) {
                groupNames.add(groupNodes.nextNode().getName());
            }
            return groupNames.toArray(new String[groupNames.size()]);

        } catch (RepositoryException e){
            log.error("Could not retrieve user groups", e);
            return new String[]{};
        }
    }

    public static List<String> getAllEmbargoEnabledGroups(Session session) {
        Node embargoGroupsMappingNode;
        try {
            embargoGroupsMappingNode = session.getRootNode().getNode(EMBARGO_DOMAIN_PATH);
        } catch (RepositoryException e) {
            log.error("Embargo domain does not exist at {}", EMBARGO_DOMAIN_PATH);
            return new ArrayList<>();
        }

        try {
            final Set<String> embargoGroupNames = new HashSet<>();
            for(final Node authRole : getEmbargoAuthRoleNodes(embargoGroupsMappingNode)) {
                if (authRole.hasProperty(HippoNodeType.HIPPO_GROUPS)) {
                    Value[] embargoGroups = authRole.getProperty(HippoNodeType.HIPPO_GROUPS).getValues();
                    for (final Value embargoGroup : embargoGroups) {
                        embargoGroupNames.add(embargoGroup.getString());
                    }
                }
            }
            return new ArrayList<>(embargoGroupNames);
        } catch (RepositoryException e){
            log.error("Could not retrieve embargo enabled groups", e);
            return new ArrayList<>();
        }
    }

    private static List<Node> getEmbargoAuthRoleNodes(final Node embargoGroupsMappingNode) throws RepositoryException {
        final List<Node> authRoleNodes = new ArrayList<>();
        final NodeIterator nodes = embargoGroupsMappingNode.getNodes();
        while(nodes.hasNext()) {
            final Node node = nodes.nextNode();
            if(node.isNodeType(HIPPOSYS_AUTHROLE)) {
                authRoleNodes.add(node);
            }
        }
        return authRoleNodes;
    }

    public static Calendar getEmbargoExpirationDate(Node hippoHandleNode) throws RepositoryException {
        if (hippoHandleNode.isNodeType(HippoNodeType.NT_HANDLE)
                && hippoHandleNode.hasNode(EMBARGO_SCHEDULE_REQUEST_NODE_NAME)) {
            Node requestNode = hippoHandleNode.getNode(EMBARGO_SCHEDULE_REQUEST_NODE_NAME);
            if (requestNode.hasNode(HIPPOSCHED_TRIGGERS_DEFAULT)) {
                Node defaultTriggerNode = requestNode.getNode(HIPPOSCHED_TRIGGERS_DEFAULT);
                if (defaultTriggerNode.hasProperty(HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME)) {
                    return defaultTriggerNode.getProperty(HIPPOSCHED_TRIGGERS_DEFAULT_PROPERTY_FIRETIME).getDate();
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
