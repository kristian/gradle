package Gradle_Check.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.common.collect.Lists
import java.io.File
import java.util.*

fun main() {
    generateCIBuildModelSubProjects(File("/Users/zhb/Projects/gradle"))
//    println(inSubProjectDir(File("/Users/zhb/Projects/gradle/subprojects/composite-builds"), "org.gradle.integtests.composite.CompositeBuildBuildSrcIntegrationTest"))
}

fun generateCIBuildModelSubProjects(gradleRootDir: File) {
    val subProjectsDir = File(gradleRootDir, "subprojects")
    val buildClassTimeJson = File(gradleRootDir, "test-class-data.json")
    val objectMapper = ObjectMapper()
    val buildTypeClassTimes: List<BuildTypeTestClassTime> = objectMapper.readValue<List<BuildTypeTestClassTime>>(buildClassTimeJson, object : TypeReference<List<BuildTypeTestClassTime>>() {})

    val subProjects: Map<String, File> = subProjectsDir.listFiles()!!.filter { it.isDirectory }.map { kebabCaseToCamelCase(it.name) to it }.toMap()
    val testClassToSubProjectNameMap: Map<String, String> = getTestClassToSubProjectMap(buildTypeClassTimes, subProjects)

    val buildTypeBucketsMap = buildTypeClassTimes.map { it.name to createBucketsFor(it, testClassToSubProjectNameMap, 50) }.toMap()
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT)
    File(gradleRootDir, "buckets.json").writeText(objectMapper.writeValueAsString(buildTypeBucketsMap))
}

fun createBucketsFor(buildTypeClassTime: BuildTypeTestClassTime, testClassToSubProject: Map<String, String>, roughBucketCount: Int): List<Bucket> {
    val expectedBucketSize: Int = buildTypeClassTime.totalTime / roughBucketCount
    val subProjectTestClassTimes: List<SubProjectTestClassTime> = buildTypeClassTime.testClassTimes
        .groupBy { testClassToSubProject[it.testClass] }
        .entries
        .filter { "UNKNOWN" != it.key }
        .map { SubProjectTestClassTime(it.key!!, it.value) }
        .sortedBy { -it.totalTime }

    return split(subProjectTestClassTimes, expectedBucketSize)
}

fun <T> split(list: List<T>, function: (T) -> Int, expectedBucketSize: Int): List<List<T>> {
    val originalList = ArrayList(list)
    val ret = mutableListOf<List<T>>()

    while (originalList.isNotEmpty()) {
        val largest = originalList.removeAt(0)
        val bucket = mutableListOf<T>()
        var restCapacity = expectedBucketSize - function(largest)

        bucket.add(largest)

        while (true) {
            // Find next largest object which can fit in resetCapacity
            val index = originalList.indexOfFirst { function(it) < restCapacity }
            if (index == -1 || originalList.isEmpty()) {
                break
            }

            val nextElementToAddToBucket = originalList.removeAt(index)
            restCapacity -= function(nextElementToAddToBucket)
            bucket.add(nextElementToAddToBucket)
        }

        ret.add(bucket)
    }
    return ret
}

fun split(subProjects: List<SubProjectTestClassTime>, expectedBucketSize: Int): List<Bucket> {
    val buckets: List<List<SubProjectTestClassTime>> = split(subProjects, SubProjectTestClassTime::totalTime, expectedBucketSize)
    val ret = mutableListOf<Bucket>()
    buckets.forEach { subProjectsInBucket ->
        if (subProjectsInBucket.size == 1) {
            // Split large project to potential multiple buckets
            ret.addAll(subProjectsInBucket[0].split(expectedBucketSize))
        } else {
            val subProjectNames = subProjectsInBucket.map { it.name }
            ret.add(SmallProjectBucket(subProjectNames.joinToString("_"), subProjectNames))
        }
    }
    return ret
}

fun getTestClassToSubProjectMap(buildTypeTestClassTimes: List<BuildTypeTestClassTime>, subProjects: Map<String, File>): Map<String, String> = buildTypeTestClassTimes.map { it.testClassTimes }
    .flatten()
    .map { it.testClass }
    .toSet()
    .map { it to findOutSubProject(it, subProjects) }
    .toMap()

// Search subprojects
fun findOutSubProject(testClassName: String, subProjects: Map<String, File>): String {
    val subProject = subProjects.entries.find { inSubProjectDir(it.value, testClassName) }
    return subProject?.key ?: "UNKNOWN"
}

fun kebabCaseToCamelCase(kebabCase: String): String {
    return kebabCase.split('-').joinToString("") { it.capitalize() }.decapitalize()
}

val sourceSets = listOf("test", "integTest", "crossVersionTest", "smokeTest")
val dirs = listOf("groovy", "kotlin", "java")
val extensions = listOf("groovy", "kt", "java")

val composition = Lists.cartesianProduct(sourceSets, dirs, extensions)

fun inSubProjectDir(subProjectDir: File, testClassName: String): Boolean {
    return composition.any {
        val sourceSet = it[0]
        val dir = it[1]
        val extension = it[2]
        val classFileName = testClassName.replace('.', '/')
        File(subProjectDir, "src/$sourceSet/$dir/$classFileName.$extension").isFile
    }
}

data class TestClassTime(var testClass: String, var buildTimeMs: Int) {
    constructor() : this("", 0)
}

data class BuildTypeTestClassTime(var name: String, var testClassTimes: List<TestClassTime>) {
    val totalTime: Int
        get() = testClassTimes.sumBy { it.buildTimeMs }

    constructor() : this("", emptyList())
}

data class SubProjectTestClassTime(val name: String, val testClassTimes: List<TestClassTime>) {
    val totalTime: Int = testClassTimes.sumBy { it.buildTimeMs }

    fun split(expectedBuildTimePerBucket: Int): List<Bucket> {
        return if (totalTime < 1.1 * expectedBuildTimePerBucket) {
            listOf(SingleProject(name))
        } else {
            val buckets: List<List<TestClassTime>> = split(testClassTimes, TestClassTime::buildTimeMs, expectedBuildTimePerBucket)
            return if (buckets.size == 1) {
                listOf(SingleProject(name))
            } else {
                buckets.mapIndexed { index: Int, classesInBucket: List<TestClassTime> ->
                    val bucketName = if (index == 0) name else "${name}_${index + 1}"
                    val include = index != buckets.size - 1
                    val classes = if (include) classesInBucket else buckets.subList(0, buckets.size - 1).flatten()
                    LargeProjectSplit(bucketName, include, classes)
                }
            }
        }
    }
}

interface Bucket

data class SingleProject(val name: String) : Bucket

data class LargeProjectSplit(val name: String, val include: Boolean, val testClasses: List<TestClassTime>) : Bucket

data class SmallProjectBucket(val name: String, val subProjects: List<String>) : Bucket
