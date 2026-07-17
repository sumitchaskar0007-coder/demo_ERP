# Authorization and Tenant Isolation

The existing `college_id` is exposed as `institutionId` and embedded in JWTs as `institution_id`.

```mermaid
flowchart TD
    A[Authenticated request] --> B[Identify user_id]
    B --> C[Load active roles and permissions]
    C --> D{Required role present?}
    D -- No --> X[403 Access denied]
    D -- Yes --> E{Required permission present?}
    E -- No --> X
    E -- Yes --> F{Super Admin?}
    F -- Yes --> G[Explicit cross-institution admin operation]
    F -- No --> H[Resolve current institution_id]
    H --> I[Service applies institution predicate]
    I --> J{Requested resource institution matches?}
    J -- No --> X
    J -- Yes --> K[Perform operation]
```

## Notice example

```mermaid
flowchart LR
    SA[Super Admin] -->|global or selected institutions| N[Notice]
    P[Principal] -->|own institution only| N
    H[HOD] -->|own institution + department students| N
    N --> V{Recipient role + institution + department match}
    V -- Yes --> SHOW[Display notice]
    V -- No --> HIDE[Do not return notice]
```

Repository queries and service checks must always include the tenant predicate; frontend filtering is never a security boundary.
