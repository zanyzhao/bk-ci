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

package com.tencent.devops.log.resources

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.log.api.AppLogResource
import com.tencent.devops.common.log.pojo.EndPageQueryLogs
import com.tencent.devops.common.log.pojo.PageQueryLogs
import com.tencent.devops.common.log.pojo.QueryLogs
import com.tencent.devops.log.service.BuildLogQueryService
import org.springframework.beans.factory.annotation.Autowired
import javax.ws.rs.core.Response

/**
 *
 * Powered By Tencent
 */
@RestResource
class AppLogResourceImpl @Autowired constructor(
    private val buildLogQuery: BuildLogQueryService,
    private val authPermissionApi: AuthPermissionApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode
) : AppLogResource {

    companion object {
        private const val defaultNum = 100
    }

    override fun getInitLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?,
        page: Int?,
        pageSize: Int?
    ): Result<PageQueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId)
        return buildLogQuery.getInitLogsPage(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            debug = debug,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getMoreLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        num: Int?,
        fromStart: Boolean?,
        start: Long,
        end: Long,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId)
        return buildLogQuery.getMoreLogs(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            debug = debug,
            num = num ?: defaultNum,
            fromStart = fromStart,
            start = start,
            end = end,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    override fun getAfterLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        start: Long,
        debug: Boolean?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId)
        return buildLogQuery.getAfterLogs(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            start = start,
            debug = debug,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    override fun getBeforeLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        end: Long,
        debug: Boolean?,
        size: Int?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId)
        return buildLogQuery.getBeforeLogs(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            end = end,
            debug = debug,
            size = size,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    override fun downloadLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): Response {
        validateAuth(userId, projectId, pipelineId, buildId)
        return buildLogQuery.downloadLogs(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount,
            fileName = null
        )
    }

    override fun getEndLogsPage(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        size: Int,
        debug: Boolean?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<EndPageQueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId)
        return buildLogQuery.getEndLogsPage(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            size = size,
            debug = debug,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    override fun getBottomLogs(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        debug: Boolean?,
        size: Int?,
        tag: String?,
        subTag: String?,
        jobId: String?,
        executeCount: Int?
    ): Result<QueryLogs> {
        validateAuth(userId, projectId, pipelineId, buildId)
        return buildLogQuery.getBottomLogs(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            debug = debug,
            size = size,
            tag = tag,
            subTag = subTag,
            jobId = jobId,
            executeCount = executeCount
        )
    }

    private fun validateAuth(userId: String, projectId: String, pipelineId: String, buildId: String) {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (pipelineId.isBlank()) {
            throw ParamBlankException("Invalid pipelineId")
        }
        if (buildId.isBlank()) {
            throw ParamBlankException("Invalid buildId")
        }

        if (!authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = pipelineAuthServiceCode,
                resourceType = AuthResourceType.PIPELINE_DEFAULT,
                projectCode = projectId,
                resourceCode = pipelineId,
                permission = AuthPermission.VIEW
            )
        ) {
            throw PermissionForbiddenException("用户($userId)无权限在工程($projectId)下查看流水线")
        }
    }
}
