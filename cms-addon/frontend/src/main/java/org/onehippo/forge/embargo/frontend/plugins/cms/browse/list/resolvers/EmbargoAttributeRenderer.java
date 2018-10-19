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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugins.standards.list.resolvers.AbstractNodeRenderer;

/**
 * Reenderer for one of the attributes in the document list view
 */
public abstract class EmbargoAttributeRenderer extends AbstractNodeRenderer {

    @Override
    protected Component getViewer(String id, final Node node) throws RepositoryException {
        return new Label(id, new EmbargoDocumentViewModel(node));
    }

    protected abstract String getObject(EmbargoDocumentView embargoDocumentView);

    class EmbargoDocumentViewModel extends AbstractReadOnlyModel<String> {

        EmbargoDocumentView embargoDocumentView;

        public EmbargoDocumentViewModel(Node node) {
            embargoDocumentView = new EmbargoDocumentView(new JcrNodeModel(node));
        }

        @Override
        public String getObject() {
            return EmbargoAttributeRenderer.this.getObject(embargoDocumentView);
        }

        @Override
        public void detach() {
            embargoDocumentView.detach();
        }

    }

}
