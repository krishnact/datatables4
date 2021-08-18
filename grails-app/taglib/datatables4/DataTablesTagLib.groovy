package datatables4

import datatables4.ColumnDefinition
import datatables4.TableDefinition
import org.grails.gsp.GroovyPageTemplate

/**
 * This taglib writes the html and javascript to generate a datatables.net table.
 * You can use one of three approaches for loading data: local, AJAX or server-side processing.
 */
class DataTablesTagLib {
    static namespace = "dt"
    static final CONTEXT = this.class.name
    def dataTablesService
    def sessionFactory
    def dataTablesScriptService

    /**
     * The datatable tag creates the HTML and JavaScript necessary to produce a table.
     */
    def datatable = { attr, body ->
        def dataTablesConfig = grailsApplication.config.grails.plugin.datatables
        def tableDefaults = dataTablesConfig?.tableDefaults?.clone()
        def tableDefinition =  new TableDefinition(attr, tableDefaults)
        def name = tableDefinition.name

        request.setAttribute(CONTEXT, tableDefinition) // This makes the tableDefinition available to the column tags.
        def otherContent = body()


        /***************************************************************
         * Write the metadata (if showMetadata == true)                *
         ***************************************************************/
        if(tableDefinition.showMetadata) {
            out << render(
                    template: "/dataTablesTemplates/metadata",
                    plugin: "grails-datatables",
                    model: [out: out, dataTablesConfig: dataTablesConfig, tableDefinition: tableDefinition, attr: attr]
            )
        }

        /***************************************************************
         * Write the heading messages.                                 *
         ***************************************************************/
        if(!tableDefinition.hasLocalData) {
            out << "<span id=\"${name}HeadingLoading\">"
            out << "<p>" << g.message(code: "datatables.language.loading", default: "Loading...") << "</p>"
            out << "</span>"
        }
        out << "<span id=\"${name}HeadingEmpty\" style='display: none'>"
        def code = "datatables.${name}.heading"
        def message = g.message(code: code + ".empty", args: tableDefinition.headingParams, default: "")
        if(message) {
            out << "<p>" << message << "</p>"
        }
        out << "</span>"
        out << "<span id=\"${name}HeadingSingular\" style='display: none'>"
        message = g.message(code: code + ".singular", args: tableDefinition.headingParams, default: "")
        if(message) {
            out << "<p>" << message << "</p>"
        }
        out << "</span>"
        out << "<span id=\"${name}HeadingPlural\" style='display: none'>"
        message = g.message(code: code + ".plural", args: tableDefinition.headingParams, default: "")
        if(message) {
            out << "<p>" << message << "</p>"
        }
        out << "</span>"

        // Write the container. This does not include the heading.
        out << "<span id='${name}Container'>"

        // Write the "showAboveTable" content.
        if(tableDefinition.showAboveTable) {
            out << tableDefinition.showAboveTable
        }

        /***************************************************************
         * Write the table.                                            *
         ***************************************************************/
        out << "<table dt_comment=\"created At ${new Date()}\" id=\"${name}\" name=\"${name}\" width=\"100%\" class=\"${attr.clazz}\"><thead><tr>"
        out << tableDefinition.headingContent
        out << "</tr></thead>"
        out << otherContent
        if(tableDefinition.hasLocalData) {
            out << "<tbody>"
            tableDefinition.dataItems.eachWithIndex { item, itemIndex ->
                out << "<tr"
                if (tableDefinition.bodyRowClass != null){
                    out << " class='${tableDefinition.bodyRowClass}'>"
                }
                out <<  ">"
                tableDefinition.columns.each { column ->
                    if(column.includedInTable) {
                        out << "<td>"
                        out << column.getTableFormattedValueFromItem(item, itemIndex)
                        out << "</td>"
                    }
                }
                out << "</tr>"
            }
            out << "</tbody>"
        }
        if(tableDefinition.columnSearching) {
            out << "<tfoot><tr>"
            tableDefinition.columns.each { column ->
                if(column.include in [null, "all", "table"]) {
                    out << "<td>"
                    if(column.searchable) {
                        out << '<input type="search" class="columnSearch" placeholder="'
                        out << g.message(code: "datatables.language.searchButton", default: "Search")
                        out << " "
                        out << column.heading
                        out << "\""
                        if(!tableDefinition.stateSave) {
                            out << " autoComplete=\"false\""
                        }
                        out << "/>"
                    }
                    out << "</td>"
                }
            }
            out << "</tr></tfoot>"
        }
        out << "</table>"

        // Write the "showBelowTable" content.
        if(tableDefinition.showBelowTable) {
            out << tableDefinition.showBelowTable
        } else {
            out << "<div class='verticalSpacer'></div>"
        }
        out << "</span>"

        /***************************************************************
         * Write the JavaScript.                                       *
         ***************************************************************/
        GroovyPageTemplate
        out << render(
                template: "/dataTablesTemplates/dataTableJavaScript",
                plugin: "grails-datatables",
                model: [out: out, dataTablesConfig: dataTablesConfig, tableDefinition: tableDefinition, attr: attr, name: name]
        )
        pageScope.functionsTemplateRendered = true
    }

    /**
     * Creates a column in the table.
     */
    def column = { attr, body ->
        def tableDefinition = request.getAttribute(CONTEXT)
        def headingContent = tableDefinition.headingContent
        def defaults = grailsApplication.config.grails.plugin.datatables.columnDefaults
        def link
        if(attr.linkController || attr.linkAction || attr.linkIdField) {
            link = g.createLink(
                    controller: attr.linkController ?: pageScope.controllerName,
                    action: attr.linkAction ?: pageScope.actionName
            )
        }
        def column = new ColumnDefinition(tableDefinition, attr, defaults, dataTablesService, link, grailsApplication, sessionFactory)
        tableDefinition.columns << column
        if(column.include in [null, "all", "table"]) {
            headingContent << "<th>"
            headingContent << column.heading
            headingContent << body()
            headingContent << "</th>"
        }
    }

    /**
     * Creates a checkbox column in the table.
     */
    def checkboxColumn = { attr, body ->
        def tableDefinition = request.getAttribute(CONTEXT)
        def headingContent = tableDefinition.headingContent
        def column = new ColumnDefinition()
        tableDefinition.columns << column
        headingContent << "<th class='checkboxColumn'>"
        if("true" == attr.selectAll || true == attr.selectAll) {
            headingContent << "<input type='checkbox' id='${tableDefinition.name}_check_all'/>"
            tableDefinition.hasSelectAll = true
        }
        headingContent << "</th>"
    }

    /**
     * Adds content above the table.
     */
    def showAboveTable = { attr, body ->
        def tableDefinition = request.getAttribute(CONTEXT)
        tableDefinition.showAboveTable = body()
    }

    /**
     * Adds content below the table.
     */
    def showBelowTable = { attr, body ->
        def tableDefinition = request.getAttribute(CONTEXT)
        tableDefinition.showBelowTable = body()
    }
}