package datatables4

class TestTagTagLib {
    static defaultEncodeAs = [taglib:'html']
    static namespace = "dt4"
    //static encodeAsForTags = [tagName: [taglib:'html'], otherTagName: [taglib:'none']]

    def datatable = { attr, body ->

        out << """<span> This is a test tag</span> """
    }
}
