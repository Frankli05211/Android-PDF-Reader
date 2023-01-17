package net.codebot.pdfviewer

import java.io.Serializable

class pageClass(page: Int): Serializable {
    var currPage: Int

    init {
        currPage = page
    }
}