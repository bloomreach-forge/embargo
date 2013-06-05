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
package org.onehippo.forge.embargo.tests;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.EventCollection;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JcrPluginConfig;
import org.hippoecm.frontend.service.ITitleDecorator;
import org.hippoecm.frontend.service.IconSize;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.forge.embargo.frontend.plugins.EmbargoWorkflowPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id: EmbargoWorkflowTest.java 79 2013-05-24 14:49:42Z mchatzidakis $
 */
public class EmbargoWorkflowTest extends PluginTest {

    private static Logger log = LoggerFactory.getLogger(EmbargoWorkflowTest.class);
/*
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        build(session, content);

        config = new JcrPluginConfig(new JcrNodeModel("/test/plugin"));
    }*/

    @Test
    public void testAddEmbargo(){

    }


    public static class ContentPanel extends RenderPlugin<Void> {
        private static final long serialVersionUID = 1L;

        ITitleDecorator decorator;

        public ContentPanel(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(decorator = new ITitleDecorator() {
                private static final long serialVersionUID = 1L;

                @SuppressWarnings("unchecked")
                public IModel<String> getTitle() {
                    return (IModel<String>) getDefaultModel();
                }

                public ResourceReference getIcon(IconSize type) {
                    // TODO Auto-generated method stub
                    return null;
                }

            }, context.getReference(this).getServiceId());
        }

        void reregister() {
            IPluginContext context = getPluginContext();
            String serviceId = context.getReference(this).getServiceId();
            context.unregisterService(decorator, serviceId);
            context.registerService(decorator, serviceId);
        }
    }

    static class ObservableModel implements IModel, IObservable {
        private static final long serialVersionUID = 1L;

        private Object object = null;
        private IObservationContext obContext;

        public Object getObject() {
            return object;
        }

        public void setObject(Object object) {
            this.object = object;
            if (obContext != null) {
                notifyObservers();
            }
        }

        public void detach() {
        }

        public void setObservationContext(IObservationContext context) {
            this.obContext = context;
        }

        public void startObservation() {
        }

        public void stopObservation() {
        }

        void notifyObservers() {
            EventCollection<IEvent<IObservable>> events = new EventCollection<IEvent<IObservable>>();
            events.add(new IEvent<IObservable>() {

                public IObservable getSource() {
                    return ObservableModel.this;
                }

            });
            obContext.notifyObservers(events);
        }
    }

    final static String[] content = {
            "/test", "nt:unstructured",
            "/test/plugin", "frontend:pluginconfig",
            "plugin.class", EmbargoWorkflowPlugin.class.getName(),
            "wicket.id", "service.root",
            "/config/panel", "frontend:plugincluster",
            "/config/panel/plugin", "frontend:plugin",
            "plugin.class", ContentPanel.class.getName(),
    };

    IPluginConfig config;

}
