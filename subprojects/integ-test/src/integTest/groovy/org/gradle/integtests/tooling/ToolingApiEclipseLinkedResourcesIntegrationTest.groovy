/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.tooling

import org.gradle.tooling.internal.protocol.eclipse.HierarchicalEclipseProjectVersion1
import org.gradle.tooling.model.eclipse.HierarchicalEclipseProject

/**
 * @author: Szczepan Faber, created at: 6/11/11
 */
class ToolingApiEclipseLinkedResourcesIntegrationTest extends ToolingApiSpecification {

    def "can build linked resources"() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = '''
apply plugin: 'java'
apply plugin: 'eclipse'

eclipse.project {
    linkedResource name: 'foo', location: '/path/to/foo', type : '2'
    linkedResource name: 'bar', locationUri: 'file://..', type : '3'
}
'''
        when:
        HierarchicalEclipseProject minimalProject = withConnection { connection -> connection.getModel(HierarchicalEclipseProject.class) }

        then:
        minimalProject.linkedResources.size() == 0
    }

    def "cannot build linked for previous version"() {
        def projectDir = dist.testDir
        projectDir.file('build.gradle').text = "apply plugin: 'java'"

        when:
        def e = maybeFailWithConnection { connection ->
            connection.modelTypeMap.put(HierarchicalEclipseProjectVersion1.class, HierarchicalEclipseProjectVersion1.class)
            connection.getModel(HierarchicalEclipseProjectVersion1.class)
        }

        then:
        e instanceof Exception
    }
}
