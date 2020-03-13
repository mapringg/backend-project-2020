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

    data class ContainerInfo(val port: Int, val process: Process)
    data class ClientRequest(val future: CompletableFuture<ResponseEntity<String>>, val response: HttpServletResponse)
    class NoPortAvailableExcetion: Exception()

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    private const val THEIA_STARTUP_WAIT = 300L
    private const val THEIA_STARTUP_CURL_TIME = 300L
    private val theiaImage: String = System.getenv("THEIA-IMAGE") ?: "theiaide/theia" //ENVIRONMENT VARIABLE FOR THEIA IMAGE
    private val THEIA_CONTAINER_TIMEOUT: Int = if (System.getenv("THEIA_TIMEOUT") == null) 5000 else System.getenv("THEIA_TIMEOUT").toInt() //ENVIRONMENT VARIABLE FOR TIMEOUT TIME FOR CONTAINERS (WHEN TO SHUT DOWN)
    private val THEIA_STARTUP_SCRIPT = "docker run --network theia-controller_default --name %s -e %d $theiaImage --port=%d" //ENVIRONMENT VARIABLE FOR EXTRA THEIA ARGUMENTS
    private val containerMap = ConcurrentHashMap<String,ContainerInfo>()
    private val freePorts = LinkedBlockingQueue<Int>()
    private val waitingClients = ConcurrentHashMap<String,LinkedList<ClientRequest>>()

    public fun getThaiaContainerName(id: String): String{
        return "theia-$id"
    }

    init {
        for(i in 49152..65535){
            freePorts.add(i)
        }
    }

    fun getRoute(id: String): String?{
        containerMap[id] ?: return null
        return id
    }

    private fun startContainer(id: String){
        logger.info("Attempting to start a container")
        if(freePorts.size == 0){
            throw NoPortAvailableExcetion()
        }
        while(true){
            val port = freePorts.poll()
            //TODO Maybe get args from environment variable too since were allowing environment variables to set docker args
            //TODO Use config server for theia stuff
            val dockerId = getThaiaContainerName(id)
            logger.info("Trying to start a container on port: $port")
            logger.info("Running command ${THEIA_STARTUP_SCRIPT.format(dockerId, port, port)}")
            val pb = ProcessBuilder(THEIA_STARTUP_SCRIPT.format(dockerId,port,port).split(" "))
            pb.redirectError(ProcessBuilder.Redirect.INHERIT)
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
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
                logger.info("Container started on port:$port")
                while (true) {
                    val cb = ProcessBuilder("curl $dockerId:$port".split(' '))
//                    cb.redirectError(ProcessBuilder.Redirect.INHERIT)
//                    cb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    val curl = cb.start()
                    curl.waitFor()
                    if (curl.exitValue() == 0) {//Curl exited with no errors. Theia container must have started
                        containerStartupSuccess(id, port, p)
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

    private fun containerStartupSuccess(id: String, port:Int, p: Process){
        logger.info("Successfully started theia container")
        containerMap[id] = ContainerInfo(port,p)
        val idPort = containerMap.mapValues { k -> k.value.port }
        NginxConfigurer.rewriteConfig(idPort,true)
        waitingClients[id]!!.forEach {
            it.response.sendRedirect("/$id")
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