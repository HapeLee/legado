package io.legato.kazusa.data.entities

import io.legato.kazusa.help.RuleBigDataHelp
import io.legato.kazusa.model.analyzeRule.RuleDataInterface
import io.legato.kazusa.utils.GSON
import io.legato.kazusa.utils.splitNotBlank

interface BaseBook : RuleDataInterface {
    var name: String
    var author: String
    var bookUrl: String
    var kind: String?
    var wordCount: String?
    var variable: String?

    var infoHtml: String?
    var tocHtml: String?

    override fun putVariable(key: String, value: String?): Boolean {
        if (super.putVariable(key, value)) {
            variable = GSON.toJson(variableMap)
        }
        return true
    }

    fun putCustomVariable(value: String?) {
        putVariable("custom", value)
    }

    fun getCustomVariable(): String {
        return getVariable("custom")
    }

    override fun putBigVariable(key: String, value: String?) {
        RuleBigDataHelp.putBookVariable(bookUrl, key, value)
    }

    override fun getBigVariable(key: String): String? {
        return RuleBigDataHelp.getBookVariable(bookUrl, key)
    }

    fun getKindList(): List<String> {
        val kindList = arrayListOf<String>()
        wordCount?.let {
            if (it.isNotBlank()) kindList.add(it)
        }
        kind?.let {
            val kinds = it.splitNotBlank(",", "\n")
            kindList.addAll(kinds)
        }
        return kindList
    }
}