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

package com.tencent.devops.common.pipeline.pojo.element

import com.tencent.devops.common.pipeline.NameAndValue

data class ElementAdditionalOptions(
    var enable: Boolean = true,
    var continueWhenFailed: Boolean = false, // 失败时继续  continueWhenFailed = true &&  manualSkip != true（自动继续）
    val manualSkip: Boolean? = null, // (continueWhenFailed = true && manualSkip = true) 出现跳过按钮（手动继续）
    val retryWhenFailed: Boolean = false,
    val retryCount: Int = 0,
    val manualRetry: Boolean = true, // 自动重试一直失败后，界面出现重试按钮, 默认允许手动重试（为了兼容旧数据使用习惯）
    val timeout: Long? = 100, // 超时分钟
    val runCondition: RunCondition?,
    var pauseBeforeExec: Boolean? = false, // 是否配置前置暂停
    val subscriptionPauseUser: String? = "", // 订阅暂停通知用户

    val otherTask: String? = null,
    val customVariables: List<NameAndValue>? = null,
    val customCondition: String? = "",
    val elementPostInfo: ElementPostInfo? = null,

    val enableCustomEnv: Boolean? = false, // 是否设置自定义环境变量
    val customEnv: List<NameAndValue>? = null
)

enum class RunCondition {
    PRE_TASK_SUCCESS, // 所有前置插件运行成功时
    PRE_TASK_FAILED_BUT_CANCEL, // 即使前面有插件运行失败也运行，除非被取消才不运行
    PRE_TASK_FAILED_EVEN_CANCEL, // 即使前面有插件运行失败也运行，即使被取消也运行 [未实现]
    PRE_TASK_FAILED_ONLY, // 只有前面有插件运行失败时才运行
    OTHER_TASK_RUNNING, // 指定插件开始运行时 [未实现]
    CUSTOM_VARIABLE_MATCH, // 自定义变量全部满足时运行
    CUSTOM_VARIABLE_MATCH_NOT_RUN, // 自定义变量全部满足时不运行
    CUSTOM_CONDITION_MATCH // 满足以下自定义条件时运行 [未实现]
}
