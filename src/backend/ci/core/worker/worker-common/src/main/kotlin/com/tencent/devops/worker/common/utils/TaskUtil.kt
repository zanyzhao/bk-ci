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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.process.pojo.BuildTask

object TaskUtil {

    private val taskThreadLocal = ThreadLocal<String>()

    fun isContinueWhenFailed(buildTask: BuildTask): Boolean {
        val params = buildTask.params
        if (params != null && null != params["additionalOptions"]) {
            val additionalOptionsStr = params["additionalOptions"]
            val additionalOptions = JsonUtil.toOrNull(additionalOptionsStr, ElementAdditionalOptions::class.java)
            return (additionalOptions?.continueWhenFailed ?: false) && additionalOptions?.manualSkip != true
        }

        return false
    }

    fun getTimeOut(buildTask: BuildTask): Long? {
        val params = buildTask.params
        if (params != null && null != params["additionalOptions"]) {
            val additionalOptionsStr = params["additionalOptions"]
            val additionalOptions = JsonUtil.toOrNull(additionalOptionsStr, ElementAdditionalOptions::class.java)
            return additionalOptions?.timeout
        }
        return 0
    }

    fun setTaskId(taskId: String) {
        taskThreadLocal.set(taskId)
    }

    fun getTaskId(): String {
        return taskThreadLocal.get() ?: ""
    }

    fun removeTaskId() {
        taskThreadLocal.remove()
    }

    fun isVmBuildEnv(containerType: String? = null): Boolean {
        return containerType == VMBuildContainer.classType
    }
}
