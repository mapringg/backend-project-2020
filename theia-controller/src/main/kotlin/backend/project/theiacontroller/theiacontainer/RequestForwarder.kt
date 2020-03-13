package backend.project.theiacontroller.theiacontainer

import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.getForEntity

object RequestForwarder {

    private val restTemplate = RestTemplate()

    public fun <T> forwardRequest(url: String, entity: HttpEntity<T>, method: HttpMethod): ResponseEntity<String> {
        val response: ResponseEntity<String> = restTemplate.getForEntity(url,method,entity)
        return response
    }
}