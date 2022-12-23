package net.technearts

import org.optaplanner.core.api.solver.Solver
import org.optaplanner.core.api.solver.SolverFactory
import org.optaplanner.core.config.solver.SolverConfig
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import java.time.Duration
import javax.inject.Inject
import io.quarkus.logging.Log.info

@Command(mixinStandardHelpOptions = true)
class Command : Runnable {
    @Inject
    var config: SchedulerConfig? = null

    @Option(names = ["-t", "--timeLimit"], description = ["How long we can wait for an answer?"], defaultValue = "10")
    var timeLimit: Long = 5

    override fun run() {
        val solverFactory: SolverFactory<EmployeeSchedule> = SolverFactory.create(
            SolverConfig().withSolutionClass(EmployeeSchedule::class.java)
                .withEntityClasses(Shift::class.java)
                .withConstraintProviderClass(EmployeeSchedulingConstraintProvider::class.java)
                // The solver runs only for 5 seconds on this small dataset.
                // It's recommended to run for at least 5 minutes ("5m") otherwise.
                .withTerminationSpentLimit(Duration.ofSeconds(timeLimit))
        )
        info("Load the problem")
        val problem: EmployeeSchedule = generateData(
            config?.year()!!,
            config?.month()!!,
            config?.employees()!!,
            config?.unavailable()!!,
            config?.desired()!!,
            config?.undesired()!!,
            config?.maxShiftsPerWeek()!!
        )
        info("Solve the problem. This may take $timeLimit seconds")
        val solver: Solver<EmployeeSchedule> = solverFactory.buildSolver()
        val solution: EmployeeSchedule = solver.solve(problem)
        info("Visualize the solution")
        printTimetable(solution)
    }

    private fun printTimetable(solution: EmployeeSchedule) {
        solution.shiftList.sortedBy { it.day }.forEach {
            info("${it.day?.dayOfMonth} -- ${it.employee?.name}")
        }
    }
}