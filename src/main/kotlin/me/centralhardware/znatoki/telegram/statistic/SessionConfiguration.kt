package me.centralhardware.znatoki.telegram.statistic

import kotliquery.sessionOf

val session =
    sessionOf(Config.Datasource.url, Config.Datasource.username, Config.Datasource.password)
