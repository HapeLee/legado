package io.legato.kazusa.data.entities

import io.legato.kazusa.help.RuleBigDataHelp
import io.legato.kazusa.model.analyzeRule.RuleDataInterface
import io.legato.kazusa.utils.GSON

interface BaseRssArticle : RuleDataInterface {

    var origin: String
    var link: String

    var variable: String?

    override fun putVariable(key: String, value: String?): Boolean {
        if (super.putVariable(key, value)) {
            variable = GSON.toJson(variableMap)
        }
        return true
    }

    override fun putBigVariable(key: String, value: String?) {
        RuleBigDataHelp.putRssVariable(origin, link, key, value)
    }

    override fun getBigVariable(key: String): String? {
        return RuleBigDataHelp.getRssVariable(origin, link, key)
    }

}