<g:set var="dataTablesService" bean="dataTablesService"/>
<div>
    <style type="text/css" scoped>
        table.metadatatable td {
            padding-left: 0px;
            padding-right: 10px;
        }
        div.datatablesMetadata {
            border: 3px #1a6699 solid;
            margin-bottom: 2px;
            border-radius: 10px;
            font-size: 11px;
            background: #9ce;
        }
        div.datatablesMetadata h3 {
            margin-top: 2px;
            margin-bottom: 2px;
            padding: 0;
        }
        div.datatablesMetadata h4 {
            margin-top: 2px;
            margin-bottom: 2px;
        }
        div.datatablesMetadataHeading {
            margin: 0;
            padding: 1px;
            text-align: center;
        }
        div.datatablesMetadataBody {
            background: white;
            padding: 5px;
            border-top: 3px #1a6699 solid;
            border-bottom: 3px #1a6699 solid;
        }
        div.datatablesMetadata h3 {
            margin: 0;
        }
        div.datatablesWatermark {
            position: absolute;
            left: 0;
            right: 0;
            margin-top: 800px;
            color: #1a6699;
            opacity: 0.15;
            font-size: 100px;
            font-weight: bold;
            z-index: 1000;
            transform: rotate(45deg);
            text-align: center;
        }
    </style>
    <div class="datatablesMetadata">
        <div class="datatablesMetadataHeading">
            <h3>DataTable Metadata for Table <i>${tableDefinition.name}</i></h3>
        </div>
        <div class="datatablesMetadataBody">
            <div class="datatablesWatermark">DataTable Metadata</div>
            %{--Configuration Settings--}%
            <h3>Configuration Options</h3>
            <table class="metadatatable">
                <g:each in="${["dateFormat", "dateTimeFormat", "timeFormat", "dbDateFormat", "dbDateTimeFormat", "dbTimeFormat", "dateFormatLength", "dateTimeFormatLength", "timeFormatLength"]}">
                    <tr><td><a href="https://bitbucket.org/ben-wilson/grailsdatatables/wiki/${it}">grails.plugin.datatables.${it}</a>:</td><td>${dataTablesService[it]}</td></tr>
                </g:each>
                <g:each in="${["functionsTemplate", "functionsTemplatePlugin", "jQueryUI", "reportService", "reportSizeLimit", "tableDefaults"]}">
                    <tr><td><a href="https://bitbucket.org/ben-wilson/grailsdatatables/wiki/${it}">grails.plugin.datatables.${it}</a>:</td><td>${dataTablesConfig[it]}</td></tr>
                </g:each>
            </table><br/>

            %{--Table Metadata--}%
            <h3>Table Options</h3>
            <table class="metadatatable">
                <g:each in="${["adjustColumnWidths", "ajaxParams", "columnSearching", "controller", "dataAction", "dataItems", "deferInitialization", "domainClass", "errorFunction", "headingParams", "hideWhenEmpty", "name", "reportAction", "reportButton", "reportFunction", "reportingEnabled", "resetButton", "searchButton", "searching", "serverDataLoad", "serverSide", "showMetadata", "sqlRestrictionFunction", "tableSearching"]}">
                    <tr><td><a href="https://bitbucket.org/ben-wilson/grailsdatatables/wiki/${it == "name" ? "tableName" : it}">${it}</a>:</td><td>${tableDefinition[it]}</td></tr>
                </g:each>
                <tr><td><a href='http://datatables.net/reference/option/ordering'>ordering</a>:</td><td>${(attr.ordering ?: "true") }</td></tr>
                <tr><td><a href='http://datatables.net/reference/option/info'>info</a>:</td><td>${(attr.info ?: "true")}</td></tr>
            </table><br/>

            %{--Table Messages--}%
            <h3>Table Messages</h3>
            <table class="metadatatable">
                <tr>
                    <td><h4>Description</h4></td>
                    <td><h4>messages.properties key</h4></td>
                    <td><h4>Value</h4></td>
                </tr>
                <tr><td>Empty Table Heading</td><td>datatables.${tableDefinition.name}.heading.empty</td><td><g:message code="datatables.${tableDefinition.name}.heading.empty" default=""/></td></tr>
                <tr><td>Singular Table Heading</td><td>datatables.${tableDefinition.name}.heading.singular</td><td><g:message code="datatables.${tableDefinition.name}.heading.singular" default=""/></td></tr>
                <tr><td>Plural Table Heading</td><td>datatables.${tableDefinition.name}.heading.plural</td><td><g:message code="datatables.${tableDefinition.name}.heading.plural" default=""/></td></tr>
                <g:each status="index" var="column" in="${tableDefinition.columns}">
                    <tr>
                        <td>Column ${index} Heading</td>
                        <td>
                            <g:if test="${column.headingKey}">${column.headingKey}</g:if>
                            <g:else>datatables.${tableDefinition.name}.${column.name}</g:else>
                        </td>
                        <td><g:message code="datatables.${tableDefinition.name}.${column.name}" default=""/></td>
                    </tr>
                </g:each>
                <tr><td>Report Filename</td><td>datatables.${tableDefinition.name}.reportFileName</td><td><g:message code="datatables.${tableDefinition.name}.reportFileName" default=""/></td></tr>
            </table><br/>

            %{--Table Messages--}%
            <h3>Language Messages</h3>
            <table class="metadatatable">
                <tr>
                    <td><h4>messages.properties key</h4></td>
                    <td><h4>Value</h4></td>
                </tr>
                <g:each in="${["emptyTable", "info", "infoEmpty", "infoFiltered", "infoPostFix", "thousands", "lengthMenu", "loadingRecords", "processing", "search", "zeroRecords", "paginate.first", "paginate.last", "paginate.next", "paginate.previous", "aria.sortAscending", "aria.sortDescending", "searchButton", "resetButton", "loading", "downloadReport.title", "downloadReport.text"]}">
                    <tr><td>datatables.language.${it} </td><td><g:message code="datatables.language.${it}" default=""/></td></tr>
                </g:each>
            </table><br/>

            %{--Columns--}%
            <h3>Table Columns</h3>
            <table class="metadatatable">
                <tr>
                    <td><h4>name:</h4></td>
                    <td><h4>heading</h4></td>
                    <td><h4>dataType</h4></td>
                    <td><h4>criteria</h4></td>
                    <td><h4>visible</h4></td>
                    <td><h4>include</h4></td>
                    <td><h4>searchable</h4></td>
                    <td><h4>orderable</h4></td>
                </tr>
                <g:each var="column" in="${tableDefinition.columns}">
                    <tr>
                        <td>${column.name}</td>
                        <td>${column.heading}</td>
                        <td>${column.dataType}</td>
                        <td>${column.criteria}</td>
                        <td>${column.visible}</td>
                        <td>${column.include ?: "all"}</td>
                        <td>${column.searchable}</td>
                        <td>${column.orderable}</td>
                    </tr>
                </g:each>
            </table><br/>

            %{--Column Display Details--}%
            <h3>Table Column Display Details</h3>
            <table class="metadatatable">
                <tr>
                    <td><h4>name:</h4></td>
                    <td><h4>dataFunction</h4></td>
                    <td><h4>link</h4></td>
                    <td><h4>linkIdField</h4></td>
                    <td><h4>trueText</h4></td>
                    <td><h4>falseText</h4></td>
                </tr>
                <g:each var="column" in="${tableDefinition.columns}">
                    <tr>
                        <td>${column.name}</td>
                        <td>${column.dataFunction ? "Yes" : ""}</td>
                        <td>${column.link}</td>
                        <td>${column.linkIdField}</td>
                        <td>${column.trueText}</td>
                        <td>${column.falseText}</td>
                    </tr>
                </g:each>
            </table>
        </div>
        <div class="datatablesMetadataHeading">
            <h3>DataTable Metadata for Table <i>${tableDefinition.name}</i></h3>
        </div>
    </div>
</div>
