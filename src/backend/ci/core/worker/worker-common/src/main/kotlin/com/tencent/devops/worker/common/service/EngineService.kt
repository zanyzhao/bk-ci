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

package com.tencent.devops.worker.common.service

import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildTaskResult
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.CI_TOKEN_CONTEXT
import com.tencent.devops.worker.common.JOB_OS_CONTEXT
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.engine.EngineBuildSDKApi
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.HttpRetryUtils
import org.slf4j.LoggerFactory

object EngineService {

    private val logger = LoggerFactory.getLogger(EngineService::class.java)

    private val buildApi = ApiFactory.create(EngineBuildSDKApi::class)

    fun setStarted(): BuildVariables {
        var retryCount = 0
        val result = HttpRetryUtils.retry {
            if (retryCount > 0) {
                logger.warn("retry|time=$retryCount|setStarted")
            }
            buildApi.setStarted(retryCount++)
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Report builder startup status failed")
        }
        val ret = result.data ?: throw RemoteServiceException("Report builder startup status failed")
        val ciToken = buildApi.getCiToken()
        return if (ciToken.isBlank()) {
            ret
        } else {
            BuildVariables(
                buildId = ret.buildId,
                vmSeqId = ret.vmSeqId,
                vmName = ret.vmName,
                projectId = ret.projectId,
                pipelineId = ret.pipelineId,
                variables = ret.variables.plus(mapOf(
                    CI_TOKEN_CONTEXT to ciToken,
                    JOB_OS_CONTEXT to AgentEnv.getOS().name
                )),
                buildEnvs = ret.buildEnvs,
                containerId = ret.containerId,
                containerHashId = ret.containerHashId,
                variablesWithType = ret.variablesWithType,
                timeoutMills = ret.timeoutMills,
                containerType = ret.containerType
            )
        }
    }

    fun claimTask(): BuildTask {
        var retryCount = 0
        val result = HttpRetryUtils.retry {
            if (retryCount > 0) {
                logger.warn("retry|time=$retryCount|claimTask")
            }
            buildApi.claimTask(retryCount++)
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to get build task")
        }
        return result.data ?: throw RemoteServiceException("Failed to get build task")
    }

    fun completeTask(taskResult: BuildTaskResult) {
        LoggerService.flush()
        var retryCount = 0
        val result = HttpRetryUtils.retry {
            if (retryCount > 0) {
                logger.warn("retry|time=$retryCount|completeTask")
            }
            buildApi.completeTask(taskResult, retryCount++)
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to complete build task")
        }
    }

    fun endBuild() {
        var retryCount = 0
        val result = HttpRetryUtils.retry {
            if (retryCount > 0) {
                logger.warn("retry|time=$retryCount|endBuild")
            }
            buildApi.endTask(retryCount++)
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to end build task")
        }
    }

    fun heartbeat() {
        var retryCount = 0
        val result = HttpRetryUtils.retryWhenHttpRetryException {
            if (retryCount > 0) {
                logger.warn("retryWhenHttpRetryException|time=$retryCount|heartbeat")
            }
            retryCount++
            buildApi.heartbeat()
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to do heartbeat task")
        }
    }

    fun timeout() {
        var retryCount = 0
        val result = HttpRetryUtils.retryWhenHttpRetryException {
            if (retryCount > 0) {
                logger.warn("retryWhenHttpRetryException|time=$retryCount|timeout")
            }
            retryCount++
            buildApi.timeout()
        }
        if (result.isNotOk()) {
            throw RemoteServiceException("Failed to report timeout")
        }
    }
}
