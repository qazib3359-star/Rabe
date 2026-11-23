package com.example

import androidx.compose.ui.geometry.Offset
import com.example.game.BubbleColor
import com.example.game.BubbleGridEngine
import com.example.game.LevelGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleGridEngineTest {

    @Test
    fun levelGeneratorProducesAll100PlayableLevelsWithIncreasingDifficulty() {
        for (levelNum in 1..LevelGenerator.TOTAL_LEVELS) {
            val def = LevelGenerator.generateLevel(levelNum)
            assertEquals(levelNum, def.levelNumber)
            assertTrue("Moves should be positive on level $levelNum", def.movesLimit in 16..42)
            val totalBubbles = def.initialRows.sumOf { row -> row.count { it != null } }
            assertTrue("Level $levelNum must contain playable bubbles", totalBubbles >= 16)
            assertTrue(
                "Star thresholds must ascend on level $levelNum",
                def.oneStarScore < def.twoStarScore && def.twoStarScore < def.threeStarScore
            )
            // Verify every starting bubble is anchored to row 0
            val anchored = BubbleGridEngine.findAnchoredCells(def.initialRows)
            assertEquals("All initial bubbles on level $levelNum must be anchored", totalBubbles, anchored.size)
        }

        val lvl1 = LevelGenerator.generateLevel(1)
        val lvl50 = LevelGenerator.generateLevel(50)
        assertTrue("Later levels should introduce more colors", lvl50.availableColors.size > lvl1.availableColors.size)
    }

    @Test
    fun matchThreePopsClusterAndDropsDisconnectedIslands() {
        val grid = MutableList(LevelGenerator.MAX_ROWS) { r ->
            MutableList<BubbleColor?>(LevelGenerator.colsForRow(r)) { null }
        }
        // Two RED bubbles anchored at row 0
        grid[0][2] = BubbleColor.RED
        grid[0][3] = BubbleColor.RED
        // A BLUE bubble hanging ONLY off (0,2) and (0,3) at (1,2)
        grid[1][2] = BubbleColor.BLUE

        // Shoot a 3rd RED bubble right next to (0,2) at (0,1)
        val targetPos = BubbleGridEngine.cellCenter(0, 1)
        val outcome = BubbleGridEngine.placeAndResolve(
            mutableGrid = grid,
            impactX = targetPos.x,
            impactY = targetPos.y,
            color = BubbleColor.RED,
            comboMultiplier = 1
        )

        assertEquals(3, outcome.poppedCells.size)
        assertEquals(1, outcome.droppedCells.size)
        assertEquals(BubbleColor.BLUE, outcome.droppedCells.first().second)
        assertTrue("Board should now be completely cleared", outcome.boardCleared)
        assertTrue("Score should award both pops and drops", outcome.scoreGained >= 250L)
    }

    @Test
    fun rainbowWildcardPopsAdjacentColorCluster() {
        val grid = MutableList(LevelGenerator.MAX_ROWS) { r ->
            MutableList<BubbleColor?>(LevelGenerator.colsForRow(r)) { null }
        }
        grid[0][4] = BubbleColor.GREEN
        grid[0][5] = BubbleColor.GREEN

        val targetPos = BubbleGridEngine.cellCenter(0, 3)
        val outcome = BubbleGridEngine.placeAndResolve(
            mutableGrid = grid,
            impactX = targetPos.x,
            impactY = targetPos.y,
            color = BubbleColor.RAINBOW,
            comboMultiplier = 1
        )

        assertEquals(3, outcome.poppedCells.size)
        assertTrue(outcome.boardCleared)
    }

    @Test
    fun aimTrajectoryComputesWallBounceDots() {
        val emptyGrid = List(LevelGenerator.MAX_ROWS) { r ->
            List<BubbleColor?>(LevelGenerator.colsForRow(r)) { null }
        }
        val cannon = Offset(0.5f, 1.35f)
        val dots = BubbleGridEngine.computeAimTrajectory(
            grid = emptyGrid,
            cannonPos = cannon,
            aimAngleDeg = -150f, // aimed strongly toward left wall
            extendedBounce = true
        )
        assertTrue("Aim trajectory should produce guide dots", dots.size >= 8)
        assertFalse("Dots must stay within horizontal board bounds", dots.any { it.x < 0f || it.x > 1f })
    }
}
