package com.ccnio.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.ccnio.plugin.data.ResourceInfo
import com.ccnio.plugin.utils.Constants
import com.ccnio.plugin.utils.Logger
import com.ccnio.plugin.utils.MD5Util
import org.gradle.api.Plugin
import org.gradle.api.Project

class ResourcePlugin implements Plugin<Project> {
    private final String TAG = "ResourcePlugin"
    private def resourceTypeMap = new HashMap<String, HashMap<String, HashSet<ResourceInfo>>>()

    @Override
    void apply(Project project) {
        boolean isApplication = project.plugins.hasPlugin(Constants.PLUGIN_APPLICATION)
        def variants = isApplication ? ((AppExtension) (project.property("android"))).applicationVariants :
                ((LibraryExtension) (project.property("android"))).libraryVariants

        project.afterEvaluate {
            variants.forEach { variant ->

                def taskName = "checkResource${variant.name.capitalize()}"
                def resourceTask = project.task(taskName).doFirst {
                    def files = variant.allRawAndroidResources.files
                    for (int i = 0; i < files.size(); i++) {
                        def file = files[i]
                        if (!file.exists()) continue

                        file.traverse { f ->
                            if (f.isFile()) {
                                if (isValueFile(f)) readValues(f)
                                else readFiles(f)
                            }
                        }
                    }
                    printConflict(project.buildDir.getAbsolutePath() + File.separator + "conflict_resource.log")
                }

                resourceTask.group = "resource"
                def taskCompile = project.tasks.findByName("compile${variant.name.capitalize()}JavaWithJavac")
                taskCompile.dependsOn(resourceTask)
            }
        }

    }

    def printConflict(String file) {
        def printWriter = new File(file).newPrintWriter()
        Logger.log(TAG, "out put file = $file")
        printWriter.write("**************** update at ${Calendar.getInstance().toLocalDateTime()} **************** \n\n")
        for (type in resourceTypeMap) {
            Logger.log(TAG, "**************** key = ${type.key} *************** + ${type.key != "string"} ")
            boolean hasDivider = false
            for (values in type.value) {
                if (values.value.size() < 2) continue
                if (!hasDivider) {
                    printWriter.write("**************** ${type.key} ****************\n")
                    hasDivider = true
                }
                Logger.log(TAG, "name: ${values.key}")
                printWriter.write("name: ${values.key}\n")
                for (i in values.value) {
                    Logger.log(TAG, "iterate values = $i")
                    printWriter.write("${i.toString()}\n")
                }
                printWriter.write("\n")
            }
        }
        printWriter.flush()
        printWriter.close()
    }

    static def isValueFile(File file) {
        return file.isFile() && file.getParentFile().name.contains(Constants.DRI_VALUES)
    }


    def readFiles(File file) {
        Logger.log("******************** readFiles name = ${file}***************")
        def name = file.name
        def value = MD5Util.getFileMD5(file)
        def res = new ResourceInfo(name, value, file.path)

        def resourceMap = resourceTypeMap.get(res.dir)
        if (resourceMap == null) {
            Logger.log(TAG, "resource map null")
            resourceMap = new HashMap<String, HashMap<String, HashSet<ResourceInfo>>>()
            resourceTypeMap.put(res.dir, resourceMap)
        }

        HashSet values = resourceMap.get(res.id)
        if (values == null) {
            values = new HashSet<ResourceInfo>()
            resourceMap.put(res.id, values)
            Logger.log(TAG, "values null")
        }
        values.add(res)
    }

    def readValues(File file) {
        def resources = new XmlParser().parse(file)
        Logger.log(TAG, "************** readValues file: " + file + " **************")

        resources.children().forEach {
            def type = it.name()
            def name = it.@name
            def value = it.text()
            Logger.log(TAG, "readValues: type = $type; name = $name; value = $value")
            if (name != null && value != null) {
                def resourceMap = resourceTypeMap.get(type)
                if (resourceMap == null) {
                    Logger.log(TAG, "resource map null")
                    resourceMap = new HashMap<String, HashMap<String, HashSet<ResourceInfo>>>()
                    resourceTypeMap.put(type, resourceMap)
                }

                def res = new ResourceInfo(name, value, file.path)
                HashSet values = resourceMap.get(res.id)
                if (values == null) {
                    values = new HashSet<ResourceInfo>()
                    resourceMap.put(res.id, values)
                    Logger.log(TAG, "values null")
                }
                values.add(res)
            }
        }
    }
}