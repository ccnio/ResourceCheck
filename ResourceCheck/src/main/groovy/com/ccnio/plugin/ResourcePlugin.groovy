package com.ccnio.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.ccnio.plugin.data.ResourceConfig
import com.ccnio.plugin.data.ResourceInfo
import com.ccnio.plugin.utils.Constants
import com.ccnio.plugin.utils.Logger
import com.ccnio.plugin.utils.MD5Util
import org.gradle.api.Plugin
import org.gradle.api.Project

class ResourcePlugin implements Plugin<Project> {
    private final String TAG = "ResourcePlugin"
    private def resourceTypeMap = new HashMap<String, HashMap<String, HashSet<ResourceInfo>>>()
    private def whitelist = new HashSet<String>()

    @Override
    void apply(Project project) {
        project.extensions.create('resourceConfig', ResourceConfig)
        ResourceConfig config = project.resourceConfig

        boolean isApplication = project.plugins.hasPlugin(Constants.PLUGIN_APPLICATION)
        def variants = isApplication ? ((AppExtension) (project.property("android"))).applicationVariants :
                ((LibraryExtension) (project.property("android"))).libraryVariants

        project.afterEvaluate {
            Logger.log(TAG, "resource enable = $config.enable;")
            if (!config.enable) return
            readWhiteList(project.rootDir.path)

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
                    def outPath = project.buildDir.getAbsolutePath() + File.separator + Constants.OUTPUT_FILE
                    Logger.log(TAG, "output file = $outPath")
                    printConflict(outPath)
                }

                resourceTask.group = "resource"
                def taskCompile = project.tasks.findByName("compile${variant.name.capitalize()}JavaWithJavac")
                taskCompile.dependsOn(resourceTask)
            }
        }

    }

    def readWhiteList(String dir) {
        def file = new File(dir + File.separator + Constants.WHITELIST_FILE)
        Logger.log(TAG, "readWhiteList file = ${file.path}")
        if (!file.exists()) return

        file.eachLine {
            Logger.log(TAG, "readWhiteList $it")
            whitelist.add(it)
        }
    }

    def printConflict(String outPutPath) {
        def file = new File(outPutPath)
        def dir = file.getParentFile()
        if (!dir.exists()) dir.mkdirs()
        if (!file.exists()) file.createNewFile()
        def printWriter = file.newPrintWriter()
        printWriter.write("**************** update at ${Calendar.getInstance().toLocalDateTime()} **************** \n\n")
        for (type in resourceTypeMap) {
            Logger.log(TAG, "**************** key = ${type.key} *************** + ${type.key != "string"} ")

            boolean whiteDivider = false, typeDivider = false
            for (i in whitelist) {
                if (type.value.containsKey(i) && type.value.get(i).size() > 1) {
                    if (!whiteDivider) {
                        printWriter.write("**************** ${type.key} ****************\n")
                        printWriter.write("<<< conflict but in whitelist >>>\n")
                        whiteDivider = true
                        typeDivider = true
                    }
                    printWriter.write("name: $i\n")
                    for (v in type.value.get(i)) {
                        printWriter.write("${v.toString()}\n")
                    }
                    printWriter.write("\n")
                }
            }

            boolean conflictDivider = false
            for (values in type.value) {
                if (values.value.size() < 2) continue
                def name = values.key
                if (whitelist.contains(name)) continue

                if (!typeDivider) {
                    printWriter.write("**************** ${type.key} ****************\n")
                    typeDivider = true
                }

                if (!conflictDivider) {
                    printWriter.write("<<< conflict >>>\n")
                    conflictDivider = true
                }


                Logger.log(TAG, "name: ${name}")
                printWriter.write("name: ${name}\n")
                for (i in values.value) {
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