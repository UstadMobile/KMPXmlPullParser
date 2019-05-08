package org.kmp.io

fun debugPrint(pp: KMPXmlParser) {
    var eventType: Int
    while (pp.next().also { eventType = it } != KMPPullParser.END_DOCUMENT) {
        println(KMPPullParser.TYPES[eventType])
        when (eventType) {
            KMPPullParser.START_DOCUMENT -> {
                println("\n")
            }
            KMPPullParser.START_TAG -> {
                println("name " + pp.getName())
                println("Prefix " + pp.getPrefix())
                println("attributes ")
                pp.attributes.forEach { if (!it.isNullOrEmpty()) print("$it, ") }
                println("\n")
            }
            KMPPullParser.TEXT -> {
                println(pp.getText())
                println("\n")
            }
            KMPPullParser.END_TAG -> {
                println("name " + pp.getName())
                println("Prefix " + pp.getPrefix())
                println("attributes ")
                pp.attributes.forEach { if (!it.isNullOrEmpty()) print("$it, ") }
                println("\n")
            }
            KMPPullParser.END_DOCUMENT -> {
                println("\n")
            }
        }
    }
}