package com.sunya.cdm.iosp

import com.sunya.cdm.array.*

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce


@OptIn(ExperimentalCoroutinesApi::class)
internal class ReadChunkConcurrent() {

    fun readChunks(nthreads : Int, chunkIter : Iterator<ArraySection>, lamda : (ArraySection) -> Unit) {

        runBlocking {
            val jobs = mutableListOf<Job>()
            val chunkProducer = produceChunks(chunkIter)
            repeat(nthreads) {
                jobs.add( launchJob(it, chunkProducer, lamda) )
            }

            // wait for all jobs to be done, then close everything
            joinAll(*jobs.toTypedArray())
        }
    }

    fun sumValues(ArraySection : ArraySection) : Double {
        return 42.0
    }

    private val allResults = mutableListOf<Double>()
    private var count = 0
    private fun CoroutineScope.produceChunks(producer: Iterator<ArraySection>): ReceiveChannel<ArraySection> =
        produce {
            for (ballot in producer) {
                send(ballot)
                yield()
                count++
            }
            channel.close()
        }

    private val mutex = Mutex()

    private fun CoroutineScope.launchJob(
        id: Int,
        input: ReceiveChannel<ArraySection>,
        lamda: (ArraySection) -> Unit,
    ) = launch(Dispatchers.Default) {
        for (arraySection in input) {
            lamda(arraySection)
            yield()
        }
    }
}