package datatables4

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Helper {
    public static final DateTimeFormatter metricFileNameFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.of("UTC"));
    public static String formatNow(){
        return metricFileNameFormatter.format(Instant.ofEpochMilli(System.currentTimeMillis()));
    }
    public static def getButtons_1(String exportFileBase){
        def ret = [
                [
                        'text': 'Copy',
                        'className': 'btn btn-default ml-1 mr-1 cannedSearchButton',
                        'extend': 'copy'
                ],
                [
                        'text': 'Download CSV',
                        'className': 'btn btn-default ml-1 mr-1 cannedSearchButton',
                        'extend': 'csv',
                        'filename': "${exportFileBase}." + formatNow()
                ],
                [
                        'text': 'Columns',
                        'className': 'btn btn-default ml-1 mr-1 cannedSearchButton',
                        'extend': 'colvis'
                ]
        ]
        return ret;
    }

    /**
     * Although this function does not use the argument passed to it, we have kept signature same as previous function
     * so that they can be switched without changing much code.
     * @param exportFileBase
     * @return
     */
    public static def getButtons_2(String exportFileBase){
        def ret = [
                [
                        'text': 'Copy',
                        'className': 'btn btn-default ml-1 mr-1 cannedSearchButton',
                        'extend': 'copy'
                ],
                [
                        'text': 'Columns',
                        'className': 'btn btn-default ml-1 mr-1 cannedSearchButton',
                        'extend': 'colvis'
                ]
        ]
        return ret;
    }
    public static String getDom_1(){
        return  '<"row"<"col-md-4"l><"myButtons"B><"col-md-4"f>r>t<"F"ip>'
    }

}
