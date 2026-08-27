# Invoice Hub UI Refinement Tasks

- `[x]` Data Layer Enhancements
    - `[x]` Update `getFilteredSalesForHub` in `SalesDao` to support Serial Number and Customer ID filtering.
- `[x]` ViewModel Updates
    - `[x]` Integrate `PartyRepository`.
    - `[x]` Add state for Party suggestions and `selectedParty`.
    - `[x]` Implement Party selection and clearing logic.
    - `[x]` Update query execution to handle new search parameters.
- `[x]` UI Implementation
    - `[x]` Update search field placeholder.
    - `[x]` Implement Party suggestion dropdown in search field.
    - `[x]` Add clearable Party filter chip.
    - `[x]` Redesign `InvoiceHubRow` into a compact single-line layout.
- `[x]` Verification
    - `[x]` Build check (`app:assembleDebug`).
    - `[x]` Verify Hybrid Search (Party selection + scoped Invoice/Serial search).
    - `[x]` Verify Serial Number search functionality.
    - `[x]` Verify compact layout readability.
    - `[x]` Ensure existing filters and PDF sharing are unaffected.
