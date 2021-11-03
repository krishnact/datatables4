package datatables4

/**
 * Generates and writes out reports based on data in DataTables. This service can be overridden to create various types of report.
 */
class DataTablesReportService {
    def dataTablesService

    /**
     * Generates a CSV format report for the items.
     * @param items The list of items to report on.
     * @param tableDefinition The Table Definition that defines the columns in the report.
     * @return
     */
    def formatReport(def items, def tableDefinition) {
        def reportData = [:]
        reportData.defaultFileName = tableDefinition.name + "Report.csv"
        reportData.contentType = "text/csv"

        def report = "" << ""
        def lineSeparator = "\r\n"

        def reportColumns = tableDefinition.getColumnsForReport()

        // Write column headings.
        report << reportColumns.heading.join(",") << lineSeparator

        // Loop through items and add them to the report.
        items.eachWithIndex { item, itemIndex ->
            def rowArray = []
            reportColumns.each { column ->
                rowArray << Helper.castSafe(column.getReportFormattedValueFromItem(item, itemIndex))
            }
            report << rowArray.join(",") << lineSeparator
        }
        reportData.report = report
        reportData
    }

    /**
     * Writes out the report.
     * @param reportData The same object that is returned by the formatReport method.
     * @param outputStream The outputStream to write the report to.
     */
    def writeReport(Map reportData, OutputStream outputStream) {
        outputStream << reportData.report
    }
}
