package Gradle_Check.model

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Lists
import configurations.FunctionalTest
import model.BuildTypeBucket
import model.CIBuildModel
import model.GradleSubProject
import model.Stage
import model.TestCoverage
import java.io.File
import java.util.*


private
val subProjectWithSlowTests = listOf("platformPlay")

val sourceSets = listOf("test", "integTest", "crossVersionTest", "smokeTest")
val dirs = listOf("groovy", "kotlin", "java")
val extensions = listOf("groovy", "kt", "java")

val composition = Lists.cartesianProduct(sourceSets, dirs, extensions)

class GradleSubProjectProvider(val subProjectDir: File) {
    val nameToSubProjectDirs: Map<String, File> = subProjectDir.listFiles().filter { isSubProjectDir(it) }.map { kebabCaseToCamelCase(it.name) to it }.toMap()
    val subProjects: List<GradleSubProject> = nameToSubProjectDirs.values.map { toGradleSubProjectData(it) }

    fun findOutSubProject(testClassName: String): String {
        val subProject = nameToSubProjectDirs.entries.find { inSubProjectDir(it.value, testClassName) }
        return subProject?.key ?: "UNKNOWN"
    }

    fun getSubProjectsFor(testConfig: TestCoverage, stage: Stage) =
        subProjects.filterNot { it.containsSlowTests && stage.omitsSlowProjects }
            .filter { it.hasTestsOf(testConfig.testType) }
            .filterNot { testConfig.os.ignoredSubprojects.contains(it.name) }

    fun getSubProjectByName(name: String?): GradleSubProject = subProjects.find { it.name == name }!!

    private
    fun kebabCaseToCamelCase(kebabCase: String): String {
        return kebabCase.split('-').joinToString("") { it.capitalize() }.decapitalize()
    }

    private
    fun toGradleSubProjectData(dir: File): GradleSubProject {
        val name = kebabCaseToCamelCase(dir.name)
        val hasUnitTests = File(dir, "test").isDirectory
        val hasFunctionalTests = File(dir, "integTest").isDirectory
        val hasCrossVersionTests = File(dir, "crossVersionTest").isDirectory
        val hasSlowTests = name in subProjectWithSlowTests
        return GradleSubProject(name, hasUnitTests, hasFunctionalTests, hasCrossVersionTests, hasSlowTests)
    }

    private
    fun inSubProjectDir(subProjectDir: File, testClassName: String) =
        composition.any {
            val sourceSet = it[0]
            val dir = it[1]
            val extension = it[2]
            val classFileName = testClassName.replace('.', '/')
            File(subProjectDir, "src/$sourceSet/$dir/$classFileName.$extension").isFile
        }

    private
    fun isSubProjectDir(dir: File) = dir.isDirectory && File(dir, "src").isDirectory
}

class GradleBuildBucketProvider(private val model: CIBuildModel, testTimeDataJson: File) {
    //    val subProjects by subProjectProvider
    // Each build project (e.g. Gradle_Check_Quick_1) has different bucket splits
    private val objectMapper = ObjectMapper()
    val buckets: Map<TestCoverage, List<BuildTypeBucket>> = buildBuckets(testTimeDataJson, model)
    private val subProjectProvider = GradleSubProjectProvider(File(testTimeDataJson, "subprojects"))

    private
    fun buildBuckets(buildClassTimeJson: File, model: CIBuildModel): Map<TestCoverage, List<BuildTypeBucket>> {
        println(buildClassTimeJson.readText().length)
        val buildProjectClassTimes: List<BuildProjectTestClassTime> = objectMapper.readValue<List<BuildProjectTestClassTime>>(buildClassTimeJson, object : TypeReference<List<BuildProjectTestClassTime>>() {})
        val testClassToSubProjectNameMap: Map<String, String> = getTestClassToSubProjectMap(buildProjectClassTimes)

        val result = mutableMapOf<TestCoverage, List<BuildTypeBucket>>()
        for (stage in model.stages) {
            for (testConfig in stage.functionalTests) {
                result[testConfig] = createBucketsForBuildProject(testConfig, stage, buildProjectClassTimes, testClassToSubProjectNameMap)
            }
        }
        return result
    }

    private
    fun createBucketsForBuildProject(testCoverage: TestCoverage, stage: Stage, buildProjectClassTimes: List<BuildProjectTestClassTime>, testClassToSubProject: Map<String, String>): List<BuildTypeBucket> {
        val validSubProjects = subProjectProvider.getSubProjectsFor(testCoverage, stage)

        // Build project not found, don't split into buckets
        val buildProjectClassTime = buildProjectClassTimes.find { it.name == testCoverage.asId(model) } ?: return validSubProjects

        val expectedBucketSize: Int = buildProjectClassTime.totalTime / testCoverage.expectedBucketNumber
        val subProjectTestClassTimes: List<SubProjectTestClassTime> = buildProjectClassTime.testClassTimes
            .groupBy { testClassToSubProject[it.testClass] }
            .entries
            .filter { "UNKNOWN" != it.key }
            .map { SubProjectTestClassTime(subProjectProvider.getSubProjectByName(it.key), it.value) }
            .sortedBy { -it.totalTime }

        return split(subProjectTestClassTimes, expectedBucketSize)
    }


    private
    fun split(subProjects: List<SubProjectTestClassTime>, expectedBucketSize: Int): List<BuildTypeBucket> {
        val buckets: List<List<SubProjectTestClassTime>> = split(subProjects, SubProjectTestClassTime::totalTime, expectedBucketSize)
        val ret = mutableListOf<BuildTypeBucket>()
        buckets.forEach { subProjectsInBucket ->
            if (subProjectsInBucket.size == 1) {
                // Split large project to potential multiple buckets
                ret.addAll(subProjectsInBucket[0].split(expectedBucketSize))
            } else {
                val subProjectNames = subProjectsInBucket.map { it.subProject.name }
                ret.add(SmallSubProjectBucket(subProjectNames.joinToString("_"), subProjectsInBucket.map { it.subProject }))
            }
        }
        return ret
    }

    private
    fun getTestClassToSubProjectMap(buildTypeTestClassTimes: List<BuildProjectTestClassTime>): Map<String, String> = buildTypeTestClassTimes.map { it.testClassTimes }
        .flatten()
        .map { it.testClass }
        .toSet()
        .map { it to subProjectProvider.findOutSubProject(it) }
        .toMap()


    fun createFunctionalTestsFor(stage: Stage, testConfig: TestCoverage): List<FunctionalTest> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
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

class LargeSubProjectSplitBucket(val subProject: GradleSubProject, val number: Int, val include: Boolean, val classes: List<TestClassTime>) : BuildTypeBucket by subProject {
    val name = if (number == 1) subProject.name else "${subProject.name}_$number"

    override fun createFunctionalTestsFor(model: CIBuildModel, stage: Stage, testCoverage: TestCoverage): FunctionalTest = FunctionalTest(model,
        testCoverage.asConfigurationId(model, name),
        "${testCoverage.asName()} ($name)",
        "${testCoverage.asName()} for projects $name",
        testCoverage,
        stage,
        listOf(subProject.name),
        "-PtestBucket=$name"
    )
}

class SmallSubProjectBucket(val name: String, val subProjects: List<GradleSubProject>) : BuildTypeBucket {
    override fun createFunctionalTestsFor(model: CIBuildModel, stage: Stage, testCoverage: TestCoverage): FunctionalTest =
        FunctionalTest(model, testCoverage.asConfigurationId(model, name),
            "${testCoverage.asName()} (${subProjects.joinToString(", ") { name }})",
            "${testCoverage.asName()} for ${subProjects.joinToString(", ") { name }}",
            testCoverage,
            stage,
            subProjects.map { it.name }
        )
}

//fun main() {
//    generateCIBuildModelSubProjects(File("/Users/zhb/Projects/gradle"))
////    println(inSubProjectDir(File("/Users/zhb/Projects/gradle/subprojects/composite-builds"), "org.gradle.integtests.composite.CompositeBuildBuildSrcIntegrationTest"))
//}

//val objectMapper = ObjectMapper()

//fun scanGradleSubProjects(subProjectsDir: File): List<GradleSubProject> =
//    subProjectsDir.listFiles()
//        .filter { isSubProjectDir(it) }
//        .map { toGradleSubProjectData(it) }
//
//

//fun generateCIBuildModelSubProjects(gradleRootDir: File) {
//    val subProjectsDir = File(gradleRootDir, "subprojects")
//    val buildClassTimeJson = File(gradleRootDir, "test-class-data.json")
//    val buildTypeClassTimes: List<BuildTypeTestClassTime> = objectMapper.readValue<List<BuildTypeTestClassTime>>(buildClassTimeJson, object : TypeReference<List<BuildTypeTestClassTime>>() {})
//    val subProjects: Map<String, File> = subProjectsDir.listFiles().filter { isSubProjectDir(it) }.map { kebabCaseToCamelCase(it.name) to it }.toMap()
//
////    val subProjects = scanGradleSubProjects(subProjectsDir)
//    val testClassToSubProjectNameMap: Map<String, String> = getTestClassToSubProjectMap(buildTypeClassTimes, subProjects)
//
//    val buildTypeBucketsMap: Map<String, List<Bucket>> = buildTypeClassTimes.map { it.name to createBucketsFor(it, testClassToSubProjectNameMap, 40) }.toMap()
//    objectMapper.enable(SerializationFeature.INDENT_OUTPUT)
//    File(gradleRootDir, "buckets.json").writeText(objectMapper.writeValueAsString(buildTypeBucketsMap))
//}


data class TestClassTime(var testClass: String, var buildTimeMs: Int) {
    constructor() : this("", 0)
}

data class BuildProjectTestClassTime(var name: String, var testClassTimes: List<TestClassTime>) {
    val totalTime: Int
        get() = testClassTimes.sumBy { it.buildTimeMs }

    constructor() : this("", emptyList())
}

data class SubProjectTestClassTime(val subProject: GradleSubProject, val testClassTimes: List<TestClassTime>) {
    val totalTime: Int = testClassTimes.sumBy { it.buildTimeMs }

    fun split(expectedBuildTimePerBucket: Int): List<BuildTypeBucket> {
        return if (totalTime < 1.1 * expectedBuildTimePerBucket) {
            listOf(subProject)
        } else {
            val buckets: List<List<TestClassTime>> = split(testClassTimes, TestClassTime::buildTimeMs, expectedBuildTimePerBucket)
            return if (buckets.size == 1) {
                listOf(subProject)
            } else {
                buckets.mapIndexed { index: Int, classesInBucket: List<TestClassTime> ->
                    val include = index != buckets.size - 1
                    val classes = if (include) classesInBucket else buckets.subList(0, buckets.size - 1).flatten()
                    LargeSubProjectSplitBucket(subProject, index + 1, include, classes)
                }
            }
        }
    }
}


