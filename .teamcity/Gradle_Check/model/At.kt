package Gradle_Check.model

import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.type.TypeBindings
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.InstanceCreator
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import configurations.FunctionalTest
import model.BuildTypeBucket
import model.CIBuildModel
import model.GradleSubProject
import model.Stage
import model.TestCoverage
import java.io.File
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap


private
val subProjectWithSlowTests = listOf("platformPlay")

//val sourceSets = listOf("test", "integTest", "crossVersionTest", "smokeTest")
//val dirs = listOf("groovy", "kotlin", "java")
//val extensions = listOf("groovy", "kt", "java")
//
//val composition = Lists.cartesianProduct(sourceSets, dirs, extensions)

typealias BuildProjectToSubProjectTestClassTimes = Map<String, Map<String, List<TestClassTime>>>
typealias BuildProjectToSubProjectTestClassTimesMap = Map<String, Map<String, List<Map<String, Object>>>>

class GradleSubProjectProvider(private val subProjects: List<GradleSubProject>) {
    private val nameToSubProject = subProjects.map { it.name to it }.toMap()
    fun getSubProjectsFor(testConfig: TestCoverage, stage: Stage) =
        subProjects.filterNot { it.containsSlowTests && stage.omitsSlowProjects }
            .filter { it.hasTestsOf(testConfig.testType) }
            .filterNot { testConfig.os.ignoredSubprojects.contains(it.name) }

    fun getSubProjectByName(name: String) = nameToSubProject[name]
}

//class TestClassTimeAdpater : JsonDeserializer<TestClassTime>() {
//    override fun deserialize(p: JsonParser?, ctxt: DeserializationContext?): TestClassTime {
//        val node = p!!.codec.readTree<ObjectNode>(p)
//        val testClass = node.get("testClass").asText()
//        val builtTimeMs = node.get("buildTimeMs").numberValue()
//        return TestClassTime(testClass, builtTimeMs.toInt())
//    }
//
//}

class LinkedHashMapAapter : JsonDeserializer<LinkedHashMap<Any, Any>> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): LinkedHashMap<Any, Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class GsonTestClassTimeAdapter : JsonDeserializer<TestClassTime> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): TestClassTime {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class ArrayListAdapter : JsonDeserializer<ArrayList<Any>> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): ArrayList<Any> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class GradleBuildBucketProvider(private val model: CIBuildModel, testTimeDataJson: File) {
    //    val subProjects by subProjectProvider
    // Each build project (e.g. Gradle_Check_Quick_1) has different bucket splits
    private val objectMapper = ObjectMapper()
    private val subProjectProvider = GradleSubProjectProvider(model.subProjects)
    //    private val gson = Gson()
    private val buckets: Map<TestCoverage, List<BuildTypeBucket>> = buildBuckets(testTimeDataJson, model)

//    private fun readJson(buildClassTimeJson: File): BuildProjectToSubProjectTestClassTimes {
//        // Can't use TypeReference:
//        // Runtime error Gradle_Check: Gradle_Check.model.GradleBuildBucketProvider[53]: java.security.AccessControlException: access denied ("java.lang.RuntimePermission" "accessDeclaredMembers")
//        val map: BuildProjectToSubProjectTestClassTimesMap = objectMapper.readValue<BuildProjectToSubProjectTestClassTimesMap>(buildClassTimeJson, object : TypeReference<BuildProjectToSubProjectTestClassTimesMap>() {})
//        val tree: ObjectNode = objectMapper.readTree(buildClassTimeJson) as ObjectNode
//
//
//        return map.entries.map { it.key to convert(it.value) }.toMap()
//    }
//
//    private fun convert(map: Map<String, List<Map<String, Object>>>): Map<String, List<TestClassTime>> {
//
//    }

//    private
//    fun constructJavaType(type: Type): JavaType? {
//        if (type is ParameterizedType) {
//            val javaTypeArgs: Array<JavaType?> = arrayOfNulls<JavaType>((type as ParameterizedType).getActualTypeArguments().size)
//            for (i in 0 until (type as ParameterizedType).getActualTypeArguments().size) {
//                javaTypeArgs[i] = constructJavaType((type as ParameterizedType).getActualTypeArguments().get(i))
//            }
//            return objectMapper.getTypeFactory().constructType(type,
//                TypeBindings.create((type as ParameterizedType).getRawType() as Class<*>?, javaTypeArgs))
//        } else {
//            return objectMapper.getTypeFactory().constructType(type)
//        }
//    }

    private
    fun buildBuckets(buildClassTimeJson: File, model: CIBuildModel): Map<TestCoverage, List<BuildTypeBucket>> {
//        val module: SimpleModule = SimpleModule()
//        module.addDeserializer(TestClassTime::class.java, TestClassTimeAdpater())
//        module.addDeserializer(LinkedHashMap::class.java, LinkedHashMapAapter())
//        objectMapper.registerModule(module)

//        objectMapper.readTree(buildClassTimeJson)

//        val ListTestClassTime = objectMapper.typeFactory.constructType(List::javaClass, TypeBindings.create(List::class.java, arrayOf<JavaType>(TestClassTime::javaClass)))

//        objectMapper.disable(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)
//        objectMapper.disable(MapperFeature.AUTO_DETECT_CREATORS)
        val type = object : TypeReference<BuildProjectToSubProjectTestClassTimes>() {}.type
//        val javaType = constructJavaType(type)
        val gson = Gson()
//        GsonBuilder().registerTypeAdapter(TestClassTime::class.java, GsonTestClassTimeAdapter())
//            .registerTypeAdapter(LinkedHashMap::class.java, LinkedHashMapAapter())
//            .registerTypeAdapter(ArrayList::class.java, ArrayListAdapter())
//            .create()
//
//        val buildProjectClassTimes: BuildProjectToSubProjectTestClassTimes = JSON.parseObject(buildClassTimeJson.readText(), type, Feature.DisableASM)
        val obj = JSON.parseObject(buildClassTimeJson.readText()) as JSONObject
        val buildProjectClassTimes = obj.map { buildProjectToSubProjectTestClassTime ->
            buildProjectToSubProjectTestClassTime.key to (buildProjectToSubProjectTestClassTime.value as JSONObject).map { subProjectToTestClassTime ->
                subProjectToTestClassTime.key to (subProjectToTestClassTime.value as JSONArray).map { TestClassTime(it as JSONObject) }
            }.toMap()
        }.toMap()
        //gson.fromJson<BuildProjectToSubProjectTestClassTimes>(buildClassTimeJson.readText(), type)

        //JSON.parseObject<Map<String, Map<String, List<TestClassTime>>>>(buildClassTimeJson.readText(), object : TypeReference<Map<String, Map<String, List<TestClassTime>>>>() {})//
//        val buildProjectClassTimes = objectMapper.readValue<BuildProjectToSubProjectTestClassTimes>(buildClassTimeJson, object : TypeReference<BuildProjectToSubProjectTestClassTimes>() {})
//        val buildProjectClassTimes = objectMapper.readValue<BuildProjectToSubProjectTestClassTimes>(buildClassTimeJson, java)

        val result = mutableMapOf<TestCoverage, List<BuildTypeBucket>>()
        for (stage in model.stages) {
            for (testConfig in stage.functionalTests) {
                result[testConfig] = createBucketsForBuildProject(testConfig, stage, buildProjectClassTimes)
            }
        }
        return result
    }

    private
    fun createBucketsForBuildProject(testCoverage: TestCoverage, stage: Stage, buildProjectClassTimes: BuildProjectToSubProjectTestClassTimes): List<BuildTypeBucket> {
        val validSubProjects = subProjectProvider.getSubProjectsFor(testCoverage, stage)

        // Build project not found, don't split into buckets
        val subProjectToClassTimes: Map<String, List<TestClassTime>> = buildProjectClassTimes.get(testCoverage.asId(model)) ?: return validSubProjects

        val subProjectTestClassTimes: List<SubProjectTestClassTime> = subProjectToClassTimes
            .entries
            .filter { "UNKNOWN" != it.key }
            .filter { subProjectProvider.getSubProjectByName(it.key) != null }
            .map { SubProjectTestClassTime(subProjectProvider.getSubProjectByName(it.key)!!, it.value) }
            .sortedBy { -it.totalTime }
        val expectedBucketSize: Int = subProjectTestClassTimes.sumBy { it.totalTime } / testCoverage.expectedBucketNumber

        return split(subProjectTestClassTimes, expectedBucketSize)
    }

    private
    fun split(subProjects: List<SubProjectTestClassTime>, expectedBucketSize: Int): List<BuildTypeBucket> {
        val buckets: List<List<SubProjectTestClassTime>> = split(subProjects, SubProjectTestClassTime::totalTime, expectedBucketSize)
        val ret = mutableListOf<BuildTypeBucket>()
        var bucketNumber = 1
        buckets.forEach { subProjectsInBucket ->
            if (subProjectsInBucket.size == 1) {
                // Split large project to potential multiple buckets
                ret.addAll(subProjectsInBucket[0].split(expectedBucketSize))
            } else {
                ret.add(SmallSubProjectBucket("bucket${bucketNumber++}", subProjectsInBucket.map { it.subProject }))
            }
        }
        return ret
    }

//    private
//    fun getTestClassToSubProjectMap(buildTypeTestClassTimes: List<BuildProjectTestClassTime>): Map<String, String> = buildTypeTestClassTimes.map { it.testClassTimes }
//        .flatten()
//        .map { it.testClass }
//        .toSet()
//        .map { it to subProjectProvider.findOutSubProject(it) }
//        .toMap()


    fun createFunctionalTestsFor(stage: Stage, testConfig: TestCoverage) = buckets.getValue(testConfig).map { it.createFunctionalTestsFor(model, stage, testConfig) }
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

class LargeSubProjectSplitBucket(private val subProject: GradleSubProject, private val number: Int, private val include: Boolean, val classes: List<TestClassTime>) : BuildTypeBucket by subProject {
    val name = if (number == 1) subProject.name else "${subProject.name}_$number"

    override fun createFunctionalTestsFor(model: CIBuildModel, stage: Stage, testCoverage: TestCoverage): FunctionalTest =
        FunctionalTest(model,
            testCoverage.asConfigurationId(model, name),
            "${testCoverage.asName()} ($name)",
            "${testCoverage.asName()} for projects $name",
            testCoverage,
            stage,
            listOf(subProject.name),
            "-P${if (include) "includeTestClasses" else "excludeTestClasses"}=${classes.joinToString(",") { it.testClassSimpleName }}"
        )
}

class SmallSubProjectBucket(val name: String, private val subProjects: List<GradleSubProject>) : BuildTypeBucket {
    override fun createFunctionalTestsFor(model: CIBuildModel, stage: Stage, testCoverage: TestCoverage): FunctionalTest =
        FunctionalTest(model, testCoverage.asConfigurationId(model, name),
            "${testCoverage.asName()} (${subProjects.joinToString(", ") { it.name }})",
            "${testCoverage.asName()} for ${subProjects.joinToString(", ") { it.name }}",
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


class TestClassTime(var testClass: String, var buildTimeMs: Int) {
    val testClassSimpleName: String
        get() = this.testClass.split(".").last()

    constructor(jsonObject: JSONObject) : this(jsonObject.getString("testClass"), jsonObject.getIntValue("buildTimeMs"))
}

//data class BuildProjectTestClassTime(var name: String, var testClassTimes: List<TestClassTime>) {
//    val totalTime: Int
//        get() = testClassTimes.sumBy { it.buildTimeMs }
//
//    constructor() : this("", emptyList())
//}

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


