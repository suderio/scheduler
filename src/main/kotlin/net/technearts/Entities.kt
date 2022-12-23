package net.technearts

import org.optaplanner.core.api.domain.entity.PlanningEntity
import org.optaplanner.core.api.domain.lookup.PlanningId
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty
import org.optaplanner.core.api.domain.solution.PlanningScore
import org.optaplanner.core.api.domain.solution.PlanningSolution
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider
import org.optaplanner.core.api.domain.variable.PlanningVariable
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore
import java.sql.Date
import java.text.SimpleDateFormat
import java.time.DateTimeException
import java.time.Month
import java.time.MonthDay
import kotlin.math.min


enum class AvailabilityType {
    DESIRED, UNDESIRED, UNAVAILABLE
}

data class Availability(
    var date: MonthDay? = null,
    var employee: Employee? = null,
    var availabilityType: AvailabilityType?
)


data class Employee(
    var name: String? = null,
    var maxShiftPerWeek: Int? = null
)

@PlanningEntity
data class Shift(
    @PlanningId
    var day: MonthDay? = null,
    @PlanningVariable(valueRangeProviderRefs = ["employeeRange"])
    var employee: Employee? = null,
    var year: Int? = null
) {
    fun nextDay(): MonthDay = MonthDay.of(day?.month, min(day!!.dayOfMonth + 1, day!!.month.maxLength()))
    fun yearWeek(): String = SimpleDateFormat("w").format(Date.valueOf(day!!.atYear(year!!)))
}

@PlanningSolution
data class EmployeeSchedule(
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "employeeRange")
    var employeeList: List<Employee> = emptyList(),
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "availabilityRange")
    var availabilityList: List<Availability> = emptyList(),
    @PlanningEntityCollectionProperty
    var shiftList: List<Shift> = emptyList(),
    @PlanningScore
    var score: HardSoftScore? = null,
)

fun shiftList(month: Int, year: Int) = (1..Month.of(month).maxLength()).map { day ->
    Shift(MonthDay.of(month, day), null, year)
}

fun employeeList(employees: List<String?>, maxShiftPerWeek: Map<String?, Int?>?) = employees.map {
    Employee(
        it,
        maxShiftPerWeek?.get(it)
    )
}

fun availabilityList(
    month: Int,
    unavailable: Map<String?, List<Int?>?>,
    desired: Map<String?, List<Int?>?>,
    undesired: Map<String?, List<Int?>?>
): List<Availability> {
    val result = mutableListOf<Availability>()
    try {
        unavailable.forEach { entry ->
            result += entry.value?.map {
                Availability(
                    MonthDay.of(month, it!!),
                    Employee(entry.key),
                    AvailabilityType.UNAVAILABLE
                )
            }!!
        }
        desired.forEach { entry ->
            result += entry.value?.map {
                Availability(
                    MonthDay.of(month, it!!),
                    Employee(entry.key),
                    AvailabilityType.DESIRED
                )
            }!!
        }
        undesired.forEach { entry ->
            result += entry.value?.map {
                Availability(
                    MonthDay.of(month, it!!),
                    Employee(entry.key),
                    AvailabilityType.UNDESIRED
                )
            }!!
        }
    } catch (e: DateTimeException) {
        println(e.message)
        throw IllegalArgumentException("Wrong number of days in month")
    }
    return result
}

fun generateData(
    year: Int,
    month: Int,
    employees: List<String?>,
    unavailable: Map<String?, List<Int?>?>,
    desired: Map<String?, List<Int?>?>,
    undesired: Map<String?, List<Int?>?>,
    maxShiftPerWeek: Map<String?, Int?>
): EmployeeSchedule {
    return EmployeeSchedule(
        employeeList(employees, maxShiftPerWeek),
        availabilityList(month, unavailable, desired, undesired),
        shiftList(month, year),
        null
    )
}