package net.technearts

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore
import org.optaplanner.core.api.score.stream.Constraint
import org.optaplanner.core.api.score.stream.ConstraintCollectors.count
import org.optaplanner.core.api.score.stream.ConstraintFactory
import org.optaplanner.core.api.score.stream.ConstraintProvider
import org.optaplanner.core.api.score.stream.Joiners

class EmployeeSchedulingConstraintProvider : ConstraintProvider {

    override fun defineConstraints(constraintFactory: ConstraintFactory): Array<Constraint> {
        return arrayOf(
            noConsecutiveShifts(constraintFactory),
            unavailableEmployee(constraintFactory),
            desiredDayForEmployee(constraintFactory),
            undesiredDayForEmployee(constraintFactory),
            noMoreThanXShifts(constraintFactory),
            noShiftsInWeek(constraintFactory),
            atLeastXShifts(constraintFactory)
        )
    }

    private fun noConsecutiveShifts(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(Shift::class.java).ifExistsOther(
                Shift::class.java,
                Joiners.equal(Shift::employee, Shift::employee),
                Joiners.equal(Shift::day, Shift::nextDay)
            ).penalize(
                HardSoftScore.ONE_HARD
            ).asConstraint("No two consecutive shifts")
    }

    private fun noMoreThanXShifts(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(Shift::class.java).groupBy(Shift::employee, Shift::yearWeek, count())
            .filter { employee, _, count -> count > employee?.maxShiftPerWeek!! }.penalize(
                HardSoftScore.ONE_SOFT
            ).asConstraint("No more than X shifts a day")
    }

    private fun atLeastXShifts(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(Shift::class.java).groupBy(Shift::employee, Shift::yearWeek, count())
            .filter { employee, _, count -> count < employee?.maxShiftPerWeek!! }.penalize(
                HardSoftScore.ONE_SOFT
            ).asConstraint("At least X shifts a day")
    }

    private fun noShiftsInWeek(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(Shift::class.java).groupBy(Shift::employee, Shift::yearWeek, count())
            .filter { _, _, count -> count == 0 }.penalize(
                HardSoftScore.ONE_HARD
            ).asConstraint("No shifts in the week")
    }

    private fun unavailableEmployee(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(Shift::class.java).join(
            Availability::class.java,
            Joiners.equal({ shift: Shift -> shift.day }, Availability::date),
            Joiners.equal(Shift::employee, Availability::employee)
        ).filter { _, availability -> availability.availabilityType === AvailabilityType.UNAVAILABLE }
            .penalize(HardSoftScore.ONE_HARD).asConstraint("Unavailable employee")
    }

    private fun desiredDayForEmployee(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(Shift::class.java).join(
            Availability::class.java,
            Joiners.equal({ shift: Shift -> shift.day }, Availability::date),
            Joiners.equal(Shift::employee, Availability::employee)
        ).filter { _, availability -> availability.availabilityType === AvailabilityType.DESIRED }
            .reward(HardSoftScore.ONE_SOFT).asConstraint("Desired day for employee")
    }

    private fun undesiredDayForEmployee(constraintFactory: ConstraintFactory): Constraint {
        return constraintFactory.forEach(Shift::class.java).join(
                Availability::class.java,
                Joiners.equal({ shift: Shift -> shift.day }, Availability::date),
                Joiners.equal(Shift::employee, Availability::employee)
            ).filter { _, availability -> availability.availabilityType === AvailabilityType.UNDESIRED }
            .penalize(HardSoftScore.ONE_SOFT).asConstraint("Undesired day for employee")
    }

}
