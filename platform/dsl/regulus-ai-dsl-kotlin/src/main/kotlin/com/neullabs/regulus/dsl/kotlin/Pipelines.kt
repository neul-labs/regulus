package com.neullabs.regulus.dsl.kotlin

class AiPipeline {
    fun retrieval(block: () -> Unit) {}
    fun tools(block: () -> Unit) {}
    fun policy(block: () -> Unit) {}
    fun planner(block: () -> Unit) {}
}

fun aiPipeline(name: String, block: AiPipeline.() -> Unit) {
    // TODO: Implement DSL parsing logic
}
