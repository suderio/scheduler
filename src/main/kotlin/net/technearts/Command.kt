package net.technearts

import io.quarkus.logging.Log.info
import io.quarkus.logging.Log.warn
import io.quarkus.runtime.configuration.ProfileManager
import org.optaplanner.core.api.solver.Solver
import org.optaplanner.core.api.solver.SolverFactory
import org.optaplanner.core.config.solver.SolverConfig
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Files.copy
import java.nio.file.Path
import java.time.Duration
import javax.inject.Inject
import kotlin.system.exitProcess

@Command(mixinStandardHelpOptions = true)
class Command : Runnable {
    @Inject
    var config: SchedulerConfig? = null

    @Option(names = ["-t", "--timeLimit"], description = ["How long we can wait for an answer?"], defaultValue = "10")
    var timeLimit: Long = 5

    override fun run() {
        if (config!!.createConfig()) {
            checkConfigFileCreated()
        } else {
            info("Using example configuration from dev profile.")
        }
        val solverFactory: SolverFactory<EmployeeSchedule> = SolverFactory.create(
            SolverConfig().withSolutionClass(EmployeeSchedule::class.java)
                .withEntityClasses(Shift::class.java)
                .withConstraintProviderClass(EmployeeSchedulingConstraintProvider::class.java)
                // The solver runs only for 5 seconds on this small dataset.
                // It's recommended to run for at least 5 minutes ("5m") otherwise.
                .withTerminationSpentLimit(Duration.ofSeconds(timeLimit))
        )

        info("Loading the problem from the config file")
        val problem: EmployeeSchedule = generateData(
            config?.year()!!,
            config?.month()!!,
            config?.employees()!!,
            config?.unavailable()!!,
            config?.desired()!!,
            config?.undesired()!!,
            config?.maxShiftsPerWeek()!!
        )
        info("Solving the problem. This may take $timeLimit seconds")
        val solver: Solver<EmployeeSchedule> = solverFactory.buildSolver()
        val solution: EmployeeSchedule = solver.solve(problem)
        info("Showing the solution")
        printTimetable(solution)
    }

    private fun checkConfigFileCreated() {
        val configDir = Path.of(System.getProperty("user.dir")).resolve("config")
        if (Files.notExists(configDir.toAbsolutePath())) {
            info("creating config dir: $configDir")
            Files.createDirectory(configDir)
        }

        val configFile = configDir.resolve("application.yml")
        if (Files.notExists(configFile)) {
            val source = configExample.byteInputStream()
            info("Creating example config file: $configFile")
            try {
                copy(source, configFile)
            } catch (ex: IOException) {
                warn(ex)
                error("Config file copy failed.")
            }
            info("Config file created. Exiting application to reload.")
            exitProcess(0)
        }
    }

    private fun printTimetable(solution: EmployeeSchedule) {
        solution.shiftList.sortedBy { it.day }.forEach {
            info("${it.day?.dayOfMonth} -- ${it.employee?.name}")
        }
    }
}

val configExample = """
|scheduler:
|  year: 2023
|  month: 12
|  employees:
|    - Valdemar
|    - Rosivaldo
|    - Genilson
|  unavailable:
|    Valdemar:
|      - 01
|  desired:
|    Genilson:
|      - 15
|  undesired:
|    Rosivaldo:
|      - 25
|      - 29
|  maxPerWeek:
|    Valdemar: 2
|    Genilson: 1
|    Rosivaldo: 2
|quarkus:
|  live-reload:
|    instrumentation: true
|  log:
|    level: WARN
|    console:
|      format: "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n"
|    category:
|      "net.technearts":
|        level: INFO
|        handlers: "net.technearts"
|        use-parent-handlers: false
|    handler:
|      console:
|        "net.technearts":
|          format: "%s%e%n"
|  banner:
|    enabled: false
|""".trimMargin()