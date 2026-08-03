package com.yeyint.recipeapp.backend.plugins

import com.yeyint.recipeapp.backend.di.Dependencies
import com.yeyint.recipeapp.backend.routes.adminRoutes
import com.yeyint.recipeapp.backend.routes.authRoutes
import com.yeyint.recipeapp.backend.routes.exploreRoutes
import com.yeyint.recipeapp.backend.routes.healthRoutes
import com.yeyint.recipeapp.backend.routes.ingredientRoutes
import com.yeyint.recipeapp.backend.routes.pantryRoutes
import com.yeyint.recipeapp.backend.routes.recipeRoutes
import com.yeyint.recipeapp.backend.routes.shoppingRoutes
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

/** All routes live under a versioned prefix so v2 can ship side by side. */
fun Application.configureRouting(deps: Dependencies) {
    routing {
        healthRoutes()
        route("/api/v1") {
            authRoutes(deps.authService)
            ingredientRoutes(deps.ingredientService)
            recipeRoutes(deps.recipeService)
            pantryRoutes(deps.pantryService)
            shoppingRoutes(deps.shoppingService)
            exploreRoutes(deps.exploreService)
            adminRoutes(deps.adminService, deps.ingredientService)
        }
    }
}
