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

package com.exclamationlabs.connid.microsoft.graph.model;

import com.exclamationlabs.connid.base.connector.model.IdentityModel;
import com.microsoft.graph.models.User;
import java.util.HashSet;
import java.util.Set;

public class MicrosoftGraphUser implements IdentityModel {

  private User graphUser;

  private Set<String> memberOf;

  /**
   * True when this user was read with the full detail $select rather than the summary one used by
   * an unfiltered getAll. Attributes outside the summary set must not be reported from a shallow
   * read - null there means "not requested", and Midpoint would clear the value on the strength of
   * it.
   */
  private boolean detailLoaded = false;

  /**
   * True when the fields Graph refuses to $select alongside a $filter (skills, responsibilities,
   * hireDate) were requested. Only getOne reads them, so a filtered getAll is detail-loaded without
   * them, and the same "not requested" rule applies.
   */
  private boolean filterIncompatibleLoaded = false;

  private Set<String> licenseIdsToAdd;
  private Set<String> licenseIdsToRemove;

  private Set<String> groupIdsToAdd;
  private Set<String> groupIdsToRemove;

  public MicrosoftGraphUser(User userData) {
    setGraphUser(userData);
    setGroupIdsToAdd(new HashSet<>());
    setGroupIdsToRemove(new HashSet<>());
    setLicenseIdsToAdd(new HashSet<>());
    setLicenseIdsToRemove(new HashSet<>());
  }

  @Override
  public String getIdentityIdValue() {
    return getGraphUser().id;
  }

  @Override
  public String getIdentityNameValue() {
    // __NAME__ maps to userPrincipalName (tenant-unique in Entra), not displayName,
    // which is not unique and caused ObjectAlreadyExistsException on name collisions.
    return getGraphUser().userPrincipalName;
  }

  public User getGraphUser() {
    return graphUser;
  }

  public void setGraphUser(User graphUser) {
    this.graphUser = graphUser;
  }

  public Set<String> getMemberOf() {
    return memberOf;
  }

  public void setMemberOf(Set<String> memberOf) {
    this.memberOf = memberOf;
  }

  public boolean isDetailLoaded() {
    return detailLoaded;
  }

  public void setDetailLoaded(boolean detailLoaded) {
    this.detailLoaded = detailLoaded;
  }

  public boolean isFilterIncompatibleLoaded() {
    return filterIncompatibleLoaded;
  }

  public void setFilterIncompatibleLoaded(boolean filterIncompatibleLoaded) {
    this.filterIncompatibleLoaded = filterIncompatibleLoaded;
  }

  public Set<String> getLicenseIdsToAdd() {
    return licenseIdsToAdd;
  }

  public void setLicenseIdsToAdd(Set<String> licenseIdsToAdd) {
    this.licenseIdsToAdd = licenseIdsToAdd;
  }

  public Set<String> getLicenseIdsToRemove() {
    return licenseIdsToRemove;
  }

  public void setLicenseIdsToRemove(Set<String> licenseIdsToRemove) {
    this.licenseIdsToRemove = licenseIdsToRemove;
  }

  public Set<String> getGroupIdsToAdd() {
    return groupIdsToAdd;
  }

  public void setGroupIdsToAdd(Set<String> groupIdsToAdd) {
    this.groupIdsToAdd = groupIdsToAdd;
  }

  public Set<String> getGroupIdsToRemove() {
    return groupIdsToRemove;
  }

  public void setGroupIdsToRemove(Set<String> groupIdsToRemove) {
    this.groupIdsToRemove = groupIdsToRemove;
  }
}
