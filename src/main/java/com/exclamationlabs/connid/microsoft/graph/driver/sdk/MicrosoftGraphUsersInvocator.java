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
import com.exclamationlabs.connid.microsoft.graph.model.MicrosoftGraphUser;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import java.util.*;
import java.util.List;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Name;

public class MicrosoftGraphUsersInvocator
    implements DriverInvocator<MicrosoftGraphDriver, MicrosoftGraphUser> {

  private static final Set<String> summaryFields;

  private static final Set<String> detailFields;

  private static final Map<String, String> filterAttributeToFieldMap;

  static {
    filterAttributeToFieldMap = new HashMap<>();
    filterAttributeToFieldMap.put(Name.NAME, "displayName");
    filterAttributeToFieldMap.put(MicrosoftGraphUserAttribute.DISPLAY_NAME.name(), "displayName");
    filterAttributeToFieldMap.put(MicrosoftGraphUserAttribute.EMAIL.name(), "mail");
    filterAttributeToFieldMap.put(MicrosoftGraphUserAttribute.USER_PRINCIPAL_NAME.name(), "mail");
    filterAttributeToFieldMap.put(MicrosoftGraphUserAttribute.USER_TYPE.name(), "userType");
    filterAttributeToFieldMap.put(MicrosoftGraphUserAttribute.GIVEN_NAME.name(), "givenName");
    filterAttributeToFieldMap.put(MicrosoftGraphUserAttribute.SURNAME.name(), "surname");

    summaryFields =
        new HashSet<>(
            Arrays.asList(
                "id",
                "displayName",
                "accountEnabled",
                "businessPhones",
                "companyName",
                "createdDateTime",
                "creationType",
                "department",
                "displayName",
                "employeeHireDate",
                "employeeId",
                "employeeType",
                "givenName",
                "jobTitle",
                "mail",
                "mailNickname",
                "officeLocation",
                "preferredLanguage",
                "surname",
                "userPrincipalName",
                "userType"));

    detailFields = new HashSet<>();
    detailFields.addAll(summaryFields);
    detailFields.addAll(
        Arrays.asList(
            "forceChangePasswordNextSignIn",
            "forceChangePasswordNextSignInWithMfa",
            "ageGroup",
            "city",
            "businessPhones",
            "country",
            "employeeOrgData.costCenter",
            "employeeOrgData.division",
            "externalUserState",
            "externalUserStateChangeDateTime",
            "imAddresses",
            "lastPasswordChangeDateTime",
            "licenseAssignmentStates",
            "onPremisesDistinguishedName",
            "onPremisesDomainName",
            "onPremisesImmutableId",
            "onPremisesLastSyncDateTime",
            "onPremisesSamAccountName",
            "onPremisesSecurityIdentifier",
            "onPremisesSyncEnabled",
            "onPremisesUserPrincipalName",
            "otherMails",
            "passwordPolicies",
            "postalCode",
            "preferredDataLocation",
            "proxyAddresses",
            "securityIdentifier",
            "state",
            "streetAddress",
            "usageLocation",
            "hireDate",
            "memberOf",
            "responsibilities",
            "skills"));
  }

  @Override
  public String create(MicrosoftGraphDriver driver, MicrosoftGraphUser newUser)
      throws ConnectorException {
    if (driver.getConfiguration().getEnableDebugHttpLogging()) {
      try {
        driver.logTransactionPayload(this, "post", newUser.getGraphUser());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    try {
      final User createdUser =
          driver.getGraphClient().users().buildRequest().post(newUser.getGraphUser());

      newUser
          .getGroupIdsToAdd()
          .forEach(groupId -> addGroupToUser(groupId, createdUser.id, driver));
      updateUserLicenseAssignments(
          newUser.getLicenseIdsToAdd(), newUser.getLicenseIdsToRemove(), createdUser.id, driver);

      return createdUser.id;
    } catch (GraphServiceException gse) {
      driver.handleGraphServiceException(gse);
      if (gse.toString() != null && driver.getConfiguration().getEnableDebugHttpLogging()) {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during user create:" + gse.toString(), gse);
      } else {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during user create:", gse);
      }
    }
  }

  @Override
  public void update(MicrosoftGraphDriver driver, String id, MicrosoftGraphUser modifiedUser)
      throws ConnectorException {
    if (driver.getConfiguration().getEnableDebugHttpLogging()) {
      try {
        driver.logTransactionPayload(this, "patch", modifiedUser.getGraphUser());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    try {
      Objects.requireNonNull(
              driver.getGraphClient().users().byId(id),
              String.format("MSGraph user byId %s produced null result", id))
          .buildRequest()
          .patch(modifiedUser.getGraphUser());
      modifiedUser.getGroupIdsToAdd().forEach(groupId -> addGroupToUser(groupId, id, driver));
      modifiedUser
          .getGroupIdsToRemove()
          .forEach(groupId -> removeGroupFromUser(groupId, id, driver));
      updateUserLicenseAssignments(
          modifiedUser.getLicenseIdsToAdd(), modifiedUser.getLicenseIdsToRemove(), id, driver);

    } catch (GraphServiceException gse) {
      driver.handleGraphServiceException(gse);
      if (gse.toString() != null && driver.getConfiguration().getEnableDebugHttpLogging()) {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during user update:" + gse.toString(), gse);
      } else {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during user update:", gse);
      }
    }

  }

  @Override
  public void delete(MicrosoftGraphDriver driver, String id) throws ConnectorException {
    if (driver.getConfiguration().getEnableDebugHttpLogging()) {
      try {
        driver.logTransactionPayload(
            this, "delete", driver.getGraphClient().users().byId(id));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    try {
      Objects.requireNonNull(
              driver.getGraphClient().users().byId(id),
              String.format("MSGraph user byId %s produced null result", id))
          .buildRequest()
          .delete();
    } catch (GraphServiceException gse) {
      driver.handleGraphServiceException(gse);
      if (gse.toString() != null && driver.getConfiguration().getEnableDebugHttpLogging()) {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during user delete:" + gse.toString(), gse);
      } else {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during user delete:", gse);
      }
    }
  }

  @Override
  public Set<MicrosoftGraphUser> getAll(
      MicrosoftGraphDriver driver,
      ResultsFilter resultsFilter,
      ResultsPaginator resultsPaginator,
      Integer integer)
      throws ConnectorException {
    List<User> usersList = new ArrayList<>();
    Set<MicrosoftGraphUser> response = new HashSet<>();
    UserCollectionPage usersPage;
    try {

      String modelFieldName =
          resultsFilter.hasFilter()
              ? filterAttributeToFieldMap.get(resultsFilter.getAttribute())
              : null;
      if (modelFieldName != null) {
        usersPage =
            driver
                .getGraphClient()
                .users()
                .buildRequest()
                .select(String.join(",", summaryFields))
                .filter(modelFieldName + " eq '" + resultsFilter.getValue() + "'")
                .get();
        usersList = usersPage.getCurrentPage();
      } else {
        usersPage =
            driver
                .getGraphClient()
                .users()
                .buildRequest()
                .select(String.join(",", summaryFields))
                .top(MicrosoftGraphDriver.SDK_FETCH_COUNT)
                .get();
        while (usersPage != null) {
          usersList.addAll(usersPage.getCurrentPage());

          final UserCollectionRequestBuilder nextPage = usersPage.getNextPage();
          if (nextPage == null) {
            break;
          } else {
            usersPage = nextPage.buildRequest().get();
          }
        }
      }

      if (usersPage == null) {
        throw new ConnectorException("Failure retrieving page of users.");
      }

      usersList.forEach(it -> response.add(new MicrosoftGraphUser(it)));
    } catch (GraphServiceException gse) {
      if (gse.toString() != null && driver.getConfiguration().getEnableDebugHttpLogging()) {
        Logger.error(this, String.format("Exception in user.getAll %s", gse.toString()), gse);
      }
      driver.handleGraphServiceException(gse);
    }
    return response;
  }

  @Override
  public MicrosoftGraphUser getOne(MicrosoftGraphDriver driver, String id, Map<String, Object> map)
      throws ConnectorException {
    try {
      User matchingUser =
          Objects.requireNonNull(
                  driver.getGraphClient().users().byId(id),
                  String.format("MSGraph user byId %s produced null result", id))
              .buildRequest()
              .select(String.join(",", detailFields))
              .expand("memberOf")
              .get();
      return new MicrosoftGraphUser(matchingUser);
    } catch (GraphServiceException gse) {
      if (gse.toString() != null && driver.getConfiguration().getEnableDebugHttpLogging()) {
        Logger.error(this, String.format("Exception in user.getOne %s", gse.toString()), gse);
      }
      driver.handleGraphServiceException(gse);
    }
    return null;
  }

  @Override
  public MicrosoftGraphUser getOneByName(MicrosoftGraphDriver driver, String nameValue) {
    Set<MicrosoftGraphUser> users =
        getAll(driver, new ResultsFilter(Name.NAME, nameValue), new ResultsPaginator(), null);
    Optional<MicrosoftGraphUser> userGet = users.stream().findFirst();
    return userGet.orElse(null);
  }

  private void updateUserLicenseAssignments(
      Set<String> licenseIdsToAdd,
      Set<String> licenseIdsToRemove,
      String userId,
      MicrosoftGraphDriver driver) {
    if (licenseIdsToAdd.isEmpty() && licenseIdsToRemove.isEmpty()) {
      return;
    }
    try {
      List<AssignedLicense> addList = new ArrayList<>();
      List<UUID> removeList = new ArrayList<>();
      for (String current : licenseIdsToAdd) {
        AssignedLicense licenseData = new AssignedLicense();
        licenseData.skuId = UUID.fromString(removeTenantID(current));
        addList.add(licenseData);
      }
      for (String current : licenseIdsToRemove) {
        removeList.add(UUID.fromString(removeTenantID(current)));
      }

      driver
          .getGraphClient()
          .users(userId)
          .assignLicense(
              UserAssignLicenseParameterSet.newBuilder()
                  .withAddLicenses(addList)
                  .withRemoveLicenses(removeList)
                  .build())
          .buildRequest()
          .post();

    } catch (GraphServiceException gse) {
      Logger.error(
          this,
          String.format(
              "Graph exception occurred while updateUserLicenseAssignments for user %s", userId),
          gse);
      if (gse.toString() != null && driver.getConfiguration().getEnableDebugHttpLogging()) {
        Logger.error(
            this,
            String.format(
                "Graph exception occurred while updateUserLicenseAssignments for user %s Details: %s",
                userId, gse.toString()),
            gse);
      }
      driver.handleGraphServiceException(gse);
      if (gse.toString() != null && driver.getConfiguration().getEnableDebugHttpLogging()) {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during updateUserLicenseAssignments:"
                + gse.toString(),
            gse);
      } else {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during updateUserLicenseAssignments:", gse);
      }
    }
  }

  private String removeTenantID(String license) {
    return license.replaceAll(".*_", "");
  }

  private void addGroupToUser(String groupId, String userId, MicrosoftGraphDriver driver)
      throws GraphServiceException {
    if (driver.getConfiguration().getEnableDebugHttpLogging()) {
      try {
        driver.logTransactionPayload(
            this,
            "post",
            driver.getGraphClient().groups(groupId).members().references());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    try {
      DirectoryObject directoryObject = new DirectoryObject();
      directoryObject.id = userId;
      Objects.requireNonNull(
              driver.getGraphClient().groups(groupId).members().references(),
              String.format(
                  "MSGraph user to groupId member check produced null result for user %s", userId))
          .buildRequest()
          .post(directoryObject);

    } catch (GraphServiceException gse) {
      Logger.error(
          this,
          String.format(
              "Graph exception occurred while adding group %s to user %s", groupId, userId),
          gse);
      driver.handleGraphServiceException(gse);
      if (gse.toString() != null && driver.getConfiguration().getEnableDebugHttpLogging()) {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during addGroupToUser:" + gse.toString(),
            gse);
      } else {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during addGroupToUser:", gse);
      }
    }
  }

  private void removeGroupFromUser(String groupId, String userId, MicrosoftGraphDriver driver)
      throws GraphServiceException {
    if (driver.getConfiguration().getEnableDebugHttpLogging()) {
      try {
        driver.logTransactionPayload(
            this,
            "delete",
            driver.getGraphClient().groups(groupId).members().references());
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    try {
      Objects.requireNonNull(
              driver.getGraphClient().groups(groupId).members(userId).reference(),
              String.format(
                  "MSGraph user to groupId member check produced null result for user %s", userId))
          .buildRequest()
          .delete();
    } catch (GraphServiceException gse) {
      Logger.error(
          this,
          String.format(
              "Graph exception occurred while removing group %s from user %s", groupId, userId),
          gse);
      driver.handleGraphServiceException(gse);
      if (gse.toString() != null && driver.getConfiguration().getEnableDebugHttpLogging()) {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during removeGroupFromUser:"
                + gse.toString(),
            gse);
      } else {
        throw new ConnectorException(
            "Unexpected GraphServiceException occurred during removeGroupFromUser:", gse);
      }
    }
  }
}
