package com.android.voidrise

import com.android.voidrise.game.GameScreen
import com.badlogic.gdx.Game
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class VoidriseGame : Game() {
    lateinit var batch: SpriteBatch
        private set
    lateinit var shapes: ShapeRenderer
        private set

    override fun create() {
        batch = SpriteBatch()
        shapes = ShapeRenderer()
        setScreen(GameScreen(this))
    }

    override fun dispose() {
        batch.dispose()
        shapes.dispose()
        screen?.dispose()
    }
}
