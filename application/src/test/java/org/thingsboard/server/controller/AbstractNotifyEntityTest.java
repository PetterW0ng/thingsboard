/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Locale;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.thingsboard.server.service.entitiy.DefaultTbNotificationEntityService.edgeTypeByActionType;

@Slf4j
public abstract class AbstractNotifyEntityTest extends AbstractWebTest {

    @SpyBean
    protected TbClusterService tbClusterService;

    @SpyBean
    protected AuditLogService auditLogService;

    protected void testNotifyEntityAllOneTime(HasName entity, EntityId entityId, EntityId originatorId,
                                              TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                              ActionType actionType, Object... additionalInfo) {
        testSendNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, 1);
        testLogEntityActionOneTime(entity, originatorId, tenantId, customerId, userId, userName, actionType, additionalInfo);
        testPushMsgToRuleEngineTime(originatorId, tenantId, 1);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityNeverMsgToEdgeServiceOneTime(HasName entity, EntityId entityId, TenantId tenantId,
                                                                ActionType actionType) {
        testSendNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, 1);
        testLogEntityActionNever(entityId, entity);
        testPushMsgToRuleEngineNever(entityId);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityOneTimeMsgToEdgeServiceNever(HasName entity, EntityId entityId, EntityId originatorId,
                                                                TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                ActionType actionType, Object... additionalInfo) {
        testNotificationMsgToEdgeServiceNever(entityId);
        testLogEntityActionOneTime(entity, originatorId, tenantId, customerId, userId, userName, actionType, additionalInfo);
        testPushMsgToRuleEngineTime(originatorId, tenantId, 1);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityManyTimeMsgToEdgeServiceNever(HasName entity, HasName originator,
                                                                 TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                 ActionType actionType, int cntTime, Object... additionalInfo) {
        EntityId entityId = createEntityId_NULL_UUID(entity);
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testNotificationMsgToEdgeServiceNever(entityId);
        testLogEntityActionManyTime(entity,originatorId, tenantId, customerId,
                userId, userName, actionType, cntTime, additionalInfo);
        testPushMsgToRuleEngineTime(originatorId, tenantId, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(HasName entity, EntityId entityId, EntityId originatorId,
                                                                                               TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                                               ActionType actionType, Object... additionalInfo) {
        testNotificationMsgToEdgeServiceNever(entityId);
        testLogEntityActionOneTime(entity, originatorId, tenantId, customerId, userId, userName, actionType, additionalInfo);
        testPushMsgToRuleEngineTime(originatorId, tenantId, 1);
        testBroadcastEntityStateChangeEventTime(entityId, tenantId, 1);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityOneTimeError(HasName entity, TenantId tenantId,
                                                UserId userId, String userName, ActionType actionType, Exception exp,
                                                Object... additionalInfo) {
        CustomerId customer_NULL_UUID = (CustomerId) EntityIdFactory.getByTypeAndUuid(EntityType.CUSTOMER, ModelConstants.NULL_UUID);
        EntityId entity_NULL_UUID = createEntityId_NULL_UUID(entity);
        testNotificationMsgToEdgeServiceNever(entity_NULL_UUID);
        if (additionalInfo.length > 0) {
            Mockito.verify(auditLogService, times(1))
                    .logEntityAction(Mockito.eq(tenantId),
                            Mockito.eq(customer_NULL_UUID), Mockito.eq(userId), Mockito.eq(userName),
                            Mockito.eq(entity_NULL_UUID), Mockito.any(entity.getClass()), Mockito.eq(actionType),
                            Mockito.argThat(argument ->
                                    argument.getMessage().contains(exp.getMessage())),
                            Mockito.eq(additionalInfo)
                    );
        } else {
            Mockito.verify(auditLogService, times(1))
                    .logEntityAction(Mockito.eq(tenantId),
                            Mockito.eq(customer_NULL_UUID), Mockito.eq(userId), Mockito.eq(userName),
                            Mockito.eq(entity_NULL_UUID), Mockito.any(entity.getClass()), Mockito.eq(actionType),
                            Mockito.argThat((argument -> argument.getMessage().contains(exp.getMessage()) & argument.getClass().equals(exp.getClass())))
                    );
        }
        testPushMsgToRuleEngineNever(entity_NULL_UUID);
    }

    protected void testNotifyEntityNever(EntityId entityId, HasName entity) {
        entityId = entityId == null ? createEntityId_NULL_UUID(entity) : entityId;
        testNotificationMsgToEdgeServiceNever(entityId);
        testLogEntityActionNever(entityId, entity);
        testPushMsgToRuleEngineNever(entityId);
        Mockito.reset(tbClusterService, auditLogService);
    }

    private void testNotificationMsgToEdgeServiceNever(EntityId entityId) {
        Mockito.verify(tbClusterService, never()).sendNotificationMsgToEdgeService(Mockito.any(),
                Mockito.any(), Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any(), Mockito.any());
    }

    private void testLogEntityActionNever(EntityId entityId, HasName entity) {
        Mockito.verify(auditLogService, never()).logEntityAction(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(entityId.getClass()), Mockito.any(entity.getClass()),
                Mockito.any(), Mockito.any());
    }

    private void testPushMsgToRuleEngineNever(EntityId entityId) {
        Mockito.verify(tbClusterService, never()).pushMsgToRuleEngine(Mockito.any(),
                Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any());
    }

    private void testLogEntityActionOneTime(HasName entity, EntityId originatorId, TenantId tenantId, CustomerId customerId,
                                            UserId userId, String userName, ActionType actionType, Object... additionalInfo) {
        if (additionalInfo.length == 0) {
            Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId), Mockito.eq(customerId),
                    Mockito.eq(userId), Mockito.eq(userName), Mockito.eq(originatorId),
                    Mockito.eq(entity), Mockito.eq(actionType), Mockito.isNull());
        } else {
            String additionalInfoStr = extractParameter(String.class, 0, additionalInfo);
            Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId), Mockito.eq(customerId),
                    Mockito.eq(userId), Mockito.eq(userName), Mockito.eq(originatorId),
                    Mockito.eq(entity), Mockito.eq(actionType), Mockito.isNull(), Mockito.eq(additionalInfoStr));
        }
    }

    private void testLogEntityActionManyTime(HasName entity, EntityId originatorId, TenantId tenantId, CustomerId customerId,
                                             UserId userId, String userName, ActionType actionType, int cntTime, Object... additionalInfo) {
        if (additionalInfo.length == 0) {
            Mockito.verify(auditLogService, times(cntTime)).logEntityAction(Mockito.eq(tenantId), Mockito.eq(customerId),
                    Mockito.eq(userId), Mockito.eq(userName), Mockito.any(originatorId.getClass()),
                    Mockito.any(entity.getClass()), Mockito.eq(actionType), Mockito.isNull());
        } else {
            String additionalInfoStr = extractParameter(String.class, 0, additionalInfo);
            Mockito.verify(auditLogService, times(cntTime)).logEntityAction(Mockito.eq(tenantId), Mockito.eq(customerId),
                    Mockito.eq(userId), Mockito.eq(userName), Mockito.any(originatorId.getClass()),
                    Mockito.any(entity.getClass()), Mockito.eq(actionType), Mockito.isNull(), Mockito.eq(additionalInfoStr));
        }
    }

    private void testPushMsgToRuleEngineTime(EntityId originatorId, TenantId tenantId, int cntTime) {
        if (cntTime ==1) {
            Mockito.verify(tbClusterService, times(cntTime)).pushMsgToRuleEngine(Mockito.eq(tenantId),
                    Mockito.eq(originatorId), Mockito.any(TbMsg.class), Mockito.isNull());
        } else {
            Mockito.verify(tbClusterService, times(cntTime)).pushMsgToRuleEngine(Mockito.eq(tenantId),
                    Mockito.any(originatorId.getClass()), Mockito.any(TbMsg.class), Mockito.isNull());
        }
    }

    private void testSendNotificationMsgToEdgeServiceTime(EntityId entityId, TenantId tenantId, ActionType actionType, int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdgeService(Mockito.eq(tenantId),
                Mockito.isNull(), Mockito.eq(entityId), Mockito.isNull(), Mockito.isNull(),
                Mockito.eq(edgeTypeByActionType(actionType)));
    }

    private void testBroadcastEntityStateChangeEventTime(EntityId entityId, TenantId tenantId, int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).broadcastEntityStateChangeEvent(Mockito.eq(tenantId),
                Mockito.any(entityId.getClass()), Mockito.any(ComponentLifecycleEvent.class));
    }

    private <T> T extractParameter(Class<T> clazz, int index, Object... additionalInfo) {
        T result = null;
        if (additionalInfo != null && additionalInfo.length > index) {
            Object paramObject = additionalInfo[index];
            if (clazz.isInstance(paramObject)) {
                result = clazz.cast(paramObject);
            }
        }
        return result;
    }

    private EntityId createEntityId_NULL_UUID(HasName entity) {
        return EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(entity.getClass().toString()
                        .substring(entity.getClass().toString().lastIndexOf(".") + 1).toUpperCase(Locale.ENGLISH)),
                ModelConstants.NULL_UUID);
    }
}
