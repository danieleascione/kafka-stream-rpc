package com.hello.kafka.stream

import org.apache.kafka.streams.processor.Processor
import org.apache.kafka.streams.processor.ProcessorContext
import org.apache.kafka.streams.processor.PunctuationType
import org.apache.kafka.streams.processor.StateStore
import org.apache.kafka.streams.state.KeyValueStore
import java.time.Duration

class WordCounterProcessor : Processor<String, String> {

    val name = "WordCounterProcessor"
    val stateStoreName = "Counts"
    private lateinit var kvStore: KeyValueStore<String, String>
    private lateinit var context: ProcessorContext

    @SuppressWarnings("unchecked")
    override fun init(context: ProcessorContext?) {
        if (context != null) {
            this.context = context
            kvStore = context.getStateStore(stateStoreName) as KeyValueStore<String, String>
        }

        context?.let { ctx ->
            ctx.schedule(
                Duration.ofMinutes(1),
                PunctuationType.STREAM_TIME
            ) { _ ->
                kvStore.all().use { iter ->
                    iter.forEachRemaining {
                        kvStore.delete(it.key)
                    }
                }
                ctx.commit()
            }
        }

    }

    override fun process(key: String?, value: String?) {
        println("$key: $value")
        value
            ?.toLowerCase()
            ?.split(regex = "\\W+".toRegex())
            ?.stream()
            ?.forEach { word ->
                val count = kvStore.get(word)
                        ?.toInt()
                        ?.let {
                            it + 1
                        } ?: 1
                count.toString().run {
                    kvStore.put(word, this)
                    context.forward(word, this)
                    context.commit()
                }
            }
    }

    override fun close() {
        // Nothing to do
    }
}
