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

package com.tencent.devops.quality.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.QualityAuthServiceCode
import com.tencent.devops.common.service.utils.MessageCodeUtil

@Suppress("ALL")
abstract class AbsQualityPermissionServiceImpl constructor(
    open val authPermissionApi: AuthPermissionApi,
    open val authResourceApi: AuthResourceApi,
    open val qualityAuthServiceCode: QualityAuthServiceCode
) : QualityPermissionService {

    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = qualityAuthServiceCode,
                resourceType = AuthResourceType.QUALITY_GROUP,
                projectCode = projectId,
                resourceCode = HashUtil.encodeLongId(groupId),
                permission = authPermission
            )) {
            val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${authPermission.value}",
                defaultMessage = authPermission.alias
            )
            throw PermissionForbiddenException(
                message = message,
                params = arrayOf(permissionMsg))
        }
    }

    override fun createGroupResource(userId: String, projectId: String, groupId: Long, groupName: String) {
        authResourceApi.createResource(
            user = userId,
            serviceCode = qualityAuthServiceCode,
            resourceType = AuthResourceType.QUALITY_GROUP,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    override fun modifyGroupResource(projectId: String, groupId: Long, groupName: String) {
        authResourceApi.modifyResource(
            serviceCode = qualityAuthServiceCode,
            resourceType = AuthResourceType.QUALITY_GROUP,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId),
            resourceName = groupName
        )
    }

    override fun deleteGroupResource(projectId: String, groupId: Long) {
        authResourceApi.deleteResource(
            serviceCode = qualityAuthServiceCode,
            resourceType = AuthResourceType.QUALITY_GROUP,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(groupId)
        )
    }

    override fun filterGroup(
        user: String,
        projectId: String,
        authPermissions: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val permissionResourceMap = authPermissionApi.getUserResourcesByPermissions(
            user = user,
            serviceCode = qualityAuthServiceCode,
            resourceType = AuthResourceType.QUALITY_GROUP,
            projectCode = projectId,
            permissions = authPermissions,
            supplier = supplierForPermissionRule(projectId)
        )
        val map = mutableMapOf<AuthPermission, List<Long>>()
        permissionResourceMap.forEach { (key, value) ->
            map[key] = value.map { HashUtil.decodeIdToLong(it) }
        }
        return map
    }

    override fun validateRulePermission(userId: String, projectId: String, authPermission: AuthPermission): Boolean {
        return authPermissionApi.validateUserResourcePermission(
            user = userId,
            serviceCode = qualityAuthServiceCode,
            resourceType = AuthResourceType.QUALITY_GROUP,
            projectCode = projectId,
            permission = authPermission
        )
    }

    override fun validateRulePermission(
        userId: String,
        projectId: String,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = qualityAuthServiceCode,
                resourceType = AuthResourceType.QUALITY_GROUP,
                projectCode = projectId,
                permission = authPermission
            )) {
            val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${authPermission.value}",
                defaultMessage = authPermission.alias
            )
            throw PermissionForbiddenException(
                message = message,
                params = arrayOf(permissionMsg)
            )
        }
    }

    override fun validateRulePermission(
        userId: String,
        projectId: String,
        ruleId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        if (!authPermissionApi.validateUserResourcePermission(
                user = userId,
                serviceCode = qualityAuthServiceCode,
                resourceType = AuthResourceType.QUALITY_GROUP,
                projectCode = projectId,
                resourceCode = HashUtil.encodeLongId(ruleId),
                permission = authPermission)) {
            val permissionMsg = MessageCodeUtil.getCodeLanMessage(
                messageCode = "${CommonMessageCode.MSG_CODE_PERMISSION_PREFIX}${authPermission.value}",
                defaultMessage = authPermission.alias
            )
            throw PermissionForbiddenException(
                message = message,
                params = arrayOf(permissionMsg)
            )
        }
    }

    override fun createRuleResource(userId: String, projectId: String, ruleId: Long, ruleName: String) {
        authResourceApi.createResource(
            user = userId,
            serviceCode = qualityAuthServiceCode,
            resourceType = AuthResourceType.QUALITY_RULE,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(ruleId),
            resourceName = ruleName
        )
    }

    override fun modifyRuleResource(projectId: String, ruleId: Long, ruleName: String) {
        authResourceApi.modifyResource(
            serviceCode = qualityAuthServiceCode,
            resourceType = AuthResourceType.QUALITY_RULE,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(ruleId),
            resourceName = ruleName
        )
    }

    override fun deleteRuleResource(projectId: String, ruleId: Long) {
        authResourceApi.deleteResource(
            serviceCode = qualityAuthServiceCode,
            resourceType = AuthResourceType.QUALITY_RULE,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(ruleId)
        )
    }

    override fun filterRules(
        userId: String,
        projectId: String,
        bkAuthPermissionSet: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        val permissionResourceMap = authPermissionApi.getUserResourcesByPermissions(
            user = userId,
            serviceCode = qualityAuthServiceCode,
            resourceType = AuthResourceType.QUALITY_RULE,
            projectCode = projectId,
            permissions = bkAuthPermissionSet,
            supplier = supplierForPermissionGroup(projectId)
        )
        val permissionRuleMap = mutableMapOf<AuthPermission, List<Long>>()
        permissionResourceMap.forEach { (permission, list) ->
            permissionRuleMap[permission] = list.map { HashUtil.decodeIdToLong(it) }
        }
        return permissionRuleMap
    }

    abstract fun supplierForPermissionGroup(projectId: String): () -> MutableList<String>

    abstract fun supplierForPermissionRule(projectId: String): () -> MutableList<String>
}
