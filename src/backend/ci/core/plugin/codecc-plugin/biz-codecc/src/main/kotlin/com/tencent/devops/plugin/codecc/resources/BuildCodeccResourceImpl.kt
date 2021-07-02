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

package com.tencent.devops.plugin.codecc.resources

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.BuildCodeccResource
import com.tencent.devops.plugin.codecc.config.CodeccScriptConfig
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.plugin.codecc.service.CodeccToolDownloaderService
import com.tencent.devops.plugin.codecc.service.CodeccService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

@RestResource
class BuildCodeccResourceImpl @Autowired constructor(
    private val codeccService: CodeccService,
    private val codeccToolDownloaderService: CodeccToolDownloaderService
) : BuildCodeccResource {

    override fun downloadTool(toolName: String, osType: OSType, fileMd5: String, is32Bit: Boolean?): Response {
        return codeccToolDownloaderService.downloadTool(toolName, osType, fileMd5, is32Bit)
    }

    override fun downloadToolsScript(osType: OSType, fileMd5: String): Response {
        return codeccToolDownloaderService.downloadToolsScript(osType, fileMd5)
    }

    override fun queryCodeccTaskDetailUrl(projectId: String, pipelineId: String, buildId: String): String {
        return codeccService.queryCodeccTaskDetailUrl(projectId, pipelineId, buildId)
    }

    override fun saveCodeccTask(projectId: String, pipelineId: String, buildId: String): Result<Int> {
        return Result(codeccService.saveCodeccTask(projectId, pipelineId, buildId))
    }

    override fun getCodeccReport(buildId: String): Result<CodeccCallback?> {
        return Result(codeccService.getCodeccReport(buildId))
    }

    override fun getCodeccSingleScriptConfig(): Result<CodeccScriptConfig> {
        return Result(codeccService.getSingleCodeccScriptConfig())
    }
}
