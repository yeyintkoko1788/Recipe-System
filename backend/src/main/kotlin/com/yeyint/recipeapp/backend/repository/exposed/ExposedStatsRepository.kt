package com.yeyint.recipeapp.backend.repository.exposed

import com.yeyint.recipeapp.backend.config.dbQuery
import com.yeyint.recipeapp.backend.db.IngredientsTable
import com.yeyint.recipeapp.backend.db.RecipesTable
import com.yeyint.recipeapp.backend.db.UsersTable
import com.yeyint.recipeapp.backend.domain.IngredientStatus
import com.yeyint.recipeapp.backend.repository.StatsRepository
import com.yeyint.recipeapp.backend.repository.StatsSnapshot
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ExposedStatsRepository : StatsRepository {

    override suspend fun snapshot(): StatsSnapshot = dbQuery {
        val weekAgo = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7)
        StatsSnapshot(
            totalUsers = UsersTable.selectAll().count(),
            totalRecipes = RecipesTable.selectAll().where { RecipesTable.isDeleted eq false }.count(),
            totalIngredients = IngredientsTable.selectAll()
                .where { IngredientsTable.status eq IngredientStatus.APPROVED.name }.count(),
            pendingIngredients = IngredientsTable.selectAll()
                .where { IngredientsTable.status eq IngredientStatus.PENDING.name }.count(),
            recipesCreatedLast7Days = RecipesTable.selectAll()
                .where { (RecipesTable.isDeleted eq false) and (RecipesTable.createdAt greaterEq weekAgo) }
                .count(),
        )
    }
}
