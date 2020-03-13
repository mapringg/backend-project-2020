package backend.project.theiacontroller.theiacontainer

import backend.project.theiacontroller.NginxConfigurer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.lang.Thread.sleep
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletResponse
import kotlin.Exception

object TheiaContainerController {

    //TODO VERY URGENT
    //TODO SHUT DOWN CONTAINERS AFTER A WHILE
    //TODO STOP CURL REQUESTS IF CONTAINER GOES DOWN FOR SOME REASON

    data class ClientRequest(val future: CompletableFuture<ResponseEntity<String>>, val response: HttpServletResponse)

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    private const val THEIA_STARTUP_WAIT = 300L
    private const val THEIA_STARTUP_CURL_TIME = 300L
    private val theiaImage: String = System.getenv("THEIA-IMAGE") ?: "theiaide/theia" //ENVIRONMENT VARIABLE FOR THEIA IMAGE
    private val THEIA_CONTAINER_TIMEOUT: Int = if (System.getenv("THEIA_TIMEOUT") == null) 5000 else System.getenv("THEIA_TIMEOUT").toInt() //ENVIRONMENT VARIABLE FOR TIMEOUT TIME FOR CONTAINERS (WHEN TO SHUT DOWN)
    private val THEIA_STARTUP_SCRIPT = "docker run --network theia-controller_default --name %s $theiaImage" //ENVIRONMENT VARIABLE FOR EXTRA THEIA ARGUMENTS
    private val containerMap = ConcurrentHashMap<String,Process>()
    private val waitingClients = ConcurrentHashMap<String,LinkedList<ClientRequest>>()

    public fun getTheiaContainerName(id: String): String{
        return "theia-$id"
    }

    fun getRoute(id: String): String?{
        containerMap[id] ?: return null
        return "/$id"
    }

    private fun startContainer(id: String){
        logger.info("Attempting to start a container")
        while(true){
            //TODO Maybe get args from environment variable too since were allowing environment variables to set docker args
            //TODO Use config server for theia stuff
            val dockerId = getTheiaContainerName(id)
            logger.info("Trying to start a container")
            logger.info("Running command ${THEIA_STARTUP_SCRIPT.format(dockerId)}")
            val pb = ProcessBuilder(THEIA_STARTUP_SCRIPT.format(dockerId).split(" "))
//            pb.redirectError(ProcessBuilder.Redirect.INHERIT)
//            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
            val p = pb.start()
            p.waitFor(THEIA_STARTUP_WAIT, TimeUnit.MILLISECONDS)
            try {
                if(p.exitValue() == 126){
                    logger.info("Command failed to execute got exit status 126")
                    containerStartupFailure(id,Throwable("Server Failure. Could not start theia instance exit code 126"))
                    return
                }
                continue //Process exited (Theia container failed to startup. Probably because port in use or smth)
            } catch (e: Exception) {
                logger.info("Theia container started for $id")
                while (true) {
                    val cb = ProcessBuilder("curl $dockerId:3000".split(' '))
//                    cb.redirectError(ProcessBuilder.Redirect.INHERIT)
//                    cb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    val curl = cb.start()
                    curl.waitFor()
                    if (curl.exitValue() == 0) {//Curl exited with no errors. Theia container must have started
                        containerStartupSuccess(id, p)
                        return
                    } else sleep(THEIA_STARTUP_CURL_TIME)
                }
            }
        }
    }

    private fun containerStartupFailure(id: String, exception: Throwable){
        waitingClients[id]!!.forEach{
            it.future.completeExceptionally(exception)
        }
    }


    private fun containerStartupSuccess(id: String, p: Process){
        logger.info("Successfully started theia container")
        containerMap[id] = p
        NginxConfigurer.rewriteConfig(containerMap.keys().toList(),true)
        NginxConfigurer.waitForNginxContainer(id)
        waitingClients[id]!!.forEach {
            it.response.sendRedirect(getRoute(id))
            it.future.complete(ResponseEntity(HttpStatus.OK))
        }
    }

    fun waitForContainer(id: String,request: ClientRequest){
        if(waitingClients[id] == null) waitingClients[id] = LinkedList()
        synchronized(waitingClients[id]!!){
            waitingClients[id]!!.add(request)
            if(waitingClients[id]!!.size == 1){
                startContainer(id)
            }
        }
    }
}