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

import javax.jcr.SimpleCredentials;

/**
 * @version $Id: TestConstants.java 79 2013-05-24 14:49:42Z mchatzidakis $
 */
public class TestConstants {

    public static final SimpleCredentials ADMIN_CREDENTIALS = new SimpleCredentials("admin", "admin".toCharArray());
    public static final SimpleCredentials EDITOR_CREDENTIALS = new SimpleCredentials("editor", "editor".toCharArray());
    //public static final SimpleCredentials AUTHOR_CREDENTIALS = new SimpleCredentials("author", "author".toCharArray());
    public static final SimpleCredentials EMBARGO_USER_CREDENTIALS = new SimpleCredentials("embargouser", "embargouser".toCharArray());

    public static final String CONTENT_DOCUMENTS_PATH = "/content/documents";
    public static final String TEST_DOCUMENT_NAME = "test";

    public static final String REPOSITORY_PATH_SYSTEM_PROPERTY = "testrepopath";
}
