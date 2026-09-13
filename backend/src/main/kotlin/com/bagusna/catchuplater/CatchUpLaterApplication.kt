package com.bagusna.catchuplater

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class CatchUpLaterApplication

fun main(args: Array<String>) {
    runApplication<CatchUpLaterApplication>(*args)
}
