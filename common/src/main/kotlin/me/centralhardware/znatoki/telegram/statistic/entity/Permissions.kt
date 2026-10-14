package me.centralhardware.znatoki.telegram.statistic.entity

enum class Permissions {

    ADD_TIME,
    ADD_PAYMENT,
    ADD_CLIENT,
    ADMIN,
    FORCE_GROUP,
    EXTRA_HALF_HOUR,
    ADD_PAYMENT_FOR_OTHERS,
    ADD_TIME_FOR_OTHERS,
    WEB_INTERFACE,
    /** Reviews the tutors' monthly reports in the web app; reads everything, changes nothing. */
    DIRECTOR,
    /** Adds tutors and edits their permissions and subjects in the web app. */
    MANAGE_TUTORS,
}
