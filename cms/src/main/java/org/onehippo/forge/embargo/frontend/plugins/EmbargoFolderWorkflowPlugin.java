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
package org.onehippo.forge.embargo.frontend.plugins;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.hippoecm.addon.workflow.StdWorkflow;
import org.hippoecm.addon.workflow.WorkflowDescriptorModel;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.frontend.skin.Icon;
import org.hippoecm.repository.api.HippoWorkspace;
import org.hippoecm.repository.api.Workflow;
import org.hippoecm.repository.api.WorkflowDescriptor;
import org.hippoecm.repository.api.WorkflowManager;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.onehippo.forge.embargo.repository.EmbargoConstants;
import org.onehippo.forge.embargo.repository.EmbargoUtils;
import org.onehippo.forge.embargo.repository.workflow.EmbargoWorkflow;
import org.onehippo.repository.security.JvmCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Folder context-menu plugin that applies embargo to all unembargoed documents in the selected folder.
 * Presents a confirmation dialog with an option to recurse into sub-folders.
 * Appears only for users who belong to at least one embargo-enabled group (or are admins).
 */
public class EmbargoFolderWorkflowPlugin extends RenderPlugin<WorkflowDescriptor> {

    private static final Logger log = LoggerFactory.getLogger(EmbargoFolderWorkflowPlugin.class);

    static {
        log.warn("EMBARGO-DEBUG class loaded by classloader");
    }

    private final boolean userCanEmbargo;

    private static IPluginContext logConstructorEntry(final IPluginContext context) {
        log.warn("EMBARGO-DEBUG constructor entry (pre-super)");
        return context;
    }

    public EmbargoFolderWorkflowPlugin(final IPluginContext context, final IPluginConfig config) {
        super(logConstructorEntry(context), config);
        log.warn("EMBARGO-DEBUG constructor post-super, defaultModel={}", getDefaultModel());
        this.userCanEmbargo = currentUserCanEmbargo();
        log.warn("EMBARGO-DEBUG userCanEmbargo={}", userCanEmbargo);
        add(createEmbargoAllMenuItem());
        add(createUnEmbargoAllMenuItem());
    }

    private StdWorkflow<FolderWorkflow> createEmbargoAllMenuItem() {
        return new StdWorkflow<FolderWorkflow>("embargoAllMenuItem",
                new StringResourceModel("embargo-all-in-folder-label", this, null),
                getPluginContext(),
                (WorkflowDescriptorModel) getDefaultModel()) {

            public boolean recurseSubFolders = false;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(userCanEmbargo);
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.LOCKED);
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                return new EmbargoAllFolderDialog(this, new PropertyModel<>(this, "recurseSubFolders"),
                        new StringResourceModel("embargo-all-folder-title", EmbargoFolderWorkflowPlugin.this, null),
                        new StringResourceModel("embargo-all-confirm-text", EmbargoFolderWorkflowPlugin.this, null));
            }

            @Override
            protected String execute(final FolderWorkflow workflow) throws Exception {
                applyEmbargoToFolderDocuments(recurseSubFolders);
                return null;
            }
        };
    }

    private StdWorkflow<FolderWorkflow> createUnEmbargoAllMenuItem() {
        return new StdWorkflow<FolderWorkflow>("unEmbargoAllMenuItem",
                new StringResourceModel("unembargo-all-in-folder-label", this, null),
                getPluginContext(),
                (WorkflowDescriptorModel) getDefaultModel()) {

            public boolean recurseSubFolders = false;

            @Override
            protected void onConfigure() {
                super.onConfigure();
                setVisible(userCanEmbargo);
            }

            @Override
            protected Component getIcon(final String id) {
                return HippoIcon.fromSprite(id, Icon.UNLOCKED);
            }

            @Override
            protected IDialogService.Dialog createRequestDialog() {
                return new EmbargoAllFolderDialog(this, new PropertyModel<>(this, "recurseSubFolders"),
                        new StringResourceModel("unembargo-all-folder-title", EmbargoFolderWorkflowPlugin.this, null),
                        new StringResourceModel("unembargo-all-confirm-text", EmbargoFolderWorkflowPlugin.this, null));
            }

            @Override
            protected String execute(final FolderWorkflow workflow) throws Exception {
                applyUnEmbargoToFolderDocuments(recurseSubFolders);
                return null;
            }
        };
    }

    private void applyEmbargoToFolderDocuments(final boolean recurse) throws RepositoryException {
        final Node folderNode = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
        final javax.jcr.Session jcrSession = getJcrSession();
        final WorkflowManager wfManager = ((HippoWorkspace) jcrSession.getWorkspace()).getWorkflowManager();
        embargoFolder(folderNode, recurse, jcrSession.getUserID(), wfManager);
    }

    private void embargoFolder(final Node folder, final boolean recurse,
                                final String userId, final WorkflowManager wfManager) throws RepositoryException {
        final NodeIterator children = folder.getNodes();
        while (children.hasNext()) {
            final Node child = children.nextNode();
            if (child.isNodeType(EmbargoConstants.HIPPO_HANDLE)) {
                if (!child.isNodeType(EmbargoConstants.EMBARGO_MIXIN_NAME)) {
                    embargoDocument(child, userId, wfManager);
                }
            } else if (recurse && child.isNodeType("hippostd:folder")) {
                embargoFolder(child, true, userId, wfManager);
            }
        }
    }

    private void embargoDocument(final Node handle, final String userId, final WorkflowManager wfManager) {
        final Node[] variants;
        try {
            variants = EmbargoUtils.getDocumentVariants(handle);
        } catch (RepositoryException e) {
            log.error("Could not get variants for document at {}", handle, e);
            return;
        }
        if (variants.length == 0) {
            return;
        }
        try {
            final Workflow wf = wfManager.getWorkflow("embargo", variants[0]);
            if (wf instanceof EmbargoWorkflow) {
                ((EmbargoWorkflow) wf).addEmbargo(userId, handle.getIdentifier(), null);
            }
        } catch (Exception e) {
            log.error("Failed to set embargo on document at {}", handle, e);
        }
    }

    private void applyUnEmbargoToFolderDocuments(final boolean recurse) throws RepositoryException {
        final Node folderNode = ((WorkflowDescriptorModel) getDefaultModel()).getNode();
        final javax.jcr.Session jcrSession = getJcrSession();
        final WorkflowManager wfManager = ((HippoWorkspace) jcrSession.getWorkspace()).getWorkflowManager();
        unembargoFolder(folderNode, recurse, wfManager);
    }

    private void unembargoFolder(final Node folder, final boolean recurse,
                                  final WorkflowManager wfManager) throws RepositoryException {
        final NodeIterator children = folder.getNodes();
        while (children.hasNext()) {
            final Node child = children.nextNode();
            if (child.isNodeType(EmbargoConstants.HIPPO_HANDLE)) {
                if (child.isNodeType(EmbargoConstants.EMBARGO_MIXIN_NAME)) {
                    unembargoDocument(child, wfManager);
                }
            } else if (recurse && child.isNodeType("hippostd:folder")) {
                unembargoFolder(child, true, wfManager);
            }
        }
    }

    private void unembargoDocument(final Node handle, final WorkflowManager wfManager) {
        final Node[] variants;
        try {
            variants = EmbargoUtils.getDocumentVariants(handle);
        } catch (RepositoryException e) {
            log.error("Could not get variants for document at {}", handle, e);
            return;
        }
        if (variants.length == 0) {
            return;
        }
        try {
            final Workflow wf = wfManager.getWorkflow("embargo", variants[0]);
            if (wf instanceof EmbargoWorkflow) {
                ((EmbargoWorkflow) wf).removeEmbargo(handle.getIdentifier());
            }
        } catch (Exception e) {
            log.error("Failed to remove embargo on document at {}", handle, e);
        }
    }

    private boolean currentUserCanEmbargo() {
        final javax.jcr.Session jcrSession = getJcrSession();
        final String userId = jcrSession.getUserID();
        javax.jcr.Session configSession = null;
        try {
            configSession = jcrSession.getRepository().login(JvmCredentials.getCredentials("configuser"));
            final boolean isAdmin = EmbargoUtils.isAdminUser(configSession, userId);
            final String[] embargoGroups = EmbargoUtils.getCurrentUserEmbargoEnabledGroups(configSession, userId);
            return isAdmin || embargoGroups.length > 0;
        } catch (RepositoryException e) {
            log.error("Error checking embargo group membership for user {}", userId, e);
            return false;
        } finally {
            if (configSession != null) {
                configSession.logout();
            }
        }
    }

    private javax.jcr.Session getJcrSession() {
        return ((UserSession) Session.get()).getJcrSession();
    }
}
