package backend.project.theiacontroller

import backend.project.theiacontroller.theiacontainer.TheiaContainerController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.RequestMapping
import java.lang.Exception
import java.util.concurrent.CompletableFuture
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Controller
@RequestMapping("/**")
class TheiaRoutingController {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    init {
        logger.info("Started theia routing controller")
    }

    @RequestMapping
    @Async
    fun routeToInstance(@CookieValue("uid") id: String, request: HttpServletRequest, response: HttpServletResponse): CompletableFuture<ResponseEntity<String>>{
        return try {
            val route = TheiaContainerController.getRoute(id)
            if (route == null) {
                val completableFuture = CompletableFuture<ResponseEntity<String>>()
                logger.info("Container has not been started yet. Starting")
                TheiaContainerController.waitForContainer(id, TheiaContainerController.ClientRequest(completableFuture,response))
                completableFuture
            } else {
                logger.info("Got request from id:$id. Current route $route")
                response.sendRedirect(route)
                CompletableFuture.completedFuture(ResponseEntity("Redirecting",HttpStatus.OK))
            }
        }catch(e:Exception){
            logger.info("Something went wrong. ${e.printStackTrace()}")
            CompletableFuture.completedFuture(ResponseEntity(HttpStatus.SERVICE_UNAVAILABLE))
        }
    }
}
