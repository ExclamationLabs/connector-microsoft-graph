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

package com.exclamationlabs.connid.microsoft.graph.adapter;

import static com.exclamationlabs.connid.microsoft.graph.attribute.MicrosoftGraphGroupAttribute.*;
import static org.junit.jupiter.api.Assertions.*;

import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphGroup;
import com.microsoft.graph.models.Group;
import java.util.Set;
import java.util.stream.Collectors;
import org.identityconnectors.framework.common.objects.Attribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MicrosoftGraphGroupsAdapterTest {

  MicrosoftGraphGroupsAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new MicrosoftGraphGroupsAdapter();
  }

  private static MicrosoftGraphGroup shallowGroup() {
    Group group = new Group();
    group.id = "id-1";
    group.displayName = "Ops";
    return new MicrosoftGraphGroup(group);
  }

  private static MicrosoftGraphGroup detailGroup() {
    MicrosoftGraphGroup group = shallowGroup();
    group.setDetailLoaded(true);
    return group;
  }

  private Set<String> attributeNames(MicrosoftGraphGroup group) {
    return adapter.constructAttributes(group).stream()
        .map(Attribute::getName)
        .collect(Collectors.toSet());
  }

  /**
   * These sit outside the summary $select getAll uses, so a shallow read knows nothing about them.
   * Reporting null would have Midpoint clear whatever a previous deep read had stored.
   */
  @Test
  void shallowRead_omitsDetailOnlyAttributes() {
    Set<String> names = attributeNames(shallowGroup());

    assertFalse(names.contains(THEME.name()));
    assertFalse(names.contains(VISIBILITY.name()));
    assertFalse(names.contains(ON_PREMISES_DOMAIN_NAME.name()));
    assertFalse(names.contains(ON_PREMISES_NET_BIOS_NAME.name()));
    assertFalse(names.contains(ON_PREMISES_SAM_ACCOUNT_NAME.name()));
    assertFalse(names.contains(ON_PREMISES_SECURITY_IDENTIFIER.name()));
    assertFalse(names.contains(ON_PREMISES_SYNC_ENABLED.name()));
  }

  @Test
  void detailRead_reportsDetailOnlyAttributes() {
    Set<String> names = attributeNames(detailGroup());

    assertTrue(names.contains(THEME.name()));
    assertTrue(names.contains(VISIBILITY.name()));
    assertTrue(names.contains(ON_PREMISES_DOMAIN_NAME.name()));
    assertTrue(names.contains(ON_PREMISES_NET_BIOS_NAME.name()));
    assertTrue(names.contains(ON_PREMISES_SAM_ACCOUNT_NAME.name()));
    assertTrue(names.contains(ON_PREMISES_SECURITY_IDENTIFIER.name()));
    assertTrue(names.contains(ON_PREMISES_SYNC_ENABLED.name()));
  }

  /** Summary fields are genuinely requested by getAll, so a null there is a real cleared value. */
  @Test
  void shallowRead_stillReportsSummaryAttributes() {
    Set<String> names = attributeNames(shallowGroup());

    assertTrue(names.contains(EMAIL.name()));
    assertTrue(names.contains(SECURITY_ENABLED.name()));
    assertTrue(names.contains(SECURITY_IDENTIFIER.name()));
    assertTrue(names.contains(PREFERRED_DATA_LOCATION.name()));
    assertTrue(names.contains(CLASSIFICATION.name()));
  }

  /** Derived from groupTypes/mailEnabled/securityEnabled, so available on a shallow read. */
  @Test
  void shallowRead_stillReportsGroupTypeFlags() {
    Set<String> names = attributeNames(shallowGroup());

    assertTrue(names.contains(IS_MS_TEAM.name()));
    assertTrue(names.contains(IS_MS_365.name()));
    assertTrue(names.contains(IS_SECURITY_GROUP.name()));
    assertTrue(names.contains(IS_DISTRIBUTION_GROUP.name()));
    assertTrue(names.contains(IS_DYNAMIC.name()));
  }
}
