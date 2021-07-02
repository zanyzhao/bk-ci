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

package com.tencent.devops.worker.common.task

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.env.BuildEnv
import com.tencent.devops.worker.common.env.BuildType
import java.io.File

abstract class ITask {

    private val environment = HashMap<String, String>()

    private val monitorData = HashMap<String, Any>()

    fun run(
        buildTask: BuildTask,
        buildVariables: BuildVariables,
        workspace: File
    ) {
        val params = buildTask.params
        if (params != null && null != params["additionalOptions"]) {
            val additionalOptionsStr = params["additionalOptions"]
            val additionalOptions = JsonUtil.toOrNull(additionalOptionsStr, ElementAdditionalOptions::class.java)
            if (additionalOptions?.enableCustomEnv == true && additionalOptions.customEnv?.isNotEmpty() == true) {
                val variables = buildVariables.variables.toMutableMap()
                additionalOptions.customEnv!!.filter { !it.key.isNullOrBlank() }.forEach {
                    variables[it.key!!] = it.value ?: ""
                }
                return execute(
                    buildTask,
                    BuildVariables(
                        buildId = buildVariables.buildId,
                        vmSeqId = buildVariables.vmSeqId,
                        vmName = buildVariables.vmName,
                        projectId = buildVariables.projectId,
                        pipelineId = buildVariables.pipelineId,
                        variables = variables,
                        buildEnvs = buildVariables.buildEnvs,
                        containerId = buildVariables.containerId,
                        containerHashId = buildVariables.containerHashId,
                        variablesWithType = buildVariables.variablesWithType,
                        timeoutMills = buildVariables.timeoutMills,
                        containerType = buildVariables.containerType
                    ),
                    workspace
                )
            }
        }
        execute(buildTask, buildVariables, workspace)
    }

    protected abstract fun execute(
        buildTask: BuildTask,
        buildVariables: BuildVariables,
        workspace: File
    )

    protected fun addEnv(env: Map<String, String>) {
        environment.putAll(env)
    }

    protected fun addEnv(key: String, value: String) {
        environment[key] = value
    }

    protected fun getEnv(key: String) =
        environment[key] ?: ""

    fun getAllEnv(): Map<String, String> {
        return environment
    }

    protected fun addMonitorData(monitorDataMap: Map<String, Any>) {
        monitorData.putAll(monitorDataMap)
    }

    fun getMonitorData(): Map<String, Any> {
        return monitorData
    }

    protected fun isThirdAgent() = BuildEnv.getBuildType() == BuildType.AGENT
}
