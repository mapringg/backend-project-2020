package backend.project.theiacontroller

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
class TheiaControllerApplication

fun main(args: Array<String>) {
	runApplication<TheiaControllerApplication>(*args)
}
