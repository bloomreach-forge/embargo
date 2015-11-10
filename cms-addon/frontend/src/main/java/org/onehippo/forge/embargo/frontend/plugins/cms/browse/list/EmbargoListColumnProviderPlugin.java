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
package org.onehippo.forge.embargo.frontend.plugins.cms.browse.list;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.hippoecm.frontend.plugins.standards.list.AbstractListColumnProviderPlugin;
import org.hippoecm.frontend.plugins.standards.list.ListColumn;
import org.onehippo.forge.embargo.frontend.plugins.cms.browse.list.comparators.EmbargoDocumentViewComparator;
import org.onehippo.forge.embargo.frontend.plugins.cms.browse.list.resolvers.EmbargoAttributeRenderer;
import org.onehippo.forge.embargo.frontend.plugins.cms.browse.list.resolvers.EmbargoDocumentView;

/**
 * @version $Id$
 */
public class EmbargoListColumnProviderPlugin extends AbstractListColumnProviderPlugin {

    private static final long serialVersionUID = 1L;
    private static final CssResourceReference EMBARGO_PROVIDER_CSS = new CssResourceReference(EmbargoListColumnProviderPlugin.class, "style.css");

    public EmbargoListColumnProviderPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
    }

    @Override
    public IHeaderContributor getHeaderContributor() {
        return new IHeaderContributor() {
            private static final long serialVersionUID = 1L;
            @Override
            public void renderHead(final IHeaderResponse response) {
                response.render(CssHeaderItem.forReference(EMBARGO_PROVIDER_CSS));
            }
        };
    }

    @Override
    public List<ListColumn<Node>> getColumns() {
        return new ArrayList<>();
    }

    @Override
    public List<ListColumn<Node>> getExpandedColumns() {
        List<ListColumn<Node>> columns = getColumns();

        //Groups
        ListColumn<Node> column = new ListColumn<>(new ClassResourceModel("doclisting-embargo-groups", getClass()), "embargo-groups");
        column.setComparator(new EmbargoDocumentViewComparator() {
            private static final long serialVersionUID = -4617312936280189361L;

            @Override
            protected int compare(EmbargoDocumentView view1, EmbargoDocumentView view2) {
                return EmbargoListColumnProviderPlugin.this.compare(view1.getJoinedEmbargoGroups(), view2.getJoinedEmbargoGroups());
            }
        });
        column.setCssClass("doclisting-original-url");
        column.setRenderer(new EmbargoAttributeRenderer() {
            private static final long serialVersionUID = -1485899011687542362L;

            @Override
            protected String getObject(EmbargoDocumentView embargoDocumentView) {
                return embargoDocumentView.getJoinedEmbargoGroups();
            }
        });
        columns.add(column);


        //Expiration date
        column = new ListColumn<>(new ClassResourceModel("doclisting-embargo-expiration-date", getClass()), "embargo-expiration-date");
        column.setComparator(new EmbargoDocumentViewComparator() {
            private static final long serialVersionUID = -4617312936280189361L;

            @Override
            protected int compare(EmbargoDocumentView view1, EmbargoDocumentView view2) {
                return EmbargoListColumnProviderPlugin.this.compare(view1.getExpirationDate(), view2.getExpirationDate());
            }
        });
        column.setCssClass("doclisting-rewrite-url");
        column.setRenderer(new EmbargoAttributeRenderer() {
            private static final long serialVersionUID = -1485899011687542362L;

            @Override
            protected String getObject(EmbargoDocumentView embargoDocumentView) {
                return embargoDocumentView.getExpirationDate() != null ?
                        new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(embargoDocumentView.getExpirationDate().getTime()) :
                        "";

            }
        });
        columns.add(column);

        return columns;
    }

    protected int compare(String s1, String s2) {
        if (s1 == null && s2 == null) {
            return 0;
        } else if (s1 == null) {
            return 1;
        } else if (s2 == null) {
            return -1;
        }
        return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
    }

    protected int compare(Calendar c1, Calendar c2) {
        if (c1 == null && c2 == null) {
            return 0;
        } else if (c1 == null) {
            return 1;
        } else if (c2 == null) {
            return -1;
        }
        return c1.compareTo(c2);
    }
}
