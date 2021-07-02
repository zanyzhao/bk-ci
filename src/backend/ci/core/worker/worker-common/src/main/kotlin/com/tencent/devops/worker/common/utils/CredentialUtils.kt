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

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.DHUtil
import com.tencent.devops.ticket.pojo.enums.CredentialType
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.ticket.CredentialSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import org.slf4j.LoggerFactory
import java.util.Base64
import javax.ws.rs.NotFoundException

/**
 * This util is to get the credential from core
 * It use DH encrypt and decrypt
 */
@Suppress("ALL")
object CredentialUtils {

    private val sdkApi = ApiFactory.create(CredentialSDKApi::class)

    fun getCredential(buildId: String, credentialId: String, showErrorLog: Boolean = true): List<String> {
        return getCredentialWithType(credentialId, showErrorLog).first
    }

    fun getCredentialWithType(credentialId: String, showErrorLog: Boolean = true): Pair<List<String>, CredentialType> {
        if (credentialId.trim().isEmpty()) {
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorType = ErrorType.USER,
                errorMsg = "The credential Id is empty"
            )
        }
        try {
            val pair = DHUtil.initKey()
            val encoder = Base64.getEncoder()
            logger.info("Start to get the credential($credentialId)")

            val result = sdkApi.get(credentialId, encoder.encodeToString(pair.publicKey))

            if (result.isNotOk() || result.data == null) {
                logger.error("Fail to get the credential($credentialId) because of ${result.message}")
                throw NotFoundException(result.message!!)
            }

            val credential = result.data!!
            val list = ArrayList<String>()

            list.add(decode(credential.v1, credential.publicKey, pair.privateKey))
            if (!credential.v2.isNullOrEmpty()) {
                list.add(decode(credential.v2!!, credential.publicKey, pair.privateKey))
            }
            if (!credential.v3.isNullOrEmpty()) {
                list.add(decode(credential.v3!!, credential.publicKey, pair.privateKey))
            }
            if (!credential.v4.isNullOrEmpty()) {
                list.add(decode(credential.v4!!, credential.publicKey, pair.privateKey))
            }
            return Pair(list, credential.credentialType)
        } catch (ignored: Exception) {
            logger.warn("Fail to get the credential($credentialId), $ignored")
            if (showErrorLog) {
                LoggerService.addRedLine("获取凭证（$credentialId）失败， 原因：${ignored.message}")
            }
            throw ignored
        }
    }

    fun getCredentialContextValue(key: String): String? {
        val ticketId = getCredentialKey(key)
        if (ticketId == key) {
            return null
        }

        return try {
            val valueTypePair = getCredentialWithType(ticketId, false)
            val value = getCredentialValue(valueTypePair.first, valueTypePair.second, key)
            logger.info("get credential context value, key: $key, value: $value")
            value
        } catch (ignore: Exception) {
            logger.warn("凭证ID变量($ticketId)不存在", ignore.message)
            null
        }
    }

    private fun getCredentialKey(key: String): String {
        // 参考CredentialType
        return if (key.startsWith("settings.") && (
                key.endsWith(".password") ||
                    key.endsWith(".access_token") ||
                    key.endsWith(".username") ||
                    key.endsWith(".secretKey") ||
                    key.endsWith(".appId") ||
                    key.endsWith(".privateKey") ||
                    key.endsWith(".passphrase") ||
                    key.endsWith(".token") ||
                    key.endsWith(".cosappId") ||
                    key.endsWith(".secretId") ||
                    key.endsWith(".region")
                )) {
            key.substringAfter("settings.").substringBeforeLast(".")
        } else {
            key
        }
    }

    private fun getCredentialValue(valueList: List<String>, type: CredentialType, key: String): String? {
        if (valueList.isEmpty()) {
            return null
        }

        when (type) {
            CredentialType.PASSWORD -> {
                if (key.endsWith(".password")) {
                    return valueList[0]
                }
            }
            CredentialType.ACCESSTOKEN -> {
                if (key.endsWith(".access_token")) {
                    return valueList[0]
                }
            }
            CredentialType.USERNAME_PASSWORD -> {
                if (valueList.size >= 2) {
                    if (key.endsWith(".username")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".password")) {
                        return valueList[1]
                    }
                }
            }
            CredentialType.SECRETKEY -> {
                if (key.endsWith(".secretKey")) {
                    return valueList[0]
                }
            }
            CredentialType.APPID_SECRETKEY -> {
                if (valueList.size >= 2) {
                    if (key.endsWith(".appId")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".secretKey")) {
                        return valueList[1]
                    }
                }
            }
            CredentialType.SSH_PRIVATEKEY -> {
                if (valueList.size == 1) {
                    if (key.endsWith(".privateKey")) {
                        return valueList[0]
                    }
                }
                if (valueList.size >= 2) {
                    if (key.endsWith(".privateKey")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".passphrase")) {
                        return valueList[1]
                    }
                }
            }
            CredentialType.TOKEN_SSH_PRIVATEKEY -> {
                if (valueList.size == 2) {
                    if (key.endsWith(".token")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".privateKey")) {
                        return valueList[1]
                    }
                }
                if (valueList.size >= 3) {
                    if (key.endsWith(".token")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".privateKey")) {
                        return valueList[1]
                    }
                    if (key.endsWith(".passphrase")) {
                        return valueList[2]
                    }
                }
            }
            CredentialType.TOKEN_USERNAME_PASSWORD -> {
                if (valueList.size >= 3) {
                    if (key.endsWith(".token")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".username")) {
                        return valueList[1]
                    }
                    if (key.endsWith(".password")) {
                        return valueList[2]
                    }
                }
            }
            CredentialType.COS_APPID_SECRETID_SECRETKEY_REGION -> {
                if (valueList.size >= 4) {
                    if (key.endsWith(".cosappId")) {
                        return valueList[0]
                    }
                    if (key.endsWith(".secretId")) {
                        return valueList[1]
                    }
                    if (key.endsWith(".secretKey")) {
                        return valueList[2]
                    }
                    if (key.endsWith(".region")) {
                        return valueList[3]
                    }
                }
            }
            CredentialType.MULTI_LINE_PASSWORD -> {
                if (valueList.isNotEmpty() && key.endsWith(".password")) {
                    return valueList[0]
                }
            }
        }
        return null
    }

    private fun decode(encode: String, publicKey: String, privateKey: ByteArray): String {
        val decoder = Base64.getDecoder()
        return String(DHUtil.decrypt(decoder.decode(encode), decoder.decode(publicKey), privateKey))
    }

    private val logger = LoggerFactory.getLogger(CredentialUtils::class.java)
}
