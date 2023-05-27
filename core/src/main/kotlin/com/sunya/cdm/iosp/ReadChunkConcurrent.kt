package com.sunya.cdm.iosp

import com.sunya.cdm.api.ArraySection
import com.sunya.cdm.array.*

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce


@OptIn(ExperimentalCoroutinesApi::class)
internal class ReadChunkConcurrent() {

    fun <T> readChunks(nthreads : Int, chunkIter : Iterator<ArraySection<T>>, lamda : (ArraySection<T>) -> Unit) {

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

    private val allResults = mutableListOf<Double>()
    private var count = 0
    private fun <T> CoroutineScope.produceChunks(producer: Iterator<ArraySection<T>>): ReceiveChannel<ArraySection<T>> =
        produce {
            for (ballot in producer) {
                send(ballot)
                yield()
                count++
            }
            channel.close()
        }

    private val mutex = Mutex()

    private fun <T> CoroutineScope.launchJob(
        id: Int,
        input: ReceiveChannel<ArraySection<T>>,
        lamda: (ArraySection<T>) -> Unit,
    ) = launch(Dispatchers.Default) {
        for (arraySection in input) {
            lamda(arraySection)
            yield()
        }
    }
}