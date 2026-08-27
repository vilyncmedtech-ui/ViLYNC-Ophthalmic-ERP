# Implementation Plan - Serial Stock Register Refinements

Serial Stock Register screen ko user requirements ke according refine karne ka plan.

## Proposed Changes

### 1. Data Layer
- **SerialStockRow**: Add `serialPrefix` to capture LMDE/LMMS info from Product Master.
- **SerialStockDao**: Update SQL queries to join `products` table and select `serialPrefix`.

### 2. UI Refinements (SerialStockRegisterScreen)
- **Header**:
    - Change background to a subtle light color (White/Light Blue).
    - Update text/icon colors for light theme.
    - Add a functional "Dashboard" button.
- **Register Table Header**: Lighten background and text colors.
- **Register Row**:
    - Format Serial Number with prefix (e.g., `LMDE 123456`).
    - Format Product Name with Model (e.g., `Product (Model)`).
    - Remove the redundant company/manufacturer line.

### 3. Navigation
- Update `AppNavigation` to handle Dashboard navigation from the Serial Stock Register.

## Verification Plan

- **Manual Verification**: Check the Serial Stock Register UI for light theme and correct data formatting.
- **Navigation Check**: Ensure "Back" and "Dashboard" navigate to correct destinations.
- **Data Check**: Verify LMDE/LMMS prefix matches the Product Master data.
