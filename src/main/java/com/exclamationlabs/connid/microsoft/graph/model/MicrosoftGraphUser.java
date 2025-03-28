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
    return getGraphUser().displayName;
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
