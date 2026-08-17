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

import com.exclamationlabs.connid.base.connector.driver.DriverInvocator;
import com.exclamationlabs.connid.base.connector.logging.Logger;
import com.exclamationlabs.connid.base.connector.results.ResultsFilter;
import com.exclamationlabs.connid.base.connector.results.ResultsPaginator;
import com.exclamationlabs.connid.microsoft.graph.attribute.MicrosoftGraphUserAttribute;
import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphGroup;
import com.google.gson.JsonElement;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.microsoft.graph.requests.GroupCollectionRequest;
import com.microsoft.graph.requests.GroupCollectionRequestBuilder;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Name;

public class MicrosoftGraphGroupsInvocator
    implements DriverInvocator<MicrosoftGraphDriver, MicrosoftGraphGroup> {

  private static final Set<String> summaryFields;

  private static final Set<String> detailFields;

  private static final Map<String, String> filterAttributeToFieldMap;

  /**
   * Midpoint holds a group's Name as the composite produced by {@link
   * MicrosoftGraphGroup#getIdentityNameValue()} - "displayName (id)" - because Entra group display
   * names are not unique. Used to recover the id when looking a group up by that Name.
   */
  private static final Pattern IDENTITY_NAME_PATTERN = Pattern.compile("^(.*) \\(([^()]+)\\)$");

  private static final String TEAM_PROVISIONING_OPTION = "Team";

  private static final String RESOURCE_PROVISIONING_OPTIONS = "resourceProvisioningOptions";

  static {
    filterAttributeToFieldMap = new HashMap<>();
    filterAttributeToFieldMap.put(Name.NAME, "displayName");
    filterAttributeToFieldMap.put(MicrosoftGraphUserAttribute.DISPLAY_NAME.name(), "displayName");
    filterAttributeToFieldMap.put(MicrosoftGraphUserAttribute.EMAIL.name(), "mail");

    summaryFields =
        new HashSet<>(
            Arrays.asList(
                "id",
                "classification",
                "createdDateTime",
                "description",
                "displayName",
                "expirationDateTime",
                "groupTypes",
                "isAssignableToRole",
                "licenseProcessingState",
                "mail",
                "mailEnabled",
                "mailNickname",
                "membershipRule",
                "membershipRuleProcessingState",
                "officeLocation",
                "preferredLanguage",
                "preferredDataLocation",
                "proxyAddresses",
                "renewedDateTime",
                RESOURCE_PROVISIONING_OPTIONS,
                "securityEnabled",
                "securityIdentifier"));

    detailFields = new HashSet<>();
    detailFields.addAll(summaryFields);
    detailFields.addAll(
        Arrays.asList(
            "assignedLicenses",
            "onPremisesDomainName",
            "onPremisesLastSyncDateTime",
            "onPremisesNetBiosName",
            "onPremisesSamAccountName",
            "onPremisesSecurityIdentifier",
            "onPremisesSyncEnabled",
            "theme",
            "visibility",
            "createdOnBehalfOf"));
  }

  @Override
  public String create(MicrosoftGraphDriver driver, MicrosoftGraphGroup newGroup)
      throws ConnectorException {
    if (driver.getConfiguration().getEnableDebugHttpLogging()) {
      try {
        driver.logTransactionPayload(this, "post", newGroup.getGraphGroup());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    try {
      final Group createdGroup =
          driver.getGraphClient().groups().buildRequest().post(newGroup.getGraphGroup());
      return createdGroup.id;
    } catch (GraphServiceException gse) {
      driver.handleGraphServiceException(gse);
      if (gse.toString() != null && driver.getConfiguration().getEnableDebugHttpLogging()) {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during group create:" + gse.toString(), gse);
      } else {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during group create:", gse);
      }
    }
  }

  @Override
  public void update(MicrosoftGraphDriver driver, String id, MicrosoftGraphGroup modifiedGroup)
      throws ConnectorException {
    if (driver.getConfiguration().getEnableDebugHttpLogging()) {
      try {
        driver.logTransactionPayload(this, "patch", modifiedGroup.getGraphGroup());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    try {
      Objects.requireNonNull(
              driver.getGraphClient().groups().byId(id),
              String.format("MSGraph group byId %s produced null result", id))
          .buildRequest()
          .patch(modifiedGroup.getGraphGroup());
    } catch (GraphServiceException gse) {
      driver.handleGraphServiceException(gse);
      if (gse.toString() != null && driver.getConfiguration().getEnableDebugHttpLogging()) {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during group update:" + gse.toString(), gse);
      } else {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during group update:", gse);
      }
    }
  }

  @Override
  public void delete(MicrosoftGraphDriver driver, String id) throws ConnectorException {
    throw new UnsupportedOperationException("Delete not supported for MS Graph groups");
  }

  @Override
  public Set<MicrosoftGraphGroup> getAll(
      MicrosoftGraphDriver driver,
      ResultsFilter resultsFilter,
      ResultsPaginator resultsPaginator,
      Integer integer)
      throws ConnectorException {
    Set<MicrosoftGraphGroup> response = new HashSet<>();
    List<Group> groupList = new ArrayList<>();
    try {
      String modelFieldName =
          resultsFilter.hasFilter()
              ? filterAttributeToFieldMap.get(resultsFilter.getAttribute())
              : null;
      GroupCollectionRequest request =
          driver
              .getGraphClient()
              .groups()
              .buildRequest()
              .select(String.join(",", summaryFields))
              .top(MicrosoftGraphDriver.SDK_FETCH_COUNT);
      if (modelFieldName != null) {
        request = request.filter(modelFieldName + " eq '" + resultsFilter.getValue() + "'");
      }

      GroupCollectionPage page = request.get();
      if (page == null) {
        throw new ConnectorException("Failure retrieving page of groups.");
      }

      // Graph carries the original $filter/$select/$top forward in the nextLink, so this loop
      // pages correctly for both the filtered and unfiltered request.
      while (true) {
        groupList.addAll(page.getCurrentPage());

        final GroupCollectionRequestBuilder nextPage = page.getNextPage();
        if (nextPage == null) {
          break;
        }
        page = nextPage.buildRequest().get();
        if (page == null) {
          throw new ConnectorException(
              String.format(
                  "Failure retrieving next page of groups after %d record(s)", groupList.size()));
        }
      }

      for (Group current : groupList) {
        MicrosoftGraphGroup currentGroup = new MicrosoftGraphGroup(current);
        currentGroup.setMsTeam(isMsTeam(current));
        response.add(currentGroup);
      }
    } catch (GraphServiceException gse) {
      if (gse.toString() != null && driver.getConfiguration().getEnableDebugHttpLogging()) {
        Logger.error(this, String.format("Exception in groups.getAll %s", gse.toString()), gse);
      }
      driver.handleGraphServiceException(gse);
      // handleGraphServiceException returns without throwing for a 404.  Handing back the records
      // gathered so far is indistinguishable to Midpoint from "the rest were deleted", so fail the
      // read rather than let a partial result drive deprovisioning.
      throw new ConnectorException(
          String.format(
              "Failure retrieving groups from MS Graph after %d record(s)", groupList.size()),
          gse);
    }
    // This getAll follows every nextLink itself, so the caller has the complete set.  Say so, or
    // BaseAdapter.executeBatchImport keeps asking for the next batch and never terminates.
    resultsPaginator.setNoMoreResults(true);
    return response;
  }

  @Override
  public MicrosoftGraphGroup getOne(MicrosoftGraphDriver driver, String id, Map<String, Object> map)
      throws ConnectorException {
    try {
      Group matchingGroup =
          Objects.requireNonNull(
                  driver.getGraphClient().groups().byId(id),
                  String.format("MSGraph group byId %s produced null result", id))
              .buildRequest()
              .select(String.join(",", detailFields))
              .get();

      MicrosoftGraphGroup returnGroup = new MicrosoftGraphGroup(matchingGroup);
      returnGroup.setMsTeam(isMsTeam(matchingGroup));
      returnGroup.setDetailLoaded(true);
      return returnGroup;
    } catch (GraphServiceException gse) {
      if (gse.toString() != null && driver.getConfiguration().getEnableDebugHttpLogging()) {
        Logger.error(this, String.format("Exception in groups.getOne %s", gse.toString()), gse);
      }
      driver.handleGraphServiceException(gse);
    }
    return null;
  }

  @Override
  public MicrosoftGraphGroup getOneByName(MicrosoftGraphDriver driver, String nameValue) {
    if (nameValue == null) {
      return null;
    }
    // Midpoint holds the composite "displayName (id)" as the group Name.  Recover the id and read
    // the group directly - filtering Graph on that composite string never matches a displayName.
    String groupId = extractGroupId(nameValue);
    if (groupId != null) {
      return getOne(driver, groupId, Collections.emptyMap());
    }
    Set<MicrosoftGraphGroup> groups =
        getAll(driver, new ResultsFilter(Name.NAME, nameValue), new ResultsPaginator(), null);
    Optional<MicrosoftGraphGroup> groupGet = groups.stream().findFirst();
    return groupGet.orElse(null);
  }

  /**
   * Pull the group id out of the composite Name "displayName (id)". Returns null when the value is
   * not a composite Name, so a caller passing a raw displayName - including one that legitimately
   * ends in parentheses, such as "Sales (EMEA)" - falls through to a displayName lookup.
   */
  protected static String extractGroupId(String nameValue) {
    Matcher matcher = IDENTITY_NAME_PATTERN.matcher(nameValue);
    if (!matcher.matches()) {
      return null;
    }
    String candidate = matcher.group(2);
    try {
      UUID.fromString(candidate);
      return candidate;
    } catch (IllegalArgumentException iae) {
      return null;
    }
  }

  /**
   * Identify a Teams-provisioned group from the group payload itself. Reading
   * resourceProvisioningOptions off the group avoids a per-group GET /teams/{id} probe, which cost
   * one request per group and aborted the whole import whenever a group type produced anything
   * other than a 404.
   */
  protected static Boolean isMsTeam(Group group) {
    if (group == null) {
      return false;
    }
    JsonElement provisioningOptions =
        group.additionalDataManager().get(RESOURCE_PROVISIONING_OPTIONS);
    if (provisioningOptions == null || !provisioningOptions.isJsonArray()) {
      return false;
    }
    for (JsonElement current : provisioningOptions.getAsJsonArray()) {
      if (current != null
          && current.isJsonPrimitive()
          && TEAM_PROVISIONING_OPTION.equalsIgnoreCase(current.getAsString())) {
        return true;
      }
    }
    return false;
  }
}
