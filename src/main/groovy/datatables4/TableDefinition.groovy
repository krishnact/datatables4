package datatables4

import org.hibernate.criterion.Restrictions

class TableDefinition {
    String          name                    = "myTable"
    String          domainClass
    List            columns                 = []

    // DataTables.net properties.
    boolean         ordering                = true
    boolean         searching               = true
    boolean         serverSide              = false
    boolean         stateSave               = false

    // Custom properties.
    boolean         adjustColumnWidths      = false
    boolean         columnSearching         = false
    boolean         deferInitialization     = false
    boolean         hideWhenEmpty           = false
    boolean         reportButton            = false
    boolean         reportingEnabled        = true
    boolean         resetButton             = false
    boolean         searchButton            = false
    boolean         serverDataLoad          = false
    boolean         showMetadata            = false
    boolean         tableSearching          = true
    def             sqlRestriction
    List            dataItems
    List            headingParams
    Map             ajaxParams              = [:]
    String          controller
    String          dataAction
    String          errorFunction
    String          reportAction
    String          reportFunction
    String          showAboveTable
    String          showBelowTable
    String          sqlRestrictionFunction
    String          headerRowClass
    String          bodyRowClass

    // Internal variables.
    boolean         hasLocalData
    boolean         hasSelectAll
    StringBuffer    headingContent          = "" << ""

    /**
     * Create a new tableDefinition. We are drawing from two sources to get the properties;
     * first we check the attr map, then we check the tableDefaults.
     * If neither of these contain the value, then the default value (see above) will be used.
     *
     * @param attr The taglib attributes.
     * @param tableDefaults A copy of the map in Config.groovy. We use a copy so we can modify it without changing the original.
     */
    TableDefinition(Map attr, Map tableDefaults) {
        hasLocalData = attr.containsKey("dataItems")

        // Custom properties.
        [
                "adjustColumnWidths",
                "ajaxParams",
                "columnSearching",
                "controller",
                "dataAction",
                "dataItems",
                "deferInitialization",
                "domainClass",
                "errorFunction",
                "headingParams",
                "hideWhenEmpty",
                "name",
                "reportAction",
                "reportButton",
                "reportFunction",
                "reportingEnabled",
                "resetButton",
                "searchButton",
                "serverDataLoad",
                "showMetadata",
                "sqlRestriction",
                "sqlRestrictionFunction",
                "tableSearching",
                "headerRowClass",
                "bodyRowClass"
        ].each { property ->
            // Check if the property exists in attr. If so, use that value and remove it from attr.
            if(attr.containsKey(property)) {
                def attrSetting = attr[property]
                if(this[property] instanceof Boolean) {
                    if(attrSetting instanceof Boolean) {
                        this[property] = attrSetting
                    } else {
                        this[property] = attrSetting == "true"
                    }
                } else {
                    this[property] = attrSetting
                }
                attr.remove(property)
            } else if(tableDefaults?.containsKey(property)) { // Check for a default configuration property.
                this[property] = tableDefaults[property]
                tableDefaults.remove(property)
            }
        }
        if(sqlRestriction) {
            sqlRestriction = Restrictions.sqlRestriction(sqlRestriction)
        }

        // DataTables.net properties.
        [
                "serverSide",
                "ordering",
                "searching",
                "stateSave"
        ].each { setting ->
            // Check if the property exists in attr. If so, use that value.
            if(attr.containsKey(setting)) {
                def attrSetting = attr[setting]
                if(this[setting] instanceof Boolean) {
                    if(attrSetting instanceof Boolean) {
                        this[setting] = attrSetting
                    } else {
                        this[setting] = attrSetting == "true"
                    }
                } else {
                    this[setting] = attrSetting
                }
            } else if(tableDefaults?.containsKey(setting)) { // Check for a default configuration property.
                this[setting] = tableDefaults[setting]
            }
        }

        // Apply some logic to ensure settings are consistent.
        if(!reportingEnabled) {
            reportButton = false
        }
        columnSearching = searching && columnSearching
        tableSearching = searching && tableSearching
    }

    /**
     * Get columns which should be included in the table (as opposed to the report).
     * @return
     */
    def getColumnsForTable() {
        columns.findAll { it.includedInTable }
    }

    /**
     * Get columns which should be included in the report.
     * @return
     */
    def getColumnsForReport() {
        columns.findAll { it.includedInReport }
    }
}