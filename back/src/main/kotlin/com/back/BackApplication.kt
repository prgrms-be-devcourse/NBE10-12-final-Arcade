package com.back

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BackApplication

fun main(args: Array<String>) {
    runApplication<BackApplication>(*args)
}
