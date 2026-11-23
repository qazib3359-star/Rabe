package com.example.game

import kotlin.random.Random

data class WorldThemeInfo(
    val index: Int,
    val name: String,
    val subtitle: String,
    val startLevel: Int,
    val endLevel: Int
)

data class LevelDefinition(
    val levelNumber: Int,
    val world: WorldThemeInfo,
    val initialRows: List<List<BubbleColor?>>,
    val movesLimit: Int,
    val availableColors: List<BubbleColor>,
    val oneStarScore: Long,
    val twoStarScore: Long,
    val threeStarScore: Long
)

object LevelGenerator {
    const val TOTAL_LEVELS = 100
    const val COLS_EVEN = 9
    const val COLS_ODD = 8
    const val MAX_ROWS = 13
    const val DANGER_ROW = 11

    val worlds: List<WorldThemeInfo> = listOf(
        WorldThemeInfo(0, "Castle Meadow", "Levels 1 – 20", 1, 20),
        WorldThemeInfo(1, "Crystal Lake", "Levels 21 – 40", 21, 40),
        WorldThemeInfo(2, "Sunset Canyon", "Levels 41 – 60", 41, 60),
        WorldThemeInfo(3, "Starlight Aurora", "Levels 61 – 80", 61, 80),
        WorldThemeInfo(4, "Royal Sky Citadel", "Levels 81 – 100", 81, 100)
    )

    fun worldForLevel(levelNumber: Int): WorldThemeInfo {
        val clamped = levelNumber.coerceIn(1, TOTAL_LEVELS)
        val idx = ((clamped - 1) / 20).coerceIn(0, worlds.lastIndex)
        return worlds[idx]
    }

    fun colsForRow(row: Int): Int = if (row % 2 == 0) COLS_EVEN else COLS_ODD

    fun generateLevel(levelNumber: Int): LevelDefinition {
        val level = levelNumber.coerceIn(1, TOTAL_LEVELS)
        val world = worldForLevel(level)
        val rng = Random(seed = (level * 7919L) + 104729L)

        // Progressive color complexity across the 100 levels
        val colorCount = when {
            level <= 3 -> 3
            level <= 10 -> 4
            level <= 30 -> 5
            else -> 6
        }
        val palette = BubbleColor.standardColors.take(colorCount)

        // Progressive initial rows (4 rows on L1 up to 8 rows in late levels)
        val filledRows = when {
            level <= 2 -> 4
            level <= 8 -> 5
            level <= 25 -> 6
            level <= 60 -> 7
            else -> 8
        }

        val patternType = (level - 1) % 10
        val grid = MutableList(MAX_ROWS) { r ->
            MutableList<BubbleColor?>(colsForRow(r)) { null }
        }

        // Fill top anchor row first so structure is always grounded
        for (c in 0 until colsForRow(0)) {
            val anchorColor = when {
                patternType == 0 || patternType == 5 -> palette[(c / 2 + level) % palette.size]
                else -> palette[rng.nextInt(palette.size)]
            }
            grid[0][c] = anchorColor
        }

        // Fill remaining pattern rows with clustering bias so shots feel fun & strategic
        for (r in 1 until filledRows) {
            val cols = colsForRow(r)
            for (c in 0 until cols) {
                val shouldPlace = when (patternType) {
                    0 -> true // Classic full wall
                    1 -> !(r >= filledRows - 2 && (c == cols / 2 || c == cols / 2 - 1)) // Central archway
                    2 -> (c >= (r / 2) && c < cols - (r / 2)) // Inverted pyramid
                    3 -> c != cols / 2 // Twin towers canyon
                    4 -> (r + c) % 3 != 0 || r <= 2 // Honeycomb pockets
                    5 -> !(r >= 3 && (c == 0 || c == cols - 1)) // Diamond taper
                    6 -> (r % 2 == 0) || (c % 2 == 0) || r <= 1 // Lattice weave
                    7 -> !(r >= filledRows - 2 && c in 2..(cols - 3)) // Double-wing overhang
                    8 -> (c <= cols / 2 && r <= filledRows - 1) || (c > cols / 2 && r <= filledRows - 2) // Asymmetric cascade
                    else -> !(r == filledRows - 1 && (c == 1 || c == cols - 2)) // Fortress battlements
                }
                if (shouldPlace) {
                    // Cluster bias decreases slowly as difficulty rises
                    val clusterChance = (0.58f - (level * 0.0028f)).coerceIn(0.28f, 0.58f)
                    val neighborColor = findSampleNeighborColor(grid, r, c, rng)
                    grid[r][c] = if (neighborColor != null && rng.nextFloat() < clusterChance) {
                        neighborColor
                    } else {
                        palette[rng.nextInt(palette.size)]
                    }
                }
            }
        }

        // Prune any cells not connected to row 0 so the level starts cleanly anchored
        val anchored = BubbleGridEngine.findAnchoredCells(grid)
        var bubbleCount = 0
        for (r in 0 until MAX_ROWS) {
            for (c in 0 until colsForRow(r)) {
                if (grid[r][c] != null) {
                    if (CellCoord(r, c) !in anchored) {
                        grid[r][c] = null
                    } else {
                        bubbleCount++
                    }
                }
            }
        }

        // Guarantee minimum bubbles for fun gameplay
        if (bubbleCount < 16) {
            for (r in 0..3) {
                for (c in 0 until colsForRow(r)) {
                    if (grid[r][c] == null) {
                        grid[r][c] = palette[(r + c) % palette.size]
                        bubbleCount++
                    }
                }
            }
        }

        // Calibrate moves limit: generous early, challenging but fair at high levels
        val baseMoves = (bubbleCount * 0.48f).toInt() + when {
            level <= 5 -> 12
            level <= 20 -> 10
            level <= 50 -> 8
            level <= 80 -> 7
            else -> 6
        }
        val movesLimit = baseMoves.coerceIn(16, 42)

        val oneStar = (bubbleCount * 45L).coerceAtLeast(500L)
        val twoStar = (bubbleCount * 75L + 300L)
        val threeStar = (bubbleCount * 105L + 750L)

        return LevelDefinition(
            levelNumber = level,
            world = world,
            initialRows = grid.map { it.toList() },
            movesLimit = movesLimit,
            availableColors = palette,
            oneStarScore = oneStar,
            twoStarScore = twoStar,
            threeStarScore = threeStar
        )
    }

    private fun findSampleNeighborColor(
        grid: List<List<BubbleColor?>>,
        row: Int,
        col: Int,
        rng: Random
    ): BubbleColor? {
        val neighbors = BubbleGridEngine.getNeighbors(row, col)
            .mapNotNull { (nr, nc) -> grid.getOrNull(nr)?.getOrNull(nc) }
        return if (neighbors.isNotEmpty()) neighbors[rng.nextInt(neighbors.size)] else null
    }
}
