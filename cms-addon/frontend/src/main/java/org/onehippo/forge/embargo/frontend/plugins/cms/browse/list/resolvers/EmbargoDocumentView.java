/*
 * Copyright 2013-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.embargo.frontend.plugins.cms.browse.list.resolvers;

import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.wicket.model.IDetachable;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.Observable;
import org.hippoecm.repository.api.HippoNodeType;

import org.onehippo.forge.embargo.repository.EmbargoConstants;
import org.onehippo.forge.embargo.repository.EmbargoUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolver for one of the columns in the document list view
 */
public class EmbargoDocumentView implements IObservable, IDetachable {

    private static final Logger log = LoggerFactory.getLogger(EmbargoDocumentView.class);

    private JcrNodeModel nodeModel;
    private Observable observable;
    private transient boolean loaded = false;

    private transient String[] embargoGroups;
    private transient String joinedEmbargoGroups;
    private transient Calendar expirationDate;

    public EmbargoDocumentView(JcrNodeModel nodeModel) {
        this.nodeModel = nodeModel;
        observable = new Observable(nodeModel);
    }

    public String[] getEmbargoGroups() {
        load();
        return embargoGroups;
    }

    public String getJoinedEmbargoGroups() {
        load();
        return joinedEmbargoGroups;
    }

    public Calendar getExpirationDate() {
        load();
        return expirationDate;
    }

    public void detach() {
        loaded = false;

        embargoGroups = null;

        nodeModel.detach();
        observable.detach();
    }

    void load() {
        if (!loaded) {
            observable.setTarget(null);
            try {
                Node node = nodeModel.getNode();
                if (node != null) {

                    Node handleNode = null;

                    if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                        handleNode = node;
                        expirationDate = EmbargoUtils.getEmbargoExpirationDate(handleNode);
                    } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        handleNode = node.getParent();
                    } else if (node.isNodeType(JcrConstants.NT_VERSION)) {
                        Node frozen = node.getNode(JcrConstants.JCR_FROZENNODE);
                        String primary = frozen.getProperty(JcrConstants.JCR_FROZENPRIMARYTYPE).getString();
                        final NodeTypeManager ntMgr = frozen.getSession().getWorkspace().getNodeTypeManager();
                        final NodeType primaryType = ntMgr.getNodeType(primary);
                        if (primaryType.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                            handleNode = frozen.getParent();
                        }
                    }
                    if (handleNode != null) {
                        if (handleNode.hasProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME)) {
                            Value[] groups = handleNode.getProperty(EmbargoConstants.EMBARGO_GROUP_PROPERTY_NAME).getValues();
                            embargoGroups = new String[groups.length];
                            for (int i = 0; i < groups.length; i++) {
                                embargoGroups[i] = new String(groups[i].getString());
                            }
                            joinedEmbargoGroups = StringUtils.join(embargoGroups, ',');
                        }
                        expirationDate = EmbargoUtils.getEmbargoExpirationDate(handleNode);
                    }
                }
            } catch (RepositoryException ex) {
                log.error("Unable to obtain embargo document properties", ex);
            }
            loaded = true;
        }
    }

    public void setObservationContext(IObservationContext<? extends IObservable> context) {
        observable.setObservationContext(context);
    }

    public void startObservation() {
        observable.startObservation();
    }

    public void stopObservation() {
        observable.stopObservation();
    }


}
