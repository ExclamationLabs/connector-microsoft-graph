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
import com.exclamationlabs.connid.base.connector.results.ResultsFilter;
import com.exclamationlabs.connid.base.connector.results.ResultsPaginator;
import com.exclamationlabs.connid.microsoft.graph.attribute.MicrosoftGraphUserAttribute;
import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphGroup;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Group;
import com.microsoft.graph.models.Team;
import com.microsoft.graph.requests.GroupCollectionPage;
import com.microsoft.graph.requests.GroupCollectionRequestBuilder;
import java.util.*;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Name;

public class MicrosoftGraphGroupsInvocator
    implements DriverInvocator<MicrosoftGraphDriver, MicrosoftGraphGroup> {

  private static final Set<String> summaryFields;

  private static final Set<String> detailFields;

  private static final Map<String, String> filterAttributeToFieldMap;

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
    try {
      final Group createdGroup =
          driver.getGraphClient().groups().buildRequest().post(newGroup.getGraphGroup());
      return createdGroup.id;
    } catch (GraphServiceException gse) {
      driver.handleGraphServiceException(gse);
      throw new ConnectorException(
          "Unexpected GraphServiceException occurred during group create", gse);
    }
  }

  @Override
  public void update(MicrosoftGraphDriver driver, String id, MicrosoftGraphGroup modifiedGroup)
      throws ConnectorException {
    try {
      Objects.requireNonNull(
              driver.getGraphClient().groups().byId(id),
              String.format("MSGraph group byId %s produced null result", id))
          .buildRequest()
          .patch(modifiedGroup.getGraphGroup());
    } catch (GraphServiceException gse) {
      driver.handleGraphServiceException(gse);
      throw new ConnectorException(
          "Unexpected GraphServiceException occurred during group update", gse);
    }
  }

  @Override
  public void delete(MicrosoftGraphDriver driver, String id) throws ConnectorException {
    try {
      Objects.requireNonNull(
              driver.getGraphClient().groups().byId(id),
              String.format("MSGraph group byId %s produced null result", id))
          .buildRequest()
          .delete();
    } catch (GraphServiceException gse) {
      driver.handleGraphServiceException(gse);
      throw new ConnectorException(
          "Unexpected GraphServiceException occurred during group delete", gse);
    }
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
      GroupCollectionPage page;

      String modelFieldName =
          resultsFilter.hasFilter()
              ? filterAttributeToFieldMap.get(resultsFilter.getAttribute())
              : null;
      if (modelFieldName != null) {
        page =
            driver
                .getGraphClient()
                .groups()
                .buildRequest()
                .select(String.join(",", summaryFields))
                .filter(modelFieldName + " eq '" + resultsFilter.getValue() + "'")
                .get();
      } else {
        page =
            driver
                .getGraphClient()
                .groups()
                .buildRequest()
                .select(String.join(",", summaryFields))
                .top(MicrosoftGraphDriver.SDK_FETCH_COUNT)
                .get();
        while (page != null) {
          groupList.addAll(page.getCurrentPage());

          final GroupCollectionRequestBuilder nextPage = page.getNextPage();
          if (nextPage == null) {
            break;
          } else {
            page = nextPage.buildRequest().get();
          }
        }
      }
      driver.getGraphClient().groups().buildRequest().select(String.join(",", summaryFields)).get();
      if (page == null) {
        throw new ConnectorException("Failure retrieving page of users.");
      }

      for (Group current : groupList) {
        MicrosoftGraphGroup currentGroup = new MicrosoftGraphGroup(current);
        currentGroup.setMsTeam(checkMsTeamType(driver, currentGroup.getIdentityIdValue()));
        response.add(currentGroup);
      }
    } catch (GraphServiceException gse) {
      driver.handleGraphServiceException(gse);
    }
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
      returnGroup.setMsTeam(checkMsTeamType(driver, id));
      return returnGroup;
    } catch (GraphServiceException gse) {
      driver.handleGraphServiceException(gse);
    }
    return null;
  }

  @Override
  public MicrosoftGraphGroup getOneByName(MicrosoftGraphDriver driver, String nameValue) {
    Set<MicrosoftGraphGroup> groups =
        getAll(driver, new ResultsFilter(Name.NAME, nameValue), new ResultsPaginator(), null);
    Optional<MicrosoftGraphGroup> groupGet = groups.stream().findFirst();
    return groupGet.orElse(null);
  }

  protected Boolean checkMsTeamType(MicrosoftGraphDriver driver, String groupId) {
    // Check MS Team info for group
    try {
      Team teamInfo =
          Objects.requireNonNull(driver.getGraphClient().teams().byId(groupId))
              .buildRequest()
              .get();
      return (teamInfo != null);
    } catch (GraphServiceException gse) {
      if (gse.getResponseCode() == 404) {
        return false;
      } else {
        throw gse;
      }
    }
  }
}
