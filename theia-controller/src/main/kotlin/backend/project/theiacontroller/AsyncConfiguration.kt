package backend.project.theiacontroller

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor


@Configuration
@EnableAsync
class AsyncConfiguration {

    @Bean(name = ["containerExecutor"])
    public fun asyncExecutor(): Executor{
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 3
        executor.maxPoolSize = 3
        executor.setQueueCapacity(100)
        executor.setThreadNamePrefix("ContainerThread-")
        executor.initialize()
        return executor
    }

}