/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.worker.common.task.market

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.api.constant.ARTIFACT
import com.tencent.devops.common.api.constant.ARTIFACTORY_TYPE
import com.tencent.devops.common.api.constant.LABEL
import com.tencent.devops.common.api.constant.PATH
import com.tencent.devops.common.api.constant.REPORT
import com.tencent.devops.common.api.constant.REPORT_TYPE
import com.tencent.devops.common.api.constant.STRING
import com.tencent.devops.common.api.constant.TYPE
import com.tencent.devops.common.api.constant.URL
import com.tencent.devops.common.api.constant.VALUE
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ReplacementUtils
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.archive.element.ReportArchiveElement
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.report.enums.ReportTypeEnum
import com.tencent.devops.store.pojo.atom.AtomEnv
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.ATOM_POST_ENTRY_PARAM
import com.tencent.devops.store.pojo.common.KEY_TARGET
import com.tencent.devops.store.pojo.common.enums.BuildHostTypeEnum
import com.tencent.devops.worker.common.CI_TOKEN_CONTEXT
import com.tencent.devops.worker.common.JAVA_PATH_ENV
import com.tencent.devops.worker.common.JOB_OS_CONTEXT
import com.tencent.devops.worker.common.PIPELINE_SCRIPT_ATOM_CODE
import com.tencent.devops.worker.common.WORKSPACE_CONTEXT
import com.tencent.devops.worker.common.WORKSPACE_ENV
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.atom.AtomArchiveSDKApi
import com.tencent.devops.worker.common.api.quality.QualityGatewaySDKApi
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.env.BuildType
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskFactory
import com.tencent.devops.worker.common.utils.ArchiveUtils
import com.tencent.devops.worker.common.utils.BatScriptUtil
import com.tencent.devops.worker.common.utils.CredentialUtils
import com.tencent.devops.worker.common.utils.FileUtils
import com.tencent.devops.worker.common.utils.ShellUtil
import com.tencent.devops.worker.common.utils.TaskUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 构建脚本任务
 */
@Suppress("ALL")
open class MarketAtomTask : ITask() {

    private val atomApi = ApiFactory.create(AtomArchiveSDKApi::class)

    private val outputFile = "output.json"

    private val inputFile = "input.json"

    private val sdkFile = ".sdk.json"
    private val paramFile = ".param.json"

    private lateinit var atomExecuteFile: File

    private val qualityGatewayResourceApi = ApiFactory.create(QualityGatewaySDKApi::class)

    @Suppress("UNCHECKED_CAST")
    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()
        val taskName = taskParams["name"] as String
        val atomCode = taskParams["atomCode"] as String
        val atomVersion = taskParams["version"] as String
        val data = taskParams["data"] ?: "{}"
        val map = JsonUtil.toMutableMapSkipEmpty(data)
        logger.info("${buildTask.buildId}|RUN_ATOM|taskName=$taskName|ver=$atomVersion|code=$atomCode" +
            "|workspace=${workspace.absolutePath}")

        // 获取插件基本信息
        val atomEnvResult = atomApi.getAtomEnv(buildVariables.projectId, atomCode, atomVersion)
        logger.info("atomEnvResult is:$atomEnvResult")
        val atomData =
            atomEnvResult.data ?: throw TaskExecuteException(
                errorMsg = "can not found $taskName: ${atomEnvResult.message}",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
            )

        // val atomWorkspace = File("${workspace.absolutePath}/${atomCode}_${buildTask.taskId}_data")
        val atomTmpSpace = Files.createTempDirectory("${atomCode}_${buildTask.taskId}_data").toFile()

        if (!atomTmpSpace.exists() && !atomTmpSpace.mkdirs()) {
            atomEnvResult.data ?: throw TaskExecuteException(
                errorMsg = "create directory fail! please check ${atomTmpSpace.absolutePath}",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
            )
        }

        cleanOutput(atomTmpSpace)

        val variablesMap = buildVariables.variablesWithType.map { it.key to it.value.toString() }.toMap()
        var runtimeVariables = if (buildTask.buildVariable != null) {
            variablesMap.plus(buildTask.buildVariable!!)
        } else {
            variablesMap
        }

        val command = StringBuilder()

        // 解析输出字段模板
        val props = JsonUtil.toMutableMapSkipEmpty(atomData.props!!)

        // 解析输入参数

        val inputTemplate =
            if (props["input"] != null) {
                props["input"] as Map<String, Map<String, Any>>
            } else {
                mutableMapOf()
            }

        val systemVariables = mapOf(WORKSPACE_ENV to workspace.absolutePath)

        val atomParams = mutableMapOf<String, String>()
        try {
            val inputMap = map["input"] as Map<String, Any>?
            inputMap?.forEach { (name, value) ->
                var valueStr = JsonUtil.toJson(value)
                valueStr = ReplacementUtils.replace(valueStr, object : ReplacementUtils.KeyReplacement {
                    override fun getReplacement(key: String, doubleCurlyBraces: Boolean): String? {
                        return CredentialUtils.getCredentialContextValue(key) ?: if (doubleCurlyBraces) {
                                "\${{$key}}"
                            } else {
                                "\${$key}"
                            }
                    }
                })

                // 只有构建环境下运行的插件才有workspace变量
                if (buildTask.containerType == VMBuildContainer.classType) {
                    atomParams[name] = EnvUtils.parseEnv(
                        command = valueStr,
                        data = systemVariables,
                        contextMap = contextMap(buildTask).plus(
                            mapOf(
                                WORKSPACE_CONTEXT to workspace.absolutePath,
                                CI_TOKEN_CONTEXT to (buildVariables.variables[CI_TOKEN_CONTEXT] ?: ""),
                                JOB_OS_CONTEXT to AgentEnv.getOS().name
                            )
                        )
                    )
                } else {
                    atomParams[name] = valueStr
                }
            }
        } catch (e: Throwable) {
            logger.error("plugin input illegal! ", e)
            throw TaskExecuteException(
                errorMsg = "plugin input illegal",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
            )
        }

        val bkWorkspacePath =
            if (buildTask.containerType != VMBuildContainer.classType) {
                // 无构建环境下运行的插件的workspace取临时文件的路径
                atomTmpSpace.absolutePath
            } else {
                workspace.absolutePath
            }
        runtimeVariables = runtimeVariables.plus(Pair("bkWorkspace", Paths.get(bkWorkspacePath).normalize().toString()))
        val atomStatus = atomData.atomStatus
        val testVersionFlag = if (AtomStatusEnum.TESTING.name == atomStatus) {
            "Y"
        } else {
            "N"
        }
        runtimeVariables = runtimeVariables.plus(Pair("testVersionFlag", testVersionFlag)) // 设置是否是测试版本的标识
        // 设置插件名称和任务名称变量
        runtimeVariables = runtimeVariables.plus(mapOf("atomName" to atomData.atomName, "taskName" to taskName))
        val variables = runtimeVariables.plus(atomParams)
        logger.info("atomCode is:$atomCode ,variables is:$variables")

        val outputTemplate =
            if (props["output"] != null) {
                props["output"] as Map<String, Map<String, Any>>
            } else {
                mutableMapOf()
            }

        // 输出参数的用户命名空间：防止重名窘况
        val namespace: String? = map["namespace"] as String?

        printInput(atomData, atomParams, inputTemplate)

        if (atomData.target?.isBlank() == true) {
            throw TaskExecuteException(
                errorMsg = "can not found any plugin cmd",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
            )
        }

        // 查询插件的敏感信息
        val atomSensitiveConfResult = atomApi.getAtomSensitiveConf(atomCode)
        logger.info("atomCode is:$atomCode ,atomSensitiveConfResult is:$atomSensitiveConfResult")
        val atomSensitiveConfList = atomSensitiveConfResult.data
        val atomSensitiveConfMap = mutableMapOf<String, String>()
        atomSensitiveConfList?.forEach {
            atomSensitiveConfMap[it.fieldName] = it.fieldValue
        }
        val atomSensitiveDataMap = mapOf("bkSensitiveConfInfo" to atomSensitiveConfMap)
        writeInputFile(atomTmpSpace, variables.plus(atomSensitiveDataMap))

        writeSdkEnv(atomTmpSpace, buildTask, buildVariables)
        writeParamEnv(atomCode, atomTmpSpace, workspace, buildTask, buildVariables)

        val javaFile = getJavaFile()
        val environment = runtimeVariables.plus(
            mapOf(
                DIR_ENV to atomTmpSpace.absolutePath,
                INPUT_ENV to inputFile,
                OUTPUT_ENV to outputFile,
                JAVA_PATH_ENV to javaFile.absolutePath
            )
        )

        var error: Throwable? = null
        try {
            // 下载atom执行文件
            atomExecuteFile = downloadAtomExecuteFile(atomData.pkgPath!!, atomTmpSpace)

            checkSha1(atomExecuteFile, atomData.shaContent!!)
            val buildHostType = if (BuildEnv.isThirdParty()) BuildHostTypeEnum.THIRD else BuildHostTypeEnum.PUBLIC
            val atomLanguage = atomData.language!!
            val atomDevLanguageEnvVarsResult = atomApi.getAtomDevLanguageEnvVars(
                atomLanguage, buildHostType.name, AgentEnv.getOS().name)
            logger.info("atomCode is:$atomCode ,atomDevLanguageEnvVarsResult is:$atomDevLanguageEnvVarsResult")
            val atomDevLanguageEnvVars = atomDevLanguageEnvVarsResult.data
            val systemEnvVariables = mutableMapOf(
                "PROJECT_ID" to buildVariables.projectId,
                "BUILD_ID" to buildVariables.buildId,
                "VM_SEQ_ID" to buildVariables.vmSeqId
            )
            atomDevLanguageEnvVars?.forEach {
                systemEnvVariables[it.envKey] = it.envValue
            }
            val preCmds = mutableListOf<String>()
            if (!atomData.preCmd.isNullOrBlank()) {
                if (atomData.preCmd!!.contains(Regex("^\\s*\\[[\\w\\s\\S\\W]*\\]\\s*$"))) {
                    preCmds.addAll(JsonUtil.to(atomData.preCmd!!))
                } else {
                    preCmds.add(atomData.preCmd!!)
                }
            }
            val atomTargetHandleService = AtomTargetFactory.createAtomTargetHandleService(atomLanguage)
            val buildEnvs = buildVariables.buildEnvs
            val additionalOptions = taskParams["additionalOptions"]
            // 获取插件post操作入口参数
            var postEntryParam: String? = null
            if (additionalOptions != null) {
                val additionalOptionMap = JsonUtil.toMutableMapSkipEmpty(additionalOptions)
                val elementPostInfoMap = additionalOptionMap["elementPostInfo"] as? Map<String, Any>
                postEntryParam = elementPostInfoMap?.get(ATOM_POST_ENTRY_PARAM)?.toString()
            }
            val atomTarget = atomTargetHandleService.handleAtomTarget(
                target = atomData.target!!,
                osType = AgentEnv.getOS(),
                buildHostType = buildHostType,
                systemEnvVariables = systemEnvVariables,
                buildEnvs = buildEnvs,
                postEntryParam = postEntryParam
            )
            val errorMessage = "Fail to run the plugin"
            when {
                AgentEnv.getOS() == OSType.WINDOWS -> {
                    if (preCmds.isNotEmpty()) {
                        preCmds.forEach { cmd ->
                            command.append("\r\n$cmd\r\n")
                        }
                    }
                    command.append("\r\n$atomTarget\r\n")
                    BatScriptUtil.execute(
                        buildId = buildVariables.buildId,
                        script = command.toString(),
                        runtimeVariables = environment,
                        dir = atomTmpSpace,
                        workspace = workspace,
                        errorMessage = errorMessage,
                        elementId = buildTask.elementId
                    )
                }
                AgentEnv.getOS() == OSType.LINUX || AgentEnv.getOS() == OSType.MAC_OS -> {
                    if (preCmds.isNotEmpty()) {
                        preCmds.forEach { cmd ->
                            command.append("\n$cmd\n")
                        }
                    }
                    command.append("\n$atomTarget\n")
                    ShellUtil.execute(
                        buildId = buildVariables.buildId,
                        script = command.toString(),
                        dir = atomTmpSpace,
                        workspace = workspace,
                        buildEnvs = buildEnvs,
                        runtimeVariables = environment,
                        systemEnvVariables = systemEnvVariables,
                        errorMessage = errorMessage,
                        elementId = buildTask.elementId
                    )
                }
            }
        } catch (e: Throwable) {
            error = e
        } finally {
            output(buildTask, atomTmpSpace, File(bkWorkspacePath), buildVariables, outputTemplate, namespace, atomCode)
            if (error != null) {
                val defaultMessage = StringBuilder("Market atom env load exit with StackTrace:\n")
                defaultMessage.append(error.toString())
                error.stackTrace.forEach {
                    with(it) {
                        defaultMessage.append("\n    at $className.$methodName($fileName:$lineNumber)")
                    }
                }
                throw TaskExecuteException(
                    errorType = ErrorType.SYSTEM,
                    errorCode = ErrorCode.SYSTEM_INNER_TASK_ERROR,
                    errorMsg = defaultMessage.toString()
                )
            }
        }
    }

    private fun printInput(
        atomData: AtomEnv,
        atomParams: MutableMap<String, String>,
        inputTemplate: Map<String, Map<String, Any>>
    ) {
        LoggerService.addNormalLine("=====================================================================")
        LoggerService.addNormalLine("Task           : ${atomData.atomName}")
        if (!atomData.summary.isNullOrBlank()) {
            LoggerService.addNormalLine("Description    : ${atomData.summary}")
        }
        LoggerService.addNormalLine("Version        : ${atomData.version}")
        LoggerService.addNormalLine("Author         : ${atomData.creator}")
        if (!atomData.docsLink.isNullOrBlank()) {
            LoggerService.addNormalLine(
                "Help           : <a target=\"_blank\" href=\"${atomData.docsLink}\">More Information</a>")
        }
        LoggerService.addNormalLine("=====================================================================")

        val atomStatus = AtomStatusEnum.getAtomStatus(atomData.atomStatus)
        if (atomStatus == AtomStatusEnum.UNDERCARRIAGED) {
            LoggerService.addYellowLine(
                "[警告]该插件已被下架，有可能无法正常工作！\n[WARNING]The plugin has been removed and may not work properly."
            )
        } else if (atomStatus == AtomStatusEnum.UNDERCARRIAGING) {
            LoggerService.addYellowLine(
                "[警告]该插件处于下架过渡期，后续可能无法正常工作！\n" +
                    "[WARNING]The plugin is in the transition period and may not work properly in the future."
            )
        }

        atomParams.forEach { (key, value) ->
            if (inputTemplate[key] != null) {
                val def = inputTemplate[key] as Map<String, Any>
                val sensitiveFlag = def["isSensitive"]
                if (sensitiveFlag != null && sensitiveFlag.toString() == "true") {
                    LoggerService.addYellowLine("input(sensitive): (${def["label"]})$key=******")
                } else {
                    LoggerService.addNormalLine("input(normal): (${def["label"]})$key=$value")
                }
            } else {
                LoggerService.addYellowLine("input(except): $key=$value")
            }
        }
    }

    private fun writeSdkEnv(workspace: File, buildTask: BuildTask, buildVariables: BuildVariables) {
        val inputFileFile = File(workspace, sdkFile)
        val sdkEnv: SdkEnv = when (BuildEnv.getBuildType()) {
            BuildType.AGENT, BuildType.DOCKER, BuildType.MACOS -> {
                SdkEnv(
                    buildType = BuildEnv.getBuildType(),
                    projectId = buildVariables.projectId,
                    agentId = AgentEnv.getAgentId(),
                    secretKey = AgentEnv.getAgentSecretKey(),
                    buildId = buildTask.buildId,
                    vmSeqId = buildTask.vmSeqId,
                    gateway = AgentEnv.getGateway()
                )
            }
            BuildType.WORKER -> {
                SdkEnv(
                    buildType = BuildEnv.getBuildType(),
                    projectId = buildVariables.projectId,
                    agentId = "",
                    secretKey = "",
                    buildId = buildTask.buildId,
                    vmSeqId = buildTask.vmSeqId,
                    gateway = AgentEnv.getGateway()
                )
            }
        }
        logger.info("sdkEnv is:$sdkEnv")
        inputFileFile.writeText(JsonUtil.toJson(sdkEnv))
    }

    private fun writeParamEnv(
        atomCode: String,
        atomTmpSpace: File,
        workspace: File,
        buildTask: BuildTask,
        buildVariables: BuildVariables
    ) {
        if (atomCode in PIPELINE_SCRIPT_ATOM_CODE) {
            try {
                val param = mapOf(
                    "workspace" to jacksonObjectMapper().writeValueAsString(workspace),
                    "buildTask" to jacksonObjectMapper().writeValueAsString(buildTask),
                    "buildVariables" to jacksonObjectMapper().writeValueAsString(buildVariables)
                )
                val paramStr = jacksonObjectMapper().writeValueAsString(param)
                val inputFileFile = File(atomTmpSpace, paramFile)

                logger.info("paramFile is:$paramFile")
                inputFileFile.writeText(paramStr)
            } catch (e: Throwable) {
                logger.error("Write param exception", e)
            }
        }
    }

    data class SdkEnv(
        val buildType: BuildType,
        val projectId: String,
        val agentId: String,
        val secretKey: String,
        val gateway: String,
        val buildId: String,
        val vmSeqId: String
    )

    private fun writeInputFile(
        workspace: File,
        inputVariables: Map<String, Any>
    ) {
//        logger.info("runtimeVariables is:$runtimeVariables") // 有敏感信息
        val inputFileFile = File(workspace, inputFile)
        inputFileFile.writeText(JsonUtil.toJson(inputVariables))
    }

    private fun output(
        buildTask: BuildTask,
        atomTmpSpace: File,
        bkWorkspace: File,
        buildVariables: BuildVariables,
        outputTemplate: Map<String, Map<String, Any>>,
        namespace: String?,
        atomCode: String
    ) {
        val atomResult = readOutputFile(atomTmpSpace)
        logger.info("the atomResult from Market is :\n$atomResult")
        // 添加插件监控数据
        val monitorData = atomResult?.monitorData
        if (monitorData != null) {
            addMonitorData(monitorData)
        }
        deletePluginFile(atomTmpSpace)
        val success: Boolean
        if (atomResult == null) {
            LoggerService.addYellowLine("No output")
        } else {
            if (atomResult.status == "success") {
                success = true
                LoggerService.addNormalLine("success: ${atomResult.message ?: ""}")
            } else {
                success = false
            }

            val outputData = atomResult.data
            val env = mutableMapOf<String, String>()
            outputData?.forEach { (varKey, output) ->
                val type = output[TYPE]
                val key = if (!namespace.isNullOrBlank()) {
                    "${namespace}_$varKey" // 用户前缀_插件输出变量名
                } else {
                    varKey
                }
                /*
                "data":{          # default模板的数据格式如下：
                    "outVar1": {
                        "type": "string",
                        "value": "testaaaaa"
                    },
                    "outVar2": {
                        "type": "artifact",
                        "value": ["xxx.html", "yyy.css"] # 本地文件路径，指定后，agent自动将这些文件归档到仓库
                    },
                    "outVar3": {
                        "type": "report",
                        "label": "",  # 报告别名，用于产出物报告界面标识当前报告
                        "path": "",   # 报告目录所在路径，相对于工作空间
                        "target": "", # 报告入口文件
                        "enableEmail": true, # 是否开启发送邮件
                        "emailReceivers": [], # 邮件接收人
                        "emailTitle": "" # 邮件标题
                    }
                }
                 */
                TaskUtil.setTaskId(buildTask.taskId ?: "")
                when (type) {
                    STRING -> env[key] = output[VALUE] as String
                    REPORT -> env[key] = archiveReport(
                        buildTask = buildTask,
                        varKey = varKey,
                        output = output,
                        buildVariables = buildVariables,
                        atomWorkspace = bkWorkspace
                    )
                    ARTIFACT -> env[key] = archiveArtifact(
                        varKey = varKey,
                        output = output,
                        atomWorkspace = bkWorkspace,
                        buildVariables = buildVariables
                    )
                }

                env["steps.${buildTask.elementId ?: ""}.outputs.$key"] = env[key] ?: ""
                env["jobs.${buildVariables.containerId}.os"] = AgentEnv.getOS().name

                TaskUtil.removeTaskId()
                if (outputTemplate.containsKey(varKey)) {
                    val outPutDefine = outputTemplate[varKey]
                    val sensitiveFlag = outPutDefine!!["isSensitive"] as Boolean? ?: false
                    if (sensitiveFlag) {
                        LoggerService.addNormalLine("output(sensitive): $key=******")
                    } else {
                        LoggerService.addNormalLine("output(normal): $key=${env[key]}")
                    }
                } else {
                    LoggerService.addYellowLine("output(except): $key=${env[key]}")
                }
            }

            if (atomResult.type == "default") {
                if (env.isNotEmpty()) {
                    addEnv(env)
                }
            }

            if (atomResult.type == "quality") {
                if (env.isNotEmpty()) {
                    addEnv(env)
                }

                // 处理质量红线数据
                val qualityMap = atomResult.qualityData?.map {
                    val value = it.value["value"]?.toString() ?: ""
                    it.key to value
                }?.toMap()
                if (qualityMap != null) {
                    qualityGatewayResourceApi.saveScriptHisMetadata(atomCode, qualityMap)
                }
            } else {
                if (atomResult.qualityData != null && atomResult.qualityData.isNotEmpty()) {
                    logger.warn("qualityData is not empty, but type is ${atomResult.type}, expected 'quality' !")
                }
            }

            // 若插件执行失败返回错误信息
            if (!success) {
                throw TaskExecuteException(
                    errorMsg = "[Finish task] status: ${atomResult.status}, errorType: ${atomResult.errorType}, " +
                        "errorCode: ${atomResult.errorCode}, message: ${atomResult.message}",
                    errorType = when (atomResult.errorType) {
                        // 插件上报的错误类型，若非用户业务错误或插件内的第三方服务调用错误，统一设为插件逻辑错误
                        1 -> ErrorType.USER
                        2 -> ErrorType.THIRD_PARTY
                        else -> ErrorType.PLUGIN
                    },
                    errorCode = atomResult.errorCode ?: ErrorCode.PLUGIN_DEFAULT_ERROR
                )
            }
        }
    }

    private fun deletePluginFile(atomWorkspace: File) {
        FileUtils.deleteRecursivelyOnExit(atomWorkspace)
    }

    /**
     * 上传归档
     */
    @Suppress("UNCHECKED_CAST")
    private fun archiveArtifact(
        varKey: String,
        output: Map<String, Any>,
        atomWorkspace: File,
        buildVariables: BuildVariables
    ): String {
        var oneArtifact = ""
        try {
            val artifacts = output[VALUE] as List<String>
            val artifactoryType = (output[ARTIFACTORY_TYPE] as? String) ?: ArtifactoryType.PIPELINE.name
            artifacts.forEach { artifact ->
                oneArtifact = artifact
                if (artifactoryType == ArtifactoryType.PIPELINE.name) {
                    ArchiveUtils.archivePipelineFiles(artifact, atomWorkspace, buildVariables)
                } else if (artifactoryType == ArtifactoryType.CUSTOM_DIR.name) {
                    output[PATH] ?: throw TaskExecuteException(
                        errorMsg = "$varKey.$PATH cannot be empty",
                        errorType = ErrorType.USER,
                        errorCode = ErrorCode.USER_INPUT_INVAILD
                    )
                    val destPath = output[PATH] as String
                    ArchiveUtils.archiveCustomFiles(artifact, destPath, atomWorkspace, buildVariables)
                }
            }
        } catch (e: Exception) {
            LoggerService.addRedLine("获取输出构件[artifact]值错误：${e.message}")
            logger.error("获取输出构件[artifact]值错误", e)
        }
        return oneArtifact
    }

    /**
     * 上传报告
     */
    private fun archiveReport(
        buildTask: BuildTask,
        varKey: String,
        output: Map<String, Any>,
        buildVariables: BuildVariables,
        atomWorkspace: File
    ): String {
        output[LABEL] ?: throw TaskExecuteException(
            errorMsg = "$varKey.$LABEL cannot be empty",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_INPUT_INVAILD
        )
        val params = mutableMapOf<String, String>()
        if (buildTask.params != null) {
            params.putAll(buildTask.params!!)
        }
        val resultData: String
        val reportType = output[REPORT_TYPE] ?: ReportTypeEnum.INTERNAL.name // 报告类型，如果用户不传则默认为平台内置类型
        params[REPORT_TYPE] = reportType.toString()
        if (reportType == ReportTypeEnum.INTERNAL.name) {
            output[PATH] ?: throw TaskExecuteException(
                errorMsg = "$varKey.$PATH cannot be empty",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD
            )
            output[KEY_TARGET] ?: throw TaskExecuteException(
                errorMsg = "$varKey.$KEY_TARGET cannot be empty",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD
            )
            params["fileDir"] = output[PATH] as String
            val target = output[KEY_TARGET] as String
            params["indexFile"] = target
            resultData = target
        } else {
            output[URL] ?: throw TaskExecuteException(
                errorMsg = "$varKey.$URL cannot be empty",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD
            )
            val url = output[URL] as String
            params["reportUrl"] = url
            resultData = url
        }
        params["reportName"] = output[LABEL] as String
        val emailReceivers = output["emailReceivers"] as? String
        val emailTitle = output["emailTitle"] as? String
        if (emailReceivers != null && emailTitle != null) {
            params["enableEmail"] = output["enableEmail"].toString()
            params["emailReceivers"] = JsonUtil.toJson(emailReceivers)
            params["emailTitle"] = emailTitle
        }
        val reportArchTask = BuildTask(
            buildId = buildTask.buildId,
            vmSeqId = buildTask.vmSeqId,
            status = buildTask.status,
            taskId = buildTask.taskId,
            elementId = buildTask.elementId,
            elementName = buildTask.elementName,
            type = buildTask.type,
            params = params,
            buildVariable = buildTask.buildVariable,
            containerType = buildTask.containerType
        )
        logger.info("${buildTask.buildId}|reportArchTask|atomWorkspacePath=${atomWorkspace.absolutePath}")

        TaskFactory.create(ReportArchiveElement.classType).run(
            buildTask = reportArchTask,
            buildVariables = buildVariables, workspace = atomWorkspace
        )

        return resultData
    }

    private fun cleanOutput(workspace: File) {
        val outputFile = File(workspace, outputFile)
        if (!outputFile.exists()) {
            return
        }
        outputFile.writeText("")
    }

    private fun readOutputFile(workspace: File): AtomResult? {
        val f = File(workspace, outputFile)
        if (!f.exists()) {
            return null
        }
        if (f.isDirectory) {
            return null
        }

        val json = f.readText()
        if (json.isEmpty()) {
            return null
        }
        logger.info("drop output=${f.delete()}")
        return JsonUtil.to(json, AtomResult::class.java)
    }

    private fun checkSha1(file: File, sha1: String) {
        val fileSha1 = file.inputStream().use { ShaUtils.sha1InputStream(it) }
        if (fileSha1 != sha1) {
            throw TaskExecuteException(
                errorMsg = "Plugin File Sha1 is wrong! wrong sha1: $fileSha1",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
            )
        }
    }

    private fun downloadAtomExecuteFile(atomFilePath: String, workspace: File): File {
        try {
            // 取插件文件名
            val lastFx = atomFilePath.lastIndexOf("/")
            val file = if (lastFx > 0) {
                File(workspace, atomFilePath.substring(lastFx + 1))
            } else {
                File(workspace, atomFilePath)
            }
            atomApi.downloadAtom(atomFilePath, file)
            return file
        } catch (t: Throwable) {
            logger.error("download plugin execute file fail:", t)
            LoggerService.addRedLine("download plugin execute file fail: ${t.message}")
            throw TaskExecuteException(
                errorMsg = "download plugin execute file fail",
                errorType = ErrorType.SYSTEM,
                errorCode = ErrorCode.SYSTEM_WORKER_LOADING_ERROR
            )
        }
    }

    private fun getJavaFile() = File(System.getProperty("java.home"), "/bin/java")

    private fun contextMap(buildTask: BuildTask): Map<String, String> {
        return mapOf(
            "steps.${buildTask.elementId}.name" to (buildTask.elementName ?: ""),
            "steps.${buildTask.elementId}.id" to (buildTask.elementId ?: ""),
            "steps.${buildTask.elementId}.status" to BuildStatus.RUNNING.name
        )
    }

    companion object {
        private const val DIR_ENV = "bk_data_dir"
        private const val INPUT_ENV = "bk_data_input"
        private const val OUTPUT_ENV = "bk_data_output"
        private val logger = LoggerFactory.getLogger(MarketAtomTask::class.java)
    }
}
