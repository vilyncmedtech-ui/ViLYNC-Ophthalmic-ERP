# Implementation Plan - Opening Stock Add Product Crash Fix (Diagnosis-Driven)

`New Opening Stock` screen mein product add karte waqt hone wale crash ko fix kiya jayega.

## Diagnosis (Root Cause)

Bina real-time logs ke, code inspection aur Compose architecture ke basis par **Snapshot Apply Conflict** aur **ViewModel Instance Mismatch** ko primary causes identify kiya gaya hai:

1.  **Rapid State Updates (High Probability)**:
    - **Failing Code**: `AppNavigation.kt` mein `items.forEach { openingStockViewModel.addItem(it) }`.
    - **Issue**: Jab user 5-10 serialized units add karta hai, toh yeh loop synchronous state updates (`_uiState.update`) trigger karta hai. Compose collector (`OpeningStockEntryScreen`) har update par recompose karne ki koshish karta hai. Real device par, rapid-fire updates aur concurrent navigation (`popBackStack`) ke kaaran `SnapshotApplyConflictException` ya thread hang hone ke chances bahut zyada hote hain.
2.  **ViewModel Retrieval Fragility**:
    - **Failing Code**: `viewModel(viewModelStoreOwner = parentEntry)` jahan `parentEntry = navController.previousBackStackEntry`.
    - **Issue**: Agar navigation state transition mein hai, toh `previousBackStackEntry` temporarily wrong ya unavailable ho sakta hai. Bina explicit factory ke, naya ViewModel instance create ho jayega jiska state blank hoga, aur transition ke waqt unexpected crashes ho sakte hain.

## Proposed Minimal Fix

### 1. Atomic State Update
- **ViewModel**: `OpeningStockViewModel.kt` mein `addItems(List<OpeningStockUiItem>)` method ko activate kiya jayega (jo already define ho chuka hai). Isme loop ke bajaye ek hi `_uiState.update` call hogi.
- **Benefit**: Sirf ek recomposition trigger hogi, jisse snapshot conflicts khatam ho jayenge.

### 2. Robust ViewModel Scoping
- **Navigation**: `AppNavigation.kt` mein `add_opening_stock_item` block ko update kiya jayega taaki woh `navController.getBackStackEntry` use kare specifically `opening_stock_entry` ke liye.
- **Benefit**: Shared instance hamesha correctly resolve hoga, aur mismatch ke kaaran hone wale runtime errors ruk jayenge.

## Verification Plan

### Automated Tests
- Build verification: `app:assembleDebug`.

### Manual Verification (Real Device)
1. **Flow**: Inventory -> Opening Stock -> Add Stock -> + Add Product.
2. **Action**: Multiple serials (at least 5) enter karein aur "Add to List" click karein.
3. **Check**:
    - [ ] App crash nahi honi chahiye.
    - [ ] Saare products correctly main list mein add honge.
    - [ ] Total value update honi chahiye.
    - [ ] Saved records register mein check karein.
