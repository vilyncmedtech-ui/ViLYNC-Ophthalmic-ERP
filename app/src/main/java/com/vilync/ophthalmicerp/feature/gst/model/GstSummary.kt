package com.vilync.ophthalmicerp.feature.gst.model

data class GstSummary(
    val financialYearStart: Int,
    val outputTaxableValue: Double = 0.0,
    val outputCgst: Double = 0.0,
    val outputSgst: Double = 0.0,
    val outputIgst: Double = 0.0,
    val inputTaxableValue: Double = 0.0,
    val inputCgstPersisted: Double = 0.0,
    val inputSgstPersisted: Double = 0.0,
    val inputIgstPersisted: Double = 0.0,
    val postedSalesCount: Int = 0,
    val purchaseCount: Int = 0
) {
    val outputGst: Double get() = outputCgst + outputSgst + outputIgst
    val inputGst: Double get() = inputCgstPersisted + inputSgstPersisted + inputIgstPersisted
    val netGstPosition: Double get() = outputGst - inputGst
    val financialYearDisplayName: String
        get() = if (financialYearStart > 0) {
            "$financialYearStart-${((financialYearStart + 1) % 100).toString().padStart(2, '0')}"
        } else "Not selected"
}
