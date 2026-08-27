# Implementation Plan - Fix GST Filing Reports Navigation

Resolve the issue where tapping the "GST Filing Reports" card in the Reports Hub does not open the GST reports screen.

## Root Cause
In [ReportNavGraph.kt](file:///C:/Users/dodo/Documents/androidprojects/ViLYNC_ERP/app/src/main/java/com/vilync/ophthalmicerp/feature/sales/reports/navigation/ReportNavGraph.kt), the navigation graph definition for `reports_home` has an empty lambda for the `onGstReportsClick` parameter.

## Proposed Changes

### [ReportNavGraph.kt](file:///C:/Users/dodo/Documents/androidprojects/ViLYNC_ERP/app/src/main/java/com/vilync/ophthalmicerp/feature/sales/reports/navigation/ReportNavGraph.kt)

#### [MODIFY] `salesReportsGraph`
- Update the `onGstReportsClick` lambda to navigate to the `"gst_home"` route.

```kotlin
    composable(route = "reports_home") {
        ReportsHomeScreen(
            // ... other callbacks ...
            onGstReportsClick = { navController.navigate("gst_home") },
            // ...
        )
    }
```

## Verification Plan

### Automated Tests
- Build the app using `gradle_build("app:assembleDebug")`.

### Manual Verification
1.  Navigate to **Reports Hub**.
2.  Tap on the **GST Filing Reports** card.
3.  Confirm that the **GST Module** screen (`GstHomeScreen`) opens correctly.
4.  Verify that all other cards in the Reports Hub still function as expected.
