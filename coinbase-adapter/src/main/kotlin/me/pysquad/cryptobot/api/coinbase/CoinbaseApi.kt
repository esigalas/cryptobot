package me.pysquad.cryptobot.api.coinbase

import me.pysquad.cryptobot.api.CoinbaseAdapterService
import me.pysquad.cryptobot.config.ConfigReader
import me.pysquad.cryptobot.api.CoinbaseSubscribeRequest
import org.http4k.client.WebsocketClient
import org.http4k.core.Uri
import org.http4k.format.Jackson
import org.http4k.websocket.WsMessage
import kotlin.concurrent.thread

val coinbaseUri = Uri.of(ConfigReader.coinbase.uri)

interface CoinbaseApi {
    fun subscribe(subscribeRequest: CoinbaseSubscribeRequest): Thread

    companion object {

        fun Client(coinbaseAdapter: CoinbaseAdapterService) = object: CoinbaseApi {
            override fun subscribe(subscribeRequest: CoinbaseSubscribeRequest) = thread {
                with(WebsocketClient.blocking(coinbaseUri)) {
                    send(WsMessage(subscribeRequest.toCoinbaseJsonRequest()))
                    received().first().apply(::println)
//                    received().parseAndPrintItInChunks()
                    received().storeInChunks()
                }
            }

            private fun Sequence<WsMessage>.storeInChunks(sizeOfChunk: Int = 6) {
                for (listOfMessages in chunked(sizeOfChunk)) {
                    coinbaseAdapter.storeMessages(
                        listOfMessages.map { CoinbaseProductMessage.wsLens(it) }
                    )
                    listOfMessages.forEach(::println)
                    println("These messages have been stored!\n")
                }
            }

        }
    }
}

private fun CoinbaseSubscribeRequest.toCoinbaseJsonRequest() = Jackson {
    val productIdsJson = productIds.map { string(it.value) }
    val channelsJson = channels.map { string(it.value) }
    obj(
        "type" to type.name.toLowerCase().asJsonValue(),
        "product_ids" to array(productIdsJson),
        "channels" to array(channelsJson)
    ).toPrettyString()
}

/**
 * Helper function to print received messages in chunks.
 * The size of chunks is by default 6.
 *
 * Similar to this function we could have a "writeInChunks"
 * when we'll use a real-time db to store those messages.
 */
private fun Sequence<WsMessage>.printInChunks(sizeOfChunk: Int = 6) {
    for (listOfMessages in chunked(sizeOfChunk)) {
        listOfMessages.forEach(::println)
        println("\nReceiving and printing next chunk of size $sizeOfChunk... be patient!\n")
    }
}

/**
 * Helper function to parse received messages and print them in chunks
 */
private fun Sequence<WsMessage>.parseAndPrintItInChunks(sizeOfChunk: Int = 6) {
    for (listOfMessages in chunked(sizeOfChunk)) {
        listOfMessages.forEach { println(CoinbaseProductMessage.wsLens(it)) }
        println("\nReceiving, parsing and printing next chunk of size $sizeOfChunk... be patient!\n")
    }
}
