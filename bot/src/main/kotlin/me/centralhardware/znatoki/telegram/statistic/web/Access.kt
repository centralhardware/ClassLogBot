package me.centralhardware.znatoki.telegram.statistic.web

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.routing.Route
import io.ktor.server.routing.RouteSelector
import io.ktor.server.routing.RouteSelectorEvaluation
import io.ktor.server.routing.RoutingResolveContext

/** What a group of endpoints requires of the caller, checked once before any handler in it runs. */
enum class Access(val message: String, val check: (Caller) -> Boolean) {
    TIME("Нет права вносить занятия.", { it.canTime }),
    PAYMENT("Нет права вносить оплаты.", { it.canPayment }),
    READ_PAYMENTS("Нет доступа к оплатам.", { it.readsPayments }),
    STUDENTS("Нет доступа к ученикам.", { it.seesStudents }),
    CLIENT("Изменять учеников могут те, у кого есть право ADD_CLIENT.", { it.canClient }),
    NOTES("Нет права добавлять заметки.", { it.canTime || it.canClient }),
    SEES_ALL("Доступно администратору и директору.", { it.seesAll }),
    MANAGE_TUTORS("Управлять командой могут админ и те, у кого есть MANAGE_TUTORS.", { it.canManageTutors }),
    ADMIN("Доступно только администратору.", { it.isAdmin }),
}

private class AccessConfig { lateinit var access: Access }

private val RequiresAccess = createRouteScopedPlugin("RequiresAccess", ::AccessConfig) {
    val access = pluginConfig.access
    onCall { call -> if (!access.check(call.caller)) throw HttpError(HttpStatusCode.Forbidden, access.message) }
}

/** A transparent route node: it matches everything and only carries the access check. */
private class AccessSelector(private val access: Access) : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int) = RouteSelectorEvaluation.Transparent
    override fun toString() = "(requires $access)"
}

/** Endpoints declared in [build] are reachable only for callers that pass [access]. */
fun Route.requires(access: Access, build: Route.() -> Unit): Route =
    createChild(AccessSelector(access)).apply {
        install(RequiresAccess) { this.access = access }
        build()
    }
