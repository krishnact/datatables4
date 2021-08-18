package datatables4

import grails.util.Holders
import groovy.transform.EqualsAndHashCode
import org.grails.core.artefact.DomainClassArtefactHandler
import org.grails.orm.hibernate.cfg.GrailsDomainBinder
import org.hibernate.criterion.CriteriaSpecification
import org.hibernate.criterion.Restrictions

import java.text.SimpleDateFormat

/**
 * Class that stores data pertaining to the back-end data collection for a column.
 */
@EqualsAndHashCode
class ColumnDefinition {
    static final LOCALE = Locale.getDefault()

    // DataTables.net properties.
    boolean orderable
    boolean searchable
    boolean visible
    String name

    // Custom properties.
    Closure dataFunction
    Closure linkCondition
    List joinTypes
    String dataType
    String falseText
    String heading
    String headingKey
    String include
    String link
    String linkIdField
    String order
    String trueText

    // Internal variables.
    boolean dbColumnIsFormula
    boolean includedInReport
    boolean includedInTable
    List linkIdFieldParts
    List listOfCriteria
    List nameParts
    List path
    Map initParams
    SimpleDateFormat dateFormatter
    String alias
    String className
    String columnClass
    String dbColumnName
    String finalName
    String searchSql
    String fnCreatedCell
    ColumnDefinition(TableDefinition tableDefinition, Map attr, Map defaults, def dataTablesService, String linkString, def grailsApplication, def sessionFactory) {
        if(defaults) {
            defaults.each { key, value ->
                if(!attr.containsKey(key)) {
                    value
                }
            }
        }
        name =              attr.name
        dataType =          attr.dataType
        link =              linkString
        //Added by Krishna
        linkCondition =     attr.linkCondition
        linkIdField =       attr.linkIdField
        trueText =          attr.trueText
        falseText =         attr.falseText
        dataFunction =      attr.dataFunction
        className =         attr.className
        order =             attr.order
        visible =           attr.visible != "false"
        include =           attr.include
        fnCreatedCell=      attr.fnCreatedCell
        includedInTable =   include in [null, "all", "table"]
        includedInReport =  visible && include in [null, "all", "report"]
        headingKey =        attr.headingKey
        searchable =        visible && includedInTable && "false" != attr.searchable
        orderable =         tableDefinition.ordering && "false" != attr.orderable

        if(name.contains(".")) {
            nameParts = name.tokenize(".")
            finalName = nameParts.last()
            path = nameParts.dropRight(1)
        } else {
            finalName = name
        }
        if(linkIdField?.contains(".")) {
            linkIdFieldParts = linkIdField.tokenize(".")
        }
        if(attr.heading) {
            heading = attr.heading
        } else {
            def messageSource = Holders.applicationContext.getBean("messageSource")
            if(attr.headingKey) {
                heading = messageSource.getMessage(attr.headingKey, null, "", LOCALE)
            } else {
                def message = messageSource.getMessage("datatables." + tableDefinition.name + "." + name, null, "", LOCALE)
                if(message) {
                    heading = message
                } else {
                    heading = splitCamelCase(attr.name)
                }
            }
        }
        if(trueText || falseText) {
            dataType = "boolean"
        }

        // Set the dbColumnName and columnClass fields.
        def className = tableDefinition.domainClass
        if(className) {
            // If we are using inline data or a custom action to get the data from the server, then we will not be using a domain class.
            def objectClassName
            def lastNamePart
            def propertyType
            def namePartsList = nameParts ?: [name]
            def propertyIsCollection = false
            try {
                // Not every name needs to refer to a real field. For example, a column might have a random name and use a dataFunction to provide data.
                // Note (special case): If we have an association column that contains only objects that are of a subclass of the type of the association
                // class (as defined in the domain model), and we are referring to a property that exists in the subclass but not in the parent class,
                // then we will not be able to obtain the property type from the domain model. Therefore this try block will fail.
                namePartsList.each { namePart ->
                    objectClassName = className
                    lastNamePart = namePart
                    def domainDescriptor = grailsApplication.getArtefact(DomainClassArtefactHandler.TYPE, className)
                    if(!domainDescriptor) {
                        propertyIsCollection = true
                    } else {
                        //def property = domainDescriptor.getConstrainedProperties()[name].property.propertyType
                        propertyType = domainDescriptor.getConstrainedProperties()[name].property.propertyType
                        className = propertyType.name
                    }
                }
                columnClass = propertyType?.simpleName
                // We store this in the object since it's sometimes used during formatting.

                // If we have a date column but the dataType is not set explicitly, set it now.
                if(!dataType && columnClass == "Date") {
                    dataType = "date"
                }

                if(!propertyIsCollection) {
                    Class clazz = grailsApplication.getDomainClass(objectClassName).clazz
                    dbColumnName = sessionFactory.getClassMetadata(clazz).propertyMapping.getColumnNames(lastNamePart)[0]
                    if(!dbColumnName) {
                        // The database column name is null. This happens if the column is a formula column, so we get the formula instead.
                        def mapping = new GrailsDomainBinder().getMapping(clazz)
                        dbColumnName = mapping?.getPropertyConfig(lastNamePart)?.formula
                        dbColumnIsFormula = true
                    }
                }

                if(attr.criteria) {
                    // The criteria attribute can be presented as a single criterion or a list of criteria.
                    // Each criterion consists of a list, beginning with the name of the criterion method, followed by any parameters (not including the field name).
                    // First, determine whether the top-level list is a criterion or a list of criteria, and if not, make it so.
                    listOfCriteria = attr.criteria
                    if(!(listOfCriteria[0] instanceof List)) {
                        listOfCriteria = [listOfCriteria]
                    }
                }

                if(path && attr.joinTypes) {
                    joinTypes = []
                    def stringList = attr.joinTypes.tokenize(",")
                    stringList.each {
                        if(it == "LEFT_JOIN") {
                            joinTypes << CriteriaSpecification.LEFT_JOIN
                        } else if(it == "FULL_JOIN") {
                            joinTypes << CriteriaSpecification.FULL_JOIN
                        } else if(it == "INNER_JOIN") {
                            joinTypes << CriteriaSpecification.INNER_JOIN
                        } else {
                            joinTypes << null
                        }
                    }
                }
            } catch(e) {
                // No problem, we just could not find this field in our domain model.
                if(tableDefinition.serverSide && !tableDefinition.controller) {
                    // Searching and ordering using the built-in controller is not going to be possible with server-side processing if we are not using a domain class field.
                    searchable = false
                    attr.searchable = false
                    orderable = false
                    attr.orderable = false
                }
            }
        }

        // If this column is a date or boolean type, set the search sql.
        if(dataType) {
            def dateFormatString
            def dateFormatLength // SQL Server only.
            if("date" == dataType) {
                dateFormatString = dataTablesService.getDateFormat()
                dateFormatLength = dataTablesService.getDateFormatLength() // SQL Server only.
            } else if("datetime" == dataType) {
                dateFormatString = dataTablesService.getDateTimeFormat()
                dateFormatLength = dataTablesService.getDateTimeFormatLength() // SQL Server only.
            } else if("time" == dataType) {
                dateFormatString = dataTablesService.getTimeFormat()
                dateFormatLength = dataTablesService.getTimeFormatLength() // SQL Server only.
            }
            if(dateFormatString) {
                dateFormatter = new SimpleDateFormat(dateFormatString)
                if(dbColumnIsFormula) {
                    searchSql = dataTablesService.getDateSearchSql(dbColumnName, dateFormatString, dateFormatLength)
                } else {
                    searchSql = dataTablesService.getDateSearchSql("{alias}." + dbColumnName, dateFormatString, dateFormatLength)
                }
            } else if("boolean" == dataType) {
                searchSql =  """CASE {alias}.${dbColumnName}
                                WHEN 1 THEN '${null == trueText ? "true" : trueText.toLowerCase()}'
                                ELSE '${null == falseText ? "false" : falseText.toLowerCase()}'
                                END
                                like ?"""
            }
        }

        // Set the search sql for numeric columns.
        if(!searchSql && columnClass in ["byte", "short", "int", "long", "float", "double", "Byte", "Short", "Integer", "Long", "Float", "Double"]) {
            // Searching in numeric columns must be done using a sqlRestriction since I don't see any other suitable criterion method.
            if(dbColumnIsFormula) {
                searchSql = "CAST(${dbColumnName} AS VARCHAR(256)) like ?"
            } else {
                searchSql = "CAST({alias}.${dbColumnName} AS VARCHAR(256)) like ?"
            }
        }

        // Remove attributes that do not pertain to the DataTables options initialization.
        attr.keySet().removeAll(["dataType", "criteria", "linkController", "linkAction", "linkIdField", "trueText", "falseText", "dataFunction", "linkCondition","order", "include", "headingKey", "heading", "joinTypes"])
        initParams = attr
    }

    /**
     * Returns a list of criteria for this column. If this column has is an association column, then the alias must be created before this method is called.
     */
    def getCriteria() {
        def criteria = []
        // Iterate the list of criteria lists and convert them into real Criteria objects.
        // Note the criterion methods can take a variable number of arguments, so we use the spread operator.
        // Also note that only criterion methods that consist of the name/alias followed by a list of zero or more
        // arguments are supported, so for example, "idEq" is not supported.
        listOfCriteria.each { criterionComponents ->
            def restrictionMethod = criterionComponents[0] // This first element is the criterion method.
            def arguments = criterionComponents.drop(1) // The remaining elements are the criterion arguments.
            if (restrictionMethod == "sqlRestriction") {
                criteria << Restrictions.sqlRestriction(*arguments)
            } else {
                criteria << Restrictions."${restrictionMethod}"(alias, *arguments)
            }
        }
        criteria
    }

    /**
     * Constructor for creating a checkbox column.
     * @param tableDefinition
     * @param attr
     */
    ColumnDefinition() {
        dataType =          "checkbox"
        className =         "checkboxColumn"
        include =           "table"
        includedInTable =   true
        includedInReport =  false
        searchable =        false
        orderable =         false
        initParams = [searchable: false, orderable: false, className: "checkboxColumn"]
    }

    /**
     * Transforms a heading string to a readable heading.
     * myHeading -> My Heading
     * myThing.name -> My Thing Name
     * @param s
     * @return
     */
    private def splitCamelCase(String s) {
        s.tokenize(".").collect { it.capitalize() }.join().replaceAll(
                String.format(
                        "%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"
                ),
                " "
        )
    }

    def getAlias() {
        alias ?: name
    }

    def formatValue(value) {
        if(value == null){
            return ""
        }
        if(dateFormatter) {
            return dateFormatter.format(value)
        }
        if("boolean" == dataType) {
            return value ? (null == trueText ? "True" : trueText) : (null == falseText ? "False" : falseText)
        }
        value
    }

    /**
     * Format a value for this column for use in a table.
     * @param item
     * @param itemIndex
     * @return
     */
    def getTableFormattedValueFromItem(def item, def itemIndex) {
        def value, linkId
        if("checkbox" == dataType) {
            return "<input type='checkbox' name='cb-${item.id}' id='cb-${item.id}'/>"
        } else if(dataFunction) {
            value = applyDataFunctionToValue(item, itemIndex, dataFunction)
        } else {
            value = getValueFromItem(item)
        }
        def linkCond = true
        if (linkCondition){
            linkCond = applyDataFunctionToValue(item, itemIndex, linkCondition)
        }
        def formattedValue = formatValue(value)
        if(link && linkCond) {
            // Return a link instead of plain text.
            if(linkIdField == name) {
                if(dataFunction) {
                    linkId = getValueFromItem(item)
                } else {
                    linkId = value
                }
            } else {
                if(linkIdFieldParts) {
                    linkId = getValueFromItemByFieldName(item, linkIdFieldParts)
                } else {
                    linkId = linkIdField ? item[linkIdField] : item.id
                }
            }
            return "<a href=\"${link}/${linkId}\">${formattedValue}</a>"
        }
        formattedValue
    }

    /**
     * Format a value for this column for use in a report.
     * @param item
     * @param itemIndex
     * @return
     */
    def getReportFormattedValueFromItem(def item, def itemIndex) {
        def value
        if(dataFunction) {
            value = applyDataFunctionToValue(item, itemIndex, dataFunction)
        } else {
            value = getValueFromItem(item)
        }
        formatValue(value)
    }

    /**
     * Use the datafunction closure.
     * @return
     */
    private def applyDataFunctionToValue(def item, def itemIndex, Closure dataFunction) {
        // Use the datafunction closure.
        def numParams = dataFunction.parameterTypes.length
        if (numParams == 1) {
            return dataFunction(item)
        } else {
            return dataFunction(item, itemIndex)
        }
    }

    private def getValueFromItem(def item) {
        if(nameParts) {
            return getValueFromItemByFieldName(item, nameParts)
        }
        item[name]
    }

    private def getValueFromItemByFieldName(def item, def nameParts) {
        nameParts.each {
            if(!item) {
                return null
            }
            item = item[it]
        }
        return item
    }

    def getDateFormat() {
        dateFormatter?.toPattern()
    }

    def getSearchCriterion(def customQueryAggregator, def searchParam) {
        searchParam = "%" + searchParam.toLowerCase() + "%"
        if(searchSql) {
            return customQueryAggregator.getSqlRestriction(searchSql, searchParam, path)
        }
        // There is no search SQL, so we perform an ilike search.
        Restrictions.ilike(alias ?: name, searchParam)
    }
}