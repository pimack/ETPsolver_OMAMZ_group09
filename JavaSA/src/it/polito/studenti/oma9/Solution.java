package it.polito.studenti.oma9;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

class Solution {
	// TODO: capire se hashmap è più veloce (O(1) vs O(logn), in teoria...)
	private Map<Exam, Integer> timetable = new HashMap<>(Data.getInstance().nExm * 2, (float) 1.0);
	//private Map<Exam, Integer> timetable = new TreeMap<>();
	private ThreadLocalRandom rand = ThreadLocalRandom.current();
	private double cost;
	private static final double[] precomputedPowers = {32, 16, 8, 4, 2, 1};

	/**
	 * Clone another solution, basically.
	 *
	 * @param other solution
	 */
	Solution(Solution other) {
		timetable.putAll(other.timetable);
		cost = other.cost;
	}

	/**
	 * Create a new, random, feasible solution.
	 */
	Solution() {
		createSolution();
	}

	/**
	 * Schedule all remaining exams and make sure the solution will be feasible.
	 */
	private void createSolution() {
		boolean valid = false;
		while(!valid) {
			valid = tryScheduleRemaining();
		}
	}

	/**
	 * Attempt to schedule all remaining exams, to create a feasible solution
	 *
	 * @see Solution#createSolution()
	 * @return true if valid, false if creation failed
	 */
	private boolean tryScheduleRemaining() {
		List<Exam> sortedExams;
		int failures = 0;
		final int nExm = Data.getInstance().nExm;
		final int limit = nExm / 3; // TODO: explain limit
		boolean allScheduled = false;
		final Collection<Exam> allExams = Data.getInstance().getExams().values();

		while(!allScheduled) {
			if(failures > limit) {
				return false;
			}

			if(timetable.size() >= nExm) {
				// Should never be >, actually
				// Should never happen at all, really: createNeighbor now checks if anything was unscheduled before rescheduling
				System.out.println(Thread.currentThread().getName() + " pointless rescheduling");
				return true;
			}

			sortedExams = allExams.stream()
					.filter((Exam exam) -> !this.isScheduled(exam))
					.sorted(Comparator.comparing(this::countUnavailableTimeslots)
							.thenComparing(Exam::nConflictingExams)
							.reversed())
					.collect(Collectors.toList());

			if(sortedExams.size() == 0) {
				return true;
			}

			Exam candidate = sortedExams.get(0);
			Set<Integer> availableTimeslots = this.getAvailableTimeslots(candidate);

			if(availableTimeslots.isEmpty()) {
				// System.out.println("No slots available");
				failures++;
				for(Exam conflicting : candidate.conflicts) {
					if(this.isScheduled(conflicting)) {
						this.unschedule(conflicting);
					}
				}
			} else {
				this.scheduleRand(candidate, availableTimeslots);
				if(sortedExams.size() <= 1) {
					allScheduled = true;
				}
			}

		}
		return true;
	}

	/**
	 * Schedule exam in timeslot t
	 *
	 * @param exam exam
	 * @param ts   timeslot
	 */
	void schedule(Exam exam, int ts) {
		timetable.put(exam, ts);
		cost += examCost(exam);
	}

	/**
	 * Unschedule the exam.
	 *
	 * @param exam exam
	 */
	void unschedule(Exam exam) {
		cost -= examCost(exam);
		timetable.remove(exam);
	}

	/**
	 * Is it scheduled yet?
	 *
	 * @param exam exam
	 * @return True if the exam is scheduled, False otherwise
	 */
	private boolean isScheduled(Exam exam) {
		return timetable.containsKey(exam);
	}

	/**
	 * Get the timeslot where the exam is scheduled
	 *
	 * @param exam exam
	 * @return timeslot, null if not scheduled
	 */
	Integer getTimeslot(Exam exam) {
		return timetable.get(exam);
	}

	/**
	 * Find the number of slots where the specified exam cannot be placed
	 *
	 * @return number of slots
	 */
	private int countUnavailableTimeslots(Exam exam) {
		Set<Integer> unavailable = new HashSet<>(Data.getInstance().nExm * 2, (float) 1.0);

		// For each conflicting exam
		for(Exam conflicting : exam.conflicts) {
			// Get its scheduled position
			Integer timeslot = this.timetable.get(conflicting);
			// If it has actually been scheduled
			if(timeslot != null) {
				// Add that time slot to the list of conflicting ones
				// (Set compares Integer value, not that it is a pointer to same memory location, so everything works fine)
				unavailable.add(timeslot);
			}
		}

		return unavailable.size();
	}

	/**
	 * Return a set of all the available time slots (no conflict) for the specified exam
	 *
	 * @param exam exam
	 * @return set of available time slots
	 */
	Set<Integer> getAvailableTimeslots(Exam exam) {
		Set<Integer> all = new HashSet<>();

		// Start from all timeslots
		for(int i = 1; i <= Data.getInstance().nSlo; i++) {
			all.add(i);
		}

		// Get every conflicting exam
		for(Exam e : exam.conflicts) {
			// Is it scheduled somewhere?
			if(this.isScheduled(e)) {
				// If it is, remove that time slot
				all.remove(this.getTimeslot(e));
			}
		}

		// Return remaining set
		return all;
	}

	/**
	 * Insert the exam in an available time slot (= where there are no conflicts)
	 * Does nothing if there are no available time slots.
	 *
	 * @param e exam to schedule
	 */
	private void scheduleRand(Exam e, Set<Integer> availableTimeslots) {
		if(availableTimeslots.size() == 0) {
			return;
		}

		int n = rand.nextInt(availableTimeslots.size());
		int i = 0;
		for(Integer timeslot : availableTimeslots) {
			if(i == n) {
				this.schedule(e, timeslot);
				// System.out.println(e.getExmID() + " randomly scheduled in " + timeslot + " (position " + i + "/" + availableTimeslots.size() + ")");
				return;
			}
			i++;
		}

		throw new RuntimeException("Bad things are happening");
	}

	/**
	 * Find distance between two allocated exams.
	 * Returns -1 if at least one hasn't been scheduled.
	 *
	 * @param e1 Exam
	 * @param e2 Other exam
	 * @return distance
	 */
	private int getDistance(Exam e1, Exam e2) {
		Integer ts1 = timetable.get(e1);
		Integer ts2 = timetable.get(e2);
		if(ts1 == null || ts2 == null) {
			return -1;
		}
		return Math.abs(ts2 - ts1);
	}

	/**
	 * Cost of a single exam, calculated according to objective function.
	 * Conflicting exams that aren't yet scheduled don't increase penalty.
	 *
	 * @return exam cost
	 */
	double examCost(Exam exam) {
		double sum = 0;
		// Take every conflicting exam
		for(Exam conflicting : exam.conflicts) {
			// Measure distance
			int d = getDistance(conflicting, exam);
			// If conflicting exam hasn't been scheduled
			if(d < 0) {
				// keep looping
				continue;
			}
			// If they're scheduled to the same time slot
			if(d == 0) {
				// This shouldn't happen, hopefully
				System.out.println("Infeasible solution!");
				return Double.MAX_VALUE;
			}
			// If they're close enough to trigger a penalty
			if(d <= 5) {
				// Calculate penalty
				sum += precomputedPowers[d] * Data.getInstance().conflictsBetween(exam, conflicting);
				//System.out.println("Conflict between " + exam.conflictingStudentsCounter.getOrDefault(conflicting, 0) + " students (exam " + exam.getExmID() + " with " + conflicting.getExmID() + ")");
			}
		}
		return sum;
	}

	/**
	 * Return cost of current solution according to objective function
	 * Avoiding the division by nStu (since it's just a scaling factor) didn't provide any significant performance enhancement.
	 *
	 * @return cost
	 */
	double solutionCost() {
		return cost / Data.getInstance().nStu;
	}

	/**
	 * Create a neighbor solution starting from current solution, "unscheduling" a percentage of the exams
	 * and randomly rescheduling them.
	 *
	 * @return New solution (leaves old solution unchanged)
	 */
	@SuppressWarnings("SameParameterValue")
	Solution createNeighbor(double percentage) {
		Solution neighbor = null; // This just prevents the compiler from complaining, but it's guaranteed to be set before returning...
		boolean done = false;

		// TODO: se la % è bassa c'è il rischio che unscheduli 2 esami incastratissimi che ci stanno solo in quel buco e subito ricrea la stessa soluzione? (sì)
		// l'alternativa facile è NON far dipendere la % dalla temperatura, com'era prima...
		if(percentage < 0.04) {
			System.out.println(Thread.currentThread().getName() + " aborting neighbor generation...");
		}
		while(!done) {
			neighbor = new Solution(this);
			done = neighbor.unschedulePercentage(percentage) || neighbor.tryScheduleRemaining();
			if(!done) System.out.println(Thread.currentThread().getName() + " retrying neighbor generation...");
		}

		return neighbor;
	}

	/**
	 * "Unschedule" a percentage of exams.
	 * Make sure that all exams have been scheduled before calling!
	 *
	 * @return true if nothing has been unscheduled (percentage too low) so solution is still valid, false otherwise
	 */
	private boolean unschedulePercentage(double percentage) {
		int j = 0;
		final int nExm = Data.getInstance().nExm;
		final int limit = (int) (nExm * percentage);
		ArrayList<Exam> shuffled;

		if(limit <= 0) {
			return true;
		}

		shuffled = new ArrayList<>(Data.getInstance().getExams().values());
		Collections.shuffle(shuffled);

		while(j < limit) {
			Exam chosen = shuffled.get(j);
			//if(chosen != null && isScheduled(chosen)) {
				unschedule(chosen);
				j++;
			//}
		}
		return false;
	}

	Iterable<? extends Map.Entry<Exam, Integer>> export() {
		return timetable.entrySet();
	}
}
