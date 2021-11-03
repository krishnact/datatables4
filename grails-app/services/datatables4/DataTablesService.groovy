package datatables4

import datatables4.CriteriaAggregator
import grails.core.GrailsApplication
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.criterion.Order
import org.hibernate.criterion.Restrictions
import org.hibernate.query.NativeQuery
import org.springframework.beans.factory.annotation.Autowired

class DataTablesService {

    @Autowired
    GrailsApplication grailsApplication
    @Autowired
    SessionFactory sessionFactory


    /**
     * Gets AJAX data for display in the DataTable that uses server-side data.
     */
    def getData(
            tableDefinition,
            tableSearch,
            columnSearchParams,
            sortIndex,
            sortDir,
            displayLength,
            displayStart,
            dynamicSqlRestriction
    ) {
        def resultMap = [:]
        def dataTablesConfig = grailsApplication.config.grails.plugin.datatables
        def dc = grailsApplication.getDomainClass(tableDefinition.domainClass)
        def customQueryAggregator = null;
        Class clazz = dc.clazz

        // Create CriteriaAggregator.
        customQueryAggregator = new CriteriaAggregator(clazz)

        // Add column aliases. These are necessary for criteria that apply to association fields.
        tableDefinition.columns.each { column ->
            if(column.path) {
                column.alias = customQueryAggregator.getAssociationAlias(column.path, column.joinTypes) + "." + column.finalName
            }
        }

        // Add table sql restriction.
        if(tableDefinition.sqlRestriction) {
            customQueryAggregator.addCriterion(tableDefinition.sqlRestriction)
        }

        // Add column criteria.
        tableDefinition.columns.each { column ->
            column.getCriteria().each {
                customQueryAggregator.addCriterion(it)
            }
        }

        // Add dynamic SQL Restriction (result of sqlRestrictionFunction).
        if(dynamicSqlRestriction) {
            customQueryAggregator.addCriterion(
                    Restrictions.sqlRestriction(Helper.makeSqlDeleteAndUpdateSafe(dynamicSqlRestriction))
            )
        }

        // Count the total unfiltered items. We still apply the sqlRestriction and column criteria, just not the search filter.
        resultMap.count = customQueryAggregator.count()

        // Add sorting. The sortIndex refers to columns included in the table.
        def columnsIncludedInTable = tableDefinition.getColumnsForTable()
        if(null != sortIndex) {
            def column = columnsIncludedInTable[sortIndex]
            // Ensure we are not trying to sort on a column that is not orderable.
            if(column.orderable) {
                customQueryAggregator.addOrder(new Order(column.alias, "asc" == sortDir))
            }
        }
        // Add secondary default order by id to maintain consistency of order.
        customQueryAggregator.addOrder(new Order("id", true))

        if(!displayLength) {
            displayLength = dataTablesConfig.reportSizeLimit
            if(!displayLength) {
                displayLength = 15001
            }
            displayStart = 0
        }

        // Add general search criteria. Only columns included in the table are searchable. The search string is tokenized, and each token is searched against each column.
        def tableSearching = (tableSearch && tableDefinition.searching)
        if(tableSearching) {
            def tableSearchParams = tableSearch.tokenize()
            tableSearchParams.eachWithIndex { tableSearchParam, index ->
                columnsIncludedInTable.each { column ->
                    if(column.searchable) {
                        addColumnSearch(column, customQueryAggregator, tableSearchParam, index)
                    }
                }
            }
        }

        // Add column search criteria. The search string is tokenized, and each token is searched against the column.
        if(columnSearchParams) {
            columnSearchParams.keySet().each { column ->
                columnSearchParams[column]?.tokenize().each {
                    addColumnSearch(column, customQueryAggregator, it)
                }
            }
        }

        // Run query and add results to resultMap.
        def queryResults = customQueryAggregator.list(max: displayLength, offset: displayStart)
        if(tableSearching || columnSearchParams) {
            resultMap.filteredCount = customQueryAggregator.count() // resultMap.getTotalCount() does not work.
        } else {
            // This way eliminates the query that counts the filtered results when not needed.
            resultMap.filteredCount = resultMap.count
        }
        resultMap.items = queryResults
        resultMap
    }

    /**
     * Gets AJAX data for display in the DataTable that uses server-side data.
     */
    def getDynamicData(
            tableDefinition,
            tableSearch,
            columnSearchParams,
            sortIndex,
            sortDir,
            displayLength,
            displayStart,
            dynamicSqlRestriction
    ) {
        def resultMap = [:]
        def dataTablesConfig = grailsApplication.config.grails.plugin.datatables
        def dc = grailsApplication.getDomainClass(tableDefinition.domainClass)
        String tableName = tableDefinition.domainClass.substring(1)


        // Add column aliases. These are necessary for criteria that apply to association fields.
        String _columns = tableDefinition.columns*.name.join(',');

        // Add table sql restriction.
        String _sqlRestriction = null;
        if(tableDefinition.sqlRestriction) {
            _sqlRestriction=tableDefinition.sqlRestriction
        }

        // Add column criteria.


        // Add dynamic SQL Restriction (result of sqlRestrictionFunction).
        String _dynamicSqlRestriction = null
        if(dynamicSqlRestriction) {
            _dynamicSqlRestriction = dynamicSqlRestriction;
        }
        def restrictions = [_sqlRestriction,_dynamicSqlRestriction];
        restrictions.removeAll([null]);
        String _whereClause = restrictions.join(' and ');
        if (_whereClause != ''){
            _whereClause = 'where ' + _whereClause
        }
        // Count the total unfiltered items. We still apply the sqlRestriction and column criteria, just not the search filter.
        String _sql = "Select count(*) from ${tableName} ${_whereClause}";
        Session session = (Session)sessionFactory.openSession();
        //NativeQuery query = session.createSQLQuery('''select mac,host_name,rpcc_rpd_name from rpd,rpd_name_sync  where rpd.mac = replace(rpd_name_sync.rpd_mac_address,'.','') and (rpd.ccap_id is null or rpd.ccap_id <> rpd_name_sync.host_name or  rpd.name is null or rpd.name <> rpd_name_sync.rpcc_rpd_name )''');
        NativeQuery query = session.createSQLQuery(_sql);
        def result = query.list();

        resultMap.count = result[0]

        // Add sorting. The sortIndex refers to columns included in the table.
        String _orderBy = ''
        def columnsIncludedInTable = tableDefinition.getColumnsForTable()
        if(null != sortIndex) {
            def column = columnsIncludedInTable[sortIndex]
            // Ensure we are not trying to sort on a column that is not orderable.
            if(column.orderable) {
                _orderBy += column.alias + ' ' + ("asc" == sortDir)? 'asc': 'desc';
            }
        }
        _orderBy += 'order by ' + (_orderBy == '')? '': "${_orderBy},"+' id'

        if(!displayLength) {
            displayLength = dataTablesConfig.reportSizeLimit
            if(!displayLength) {
                displayLength = 15002
            }
            displayStart = 0
        }

        // Add general search criteria. Only columns included in the table are searchable. The search string is tokenized, and each token is searched against each column.
        def tableSearching = (tableSearch && tableDefinition.searching)
        List tblSrch = []
        if(tableSearching) {
            def tableSearchParams = tableSearch.tokenize()
            tableSearchParams.eachWithIndex { tableSearchParam, index ->
                columnsIncludedInTable.each { column ->
                    if(column.searchable) {
                        tblSrch << [column, tableSearchParam, index]
                    }
                }
            }
        }

        // Add column search criteria. The search string is tokenized, and each token is searched against the column.
        List colmnSrch = []
        if(columnSearchParams) {
            columnSearchParams.keySet().each { column ->
                columnSearchParams[column]?.tokenize().each {
                    colmnSrch << [column, it]
                }
            }
        }

        // Run query and add results to resultMap.
        _sql = "Select ${_columns} from ${tableName} ${_whereClause} ";
        def queryResults = customQueryAggregator.list(max: displayLength, offset: displayStart)
        if(tableSearching || columnSearchParams) {
            resultMap.filteredCount = customQueryAggregator.count() // resultMap.getTotalCount() does not work.
        } else {
            // This way eliminates the query that counts the filtered results when not needed.
            resultMap.filteredCount = resultMap.count
        }
        resultMap.items = queryResults
        resultMap
    }

    /**
     * Add values to a map, separating out parts.of.the.key and adding them as nested maps.
     * @param configMap
     * @param key
     * @param value
     */
    def addConfigurationParameter(Map configMap, def key, def value) {
        def keyParts = key.tokenize(".")
        def partCount = keyParts.size()
        def middleMap = configMap
        keyParts.eachWithIndex { keyPart, index ->
            if(index < partCount - 1) {
                def bottomMap = middleMap[keyPart]
                if(!bottomMap) {
                    bottomMap = [:]
                    middleMap[keyPart] = bottomMap
                }
                middleMap = bottomMap
            } else {
                middleMap[keyPart] = value
            }
        }
    }

    /**
     * Like creating JSON, only we only put quotes around Strings.
     * @param map
     */
    def serializeJavaScriptObject(def object, def buffer) {
        def firstItem = true
        if(null == object) {
            buffer << "null"
        } else if(object instanceof Map) {
            buffer << "{"
            object.each { key, value ->
                // Write a comma between items.
                if(firstItem) {
                    firstItem = false
                } else {
                    buffer << ","
                }
                // Write the key.
                buffer << "\"" << Helper.removeSpaces(key) << "\":"
                serializeJavaScriptObject(value, buffer)
            }
            buffer << "}"
        } else if(object instanceof List) {
            buffer << "["
            object.each { value ->
                // Write a comma between items.
                if(firstItem) {
                    firstItem = false
                } else {
                    buffer << ","
                }
                serializeJavaScriptObject(value, buffer)
            }
            buffer << "]"
        } else {
            if(
                    object instanceof Boolean   ||
                    object instanceof Number    ||
                    object.isNumber()
            ) {
                buffer << Helper.castSafe(object)
            } else {
                def stringValue = object.toString()
                if(
                        stringValue in ["true", "false"] ||
                        (stringValue.startsWith("function")  && stringValue.endsWith("}"))
                ) {
                    buffer <<  Helper.castSafe(stringValue)
                } else {
                    buffer << "'" <<  Helper.castSafe(stringValue) << "'"
                }
            }
        }
    }

    private def addColumnSearch(def column, def customQueryAggregator, def searchParam, def orGroup = null) {
        def criterion = column.getSearchCriterion(customQueryAggregator, searchParam)
        customQueryAggregator.addCriterion(criterion, orGroup)
    }

    /**
     * Gets a SQL expression for a Hibernate SqlCriterion for searching a date field.
     * @param columnName The name of the date column to be searched.
     * @param dateFormat The format the date is presented in.
     * @param dateFormatLength The length of the date format for SQL Server.
     * @return A SQL string in the form that is compatible with the database, e.g. "TO_CHAR(${columnName}, '${dateFormat}') like ?"
     */
    protected def getDateSearchSql(def columnName, def dateFormat, def dateFormatLength) {
        def dialect = sessionFactory.getDialect()

        if(dialect instanceof org.hibernate.dialect.MySQLDialect) {
            return "DATE_FORMAT(${columnName}, '${dateFormat}') like ?"
        }
        if(dialect instanceof org.hibernate.dialect.SQLServerDialect) {
            return "CONVERT(VARCHAR(${dateFormatLength}), ${columnName}, ${dateFormat}) like ?"
            // E.g. CONVERT(VARCHAR(10), myDateColumn, 120)
        }
        if(dialect instanceof org.hibernate.dialect.SybaseDialect) {
            return "DATEFORMAT(${columnName}, '${dateFormat}') like ?"
        }

        // Default for H2, Oracle, PostgreSQL, Informix, DB2 etc..
        return "TO_CHAR(${columnName}, '${dateFormat}') like ?"
    }

    /**
     * Get the date format from the configuration file, otherwise return the default.
     * @return
     */
    def getDateFormat() {
        grailsApplication.config.grails.plugin.datatables.dateFormat ?: "MM/dd/yyyy"
    }

    /**
     * Get the database date format from the configuration file, otherwise return the default.
     * @return
     */
    def getDbDateFormat() {
        if(grailsApplication.config.containsKey("grails.plugin.datatables.dbDateFormat")) {
            return grailsApplication.config.grails.plugin.datatables.dbDateFormat
        } else if(sessionFactory.getDialect() instanceof org.hibernate.dialect.SQLServerDialect) {
            return 101
        }
        return "MM/DD/YYYY"
    }

    /**
     * Get the date format from the configuration file, otherwise return the default.
     * @return
     */
    def getDateFormatLength() {
        grailsApplication.config.grails.plugin.datatables.dateFormatLength ?: 10
    }

    /**
     * Get the dateTime format from the configuration file, otherwise return the default.
     * @return
     */
    def getDateTimeFormat() {
        //PropertySourcesConfig
        //PropertySourcesConfig cfg = grailsApplication.config.containsKey()

        if(grailsApplication.config.containsKey("grails.plugin.datatables.dateTimeFormat")) {
            return grailsApplication.config.grails.plugin.datatables.dateTimeFormat
        } else if(sessionFactory.getDialect() instanceof org.hibernate.dialect.SQLServerDialect) {
            return "MMM dd yyyy HH:mma"
        }
        return "MM/dd/yyyy HH:mm:ss"
    }

    def getDbDateTimeFormat() {
        if(grailsApplication.config.containsKey("grails.plugin.datatables.dbDateTimeFormat")) {
            return grailsApplication.config.grails.plugin.datatables.dbDateTimeFormat
        } else if(sessionFactory.getDialect() instanceof org.hibernate.dialect.SQLServerDialect) {
            return 100
        }
        return "MM/DD/YYYY HH24:MI:SS"
    }

    def getDateTimeFormatLength() {
        grailsApplication.config.grails.plugin.datatables.dateFormatLength ?: 19
    }

    /**
     * Get the time format from the configuration file, otherwise return the default.
     * @return
     */
    def getTimeFormat() {
        grailsApplication.config.grails.plugin.datatables.timeFormat ?: "HH:mm:ss"
    }

    def getDbTimeFormat() {
        if(grailsApplication.config.containsKey("grails.plugin.datatables.dbTimeFormat")) {
            return grailsApplication.config.grails.plugin.datatables.dbTimeFormat
        } else if(sessionFactory.getDialect() instanceof org.hibernate.dialect.SQLServerDialect) {
            return 108
        }
        return "HH24:MI:SS"
    }

    def getTimeFormatLength() {
        grailsApplication.config.grails.plugin.datatables.dateFormatLength ?: 8
    }
}
