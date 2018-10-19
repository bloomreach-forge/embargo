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
package org.onehippo.forge.embargo.frontend.plugins.cms.root;

import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;

/**
 * Indicator showing red side color for embargo users
 */
public class EmbargoIndicatorPlugin extends RenderPlugin {

    private static final CssResourceReference EMBARGO_INDICATOR_CSS = new CssResourceReference(EmbargoIndicatorPlugin.class, "EmbargoIndicatorPlugin.css");

    public EmbargoIndicatorPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);
        context.registerService(this, EmbargoIndicatorPlugin.class.getName());
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(EMBARGO_INDICATOR_CSS));
    }


}
