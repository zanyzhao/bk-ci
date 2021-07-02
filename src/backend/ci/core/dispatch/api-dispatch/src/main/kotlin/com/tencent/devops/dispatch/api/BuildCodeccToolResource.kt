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

package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.enums.OSType
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(tags = ["BUILD_CODECC"], description = "构建-CODECC相关资源")
@Path("/build/codecc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildCodeccToolResource {

    @ApiOperation("下载toolName对应的tool")
    @GET
    @Path("/{toolName}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiResponses(
        ApiResponse(code = 304, message = "tool已是最新，无需下载")
    )
    fun downloadTool(
        @ApiParam("工具类型", required = true)
        @PathParam("toolName")
        toolName: String,
        @ApiParam("系统类型", required = true)
        @QueryParam("osType")
        osType: OSType,
        @ApiParam("文件md5", required = true)
        @QueryParam("fileMd5")
        fileMd5: String,
        @ApiParam("是否是32位操作系统", required = false)
        @QueryParam("is32Bit")
        is32Bit: Boolean?
    ): Response

    @ApiOperation("下载coverity脚本")
    @GET
    @Path("/coverity/script")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiResponses(
        ApiResponse(code = 304, message = "本地的coverity script已是最新，无需下载")
    )
    fun downloadCovScript(
        @ApiParam("系统类型", required = true)
        @QueryParam("osType")
        osType: OSType,
        @ApiParam("文件md5", required = true)
        @QueryParam("fileMd5")
        fileMd5: String
    ): Response

    @ApiOperation("下载多工具脚本")
    @GET
    @Path("/tools/script")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiResponses(
        ApiResponse(code = 304, message = "本地的多工具script已是最新，无需下载")
    )
    fun downloadToolsScript(
        @ApiParam("系统类型", required = true)
        @QueryParam("osType")
        osType: OSType,
        @ApiParam("文件md5", required = true)
        @QueryParam("fileMd5")
        fileMd5: String
    ): Response
}
