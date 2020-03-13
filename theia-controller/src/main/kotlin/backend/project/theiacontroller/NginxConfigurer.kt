package backend.project.theiacontroller

import backend.project.theiacontroller.theiacontainer.TheiaContainerController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.File
import java.lang.Thread.sleep
import kotlin.system.exitProcess

@Component
object NginxConfigurer {

    private val filePath = System.getenv("NGINX-CONFIG-FILEPATH")?: "/etc/nginx/nginx.conf" //ENVIRONMENT VARIABLE FOR NGINX CONFIG FILE LOCATION (MUST BE ON SAME FILESYSTEM)
    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)
    private val NGINX_CONTAINER_NAME = System.getenv("NGINX_DOCKER_NAME")?: "nginx-container"
    private val NGINX_ADDRESS = System.getenv("NGINX_ADDRESS")?: "localhost"
    private val NGINX_PORT = System.getenv("NGINX_PORT")?: 80
    private val runtime = Runtime.getRuntime()
//    private var nginx: Process? = null

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

        //Remove any floating containers
        logger.info("Removing floating containers")
        val rb = ProcessBuilder("/bin/sh","-c","docker ps --filter name=theia-* -aq | xargs docker stop | xargs docker rm")
//        rb.redirectError(ProcessBuilder.Redirect.INHERIT)
//        rb.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        val rp = rb.start()
        rp.waitFor()

        rewriteConfig(listOf(),true)

    }

    public fun waitForNginxContainer(id: String){
        logger.info("Waiting for nginx to register container connection")
        while(true) { //TODO Add timeout
            val cb = ProcessBuilder("curl $NGINX_ADDRESS/$id".split(' '))
            val cp = cb.start()
            val output = cp.inputStream.bufferedReader().readText()
            if(!output.contains("<head><title>404 Not Found</title></head>")) return
            else sleep(300)
        }
    }

    private fun waitForNginx(){
        while(true){
            val cb = ProcessBuilder("curl","$NGINX_ADDRESS:$NGINX_PORT")
            val c = cb.start()
            c.waitFor()
            if(c.exitValue() == 0) break
            else sleep(300)
        }
    }

    @Synchronized
    public final fun rewriteConfig(containers: Collection<String>, reload: Boolean){
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
            reloadNginxConfig()
    }
    private fun reloadNginxConfig(){
        logger.info("Reloading nginx config")
        waitForNginx()
        val ub = ProcessBuilder("docker exec $NGINX_CONTAINER_NAME nginx -s reload".split(' '))
        ub.redirectError(ProcessBuilder.Redirect.INHERIT)
        ub.redirectOutput(ProcessBuilder.Redirect.INHERIT)
        val uc = ub.start()
        uc.waitFor()
        if(uc.exitValue() != 0) logger.error("Error reloading nginx config")
    }
}