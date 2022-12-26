package net.technearts

import io.smallrye.config.ConfigMapping
import io.smallrye.config.WithName


@ConfigMapping(prefix = "scheduler")
interface SchedulerConfig {
    @WithName("year")
    fun year(): Int?
    @WithName("month")
    fun month(): Int?
    @WithName("employees")
    fun employees(): List<String?>?
    @WithName("unavailable")
    fun unavailable(): Map<String?, List<Int?>?>?
    @WithName("desired")
    fun desired(): Map<String?, List<Int?>?>?
    @WithName("undesired")
    fun undesired(): Map<String?, List<Int?>?>?
    @WithName("maxPerWeek")
    fun maxShiftsPerWeek(): Map<String?, Int?>?
    @WithName("createConfig")
    fun createConfig(): Boolean
}