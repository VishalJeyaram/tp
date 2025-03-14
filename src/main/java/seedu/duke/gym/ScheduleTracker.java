package seedu.duke.gym;

import seedu.duke.exceptions.DukeException;
import seedu.duke.Parser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ScheduleTracker {
    private ArrayList<ScheduledWorkout> scheduledWorkoutList;
    private static final int LOWER_BOUND_INDEX_NON_EMPTY_LIST_ONES_INDEXING = 1;
    private static final int FIRST_INDEX_IN_LIST = 0;
    private static final int DAYS_IN_A_WEEK = 7;
    public static final Logger SCHEDULE_TRACKER_LOGGER = Logger.getLogger("ScheduleTrackerLogger");

    public ScheduleTracker() {
        scheduledWorkoutList = new ArrayList<>();
        SCHEDULE_TRACKER_LOGGER.setLevel(Level.SEVERE);
    }

    public static String[] generateScheduledWorkoutParameters(String inputArguments)
            throws DukeException, DateTimeParseException {
        SCHEDULE_TRACKER_LOGGER.log(Level.INFO, "Starting generation of parameters for scheduled workout.");
        String workoutDescription = Parser.getScheduleDescription(inputArguments);
        String workoutDate = Parser.getDate(inputArguments);
        String workoutTime = Parser.getTime(inputArguments);
        String[] generatedParameters = {workoutDescription, workoutDate, workoutTime};
        SCHEDULE_TRACKER_LOGGER.log(Level.INFO, "Successfully generated parameters for scheduled workout.");
        return generatedParameters;
    }

    public void addScheduledWorkout(String inputArguments)
            throws DukeException, DateTimeParseException, NumberFormatException {
        SCHEDULE_TRACKER_LOGGER.log(Level.INFO, "Starting to try and add scheduled workout.");
        nullArgumentCheck(inputArguments);
        assert inputArguments != null : "Exception should already been thrown if argument is null";
        missingDescriptionCheck(inputArguments);
        scheduledWorkoutSeparatorCheck(inputArguments);
        // index 0: description | index 1: date | index 2: time
        String[] generatedParameters = generateScheduledWorkoutParameters(inputArguments);
        assert generatedParameters.length == 3 : "Exactly 3 parameters should be generated";
        String workoutDescription = generatedParameters[0];
        String workoutDate = generatedParameters[1];
        String workoutTime = generatedParameters[2];
        boolean isRecurringWorkout = Parser.isRecurringWorkout(inputArguments);
        scheduledWorkoutList.add(
                new ScheduledWorkout(workoutDescription, workoutDate, workoutTime, isRecurringWorkout)
        );
        ScheduledWorkout workout = scheduledWorkoutList.get(scheduledWorkoutList.size() - 1);
        System.out.println("Noted! CLI.ckFit has scheduled your " + workout.isRecurringStatusAsText()
                + "workout of description \"" + workoutDescription + "\" on " + workoutDate + " at "
                + workoutTime + ".");
        cleanUpScheduleList();
        SCHEDULE_TRACKER_LOGGER.log(Level.INFO, "Successfully added workout to schedule.");
    }

    public boolean isScheduledWorkoutNumberWithinRange(int workoutNumber) {
        int upperBound = scheduledWorkoutList.size();
        int lowerBound = LOWER_BOUND_INDEX_NON_EMPTY_LIST_ONES_INDEXING;
        return (workoutNumber >= lowerBound) && (workoutNumber <= upperBound);
    }

    public void deleteScheduledWorkout(String inputArguments) throws DukeException, NumberFormatException {
        SCHEDULE_TRACKER_LOGGER.log(Level.INFO, "Starting to try and delete scheduled workout.");
        nullArgumentCheck(inputArguments);
        assert inputArguments != null : "Exception should already been thrown if argument is null";
        emptyScheduledWorkoutListCheck();
        assert scheduledWorkoutList.size() > 0 : "List should be non empty at this point";
        int workoutNumber = Parser.parseStringToInteger(inputArguments);
        int workoutIndex = workoutNumber - 1; // 0-indexing
        if (isScheduledWorkoutNumberWithinRange(workoutNumber)) {
            ScheduledWorkout workoutToDelete = scheduledWorkoutList.get(workoutIndex);
            System.out.println("Noted! CLI.ckFit has successfully deleted your "
                    + workoutToDelete.isRecurringStatusAsText() + "scheduled workout of description \""
                    + workoutToDelete.getWorkoutDescription() + "\" on " + workoutToDelete.getWorkoutDate()
                    + " at " + workoutToDelete.getWorkoutTime() + "!");
            scheduledWorkoutList.remove(workoutIndex);
            cleanUpScheduleList();
            SCHEDULE_TRACKER_LOGGER.log(Level.INFO, "Successfully deleted scheduled workout.");
        } else {
            SCHEDULE_TRACKER_LOGGER.log(Level.WARNING, "Failed to delete scheduled workout.");
            throw new DukeException("Failed to delete that workout! Please enter an Integer within range.");
        }
    }

    public void listScheduledWorkouts() throws DukeException {
        SCHEDULE_TRACKER_LOGGER.log(Level.INFO, "Starting to try and list scheduled workouts.");
        emptyScheduledWorkoutListCheck();
        cleanUpScheduleList();
        int currentIndex = 1;
        for (ScheduledWorkout workout : scheduledWorkoutList) {
            System.out.println(currentIndex + ". " + workout.getWorkoutDescription() + workout.isRecurringStatus());
            System.out.println("Date: " + workout.getWorkoutDate());
            System.out.println("Time: " + workout.getWorkoutTime() + "\n");
            currentIndex++;
        }
        SCHEDULE_TRACKER_LOGGER.log(Level.INFO, "Successfully listed workouts.");
    }

    public void cleanUpScheduleList() {
        sortScheduleList();
        LocalDate currentDate = LocalDateTime.now().toLocalDate();
        boolean isAnyWorkoutUpdatedOrDeleted = false;
        boolean isAnyWorkoutOverdue = true;
        ScheduledWorkout firstWorkoutEntry;
        while (isAnyWorkoutOverdue) {
            firstWorkoutEntry = scheduledWorkoutList.get(FIRST_INDEX_IN_LIST);
            if (firstWorkoutEntry.getWorkoutDateAsLocalDate().isBefore(currentDate)) {
                updateOrDeleteScheduledWorkout(firstWorkoutEntry, currentDate);
                isAnyWorkoutUpdatedOrDeleted = true;
            } else {
                isAnyWorkoutOverdue = false;
            }
        }
        if (isAnyWorkoutUpdatedOrDeleted) {
            System.out.println("CLI.ckFit has detected some overdue scheduled "
                    + "workouts and has deleted/rescheduled them!");
        }
    }

    public void updateOrDeleteScheduledWorkout(ScheduledWorkout scheduledWorkout, LocalDate currentDate) {
        if (scheduledWorkout.isRecurring()) {
            rescheduleRecurringWorkout(scheduledWorkout, currentDate);
        } else {
            scheduledWorkoutList.remove(scheduledWorkoutList.indexOf(scheduledWorkout));
        }
        sortScheduleList();
    }

    public void rescheduleRecurringWorkout(ScheduledWorkout scheduledWorkout, LocalDate currentDate) {
        long daysUntilCurrentDate = ChronoUnit.DAYS.between(
                scheduledWorkout.getWorkoutDateAsLocalDate(), currentDate);
        long daysToAdd = (long) (Math.ceil((double) daysUntilCurrentDate / DAYS_IN_A_WEEK) * DAYS_IN_A_WEEK);
        scheduledWorkout.incrementWorkoutDate(daysToAdd);
    }

    public void sortScheduleList() {
        scheduledWorkoutList.sort(Comparator.comparing(ScheduledWorkout::getWorkoutDateTime));
    }

    public void nullArgumentCheck(String inputArguments) throws DukeException {
        if (inputArguments == null) {
            SCHEDULE_TRACKER_LOGGER.log(Level.WARNING, "User input argument(s) is null.");
            throw new DukeException("Please enter arguments in the format: schedule [workout_description] "
                    + "/d [dd/mm/yyyy] /t [hh:mm]");
        }
        SCHEDULE_TRACKER_LOGGER.log(Level.INFO, "User input argument(s) is not null.");
    }

    public void scheduledWorkoutSeparatorCheck(String inputArguments) throws DukeException {
        boolean areSeparatorsCorrect = Parser.containsDateSeparator(inputArguments)
                && Parser.containsTimeSeparator(inputArguments);
        if (!areSeparatorsCorrect) {
            SCHEDULE_TRACKER_LOGGER.log(Level.WARNING, "Separators in user input are missing or invalid.");
            throw new DukeException("Invalid or missing separator... " + System.lineSeparator()
                    + "Please enter in the format: schedule [workout_description] /d [dd/mm/yyyy] /t [hh:mm]");
        }
        SCHEDULE_TRACKER_LOGGER.log(Level.INFO, "Separators in user input are correct.");
    }

    public void emptyScheduledWorkoutListCheck() throws DukeException {
        if (scheduledWorkoutList.isEmpty()) {
            SCHEDULE_TRACKER_LOGGER.log(Level.WARNING, "Schedule list is empty.");
            throw new DukeException("Scheduled Workout list is empty!");
        }
        SCHEDULE_TRACKER_LOGGER.log(Level.INFO, "Schedule list is not empty.");
    }

    public void missingDescriptionCheck(String inputArguments) throws DukeException {
        int indexOfFirstDateSeparator = inputArguments.indexOf(Parser.DATE_SEPARATOR.trim());
        String subStringBeforeDateSeparator = "";
        if (indexOfFirstDateSeparator != -1) { // date separator not found
            subStringBeforeDateSeparator = inputArguments.substring(0, indexOfFirstDateSeparator).trim();
        } else {
            scheduledWorkoutSeparatorCheck(inputArguments);
        }
        if (subStringBeforeDateSeparator.isEmpty()) {
            SCHEDULE_TRACKER_LOGGER.log(Level.WARNING, "Description is missing in user input arguments.");
            throw new DukeException("I am sorry... it appears the description is missing." + System.lineSeparator()
                    + "Please enter a description for your workout!");
        }
        SCHEDULE_TRACKER_LOGGER.log(Level.INFO, "Description is present in user input arguments.");
    }
}
