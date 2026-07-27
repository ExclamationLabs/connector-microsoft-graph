# Migration: 2.0.x → 3.0.0 — `user.__NAME__` now maps to `userPrincipalName`

## What changed and why

The `user` objectClass previously bound ConnId `__NAME__` (which drives midPoint's
`namingAttribute`, `secondaryIdentifier`, and `displayNameAttribute`) to **`displayName`**.
`displayName` is **not unique** in Microsoft Entra — two real people can share a name
(e.g. two "Taylor King" accounts). When a collision existed on the resource, any account
operation failed with:

```
ObjectAlreadyExistsException: Too many iterations
```

because midPoint's uniqueness iterator had no way to make a non-unique naming attribute unique.

As of 3.0.0, `__NAME__` maps to **`userPrincipalName` (UPN)**, which Entra guarantees is
tenant-unique (it is the login identity). This removes the root cause of the exception.

| midPoint annotation (generated schema) | before      | after                 |
|----------------------------------------|-------------|-----------------------|
| `ra:identifier`                        | USER_ID     | USER_ID (unchanged)   |
| `ra:secondaryIdentifier`               | DISPLAY_NAME| **USER_PRINCIPAL_NAME**|
| `ra:namingAttribute`                   | DISPLAY_NAME| **USER_PRINCIPAL_NAME**|
| `ra:displayNameAttribute`              | DISPLAY_NAME| **USER_PRINCIPAL_NAME**|

`DISPLAY_NAME` is now an ordinary, non-identifying attribute (`ri:DISPLAY_NAME`).

> Note: all three annotations move together because ConnId derives them from the single
> `__NAME__` attribute — there is no connector-level way to keep `displayNameAttribute` pointed
> at `DISPLAY_NAME` while moving the others. If midPoint must still *display* the friendly name
> in its object lists, override `displayNameAttribute` in the resource XML `<schemaHandling>`
> (client-side); the connector cannot do this on its own.

The **`group`** objectClass is intentionally **unchanged** — no group attribute in Entra is
guaranteed unique (`displayName` is not enforced unique; `mailNickname` is unique only among
mail-enabled groups), so it still uses `displayName` as `__NAME__`.

## Connector code changes (for reference)

- `MicrosoftGraphUsersAdapter.getConnectorAttributes()` — `__NAME__` → `USER_PRINCIPAL_NAME`;
  `DISPLAY_NAME` declared as a plain attribute.
- `MicrosoftGraphUsersAdapter.constructModel()` — `userPrincipalName` read from the `__NAME__`
  value; `displayName` read as a plain attribute.
- `MicrosoftGraphUsersAdapter.constructAttributes()` — emits `DISPLAY_NAME` explicitly.
- `MicrosoftGraphUser.getIdentityNameValue()` — returns `userPrincipalName` (this is what the
  base framework emits as `__NAME__` on **read**; without this, reads would contradict the schema).
- `MicrosoftGraphUsersInvocator` filter map — `__NAME__` and `USER_PRINCIPAL_NAME` now map to the
  Graph field `userPrincipalName` (the latter previously — and incorrectly — mapped to `mail`).

## Upgrade steps (perform in a non-prod tenant first)

1. **Deploy** the 3.0.0 connector jar.
2. **Refresh the resource schema** in midPoint (or delete the `<schema>` block from the resource
   XML and let it regenerate). The connector-served schema is authoritative — the new annotations
   appear automatically. No manual XML editing of the schema block is required.
3. **Review resource XML** for anything that assumed `DISPLAY_NAME` was the naming attribute:
   - Outbound mappings targeting `ri:DISPLAY_NAME` **for naming purposes** — `DISPLAY_NAME` is now
     a plain attribute. Ensure there is an outbound mapping to `ri:USER_PRINCIPAL_NAME` for the
     naming attribute (typically UPN was already mapped).
   - Correlation expressions using `$shadow/attributes/ri:DISPLAY_NAME` are structurally
     unaffected but worth reviewing.
   - Optionally add a `displayNameAttribute` override in `<schemaHandling>` if you want midPoint to
     keep showing the friendly name.
4. **Reconcile** users. Existing shadows carry `icfs:name` = old `displayName`. After the change,
   `icfs:name` becomes the UPN, so **every user shadow name will drift on the next read** and
   reconciliation will re-key them. This is expected. `__UID__` (USER_ID) is unchanged, so objects
   are still matched by their stable identifier — no orphaning, just a name change on the shadow.
5. **Verify** create/update no longer throws `ObjectAlreadyExistsException` for display-name
   collisions.

## Rollback

Revert to the 2.0.x jar and refresh the schema again. Shadows will re-key back to `displayName`
on the next read.
