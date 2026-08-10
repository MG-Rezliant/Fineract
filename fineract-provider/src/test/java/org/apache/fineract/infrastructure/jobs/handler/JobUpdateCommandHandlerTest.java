/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.infrastructure.jobs.handler;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.apache.fineract.infrastructure.jobs.data.JobUpdateRequest;
import org.apache.fineract.infrastructure.jobs.data.JobUpdateResponse;
import org.apache.fineract.infrastructure.jobs.service.JobRegisterService;
import org.apache.fineract.infrastructure.jobs.service.SchedulerWritePlatformService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobUpdateCommandHandlerTest {

    @Mock
    SchedulerWritePlatformService schedulerWritePlatformService;

    @Mock
    JobRegisterService jobRegisterService;

    @InjectMocks
    JobUpdateCommandHandler handler;

    @Test
    void handleReschedulesWhenCronChanged() {
        JobUpdateRequest req = JobUpdateRequest.builder().jobId(1L).cronExpression("0 0 * * * ?").build();
        var cmd = new org.apache.fineract.infrastructure.jobs.command.JobUpdateCommand();
        cmd.setPayload(req);

        JobUpdateResponse expected = JobUpdateResponse.builder().resourceId(1L).changes(Map.of("cronExpression", "0 0 * * * ?")).build();
        when(schedulerWritePlatformService.updateJobDetail(req)).thenReturn(expected);

        assertSame(expected, handler.handle(cmd));
        verify(jobRegisterService).rescheduleJob(1L);
    }

    @Test
    void handleDoesNotRescheduleWhenOnlyDisplayNameChanged() {
        JobUpdateRequest req = JobUpdateRequest.builder().jobId(1L).displayName("New Name").build();
        var cmd = new org.apache.fineract.infrastructure.jobs.command.JobUpdateCommand();
        cmd.setPayload(req);

        JobUpdateResponse expected = JobUpdateResponse.builder().resourceId(1L).changes(Map.of("displayName", "New Name")).build();
        when(schedulerWritePlatformService.updateJobDetail(req)).thenReturn(expected);

        assertSame(expected, handler.handle(cmd));
        verifyNoInteractions(jobRegisterService);
    }
}
