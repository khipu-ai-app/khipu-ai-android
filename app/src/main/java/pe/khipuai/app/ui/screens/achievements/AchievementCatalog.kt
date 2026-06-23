package pe.khipuai.app.ui.screens.achievements

object AchievementCatalog {
    val items = listOf(
        AchievementItem("first_note", "Primera nota", "Subiste tu primer apunte", "📝", false),
        AchievementItem("streak_3", "Constante", "3 días seguidos de repaso", "🔥", false),
        AchievementItem("streak_7", "Semana de fuego", "7 días seguidos de repaso", "🔥", false),
        AchievementItem("streak_30", "Mes de dedicación", "30 días seguidos", "🏆", false),
        AchievementItem("concepts_10", "Primer paso", "10 conceptos estudiados", "🌱", false),
        AchievementItem("concepts_50", "Aprendiz", "50 conceptos estudiados", "📖", false),
        AchievementItem("concepts_100", "Estudioso", "100 conceptos estudiados", "🧠", false),
        AchievementItem("mastery_50", "Medio camino", "50% de dominio en cualquier curso", "🧗", false),
        AchievementItem("mastery_100", "Maestro", "100% de dominio en un curso", "👑", false),
        AchievementItem("quiz_first", "Primer quiz", "Completa tu primer quiz", "🎯", false),
        AchievementItem("tutor_first", "Primer chat", "Primera conversación con Khipu", "🤖", false),
        AchievementItem("graph_explorer", "Explorador", "Abres el grafo de conocimiento", "🗺️", false)
    )
}
