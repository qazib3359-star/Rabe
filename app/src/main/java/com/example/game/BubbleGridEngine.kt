package com.example.game

import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class CellCoord(val row: Int, val col: Int)

data class ActiveProjectile(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: BubbleColor
)

data class PopEffect(
    val x: Float,
    val y: Float,
    val color: BubbleColor,
    val progress: Float = 0f
)

data class SparkleParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: BubbleColor,
    val radius: Float,
    val life: Float = 1f
)

data class FallingBubble(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: BubbleColor
)

data class FloatingScorePopup(
    val x: Float,
    val y: Float,
    val text: String,
    val isCombo: Boolean,
    val life: Float = 1f
)

data class PlacementOutcome(
    val placedCell: CellCoord,
    val poppedCells: List<CellCoord>,
    val droppedCells: List<Pair<CellCoord, BubbleColor>>,
    val scoreGained: Long,
    val boardCleared: Boolean,
    val dangerReached: Boolean
)

object BubbleGridEngine {
    private val SQRT_3 = sqrt(3f)

    // Normalized board coordinate space: width = 1.0f
    // Even rows have 9 bubbles across -> radius = 1f / 18f
    const val BOARD_WIDTH = 1.0f
    const val BUBBLE_RADIUS = BOARD_WIDTH / (LevelGenerator.COLS_EVEN * 2f)
    val ROW_HEIGHT = BUBBLE_RADIUS * SQRT_3
    const val MIN_AIM_ANGLE_DEG = -166f
    const val MAX_AIM_ANGLE_DEG = -14f
    const val PROJECTILE_SPEED = 2.35f // normalized board widths per second

    fun cellCenter(row: Int, col: Int): Offset {
        val xOffset = if (row % 2 == 0) BUBBLE_RADIUS else BUBBLE_RADIUS * 2f
        val cx = xOffset + col * (BUBBLE_RADIUS * 2f)
        val cy = BUBBLE_RADIUS + row * ROW_HEIGHT
        return Offset(cx, cy)
    }

    fun isValidCell(row: Int, col: Int): Boolean {
        if (row < 0 || row >= LevelGenerator.MAX_ROWS) return false
        return col in 0 until LevelGenerator.colsForRow(row)
    }

    fun getNeighbors(row: Int, col: Int): List<CellCoord> {
        val evenRow = row % 2 == 0
        val offsets = if (evenRow) {
            arrayOf(
                0 to -1, 0 to 1,
                -1 to -1, -1 to 0,
                1 to -1, 1 to 0
            )
        } else {
            arrayOf(
                0 to -1, 0 to 1,
                -1 to 0, -1 to 1,
                1 to 0, 1 to 1
            )
        }
        val result = ArrayList<CellCoord>(6)
        for ((dr, dc) in offsets) {
            val nr = row + dr
            val nc = col + dc
            if (isValidCell(nr, nc)) {
                result.add(CellCoord(nr, nc))
            }
        }
        return result
    }

    fun clampAimAngleDegrees(rawDeg: Float): Float {
        return rawDeg.coerceIn(MIN_AIM_ANGLE_DEG, MAX_AIM_ANGLE_DEG)
    }

    fun angleFromTouch(cannonPos: Offset, touchPos: Offset): Float {
        val dx = touchPos.x - cannonPos.x
        val dy = (touchPos.y - cannonPos.y).coerceAtMost(-0.04f)
        val deg = (kotlin.math.atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()
        return clampAimAngleDegrees(deg)
    }

    /**
     * Computes evenly spaced trajectory guide dots with optional wall bounce reflection.
     */
    fun computeAimTrajectory(
        grid: List<List<BubbleColor?>>,
        cannonPos: Offset,
        aimAngleDeg: Float,
        extendedBounce: Boolean
    ): List<Offset> {
        val rad = aimAngleDeg * PI / 180.0
        var dx = cos(rad).toFloat()
        val dy = sin(rad).toFloat()
        var x = cannonPos.x
        var y = cannonPos.y

        val step = BUBBLE_RADIUS * 0.22f
        val dotSpacing = BUBBLE_RADIUS * 0.85f
        val maxDots = if (extendedBounce) 26 else 14
        val maxSteps = if (extendedBounce) 240 else 95
        var bounceCount = 0
        val maxBounces = if (extendedBounce) 2 else 0

        val dots = ArrayList<Offset>(maxDots)
        var distanceSinceLastDot = 0f

        for (i in 0 until maxSteps) {
            x += dx * step
            y += dy * step
            distanceSinceLastDot += step

            // Wall bounce
            if (x <= BUBBLE_RADIUS) {
                if (bounceCount >= maxBounces) break
                x = BUBBLE_RADIUS + (BUBBLE_RADIUS - x)
                dx = abs(dx)
                bounceCount++
            } else if (x >= BOARD_WIDTH - BUBBLE_RADIUS) {
                if (bounceCount >= maxBounces) break
                x = (BOARD_WIDTH - BUBBLE_RADIUS) - (x - (BOARD_WIDTH - BUBBLE_RADIUS))
                dx = -abs(dx)
                bounceCount++
            }

            // Stop at ceiling
            if (y <= BUBBLE_RADIUS) {
                if (dots.size < maxDots) dots.add(Offset(x, BUBBLE_RADIUS))
                break
            }

            // Stop upon colliding with an occupied bubble
            if (collidesWithGrid(grid, x, y, BUBBLE_RADIUS * 1.76f)) {
                break
            }

            if (distanceSinceLastDot >= dotSpacing) {
                dots.add(Offset(x, y))
                distanceSinceLastDot = 0f
                if (dots.size >= maxDots) break
            }
        }
        return dots
    }

    fun collidesWithGrid(
        grid: List<List<BubbleColor?>>,
        x: Float,
        y: Float,
        thresholdDist: Float = BUBBLE_RADIUS * 1.80f
    ): Boolean {
        val thresholdSq = thresholdDist * thresholdDist
        val approxRow = ((y - BUBBLE_RADIUS) / ROW_HEIGHT).toInt()
        val minR = (approxRow - 2).coerceAtLeast(0)
        val maxR = (approxRow + 2).coerceAtMost(LevelGenerator.MAX_ROWS - 1)
        for (r in minR..maxR) {
            val rowList = grid[r]
            for (c in rowList.indices) {
                if (rowList[c] != null) {
                    val center = cellCenter(r, c)
                    val ddx = x - center.x
                    val ddy = y - center.y
                    if (ddx * ddx + ddy * ddy <= thresholdSq) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Finds the closest unoccupied cell (preferring cells touching existing bubbles or row 0).
     */
    fun findSnapCell(grid: List<List<BubbleColor?>>, x: Float, y: Float): CellCoord {
        var bestAnchored: CellCoord? = null
        var bestAnchoredDist = Float.MAX_VALUE
        var bestAny: CellCoord? = null
        var bestAnyDist = Float.MAX_VALUE

        for (r in 0 until LevelGenerator.MAX_ROWS) {
            val cols = LevelGenerator.colsForRow(r)
            for (c in 0 until cols) {
                if (grid[r][c] != null) continue
                val center = cellCenter(r, c)
                val dist = hypot(x - center.x, y - center.y)
                if (dist < bestAnyDist) {
                    bestAnyDist = dist
                    bestAny = CellCoord(r, c)
                }
                val hasAnchorNeighbor = r == 0 || getNeighbors(r, c).any { (nr, nc) -> grid[nr][nc] != null }
                if (hasAnchorNeighbor && dist < bestAnchoredDist) {
                    bestAnchoredDist = dist
                    bestAnchored = CellCoord(r, c)
                }
            }
        }
        return bestAnchored ?: bestAny ?: CellCoord(0, 0)
    }

    /**
     * Places a projectile into the mutable grid, resolves match-3+ pops and disconnected drops.
     */
    fun placeAndResolve(
        mutableGrid: MutableList<MutableList<BubbleColor?>>,
        impactX: Float,
        impactY: Float,
        color: BubbleColor,
        comboMultiplier: Int
    ): PlacementOutcome {
        val snapCell = findSnapCell(mutableGrid, impactX, impactY)
        mutableGrid[snapCell.row][snapCell.col] = color

        val poppedSet = LinkedHashSet<CellCoord>()

        if (color == BubbleColor.RAINBOW) {
            // Rainbow wildcard matches every adjacent color cluster of size >= 1 (making total >= 2 with rainbow)
            poppedSet.add(snapCell)
            val neighborColors = getNeighbors(snapCell.row, snapCell.col)
                .mapNotNull { (nr, nc) -> mutableGrid[nr][nc] }
                .filter { it != BubbleColor.RAINBOW }
                .distinct()

            for (targetColor in neighborColors) {
                val cluster = findSameColorCluster(mutableGrid, snapCell, targetColor)
                if (cluster.size >= 2) {
                    poppedSet.addAll(cluster)
                }
            }
            // If touching no standard bubbles, still pop immediate neighbors
            if (poppedSet.size == 1) {
                for (n in getNeighbors(snapCell.row, snapCell.col)) {
                    if (mutableGrid[n.row][n.col] != null) poppedSet.add(n)
                }
            }
        } else {
            val cluster = findSameColorCluster(mutableGrid, snapCell, color)
            if (cluster.size >= 3) {
                poppedSet.addAll(cluster)
            }
        }

        // Remove popped bubbles
        for (cell in poppedSet) {
            mutableGrid[cell.row][cell.col] = null
        }

        // Detect and drop floating bubbles disconnected from row 0
        val droppedList = ArrayList<Pair<CellCoord, BubbleColor>>()
        if (poppedSet.isNotEmpty()) {
            val anchored = findAnchoredCells(mutableGrid)
            for (r in 0 until LevelGenerator.MAX_ROWS) {
                for (c in 0 until LevelGenerator.colsForRow(r)) {
                    val cellColor = mutableGrid[r][c]
                    if (cellColor != null && CellCoord(r, c) !in anchored) {
                        droppedList.add(CellCoord(r, c) to cellColor)
                        mutableGrid[r][c] = null
                    }
                }
            }
        }

        val popPoints = poppedSet.size * 50L * comboMultiplier
        val dropPoints = droppedList.size * 100L * comboMultiplier
        val bonusBigCluster = if (poppedSet.size >= 5) 150L else 0L
        val scoreGained = popPoints + dropPoints + bonusBigCluster

        var remainingCount = 0
        var lowestOccupiedRow = -1
        for (r in 0 until LevelGenerator.MAX_ROWS) {
            for (c in 0 until LevelGenerator.colsForRow(r)) {
                if (mutableGrid[r][c] != null) {
                    remainingCount++
                    if (r > lowestOccupiedRow) lowestOccupiedRow = r
                }
            }
        }

        return PlacementOutcome(
            placedCell = snapCell,
            poppedCells = poppedSet.toList(),
            droppedCells = droppedList,
            scoreGained = scoreGained,
            boardCleared = remainingCount == 0,
            dangerReached = lowestOccupiedRow >= LevelGenerator.DANGER_ROW
        )
    }

    private fun findSameColorCluster(
        grid: List<List<BubbleColor?>>,
        start: CellCoord,
        targetColor: BubbleColor
    ): Set<CellCoord> {
        val visited = LinkedHashSet<CellCoord>()
        val queue = ArrayDeque<CellCoord>()
        visited.add(start)
        queue.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (neighbor in getNeighbors(current.row, current.col)) {
                if (neighbor !in visited) {
                    val nColor = grid[neighbor.row][neighbor.col]
                    if (nColor == targetColor || nColor == BubbleColor.RAINBOW) {
                        visited.add(neighbor)
                        queue.add(neighbor)
                    }
                }
            }
        }
        return visited
    }

    fun findAnchoredCells(grid: List<List<BubbleColor?>>): Set<CellCoord> {
        val anchored = HashSet<CellCoord>()
        val queue = ArrayDeque<CellCoord>()
        val row0 = grid.getOrNull(0) ?: return anchored
        for (c in row0.indices) {
            if (row0[c] != null) {
                val coord = CellCoord(0, c)
                anchored.add(coord)
                queue.add(coord)
            }
        }
        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            for (n in getNeighbors(curr.row, curr.col)) {
                if (n !in anchored && grid[n.row][n.col] != null) {
                    anchored.add(n)
                    queue.add(n)
                }
            }
        }
        return anchored
    }

    fun remainingColorsOnBoard(grid: List<List<BubbleColor?>>, fallback: List<BubbleColor>): List<BubbleColor> {
        val present = LinkedHashSet<BubbleColor>()
        for (row in grid) {
            for (cell in row) {
                if (cell != null && cell != BubbleColor.RAINBOW) {
                    present.add(cell)
                }
            }
        }
        return if (present.isNotEmpty()) present.toList() else fallback
    }

    fun pickNextBubbleColor(
        grid: List<List<BubbleColor?>>,
        fallback: List<BubbleColor>,
        rng: Random = Random.Default
    ): BubbleColor {
        val colors = remainingColorsOnBoard(grid, fallback)
        return colors[rng.nextInt(colors.size)]
    }
}
