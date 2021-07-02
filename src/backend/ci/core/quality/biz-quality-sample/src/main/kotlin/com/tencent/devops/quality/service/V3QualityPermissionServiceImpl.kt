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

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.code.QualityAuthServiceCode

@Suppress("ALL")
class V3QualityPermissionServiceImpl constructor(
    override val authPermissionApi: AuthPermissionApi,
    override val authResourceApi: AuthResourceApi,
    override val qualityAuthServiceCode: QualityAuthServiceCode
) : AbsQualityPermissionServiceImpl(authPermissionApi, authResourceApi, qualityAuthServiceCode) {
    override fun validateGroupPermission(
        userId: String,
        projectId: String,
        groupId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        super.validateGroupPermission(userId, projectId, groupId, authPermission, message)
    }

    override fun createGroupResource(userId: String, projectId: String, groupId: Long, groupName: String) {
        super.createGroupResource(userId, projectId, groupId, groupName)
    }

    override fun modifyGroupResource(projectId: String, groupId: Long, groupName: String) {
        super.modifyGroupResource(projectId, groupId, groupName)
    }

    override fun deleteGroupResource(projectId: String, groupId: Long) {
        super.deleteGroupResource(projectId, groupId)
    }

    override fun filterGroup(user: String, projectId: String, authPermissions: Set<AuthPermission>): Map<AuthPermission, List<Long>> {
        return super.filterGroup(user, projectId, authPermissions)
    }

    override fun validateRulePermission(userId: String, projectId: String, authPermission: AuthPermission): Boolean {
        return super.validateRulePermission(userId, projectId, authPermission)
    }

    override fun validateRulePermission(userId: String, projectId: String, authPermission: AuthPermission, message: String) {
        super.validateRulePermission(userId, projectId, authPermission, message)
    }

    override fun validateRulePermission(
        userId: String,
        projectId: String,
        ruleId: Long,
        authPermission: AuthPermission,
        message: String
    ) {
        super.validateRulePermission(userId, projectId, ruleId, authPermission, message)
    }

    override fun createRuleResource(userId: String, projectId: String, ruleId: Long, ruleName: String) {
        super.createRuleResource(userId, projectId, ruleId, ruleName)
    }

    override fun modifyRuleResource(projectId: String, ruleId: Long, ruleName: String) {
        super.modifyRuleResource(projectId, ruleId, ruleName)
    }

    override fun deleteRuleResource(projectId: String, ruleId: Long) {
        super.deleteRuleResource(projectId, ruleId)
    }

    override fun filterRules(
        userId: String,
        projectId: String,
        bkAuthPermissionSet: Set<AuthPermission>
    ): Map<AuthPermission, List<Long>> {
        return super.filterRules(userId, projectId, bkAuthPermissionSet)
    }

    override fun supplierForPermissionGroup(projectId: String): () -> MutableList<String> {
        return { mutableListOf() }
    }

    override fun supplierForPermissionRule(projectId: String): () -> MutableList<String> {
        return { mutableListOf() }
    }
}
