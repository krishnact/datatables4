function(settings) {
    <g:if test="${attr.drawCallback}">
        %{--Call drawCallback function provided in attributes.--}%
        (${attr.drawCallback})(settings);
    </g:if>
    var pageInfo = this.api().page.info();
    if(Math.ceil((pageInfo.recordsDisplay) / pageInfo.length) > 1)  {
        $("#${tableDefinition.name}_paginate.dataTables_paginate").css("display", "block");
    } else {
        $("#${tableDefinition.name}_paginate.dataTables_paginate").css("display", "none");
    }
    if(pageInfo.recordsDisplay > 10) {
        $("#${tableDefinition.name}_length.dataTables_length").css("display", "block");
    } else {
        $("#${tableDefinition.name}_length.dataTables_length").css("display", "none");
    }

    <g:if test="${!tableDefinition.hasLocalData}">
        if(${tableDefinition.name} != null) {
        %{--Execute the following only after the table has been initialized.--}%
    </g:if>

    %{--Select the correct heading according to the number of rows in the table.--}%
    var numRows = pageInfo.recordsTotal;
    $("#${tableDefinition.name}HeadingEmpty").toggle(0 == numRows);
    $("#${tableDefinition.name}HeadingSingular").toggle(1 == numRows);
    $("#${tableDefinition.name}HeadingPlural").toggle(1 < numRows);

    %{--Hide or show the table according to whether it contains any rows.--}%
    <g:if test="${tableDefinition.hideWhenEmpty}">
        $("#${tableDefinition.name}Container").toggle(0 < numRows);
    </g:if>

    %{--Write callback to clear select all checkbox.--}%
    <g:if test="${!tableDefinition.hasSelectAll}">
        $("#${tableDefinition.name}_check_all").attr("checked", false);
    </g:if>

    %{--Write callback to adjust column search widths.--}%
    <g:if test="${tableDefinition.columnSearching}">
        adjust${tableDefinition.name}ColumnSearchWidths();
    </g:if>

    %{--Update the Record Count container.--}%
    $(".${tableDefinition.name}Count").html(numRows);

    <g:if test="${!tableDefinition.hasLocalData}">
        }
    </g:if>
}