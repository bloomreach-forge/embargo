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
package org.onehippo.forge.embargo.tests.helpers;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mrop
 */
 
 /*
 * $Id: RepositorySessionBuilder.java 79 2013-05-24 14:49:42Z mchatzidakis $
 */
public class RepositorySessionBuilder {

    private static final Logger log = LoggerFactory.getLogger(RepositorySessionBuilder.class);

    private HippoRepository repository;

    public RepositorySessionBuilder() {
        try {
            repository = HippoRepositoryFactory.getHippoRepository(TestConstants.HIPPO_RMI_CONNECTION);
            repository.login(TestConstants.ADMIN_CREDENTIALS);
        } catch (Exception e) {
            log.error("Could not login to HippoRepository", e);
            repository = null;
        }
    }

    public HippoRepository getRepository() {
        return repository;
    }

    public Session build(SimpleCredentials credentials) {
        try {
            if (repository != null) {
                return repository.login(credentials);
            }
        } catch (RepositoryException e) {
            log.error("Could not login to HippoRepository", e);
        }

        return null;
    }

    public void destroy() {
        // close repository
        log.info("Closing repository.");
        if (repository != null) {
            repository.close();
            repository = null;
        }

        // done
        log.info("Repository closed.");

    }

}
