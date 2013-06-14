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
package org.onehippo.forge.embargo.hst.addons;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;

import org.hippoecm.hst.core.jcr.pool.BasicPoolingRepository;
import org.hippoecm.hst.core.jcr.pool.MultipleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class SessionRefreshEventListener implements EventListener {

    private static Logger log = LoggerFactory.getLogger(SessionRefreshEventListener.class);

    private MultipleRepository repository;
    private SimpleCredentials credentials;

    @Override
    public void onEvent(final EventIterator events) {
        for(Repository aRepository : repository.getRepositoryMap().values()){
            if (aRepository instanceof BasicPoolingRepository) {
                try {
                    ((BasicPoolingRepository) aRepository).initialize();
                } catch (RepositoryException e) {
                    log.error("", e);
                }
            }
        }
    }

    public SimpleCredentials getCredentials() {
        return credentials;
    }

    public void setCredentials(final SimpleCredentials credentials) {
        this.credentials = credentials;
    }

    public MultipleRepository getRepository() {
        return repository;
    }

    public void setRepository(final MultipleRepository repository) {
        this.repository = repository;
    }
}
