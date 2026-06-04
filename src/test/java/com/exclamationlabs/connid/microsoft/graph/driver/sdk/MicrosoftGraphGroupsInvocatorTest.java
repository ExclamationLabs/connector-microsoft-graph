/*
    Copyright 2020 Exclamation Labs
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
        http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/

package com.exclamationlabs.connid.microsoft.graph.driver.sdk;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Team;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MicrosoftGraphGroupsInvocatorTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  MicrosoftGraphDriver driver;

  MicrosoftGraphGroupsInvocator invocator;

  @BeforeEach
  void setUp() {
    invocator =
        new MicrosoftGraphGroupsInvocator() {
          @Override
          protected long[] getRetryDelaysMs() {
            return new long[] {0L, 0L, 0L};
          }
        };
  }

  @Test
  void checkMsTeamType_returnsTrue_whenTeamFound() {
    when(driver.getGraphClient().teams().byId(anyString()).buildRequest().get())
        .thenReturn(new Team());

    assertTrue(invocator.checkMsTeamType(driver, "group-id"));
  }

  @Test
  void checkMsTeamType_returnsFalse_on404() {
    GraphServiceException gse = mock(GraphServiceException.class);
    when(gse.getResponseCode()).thenReturn(404);
    when(driver.getGraphClient().teams().byId(anyString()).buildRequest().get()).thenThrow(gse);

    assertFalse(invocator.checkMsTeamType(driver, "group-id"));
  }

  @Test
  void checkMsTeamType_retriesOn500_thenSucceeds() {
    GraphServiceException gse = mock(GraphServiceException.class);
    when(gse.getResponseCode()).thenReturn(500);
    when(driver.getGraphClient().teams().byId(anyString()).buildRequest().get())
        .thenThrow(gse)
        .thenReturn(new Team());

    assertTrue(invocator.checkMsTeamType(driver, "group-id"));
  }

  @Test
  void checkMsTeamType_throwsConnectorException_afterMaxRetries() {
    GraphServiceException gse = mock(GraphServiceException.class);
    when(gse.getResponseCode()).thenReturn(500);
    when(driver.getGraphClient().teams().byId(anyString()).buildRequest().get()).thenThrow(gse);

    assertThrows(ConnectorException.class, () -> invocator.checkMsTeamType(driver, "group-id"));
  }
}
