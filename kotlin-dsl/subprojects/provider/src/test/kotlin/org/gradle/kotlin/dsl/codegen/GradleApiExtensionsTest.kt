/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.kotlin.dsl.codegen

import com.nhaarman.mockito_kotlin.atMost
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions

import org.gradle.api.specs.Specs

import org.gradle.kotlin.dsl.accessors.TestWithClassPath

import org.gradle.kotlin.dsl.fixtures.codegen.ClassAndGroovyNamedArguments
import org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass
import org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClassParameterizedType
import org.gradle.kotlin.dsl.fixtures.codegen.GroovyNamedArguments
import org.gradle.kotlin.dsl.support.normaliseLineSeparators

import org.hamcrest.CoreMatchers.containsString
import org.junit.Assert.assertThat
import org.junit.Test

import org.slf4j.Logger

import java.io.File

import java.util.function.Consumer

import kotlin.reflect.KClass


class GradleApiExtensionsTest : TestWithClassPath() {

    @Test
    fun `maps java-lang-Class to kotlin-reflect-KClass`() {

        apiKotlinExtensionsGenerationFor(ClassToKClass::class) {

            assertGeneratedExtensions(
                """
                inline fun org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`rawClass`(`type`: kotlin.reflect.KClass<*>): Unit =
                    `rawClass`(`type`.java)
                """,
                """
                inline fun org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`unknownClass`(`type`: kotlin.reflect.KClass<*>): Unit =
                    `unknownClass`(`type`.java)
                """,
                """
                inline fun org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`invariantClass`(`type`: kotlin.reflect.KClass<kotlin.Number>): Unit =
                    `invariantClass`(`type`.java)
                """,
                """
                inline fun org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`covariantClass`(`type`: kotlin.reflect.KClass<out kotlin.Number>): Unit =
                    `covariantClass`(`type`.java)
                """,
                """
                inline fun org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`contravariantClass`(`type`: kotlin.reflect.KClass<in Int>): Unit =
                    `contravariantClass`(`type`.java)
                """,
                """
                inline fun org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`varargOfClasses`(vararg `types`: kotlin.reflect.KClass<*>): Unit =
                    `varargOfClasses`(*`types`.map { it.java }.toTypedArray())
                """,
                """
                inline fun org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`arrayOfClasses`(`types`: kotlin.Array<kotlin.reflect.KClass<*>>): Unit =
                    `arrayOfClasses`(`types`.map { it.java }.toTypedArray())
                """,
                """
                inline fun org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`collectionOfClasses`(`types`: kotlin.collections.Collection<kotlin.reflect.KClass<out kotlin.Number>>): Unit =
                    `collectionOfClasses`(`types`.map { it.java })
                """,
                """
                inline fun <T : Any> org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`methodParameterizedClass`(`type`: kotlin.reflect.KClass<T>): T =
                    `methodParameterizedClass`(`type`.java)
                """,
                """
                inline fun <T : kotlin.Number> org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`covariantMethodParameterizedClass`(`type`: kotlin.reflect.KClass<T>): T =
                    `covariantMethodParameterizedClass`(`type`.java)
                """,
                """
                inline fun <T : Any> org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`methodParameterizedCovariantClass`(`type`: kotlin.reflect.KClass<out T>): T =
                    `methodParameterizedCovariantClass`(`type`.java)
                """,
                """
                inline fun <T : Any> org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`methodParameterizedContravariantClass`(`type`: kotlin.reflect.KClass<in T>): T =
                    `methodParameterizedContravariantClass`(`type`.java)
                """,
                """
                inline fun <T : kotlin.Number> org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`covariantMethodParameterizedCovariantClass`(`type`: kotlin.reflect.KClass<out T>): T =
                    `covariantMethodParameterizedCovariantClass`(`type`.java)
                """,
                """
                inline fun <T : kotlin.Number> org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClass.`covariantMethodParameterizedContravariantClass`(`type`: kotlin.reflect.KClass<in T>): T =
                    `covariantMethodParameterizedContravariantClass`(`type`.java)
                """
            )

            assertUsageCompilation(
                """
                import kotlin.reflect.*

                fun classToKClass(subject: ClassToKClass) {

                    subject.rawClass(type = String::class)
                    subject.unknownClass(type = String::class)
                    subject.invariantClass(type = Number::class)
                    subject.covariantClass(type = Int::class)
                    subject.contravariantClass(type = Number::class)

                    subject.varargOfClasses(Number::class, Int::class)
                    subject.arrayOfClasses(types = arrayOf(Number::class, Int::class))
                    subject.collectionOfClasses(listOf(Number::class, Int::class))

                    subject.methodParameterizedClass(type = Int::class)
                    subject.covariantMethodParameterizedClass(type = Int::class)
                    subject.methodParameterizedCovariantClass(type = Int::class)
                    subject.methodParameterizedContravariantClass(type = Int::class)
                    subject.covariantMethodParameterizedCovariantClass(type = Int::class)
                    subject.covariantMethodParameterizedContravariantClass(type = Int::class)
                }
                """
            )
        }
    }

    @Test
    fun `maps Groovy named arguments to Kotlin vararg of Pair`() {

        apiKotlinExtensionsGenerationFor(GroovyNamedArguments::class, Consumer::class) {

            assertGeneratedExtensions(
                """
                inline fun org.gradle.kotlin.dsl.fixtures.codegen.GroovyNamedArguments.`rawMap`(vararg `args`: Pair<String, Any?>): Unit =
                    `rawMap`(mapOf(*`args`))
                """,
                """
                inline fun org.gradle.kotlin.dsl.fixtures.codegen.GroovyNamedArguments.`stringUnknownMap`(vararg `args`: Pair<String, Any?>): Unit =
                    `stringUnknownMap`(mapOf(*`args`))
                """,
                """
                inline fun org.gradle.kotlin.dsl.fixtures.codegen.GroovyNamedArguments.`stringObjectMap`(vararg `args`: Pair<String, Any?>): Unit =
                    `stringObjectMap`(mapOf(*`args`))
                """,
                """
                inline fun org.gradle.kotlin.dsl.fixtures.codegen.GroovyNamedArguments.`mapWithOtherParameters`(`foo`: String, `bar`: Int, vararg `args`: Pair<String, Any?>): Unit =
                    `mapWithOtherParameters`(mapOf(*`args`), `foo`, `bar`)
                """,
                """
                inline fun org.gradle.kotlin.dsl.fixtures.codegen.GroovyNamedArguments.`mapWithLastSamAndOtherParameters`(`foo`: String, vararg `args`: Pair<String, Any?>, `bar`: java.util.function.Consumer<String>): Unit =
                    `mapWithLastSamAndOtherParameters`(mapOf(*`args`), `foo`, `bar`)
                """
            )

            assertUsageCompilation(
                """
                import java.util.function.Consumer

                fun usage(subject: GroovyNamedArguments) {

                    subject.rawMap("foo" to 42, "bar" to 23L, "bazar" to "cathedral")
                    subject.stringUnknownMap("foo" to 42, "bar" to 23L, "bazar" to "cathedral")
                    subject.stringObjectMap("foo" to 42, "bar" to 23L, "bazar" to "cathedral")

                    subject.mapWithOtherParameters(foo = "foo", bar = 42)
                    subject.mapWithOtherParameters("foo", 42, "bar" to 23L, "bazar" to "cathedral")

                    subject.mapWithLastSamAndOtherParameters(foo = "foo") { println(it.toUpperCase()) }
                    subject.mapWithLastSamAndOtherParameters("foo", "bar" to 23L, "bazar" to "cathedral") { println(it.toUpperCase()) }
                    subject.mapWithLastSamAndOtherParameters("foo", *arrayOf("bar" to 23L, "bazar" to "cathedral")) { println(it.toUpperCase()) }
                    subject.mapWithLastSamAndOtherParameters(foo = "foo", bar = Consumer { println(it.toUpperCase()) })
                    subject.mapWithLastSamAndOtherParameters(foo = "foo", bar = Consumer<String> { println(it.toUpperCase()) })
                }
                """
            )
        }
    }

    @Test
    fun `maps mixed java-lang-Class and Groovy named arguments`() {

        apiKotlinExtensionsGenerationFor(ClassAndGroovyNamedArguments::class, Consumer::class) {

            assertGeneratedExtensions(
                """
                inline fun <T : Any> org.gradle.kotlin.dsl.fixtures.codegen.ClassAndGroovyNamedArguments.`mapAndClass`(`type`: kotlin.reflect.KClass<out T>, vararg `args`: Pair<String, Any?>): Unit =
                    `mapAndClass`(mapOf(*`args`), `type`.java)
                """,
                """
                inline fun <T : Any> org.gradle.kotlin.dsl.fixtures.codegen.ClassAndGroovyNamedArguments.`mapAndClassAndVarargs`(`type`: kotlin.reflect.KClass<out T>, `options`: kotlin.Array<String>, vararg `args`: Pair<String, Any?>): Unit =
                    `mapAndClassAndVarargs`(mapOf(*`args`), `type`.java, *`options`)
                """,
                """
                inline fun <T : Any> org.gradle.kotlin.dsl.fixtures.codegen.ClassAndGroovyNamedArguments.`mapAndClassAndSAM`(`type`: kotlin.reflect.KClass<out T>, vararg `args`: Pair<String, Any?>, `action`: java.util.function.Consumer<in T>): Unit =
                    `mapAndClassAndSAM`(mapOf(*`args`), `type`.java, `action`)
                """
            )

            assertUsageCompilation(
                """
                import java.util.function.Consumer

                fun usage(subject: ClassAndGroovyNamedArguments) {

                    subject.mapAndClass<Number>(Int::class)
                    subject.mapAndClass<Number>(Int::class, "foo" to 42, "bar" to "bazar")

                    subject.mapAndClassAndVarargs<Number>(Int::class, arrayOf("foo", "bar"))
                    subject.mapAndClassAndVarargs<Number>(Int::class, arrayOf("foo", "bar"), "bazar" to "cathedral")
                }
                """
            )
        }
    }

    @Test
    fun `maps target type, mapped function and parameters generics`() {

        apiKotlinExtensionsGenerationFor(ClassToKClassParameterizedType::class) {

            assertGeneratedExtensions(
                """
                inline fun <T : java.io.Serializable> org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClassParameterizedType<T>.`invariantClass`(`type`: kotlin.reflect.KClass<T>, `list`: kotlin.collections.List<T>): T =
                    `invariantClass`(`type`.java, `list`)
                """,
                """
                inline fun <T : java.io.Serializable> org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClassParameterizedType<T>.`covariantClass`(`type`: kotlin.reflect.KClass<out T>, `list`: kotlin.collections.List<T>): T =
                    `covariantClass`(`type`.java, `list`)
                """,
                """
                inline fun <T : java.io.Serializable> org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClassParameterizedType<T>.`contravariantClass`(`type`: kotlin.reflect.KClass<in T>, `list`: kotlin.collections.List<T>): T =
                    `contravariantClass`(`type`.java, `list`)
                """,
                """
                inline fun <V : T, T : java.io.Serializable> org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClassParameterizedType<T>.`covariantMethodParameterizedInvariantClass`(`type`: kotlin.reflect.KClass<V>, `list`: kotlin.collections.List<V>): V =
                    `covariantMethodParameterizedInvariantClass`(`type`.java, `list`)
                """,
                """
                inline fun <V : T, T : java.io.Serializable> org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClassParameterizedType<T>.`covariantMethodParameterizedCovariantClass`(`type`: kotlin.reflect.KClass<out V>, `list`: kotlin.collections.List<out V>): V =
                    `covariantMethodParameterizedCovariantClass`(`type`.java, `list`)
                """,
                """
                inline fun <V : T, T : java.io.Serializable> org.gradle.kotlin.dsl.fixtures.codegen.ClassToKClassParameterizedType<T>.`covariantMethodParameterizedContravariantClass`(`type`: kotlin.reflect.KClass<in V>, `list`: kotlin.collections.List<out V>): V =
                    `covariantMethodParameterizedContravariantClass`(`type`.java, `list`)
                """
            )

            assertUsageCompilation(
                """
                import java.io.Serializable

                fun usage(subject: ClassToKClassParameterizedType<Number>) {

                    subject.invariantClass(Number::class, emptyList())
                    subject.covariantClass(Int::class, emptyList())
                    subject.contravariantClass(Serializable::class, emptyList())

                    subject.covariantMethodParameterizedInvariantClass(Number::class, emptyList())
                    subject.covariantMethodParameterizedCovariantClass(Int::class, emptyList())
                    subject.covariantMethodParameterizedContravariantClass(Serializable::class, emptyList())
                }
                """
            )
        }
    }

    private
    fun apiKotlinExtensionsGenerationFor(vararg classes: KClass<*>, action: ApiKotlinExtensionsGeneration.() -> Unit) =
        ApiKotlinExtensionsGeneration(jarClassPathWith(*classes).asFiles).apply(action)

    private
    data class ApiKotlinExtensionsGeneration(val apiJars: List<File>) {
        lateinit var generatedSourceFiles: List<File>
    }

    private
    fun ApiKotlinExtensionsGeneration.assertGeneratedExtensions(vararg expectedExtensions: String) {

        generatedSourceFiles = generateKotlinDslApiExtensionsSourceTo(
            file("src").also { it.mkdirs() },
            "org.gradle.kotlin.dsl",
            "SourceBaseName",
            apiJars,
            emptyList(),
            Specs.satisfyAll(),
            fixtureParameterNamesSupplier
        )

        val generatedSourceCode = generatedSourceFiles.joinToString("") {
            it.readText().substringAfter("package org.gradle.kotlin.dsl\n\n")
        }

        println(generatedSourceCode)

        expectedExtensions.forEach { expectedExtension ->
            assertThat(generatedSourceCode, containsString(expectedExtension.normaliseLineSeparators().trimIndent()))
        }
    }

    private
    fun ApiKotlinExtensionsGeneration.assertUsageCompilation(vararg extensionsUsages: String) {

        val useDir = file("use").also { it.mkdirs() }
        val usageFiles = extensionsUsages.mapIndexed { idx, usage ->
            useDir.resolve("usage$idx.kt").also {
                it.writeText("""
                import org.gradle.kotlin.dsl.fixtures.codegen.*
                import org.gradle.kotlin.dsl.*

                $usage
                """.trimIndent())
            }
        }

        val logger = mock<Logger> {
            on { isTraceEnabled } doReturn false
        }
        compileKotlinApiExtensionsTo(
            file("out").also { it.mkdirs() },
            generatedSourceFiles + usageFiles,
            apiJars,
            logger
        )
        // Assert no warnings were emitted
        verify(logger, atMost(1)).isTraceEnabled
        verifyNoMoreInteractions(logger)
    }
}


private
val fixtureParameterNamesSupplier = { key: String ->
    when {
        key.startsWith("${ClassToKClass::class.qualifiedName}.") -> when {
            key.contains("Class(") -> listOf("type")
            key.contains("Classes(") -> listOf("types")
            else -> null
        }
        key.startsWith("${GroovyNamedArguments::class.qualifiedName}.") -> when {
            key.contains("Map(") -> listOf("args")
            key.contains("Parameters(") -> listOf("args", "foo", "bar")
            else -> null
        }
        key.startsWith("${ClassAndGroovyNamedArguments::class.qualifiedName}.") -> when {
            key.contains("mapAndClass(") -> listOf("args", "type")
            key.contains("mapAndClassAndVarargs(") -> listOf("args", "type", "options")
            key.contains("mapAndClassAndSAM(") -> listOf("args", "type", "action")
            else -> null
        }
        key.startsWith("${ClassToKClassParameterizedType::class.qualifiedName}.") -> when {
            key.contains("Class(") -> listOf("type", "list")
            else -> null
        }
        else -> null
    }
}
