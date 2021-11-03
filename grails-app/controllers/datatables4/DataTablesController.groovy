package datatables4

import grails.converters.JSON
import grails.util.Holders
import org.springframework.context.ApplicationContext

class DataTablesController {
    DataTablesService dataTablesService
    //def grailsApplication

    /**
     * Get AJAX data for a table.
     * @return
     */
    def getData() {
        def time0 = System.currentTimeMillis()
        def tableDefinition = getTableDefinition(params.name)
        if(!tableDefinition) {
            def resultMap = [:]
            if(params.draw) {
                resultMap.draw = params.draw as int
            }
            resultMap.error = "Your session has expired."
            render resultMap as JSON
            return
        }
        def sortIndex = params."order[0][column]"
        if(null != sortIndex) {
            sortIndex = sortIndex as int
        }
        String domainClass = tableDefinition.domainClass
        if ( domainClass&& domainClass.startsWith('_')){
            return getDynamicData()
        }
        // Get the item data.
        def itemData = dataTablesService.getData(
                tableDefinition,
                params."search[value]",
                getColumnSearchParams(params, tableDefinition),
                sortIndex,
                params."order[0][dir]",
                params.length,
                params.start,
                params.dynamicSqlRestriction
        )

        // Loop through items and add them to the output.
        def data = []
        def columns = tableDefinition.getColumnsForTable()
        def itemIndex = params.start ?: 0
        for(item in itemData.items) {
            def rowArray = []
            columns.each { column ->
                rowArray << Helper.castSafe(column.getTableFormattedValueFromItem(item, itemIndex))
            }
            data << rowArray
            itemIndex++
        }

        def resultMap = [:]
        if(params.draw) {
            resultMap.draw = params.draw as int
        }
        resultMap.recordsTotal = itemData.count ?: 0
        resultMap.recordsFiltered = itemData.filteredCount ?: 0
        resultMap.data = data
        render resultMap as JSON
        def time1 = System.currentTimeMillis()
        log.debug "DataTables controller delivered AJAX data in ${time1 - time0}ms."
    }

    /**
     * Download a report.
     */
    def getReport() {
        def tableDefinition = getTableDefinition(params.name)

        // Check that reporting is enabled for this table.
        if(!tableDefinition?.reportingEnabled) {
            response.status = 404
            return
        }

        def sortIndex = params.sortIndex
        if(null != sortIndex) {
            sortIndex = sortIndex as int
        }

        // Get the item data.
        def itemData = dataTablesService.getData(
                tableDefinition,
                params.search,
                getColumnSearchParams(params, tableDefinition),
                sortIndex,
                params.sortDir,
                0,
                0,
                params.dynamicSqlRestriction
        )

        // Find which service to use to generate report.
        def reportServiceName = grailsApplication.config.grails.plugin.datatables.reportService
        if(!reportServiceName) {
            reportServiceName = "dataTablesReportService"
        }
        ApplicationContext ctx = Holders.grailsApplication.mainContext
        def reportService =  ctx.getBean(reportServiceName);
        def reportData = reportService.formatReport(itemData.items, tableDefinition)

        // Send it off.
        response.setContentType(reportData.contentType)
        def fileName = g.message(code: "datatables.${tableDefinition.name}.reportFileName", default: reportData.defaultFileName)
        response.setHeader("Content-disposition", "attachment;filename=${fileName}")
        // Add cookie for the JQuery fileDownload plugin.
        javax.servlet.http.Cookie cookie = new javax.servlet.http.Cookie("fileDownload", "true")
        cookie.path = "/"
        cookie.maxAge = 100
        cookie.setSecure(true);
        // Sometimes we get a broken pipe exception, presumably if the user cancels the download. It's not reproducable. Use try-catch block to resolve.
        try {
            response.addCookie(cookie)
            response.setHeader("Cache-Control", "no-store, no-cache") // This line makes IE8 and worse behave.
            reportService.writeReport(reportData, response.outputStream)
            response.outputStream.flush()
            response.outputStream.close()
        } catch(e) {
            log.debug "Error occurred while writing report to response: ${e}"
        }
    }

    /**
     * Gets a TableDefinition from the session.
     * @param params
     * @return
     */
    private def getTableDefinition(def name) {
        def tableDefinitions = session.tableDefinitions
        if(!tableDefinitions) {
            // Session must have expired or request is bogus.
            return null
        }
        tableDefinitions[name]
    }

    /**
     * Get column search parameters from params.
     * @param params
     * @param tableDefinition
     * @return a Map containing the column search parameters, using the columns as the keys.
     */
    private def getColumnSearchParams(def params, def tableDefinition) {
        def columnSearchParams = null
        if(tableDefinition.columnSearching) {
            columnSearchParams = [:]
            tableDefinition.getColumnsForTable().eachWithIndex { column, index ->
                if(column.searchable) {
                    def columnSearchParameter = params."columns[${index}][search][value]"
                    if(columnSearchParameter) {
                        columnSearchParams[column] = columnSearchParameter
                    }
                }
            }
        }
        columnSearchParams
    }
}
