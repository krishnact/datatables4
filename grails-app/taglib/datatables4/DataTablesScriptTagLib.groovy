package grails.plugin

class DataTablesScriptTagLib {
    static namespace = "dt"
    def dataTablesService

    /**
     * Writes the JavaScript to initialize the DataTable object.
     * This is done by putting all the configuration into a map, then converting the map to a JavaScript object definition.
     */
    def writeInitializationScript = { attr, body ->
        def tableDefinition = attr.tableDefinition
        def dataTablesConfig = attr.dataTablesConfig
        attr = attr.attr // Use main attr from dt:datatable tag.
        def name = tableDefinition.name
        def columns = tableDefinition.getColumnsForTable()
        def initializationObject = [:]

        // Add defaults.
        initializationObject << dataTablesConfig.tableDefaults

        // Write the AJAX source action (if data is being loaded via AJAX).
        if(tableDefinition.serverDataLoad || tableDefinition.serverSide) {
            // Store tableDefinition in the session.
            def tableDefinitions = session.tableDefinitions
            if(!tableDefinitions) {
                tableDefinitions = [:]
                session.tableDefinitions = tableDefinitions
            }
            tableDefinitions[name] = tableDefinition
            tableDefinition.ajaxParams.name = name
            // Now we add the ajax link into the initialization script. It can link either to the controller in this
            // plugin, or to a custom controller/action.
            def url, data
            if(!attr["ajax.url"]) {
                if(tableDefinition.controller || tableDefinition.dataAction) {
                    url = g.createLink(controller: tableDefinition.controller, action: tableDefinition.dataAction ?: "getData", params: tableDefinition.ajaxParams)
                } else {
                    url = g.createLink(controller: "dataTables", action: "getData", params: tableDefinition.ajaxParams)
                }
                dataTablesService.addConfigurationParameter(initializationObject, "ajax.url", url)
            }
            dataTablesService.addConfigurationParameter(initializationObject, "ajax.type", "POST")
            def ajaxData = attr["ajax.data"]
            if(tableDefinition.sqlRestrictionFunction) {
                if(ajaxData) {
                    ajaxData = "function(d){d.dynamicSqlRestriction=${tableDefinition.sqlRestrictionFunction}();${ajaxData}}\n"
                } else {
                    ajaxData = "function(d){d.dynamicSqlRestriction=${tableDefinition.sqlRestrictionFunction}();\n}"
                }
            }
            if(ajaxData) {
                dataTablesService.addConfigurationParameter(initializationObject, "ajax.data", ajaxData)
                // Remove "ajax.data" from attr.
                attr["ajax.data"] = null
                // Add ajax.dataSrc to hide the loading text
                String dataSrc = "function(d){\n ${'$'}('#${name}HeadingLoading').toggle(false); \n return d.data;\n}"
                dataTablesService.addConfigurationParameter(initializationObject, "ajax.dataSrc", dataSrc)
            }
        }

        // Add the pagingType.
        if(!attr.pagingType) {
            initializationObject.pagingType = "full_numbers"

        }
        String buttonsDom = '';
        if (attr.buttons){
            buttonsDom='B'
        }
        // This section inserts the divs for the optional search and reset buttons.
        // Note that when the search button is used, the default search box is removed and a new one replaces it.
        if(tableDefinition.searchButton || tableDefinition.resetButton || tableDefinition.reportButton || (tableDefinition.searching && !tableDefinition.tableSearching)) {
            // Add custom dom.
            def searchSpan = ""
            if(tableDefinition.tableSearching) {
                if(tableDefinition.searchButton || tableDefinition.resetButton) {
                    searchSpan = "<\"#${name}Search\">"
                }
                if(!tableDefinition.searchButton) {
                    searchSpan += "f"
                }
            }
            def reportSpan = ""
            if(tableDefinition.reportButton) {
                reportSpan = "<\"#${name}Report\">"
            }
            if(dataTablesConfig.jQueryUI) {
                initializationObject.dom = "<\"H\"l${buttonsDom}${searchSpan}r>t<\"F\"ip${reportSpan}>"
                initializationObject.jQueryUI = true
                // Deprecated feature, but required since the styling does not work with a custom DOM otherwise.
                // http://datatables.net/forums/discussion/24479/datatable-dom-option-cancels-jquery-ui-theming-using-jquery-ui-specific-dom-option
            } else {
                initializationObject.dom = "l${buttonsDom}${searchSpan}rtip${reportSpan}"
            }
        }else{
            initializationObject.dom = attr.dom
        }

        // Write the default order property.
        def orders = []
        columns.eachWithIndex() { column, i ->
            def order = column.order
            if(order) {
                orders << [i, order]
            }
        }
        if(orders) {
            initializationObject.order = orders
        }

        // Write the language section. This copies language entries from the messages.properties file.
        def languagePaginateMap = [:]
        ["first","last","next","previous"].each {
            def message = g.message(code: "datatables.language.paginate.${it}", default: null)
            if(message) {
                languagePaginateMap[it] = message
            }
        }
        def languageAriaMap = [:]
        ["sortAscending","sortDescending"].each {
            def message = g.message(code: "datatables.language.aria.${it}", default: null)
            if(message) {
                languageAriaMap[it] = message
            }
        }
        def languageMap = [:]
        ["emptyTable","info","infoEmpty","infoFiltered","infoPostFix","thousands","lengthMenu",
         "loadingRecords","processing","search","zeroRecords"].each {
            def message = g.message(code: "datatables.language.${it}", default: null)
            if(message) {
                languageMap[it] = message
            }
        }
        if(languagePaginateMap) {
            languageMap.paginate = languagePaginateMap
        }
        if(languageAriaMap) {
            languageMap.aria = languageAriaMap
        }
        if(languageMap) {
            initializationObject.language = languageMap
        }

        initializationObject.columns = columns.initParams

        // Add the drawCallback function using a template.
        initializationObject.drawCallback = render(
                template: "/dataTablesTemplates/drawCallbackFunction",
                plugin: "grails-datatables",
                model: [dataTablesConfig: dataTablesConfig, tableDefinition: tableDefinition, attr: attr]
        )
        attr.remove("drawCallback")

//        if ( attr.rowCallback){
//            initializationObject.rowCallback = attr.rowCallback
//        }
//        attr.remove("rowCallback")
        // Add pre-draw callback to adjust column widths before each draw.
        if(tableDefinition.adjustColumnWidths || tableDefinition.columnSearching || attr.preDrawCallback) {
            def preDrawCallbackFunction = "" << "function(settings) {"
            if(attr.preDrawCallback) {
                // Call preDrawCallback function provided in attributes.
                preDrawCallbackFunction << "(" << attr.preDrawCallback << ")(settings);"
                attr.remove("preDrawCallback")
            }
            if(tableDefinition.adjustColumnWidths) {
               preDrawCallbackFunction << "if(null != ${name}) {${name}.columns.adjust();}"
            }
            if(tableDefinition.columnSearching) {
                preDrawCallbackFunction << "if(null == ${name}) {adjust${name}ColumnSearchWidths();}"
            }
            preDrawCallbackFunction << "}"
            initializationObject.preDrawCallback = preDrawCallbackFunction
        }

        // Add the initComplete function using a template.
        initializationObject.initComplete = render(
                template: "/dataTablesTemplates/initCompleteFunction",
                plugin: "grails-datatables",
                model: [dataTablesConfig: dataTablesConfig, tableDefinition: tableDefinition, attr: attr]
        )
        attr.remove("initComplete")

        // Add DataTables properties.
        initializationObject << attr

        // Write the initialization.
        out << "${name} = \$('#${name}').DataTable("
        dataTablesService.serializeJavaScriptObject(initializationObject, out)
        out << ");"
    }
}