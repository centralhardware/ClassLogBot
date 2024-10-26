package me.centralhardware.znatoki.telegram.statistic.configuration

import kotliquery.sessionOf
import me.centralhardware.znatoki.telegram.statistic.Config

val session =
    sessionOf(Config.Datasource.url, Config.Datasource.username, Config.Datasource.password)
