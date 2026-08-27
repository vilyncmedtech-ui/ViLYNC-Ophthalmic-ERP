# Implementation Plan - Invoice Hub UI Refinement

Refine the Invoice Hub UI for better search capabilities (including Serial Number) and a more compact, data-dense horizontal list layout.

## User Review Required

> [!IMPORTANT]
> The search field will now support a "Hybrid" mode: typing will show Party suggestions. Selecting a party locks it as an active filter (shown as a clearable chip), while continuing to allow text search for Invoice and Serial numbers.
> Each invoice row will be compressed into a single horizontal line to maximize the number of invoices visible on the screen.

## Proposed Changes

### Data Layer

#### [MODIFY] [SalesDao.kt](file:///C:/Users/dodo/Documents/androidprojects/ViLYNC_ERP/app/src/main/java/com/vilync/ophthalmicerp/data/dao/SalesDao.kt)
- Update `getFilteredSalesForHub` to:
    - Support searching across `sales.customerName`, `sales.invoiceNumber`, and `sale_lenses.serialNumber`.
    - Accept an optional `customerId` to filter by a specific selected party.
    - Use `DISTINCT` to avoid duplicate invoice rows when multiple serial numbers match the query.

#### [MODIFY] [SalesRepository.kt](file:///C:/Users/dodo/Documents/androidprojects/ViLYNC_ERP/app/src/main/java/com/vilync/ophthalmicerp/data/repository/SalesRepository.kt)
- Update method signature to match DAO changes.

### Presentation / UI Logic

#### [MODIFY] [InvoiceHubViewModel.kt](file:///C:/Users/dodo/Documents/androidprojects/ViLYNC_ERP/app/src/main/java/com/vilync/ophthalmicerp/feature/sales/invoicehub/presentation/InvoiceHubViewModel.kt)
- Add `PartyRepository` dependency.
- Add state for `allParties` (cached for suggestions) and `selectedParty: PartyMaster?`.
- Update `loadInvoices` to pass both `selectedParty.id` and the raw `searchQuery` to the repository.
- Implement `selectParty(party: PartyMaster?)` to handle filter application and clearing.

### Presentation / UI

#### [MODIFY] [InvoiceHubScreen.kt](file:///C:/Users/dodo/Documents/androidprojects/ViLYNC_ERP/app/src/main/java/com/vilync/ophthalmicerp/feature/sales/invoicehub/presentation/InvoiceHubScreen.kt)
- **Search Header**:
    - Update placeholder to "Search Party Name / Invoice No. / Serial No.".
    - Implement a custom search field with a `DropdownMenu` showing Party suggestions.
    - Add an `AssistChip` or `InputChip` below the search field when a Party is selected to show the active filter and allow clearing it.
- **Invoice List**:
    - Redesign `InvoiceHubRow` to a single horizontal row: `Checkbox | Invoice No. | Party Name | Status | Invoice Date | Amount`.
    - Use weighted columns to ensure Party Name and Invoice Number get adequate space.
    - Remove vertical card padding/spacing to achieve the requested high-density look.

### Navigation

#### [MODIFY] [ReportNavGraph.kt](file:///C:/Users/dodo/Documents/androidprojects/ViLYNC_ERP/app/src/main/java/com/vilync/ophthalmicerp/feature/sales/reports/navigation/ReportNavGraph.kt)
- Inject `PartyRepository` into `InvoiceHubViewModelFactory`.

## Verification Plan

### Automated Tests
- Run `app:assembleDebug` to ensure build stability.

### Manual Verification
1.  **Search Functionality**:
    *   Type a partial Party Name; verify suggestions appear.
    *   Select a Party; verify the list filters and a "filter chip" appears.
    *   Clear the chip; verify the list resets.
    *   Type an Invoice Number; verify matching results.
    *   Type a known Serial Number; verify matching results.
2.  **UI Density**:
    *   Confirm invoices are displayed in a single horizontal line.
    *   Confirm all 6 fields (`Checkbox`, `Invoice No.`, `Party`, `Status`, `Date`, `Amount`) are visible and correctly aligned.
3.  **Regressions**:
    *   Verify Date Range and Status filters still work as before.
    *   Verify Bulk Share PDF still functions correctly with the new selection layout.
