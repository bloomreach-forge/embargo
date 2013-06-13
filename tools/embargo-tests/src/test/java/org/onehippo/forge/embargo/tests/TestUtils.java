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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class TestUtils {

    public static Logger log = LoggerFactory.getLogger(TestUtils.class);

    public static void printTree(Node node) throws RepositoryException {
        printTree(node, 1);
    }

    public static void printTree(Node node, int depth) throws RepositoryException {
        final NodeIterator nodeIterator = node.getNodes();
        while (nodeIterator.hasNext()) {
            final Node node1 = nodeIterator.nextNode();

            log.info(printDash(depth) + node1.getName());
            if (node1.hasNodes()) {
                printTree(node1, depth + 1);
            }
        }
    }

    public static String printDash(int depth) {
        String dashes = "";
        for (int i = 0; i < depth; i++) {
            if (i != depth) {
                dashes += "-";
            }
        }
        return dashes;
    }
}
