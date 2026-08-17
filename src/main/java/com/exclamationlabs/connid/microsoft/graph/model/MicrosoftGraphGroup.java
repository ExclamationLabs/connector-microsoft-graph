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
import com.microsoft.graph.models.Group;

public class MicrosoftGraphGroup implements IdentityModel {

  private Group graphGroup;
  private Boolean msTeam = false;

  /**
   * True when this group was read with the full detail $select rather than the summary one used by
   * getAll. Attributes outside the summary set must not be reported from a shallow read - null
   * there means "not requested", and Midpoint would clear the value on the strength of it.
   */
  private boolean detailLoaded = false;

  public MicrosoftGraphGroup(Group group) {
    setGraphGroup(group);
  }

  @Override
  public String getIdentityIdValue() {
    return getGraphGroup().id;
  }

  @Override
  public String getIdentityNameValue() {
    return String.format("%s (%s)", getGraphGroup().displayName, getGraphGroup().id);
  }

  public Group getGraphGroup() {
    return graphGroup;
  }

  public void setGraphGroup(Group graphGroup) {
    this.graphGroup = graphGroup;
  }

  public Boolean getMsTeam() {
    return msTeam;
  }

  public void setMsTeam(Boolean msTeam) {
    this.msTeam = msTeam;
  }

  public boolean isDetailLoaded() {
    return detailLoaded;
  }

  public void setDetailLoaded(boolean detailLoaded) {
    this.detailLoaded = detailLoaded;
  }
}
