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

import static com.exclamationlabs.connid.microsoft.graph.attribute.MicrosoftGraphUserAttribute.*;
import static org.junit.jupiter.api.Assertions.*;

import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphUser;
import com.microsoft.graph.models.User;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.identityconnectors.framework.common.objects.Attribute;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MicrosoftGraphUsersAdapterTest {

  MicrosoftGraphUsersAdapter adapter;

  @BeforeEach
  void setUp() {
    adapter = new MicrosoftGraphUsersAdapter();
  }

  private static MicrosoftGraphUser shallowUser() {
    User user = new User();
    user.id = "id-1";
    user.displayName = "Ann";
    return new MicrosoftGraphUser(user);
  }

  private static MicrosoftGraphUser detailUser() {
    MicrosoftGraphUser user = shallowUser();
    user.setDetailLoaded(true);
    return user;
  }

  private Set<String> attributeNames(MicrosoftGraphUser user) {
    return adapter.constructAttributes(user).stream()
        .map(Attribute::getName)
        .collect(Collectors.toSet());
  }

  // --- ASSIGNED_GROUPS: an unfetched membership must never look like an emptied one ---

  /**
   * memberOf is only populated by getOne. Reporting it from a shallow read told Midpoint every
   * group assignment had been removed, and it deprovisioned them - the user-side half of FIN-14541.
   */
  @Test
  void shallowRead_omitsAssignedGroupsEntirely() {
    assertFalse(attributeNames(shallowUser()).contains(ASSIGNED_GROUPS.name()));
  }

  @Test
  void detailRead_reportsAssignedGroups() {
    MicrosoftGraphUser user = detailUser();
    user.setMemberOf(new HashSet<>(Arrays.asList("group-1", "group-2")));

    assertTrue(attributeNames(user).contains(ASSIGNED_GROUPS.name()));
  }

  /** Guarding on null rather than on emptiness, so genuine removals still propagate. */
  @Test
  void detailRead_reportsAnEmptyAssignedGroups_soRemovalsStillPropagate() {
    MicrosoftGraphUser user = detailUser();
    user.setMemberOf(Collections.emptySet());

    assertTrue(attributeNames(user).contains(ASSIGNED_GROUPS.name()));
  }

  // --- detail-only attributes ---

  @Test
  void shallowRead_omitsDetailOnlyAttributes() {
    Set<String> names = attributeNames(shallowUser());

    assertFalse(names.contains(CITY.name()));
    assertFalse(names.contains(STATE.name()));
    assertFalse(names.contains(POSTAL_CODE.name()));
    assertFalse(names.contains(STREET_ADDRESS.name()));
    assertFalse(names.contains(USAGE_LOCATION.name()));
    assertFalse(names.contains(PROXY_ADDRESSES.name()));
    assertFalse(names.contains(SECURITY_IDENTIFIER.name()));
    assertFalse(names.contains(ON_PREMISES_SYNC_ENABLED.name()));
  }

  @Test
  void detailRead_reportsDetailOnlyAttributes() {
    Set<String> names = attributeNames(detailUser());

    assertTrue(names.contains(CITY.name()));
    assertTrue(names.contains(STATE.name()));
    assertTrue(names.contains(POSTAL_CODE.name()));
    assertTrue(names.contains(STREET_ADDRESS.name()));
    assertTrue(names.contains(USAGE_LOCATION.name()));
    assertTrue(names.contains(PROXY_ADDRESSES.name()));
    assertTrue(names.contains(SECURITY_IDENTIFIER.name()));
    assertTrue(names.contains(ON_PREMISES_SYNC_ENABLED.name()));
  }

  /** Summary fields are genuinely requested by getAll, so a null there is a real cleared value. */
  @Test
  void shallowRead_stillReportsSummaryAttributes() {
    Set<String> names = attributeNames(shallowUser());

    assertTrue(names.contains(EMAIL.name()));
    assertTrue(names.contains(GIVEN_NAME.name()));
    assertTrue(names.contains(SURNAME.name()));
    assertTrue(names.contains(USER_TYPE.name()));
    assertTrue(names.contains(OFFICE_LOCATION.name()));
    assertTrue(names.contains(ON_PREMISES_SAM_ACCOUNT_NAME.name()));
    assertTrue(names.contains(COMPANY_NAME.name()));
  }

  // --- filter-incompatible attributes: only getOne requests them ---

  /**
   * A filtered getAll is detail-loaded, but Graph rejects skills/responsibilities/hireDate alongside
   * a $filter, so they are left out of its $select and must not be reported as cleared.
   */
  @Test
  void filteredDetailRead_omitsFilterIncompatibleAttributes() {
    Set<String> names = attributeNames(detailUser());

    assertTrue(names.contains(CITY.name()));
    assertFalse(names.contains(SKILLS.name()));
    assertFalse(names.contains(RESPONSIBILITIES.name()));
  }

  @Test
  void fullRead_reportsFilterIncompatibleAttributes() {
    MicrosoftGraphUser user = detailUser();
    user.setFilterIncompatibleLoaded(true);
    Set<String> names = attributeNames(user);

    assertTrue(names.contains(SKILLS.name()));
    assertTrue(names.contains(RESPONSIBILITIES.name()));
  }
}
