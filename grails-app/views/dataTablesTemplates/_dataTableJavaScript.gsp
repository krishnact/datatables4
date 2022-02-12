%{--Javascript Template for the DataTable.--}%
<asset:script type="text/javascript">
    <!-- These scripts are executed at the end only when you use else thy are not included-->
    %{--Write the table initialization script.--}%
    var ${name} = null;
    <g:if test="${tableDefinition.deferInitialization}">
        %{--This creates a function named initializeTableName (where TableName is the name of the table) that can be used to initialize the datatable--}%
        %{--sometime after the page is loaded.--}%
        function initialize${name[0].toUpperCase()}${name[1..-1]}() {
            <dt:writeInitializationScript attr="${attr}" tableDefinition="${tableDefinition}" dataTablesConfig="${dataTablesConfig}"/>
        }
    </g:if>


    %{--Beginning of document ready section.--}%
    $(function() {

        <g:if test="${!tableDefinition.deferInitialization}">
            <dt:writeInitializationScript attr="${attr}" tableDefinition="${tableDefinition}" dataTablesConfig="${dataTablesConfig}"/>
        </g:if>

        %{--Add Search and Reset buttons to DOM.--}%
        var searchDiv = $("#${name}Search");
        searchDiv.addClass("searchGroup");
        searchDiv.html(' \
        <g:if test="${tableDefinition.searchButton}"> \
            <input id="${name}SearchBox"/> \
            <span class="buttons"> \
                <button id="${name}SearchButton" class="datatablesButtons" type="button"> \
                    <g:message code="datatables.language.searchButton" default="Search"/> \
                </button> \
            </span class="buttons"> \
        </g:if> \
        <g:if test="${tableDefinition.resetButton}"> \
            <span class="buttons"> \
                <button id="${name}ResetButton" class="datatablesButtons" type="button"> \
                    <g:message code="datatables.language.resetButton" default="Reset"/> \
                </button> \
            </span class="buttons"> \
        </g:if> \
        ');

        %{--Add actions to Search and Reset buttons.--}%
        <g:if test="${tableDefinition.searchButton}">
            $("#${name}SearchBox").keypress(function(e){if(e.which == 13){${name}.search($("#${name}SearchBox").val()).draw();}});
            $("#${name}SearchButton").click(function(){${name}.search($("#${name}SearchBox").val()).draw();});
        </g:if>
        <g:if test="${tableDefinition.resetButton}">
            $("#${name}ResetButton").click(function() {
                <g:if test="${tableDefinition.searchButton}">
                    $("#${name}SearchBox").val("");
                </g:if>
                ${name}.search("").draw();
            });
        </g:if>

        %{--Restore state for column search input boxes.--}%
        <g:if test="${tableDefinition.columnSearching && tableDefinition.stateSave}">
            var state = ${name}.state.loaded();
            if(state) {
                ${name}.columns().eq(0).each(function(colIdx) {
                    var colSearch = state.columns[colIdx].search;
                    if(colSearch.search) {
                        $('input', ${name}.column(colIdx).footer()).val(colSearch.search);
                    }
                });
            }
        </g:if>

        %{--Add column searching function.--}%
        <g:if test="${tableDefinition.columnSearching}">
            ${name}.columns().every(function() {
                var that = this;
                $('input', this.footer()).on('keyup change', function() {
                    if(that.search() !== this.value) {
                        that.search(this.value).draw();
                    }
                });
            });

            %{--Adjust search box widths whenever column widths are adjusted.--}%
            ${name}.on('column-sizing.dt', function(e, settings) {
                adjust${name}ColumnSearchWidths();
            });
        </g:if>

        %{--Write the report URL function.--}%
        <g:if test="${tableDefinition.reportingEnabled}">
            function get${name}ReportUrl() {
                <g:if test="${tableDefinition.controller || tableDefinition.reportAction}">
                    var reportUrl = "<g:createLink controller="${tableDefinition.controller}" action="${tableDefinition.reportAction ?: "getReport"}" params="${tableDefinition.ajaxParams}"/>"
                </g:if><g:else>
                    var reportUrl = "<g:createLink controller="dataTables" action="getReport" params="${tableDefinition.ajaxParams}"/>"
                </g:else>

                var order = ${name}.order();
                <g:if test="${tableDefinition.columnSearching}">
                    var columnSearches = "";
                    ${name}.columns().every(function() {
                        columnSearches += '&columns[' + this.index() + '][search][value]=' + this.search();
                    });
                </g:if>
                return reportUrl + "&search=" + ${name}.search() + "&sortIndex=" + order[0][0] + "&sortDir=" + order[0][1] +
                    <g:if test="${tableDefinition.columnSearching}">
                            columnSearches +
                    </g:if>
                    <g:if test="${tableDefinition.sqlRestrictionFunction}">
                        "&dynamicSqlRestriction=" + ${tableDefinition.sqlRestrictionFunction}() +
                    </g:if>
                    "&report=${name}";
            }
        </g:if>

        %{--Write the script for the report button.--}%
        <g:if test="${tableDefinition.reportButton}">
            var reportDiv = $("#${name}Report");
            reportDiv.addClass("reportGroup");
            reportDiv.html(' \
                <button id="${name}ReportButton" class="datatablesButtons" type="button" title="${g.message(code: "datatables.language.downloadReport.title", default: "")}"> \
                    <g:message code="datatables.language.downloadReport.text" default="Download Report"/> \
                </button> \
            ');
            $("#${name}ReportButton").click(function(clickEvent){
                var numRecords = ${name}.page.info().recordsDisplay;
                <g:if test="${tableDefinition.reportFunction}">
                    ${tableDefinition.reportFunction}(clickEvent, numRecords, get${name}ReportUrl());
                </g:if><g:else>
                    %{--Stop the browser from following.--}%
                    clickEvent.preventDefault();
                    window.location.href = get${name}ReportUrl();
                </g:else>
            });
        </g:if>

        %{--If using jQueryUI, style the length dropdown. It's necessary to rewire the event from the jQueryUI selectmenu back to the original html one.--}%
        %{--Also, style the buttons.--}%
        <g:if test="${dataTablesConfig.jQueryUI}">
            var lengthSelect=$("#${name}_length select");
            lengthSelect.selectmenu({
                change:function() {
                    lengthSelect.change();
                }
            });
            $("button.datatablesButtons").button();
        </g:if>

        %{--If there is an error function, wire it up.--}%
        <g:if test="${tableDefinition.errorFunction}">
            $.fn.dataTable.ext.errMode = "none";
            ${name}.on("error.dt", ${tableDefinition.errorFunction});
        </g:if>

        %{--Write the select all function.--}%
        <g:if test="${tableDefinition.hasSelectAll}">
            $("#${name}_check_all").click(function () {
                $("input", ${name}.rows().nodes()).prop("checked", this.checked);
            });
        </g:if>

    %{--End of document ready section.--}%
    });

    %{--Write function for adjusting column search input widths.--}%
    <g:if test="${tableDefinition.columnSearching}">
        function adjust${name}ColumnSearchWidths() {
            $("table#${name} tfoot tr td input.columnSearch").each(function() {
                var parentWidth = $(this.parentNode).width();
                if(parentWidth > 4) {
                    $(this).width(parentWidth - 4);
                }
            });
        }
    </g:if>

    %{--Write function for adjusting column widths.--}%
    function adjust${name}ColumnWidths() {
        ${name}.columns.adjust();
    }

    %{--Render the custom functions template, if it exists and has not already been rendered on this page.--}%
    <g:if test="${dataTablesConfig.functionsTemplate && !pageScope.functionsTemplateRendered}">
        <g:render template="${dataTablesConfig.functionsTemplate}"
                  plugin="${dataTablesConfig.functionsTemplatePlugin}"
                  model="${[out: out, dataTablesConfig: dataTablesConfig, tableDefinition: tableDefinition, attr: attr]}"/>
    </g:if>

</asset:script>