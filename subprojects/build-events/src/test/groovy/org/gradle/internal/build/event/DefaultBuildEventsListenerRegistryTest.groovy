/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.internal.build.event

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.internal.provider.Providers
import org.gradle.initialization.BuildEventConsumer
import org.gradle.internal.build.event.types.DefaultTaskDescriptor
import org.gradle.internal.build.event.types.DefaultTaskFailureResult
import org.gradle.internal.build.event.types.DefaultTaskFinishedProgressEvent
import org.gradle.internal.build.event.types.DefaultTaskSkippedResult
import org.gradle.internal.build.event.types.DefaultTaskSuccessResult
import org.gradle.internal.event.DefaultListenerManager
import org.gradle.internal.operations.BuildOperationListenerManager
import org.gradle.test.fixtures.concurrent.ConcurrentSpec
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFailureResult
import org.gradle.tooling.events.task.TaskFinishEvent
import org.gradle.tooling.events.task.TaskSkippedResult
import org.gradle.tooling.events.task.TaskSuccessResult

class DefaultBuildEventsListenerRegistryTest extends ConcurrentSpec {
    def factory = new MockBuildEventListenerFactory()
    def listenerManager = new DefaultListenerManager()
    def registry = new DefaultBuildEventsListenerRegistry([factory], listenerManager, Stub(BuildOperationListenerManager), executorFactory)

    def cleanup() {
        // Signal the end of the build, to stop everything
        signalBuildFinished()
    }

    def "listener receives task finish events"() {
        def listener = Mock(OperationCompletionListener)
        def provider = Providers.of(listener)
        def success = taskFinishEvent()
        def failure = failedTaskFinishEvent()
        def skipped = skippedTaskFinishEvent()

        when:
        registry.subscribe(provider)

        then:
        registry.subscriptions.size() == 1
        0 * listener._

        when:
        async {
            factory.fire(success)
            factory.fire(failure)
            factory.fire(skipped)
            signalBuildFinished()
        }

        then:
        1 * listener.onFinish({ it instanceof TaskFinishEvent && it.result instanceof TaskSuccessResult })
        1 * listener.onFinish({ it instanceof TaskFinishEvent && it.result instanceof TaskFailureResult })
        1 * listener.onFinish({ it instanceof TaskFinishEvent && it.result instanceof TaskSkippedResult })
        0 * listener._
    }

    def "does nothing when listener is already subscribed"() {
        def listener = Mock(OperationCompletionListener)
        def provider = Providers.of(listener)

        when:
        registry.subscribe(provider)
        registry.subscribe(provider)

        then:
        registry.subscriptions.size() == 1
    }

    def "broken listener is quarantined and failure rethrown at completion of build"() {
        def failure = new RuntimeException()
        def listener1 = Mock(OperationCompletionListener)
        def listener2 = Mock(OperationCompletionListener)

        when:
        registry.subscribe(Providers.of(listener1))
        registry.subscribe(Providers.of(listener2))
        async {
            factory.fire(taskFinishEvent())
            thread.blockUntil.handled
            factory.fire(taskFinishEvent())
            signalBuildFinished()
        }

        then:
        1 * listener1.onFinish(_) >> {
            instant.handled
            throw failure
        }
        2 * listener2.onFinish(_)
        0 * listener1._
        0 * listener2._

        and:
        def e = thrown(RuntimeException)
        e.is(failure)
    }

    private signalBuildFinished() {
        listenerManager.getBroadcaster(BuildListener).buildFinished(Stub(BuildResult))
    }

    private DefaultTaskFinishedProgressEvent taskFinishEvent() {
        new DefaultTaskFinishedProgressEvent(123L, Stub(DefaultTaskDescriptor), Stub(DefaultTaskSuccessResult))
    }

    private DefaultTaskFinishedProgressEvent failedTaskFinishEvent() {
        new DefaultTaskFinishedProgressEvent(123L, Stub(DefaultTaskDescriptor), Stub(DefaultTaskFailureResult))
    }

    private DefaultTaskFinishedProgressEvent skippedTaskFinishEvent() {
        new DefaultTaskFinishedProgressEvent(123L, Stub(DefaultTaskDescriptor), Stub(DefaultTaskSkippedResult))
    }

    class MockBuildEventListenerFactory implements BuildEventListenerFactory {
        private List<BuildEventConsumer> consumers = []

        def fire(Object event) {
            consumers.forEach {
                it.dispatch(event)
            }
        }

        @Override
        Iterable<Object> createListeners(BuildEventSubscriptions subscriptions, BuildEventConsumer consumer) {
            consumers.add(consumer)
            return []
        }
    }
}
