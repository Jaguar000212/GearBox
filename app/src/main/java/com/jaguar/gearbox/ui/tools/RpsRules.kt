package com.jaguar.gearbox.ui.tools

internal enum class RpsMove(val emoji: String, val label: String) {
    ROCK("✊", "Rock"), PAPER("✋", "Paper"), SCISSORS("✌", "Scissors")
}

internal enum class RpsOutcome(val message: String) {
    WIN("You win!"), LOSS("CPU wins"), DRAW("Draw")
}

/** Pure rules shared by presentation and tests. The CPU still chooses uniformly at random. */
internal fun rpsOutcome(player: RpsMove, computer: RpsMove): RpsOutcome = when {
    player == computer -> RpsOutcome.DRAW
    player == RpsMove.ROCK && computer == RpsMove.SCISSORS ||
        player == RpsMove.PAPER && computer == RpsMove.ROCK ||
        player == RpsMove.SCISSORS && computer == RpsMove.PAPER -> RpsOutcome.WIN
    else -> RpsOutcome.LOSS
}
