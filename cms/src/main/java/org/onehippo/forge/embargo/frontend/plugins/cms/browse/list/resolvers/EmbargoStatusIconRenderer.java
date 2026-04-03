/*
 * Copyright 2026 Bloomreach B.V. (http://www.bloomreach.com)
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;
import org.hippoecm.frontend.skin.Icon;

/**
 * Renders a lock icon for embargoed documents in the document list view,
 * or nothing for documents that are not embargoed.
 */
public class EmbargoStatusIconRenderer extends AbstractNodeRenderer {

    @Override
    protected Component getViewer(final String id, final Node node) throws RepositoryException {
        final EmbargoDocumentView view = new EmbargoDocumentView(new JcrNodeModel(node));
        if (view.isEmbargoed()) {
            return HippoIcon.fromSprite(id, Icon.LOCKED);
        }
        return new Label(id).setVisible(false);
    }
}
