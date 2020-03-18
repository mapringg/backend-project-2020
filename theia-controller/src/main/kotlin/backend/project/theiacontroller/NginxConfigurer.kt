package backend.project.theiacontroller

import backend.project.theiacontroller.theiacontainer.TheiaContainerController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.File
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

@Component
object NginxConfigurer {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private val NGINX_CONTAINER_NAME = System.getenv("NGINX_DOCKER_NAME")?: "nginx-container"
    private val NGINX_PORT = System.getenv("NGINX_PORT")?: 80
    private val NGINX_CONF_VOLUME_NAME = System.getenv("NGINX_CONF_VOLUME_NAME")?: "theia-controller_nginx_config"
    private val NGINX_CONF_VOLUME_DIRECTORY = System.getenv("NGINX_CONF_VOLUME_DIRECTORY")?: "/etc/nginx/"
    private val filePath = "$NGINX_CONF_VOLUME_DIRECTORY/nginx.conf" //ENVIRONMENT VARIABLE FOR NGINX CONFIG FILE LOCATION (MUST BE ON SAME FILESYSTEM)
    private val NGINX_STARTUP_COMMAND = "docker run -v $NGINX_CONF_VOLUME_NAME:$NGINX_CONF_VOLUME_DIRECTORY --network theia-controller_default -p 80:80 --restart unless-stopped --name $NGINX_CONTAINER_NAME nginx"
    private val WAIT_NGINX_TIMEOUT = if(System.getenv("NGINX_WAIT_TIMEOUT") == null) 10000 else System.getenv("NGINX_WAIT_TIMEOUT").toLong()
    private val NGINX_CURL_DELAY = if(System.getenv("NGINX_CURL_DELAY") == null) 300 else System.getenv("NGINX_CURL_DELAY").toLong()
    private val NGINX_STARTUP_WAIT = if(System.getenv("NGINX_STARTUP_WAIT") == null) 10000 else System.getenv("NGINX_STARTUP_WAIT").toLong()

    init {
        val nginxConfig = File(filePath)
        logger.info("Nginx config filepath set to $filePath")
        nginxConfig.parentFile.mkdirs()
        if (nginxConfig.exists()) {
            if (!nginxConfig.delete()) {
                logger.error("Failed to delete old configuration. Did you check permissions?")
                exitProcess(1)
            }
        }
        else if (!nginxConfig.createNewFile()) {
            logger.error("Failed to create nginx configuration file. Did you check permissions?")
            exitProcess(1)
        }

        logger.info("Removing old nginx container if its running")
        val stopper = ProcessBuilder("docker container stop $NGINX_CONTAINER_NAME".split(' ')).redirectError(ProcessBuilder.Redirect.INHERIT).redirectOutput(ProcessBuilder.Redirect.INHERIT).start()
        stopper.waitFor()
        val removal = ProcessBuilder("docker container rm $NGINX_CONTAINER_NAME".split(' ')).start()
        removal.waitFor()

        rewriteConfig(listOf(),false)

        logger.info("Starting nginx server usng command $NGINX_STARTUP_COMMAND")
        val nb = ProcessBuilder(NGINX_STARTUP_COMMAND.split(' '))
        nb.redirectError(ProcessBuilder.Redirect.INHERIT)
        nb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        val nginx = nb.start()

        nginx.waitFor(3000,TimeUnit.MILLISECONDS)
        if(!nginx.isAlive)
            throw Exception("Nginx failed to startup. Exit value: ${nginx.exitValue()}")

        if(!rewriteConfig(listOf(),true)){
            logger.error("Could not communicate with nginx server.")
            exitProcess(3)
        }

    }

    fun waitForNginxContainer(id: String){
        logger.info("Waiting for nginx to register container connection")
        val loops = WAIT_NGINX_TIMEOUT/ NGINX_CURL_DELAY
        for(i in 0..loops) {
            val cb = ProcessBuilder("curl -I $NGINX_CONTAINER_NAME/$id | head -1".split(' '))
            val cp = cb.start()
            val output = cp.inputStream.bufferedReader().readText()
//            println(output)
            if(!output.contains("404")) return
            else sleep(NGINX_CURL_DELAY)
        }
        throw Exception("Could not connect to theia instance through nginx")
    }

    private fun waitForNginxStartup(){
        for(i in 0..NGINX_STARTUP_WAIT/ NGINX_CURL_DELAY){
//            logger.info("Polling nginx")
            val cb = ProcessBuilder("curl","$NGINX_CONTAINER_NAME:$NGINX_PORT")
            val c = cb.start()
            c.waitFor()
            if(c.exitValue() == 0) return
            else sleep(NGINX_CURL_DELAY)
        }
        throw Exception("Nginx not detected")
    }

    @Synchronized
    final fun rewriteConfig(containers: Collection<String>, reload: Boolean): Boolean{
        val preConfig = ClassPathResource("/templates/nginx-pre-default").inputStream.reader().readText()
        val nginxConfig = File(filePath)

        val writer = nginxConfig.writer()
        writer.write(preConfig)
        val upstreamTemplate = ClassPathResource("/templates/nginx-upstream-template").inputStream.reader().readText()
        for (c in containers) {
            val containerName = TheiaContainerController.getTheiaContainerName(c)
            writer.write(upstreamTemplate.format(c,containerName))
        }
        val midConfig = ClassPathResource("/templates/nginx-mid-default").inputStream.reader().readText()
        writer.write(midConfig)
        val serverConfig = ClassPathResource("/templates/nginx-server-template").inputStream.reader().readText()
        for(c in containers) {
            writer.write(serverConfig.format(c,c))
        }
        val endConfig = ClassPathResource("/templates/nginx-end-default").inputStream.reader().readText()
        writer.write(endConfig)
        writer.flush()
        writer.close()

//        logger.info("Wrote to nginx config. File contents:")
//        logger.info(nginxConfig.readText())

        if(reload)
            return reloadNginxConfig()
        return true
    }
    private fun reloadNginxConfig(): Boolean{
        logger.info("Reloading nginx config")
        waitForNginxStartup()
        val ub = ProcessBuilder("docker exec $NGINX_CONTAINER_NAME nginx -s reload".split(' '))
//        ub.redirectError(ProcessBuilder.Redirect.INHERIT)
//        ub.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        val uc = ub.start()
        uc.waitFor()
        if(uc.exitValue() != 0) {
            logger.error("Error reloading nginx config")
            return false
        }
        logger.info("Reloaded nginx config")
        return true
    }
}