$.fn.dataTableExt.oPagination.full_numbers_callback = {
	/*
	 * Function: oPagination.full_numbers_callback.fnInit
	 * Purpose:  Initialise dom elements required for pagination with a list of the pages
	 * Returns:  -
	 * Inputs:   object:oSettings - dataTables settings object
	 *           node:nPaging - the DIV which contains this pagination control
	 *           function:fnCallbackDraw - draw function which must be called on update
	 */
	"fnInit": function(oSettings, nPaging, fnCallbackDraw) {
		var oLang = oSettings.oLanguage.oPaginate;
		var oClasses = oSettings.oClasses;
		var fnChangePage = function(e) {
			if(oSettings.oApi._fnPageChange(oSettings, e.data.action)) {
				fnCallbackDraw(oSettings);
			}
		};
		var fnClickHandler = function(e) {
			return fnPaginationClickHandler(e, fnChangePage, oSettings);
		};

		$(nPaging).append(
			'<a test="1" tabindex="'+oSettings.iTabIndex+'" class="'+oClasses.sPageButton+" "+oClasses.sPageFirst+'">'+oLang.sFirst+'</a>'+
			'<a test="2" tabindex="'+oSettings.iTabIndex+'" class="'+oClasses.sPageButton+" "+oClasses.sPagePrevious+'">'+oLang.sPrevious+'</a>'+
			'<span></span>'+
			'<a test="3" tabindex="'+oSettings.iTabIndex+'" class="'+oClasses.sPageButton+" "+oClasses.sPageNext+'">'+oLang.sNext+'</a>'+
			'<a test="4" tabindex="'+oSettings.iTabIndex+'" class="'+oClasses.sPageButton+" "+oClasses.sPageLast+'">'+oLang.sLast+'</a>'
		);
		var els = $('a', nPaging);
		var nFirst = els[0],
			nPrev = els[1],
			nNext = els[2],
			nLast = els[3];
		
		oSettings.oApi._fnBindAction(nFirst, {action: "first"},    fnClickHandler);
		oSettings.oApi._fnBindAction(nPrev,  {action: "previous"}, fnClickHandler);
		oSettings.oApi._fnBindAction(nNext,  {action: "next"},     fnClickHandler);
		oSettings.oApi._fnBindAction(nLast,  {action: "last"},     fnClickHandler);
		
		/* ID the first elements only */
		if(!oSettings.aanFeatures.p) {
			nPaging.id = oSettings.sTableId+'_paginate';
			nFirst.id =oSettings.sTableId+'_first';
			nPrev.id =oSettings.sTableId+'_previous';
			nNext.id =oSettings.sTableId+'_next';
			nLast.id =oSettings.sTableId+'_last';
		}
	},
	
	/*
	 * Function: oPagination.full_numbers_callback.fnUpdate
	 * Purpose:  Update the list of page buttons shows
	 * Returns:  -
	 * Inputs:   object:oSettings - dataTables settings object
	 *           function:fnCallbackDraw - draw function to call on page change
	 */
	"fnUpdate": function(oSettings, fnCallbackDraw) {
		if(!oSettings.aanFeatures.p) {
			return;
		}
		
		//var iPageCount = jQuery.fn.dataTable.ext.oPagination.iFullNumbersShowPages;
		var iPageCount = 5;
		var iPageCountHalf = Math.floor(iPageCount / 2);
		var iPages = Math.ceil((oSettings.fnRecordsDisplay()) / oSettings._iDisplayLength);
		var iCurrentPage = Math.ceil(oSettings._iDisplayStart / oSettings._iDisplayLength) + 1;
		var sList = "";
		var iStartButton, iEndButton, i, iLen;
		var oClasses = oSettings.oClasses;
		var anButtons, anStatic, nPaginateList, nNode;
		var an = oSettings.aanFeatures.p;
		var fnChangePage = function(e) {
			oSettings.oApi._fnPageChange(oSettings, e.data.page);
			fnCallbackDraw(oSettings);
			e.preventDefault();
		};
		var fnBind = function(j) {
			oSettings.oApi._fnBindAction(this, {"page": j+iStartButton-1}, function(e) {
				/* Use the information in the element to jump to the required page */
				return fnPaginationClickHandler(e, fnChangePage, oSettings);
			});
		};
		
		/* Pages calculation */
		if(oSettings._iDisplayLength === -1) {
			iStartButton = 1;
			iEndButton = 1;
			iCurrentPage = 1;
		}
		else if(iPages < iPageCount) {
			iStartButton = 1;
			iEndButton = iPages;
		}
		else if(iCurrentPage <= iPageCountHalf) {
			iStartButton = 1;
			iEndButton = iPageCount;
		}
		else if(iCurrentPage >= (iPages - iPageCountHalf)) {
			iStartButton = iPages - iPageCount + 1;
			iEndButton = iPages;
		}
		else {
			iStartButton = iCurrentPage - Math.ceil(iPageCount / 2) + 1;
			iEndButton = iStartButton + iPageCount - 1;
		}

		
		/* Build the dynamic list */
		for(i=iStartButton ; i<=iEndButton ; i++) {
			sList += (iCurrentPage !== i) ?
				'<a tabindex="'+oSettings.iTabIndex+'" class="'+oClasses.sPageButton+'">'+oSettings.fnFormatNumber(i)+'</a>' :
				'<a tabindex="'+oSettings.iTabIndex+'" class="'+oClasses.sPageButton+" "+oClasses.sPageButtonDisabled+'">'+oSettings.fnFormatNumber(i)+'</a>';
		}
		
		/* Loop over each instance of the pager */
		for(i=0, iLen=an.length ; i<iLen ; i++) {
			nNode = an[i];
			if(!nNode.hasChildNodes()) {
				continue;
			}
			
			/* Build up the dynamic list first - html and listeners */
			$('span:eq(0)', nNode)
				.html(sList)
				.children('a').each(fnBind);
			
			/* Update the permanent button's classes */
			anButtons = nNode.getElementsByTagName('a');
			anStatic = [
				anButtons[0], anButtons[1], 
				anButtons[anButtons.length-2], anButtons[anButtons.length-1]
			];

			// $(anStatic).removeClass(oClasses.sPageButton+" "+oClasses.sPageButtonActive+" "+oClasses.sPageButtonStaticDisabled);
			$(anStatic).removeClass(oClasses.sPageButtonDisabled);

            if(iCurrentPage==1) {
			    $([anStatic[0], anStatic[1]]).addClass(oClasses.sPageButtonDisabled);
            }
            if(iPages===0 || iCurrentPage===iPages || oSettings._iDisplayLength===-1) {
                $([anStatic[2], anStatic[3]]).addClass(oClasses.sPageButtonDisabled)
            }
		}
	}
};

var fnPaginationClickHandler = function(e, fnChangePage, oSettings) {
	var numSelected = $(oSettings.oInstance.fnGetNodes()).find('input:checked').length;
	if(numSelected > 0) {
		var msg = numSelected > 1 ?
				"You have selected some items in the table. If you proceed, your selections will be lost." :
				"You have selected an item in the table. If you proceed, your selection will be lost."
		var flash = numSelected > 1 ?
				"Selections Will Be Lost" :
				"Selection Will Be Lost"
		jqConfirm(
				msg, 
				function() {
					fnChangePage(e);
				}, 
				"Proceed", 
				null, 
				"Stay On Page", 
				flash, 
				"warning"
		);
		return false;
	} else {
		fnChangePage(e);
	}
};