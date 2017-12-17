package it.polito.studenti.oma9;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

class Exam implements Comparable<Exam>, Serializable {
	Map<Integer, Student> students = new TreeMap<>();
	Set<Exam> exmConflict = new TreeSet<>();
	private int exmID;
	/**
	 * @deprecated
	 */
	private Integer timeslot = null;
	private Data data;

	/**
	 * Default constructor
	 *
	 * @param exmID exam ID
	 */
	Exam(int exmID, Data data) {
		this.exmID = exmID;
		this.data = data;
	}

	/**
	 * Getter ExmID
	 *
	 * @return exam ID
	 */
	int getExmID() {
		return exmID;
	}

	/**
	 * Add a student s in the exam
	 *
	 * @param s Student
	 */
	void addStudent(Student s) {
		students.put(s.getStuID(), s);
	}

	/**
	 * Schedule exam in timeslot t
	 *
	 * @param t timeslot
	 * @deprecated
	 */
	void schedule(int t) {
		this.timeslot = t;
	}

	/**
	 * Unschedule the exam
	 *
	 * @deprecated
	 */
	void unschedule() {
		timeslot = null;
	}

	/**
	 * Is it scheduled yet?
	 *
	 * @return True if the exam is scheduled, False otherwise
	 * @deprecated
	 */
	boolean isScheduled() {
		return timeslot != null;
	}

	/**
	 * Get the timeslot where the exam is scheduled
	 *
	 * @return timeslot, null if not scheduled
	 * @deprecated
	 */
	Integer getTimeslot() {
		return timeslot;
	}

	/**
	 * Add a conflicting exam
	 *
	 * @param e exam in conflict
	 */
	void addConflict(Exam e) {
		this.exmConflict.add(e);
	}

	/**
	 * Return a set of all the available time slots (no conflict) in the current context
	 *
	 * @return set of available time slots
	 * @deprecated
	 */
	Set<Integer> timeslotAvailable() {
		Set<Integer> all = new HashSet<>();

		// Start from all timeslots
		for(int i = 1; i <= data.nSlo; i++) {
			all.add(i);
		}

		// Get every conflicting exam
		for(Exam exam : exmConflict) {
			// Is it scheduled somewhere?
			if(exam.isScheduled()) {
				// If it is, remove that time slot
				all.remove(exam.getTimeslot());
			}
		}

		// Return remaining set
		return all;
	}

	/**
	 * Find the number of slots where this exam cannot be placed
	 *
	 * @return number of slots
	 * @deprecated
	 */
	int nTimeslotNoWay() {
		Set<Integer> timeslots = new HashSet<>();

		// For each conflicting exam
		for(Exam conflicting : exmConflict) {
			// If it has been scheduled
			if(conflicting.isScheduled()) {
				// Add that time slot to the list of conflicting ones
				// (Set compares Integer value, not that it is a pointer to same memory location, so everything works fine)
				timeslots.add(conflicting.getTimeslot());
			}
		}

		return timeslots.size();
	}

	/**
	 * Number of conflict of the exam
	 *
	 * @return number of conflicts
	 */
	int nConflict() {
		return exmConflict.size();
	}

	/**
	 * @deprecated
	 * @param nStu
	 * @return
	 */
	double costExam(int nStu) {
		double sum = 0;
		for(Exam e2 : this.exmConflict) {
			int d = Math.abs(e2.getTimeslot() - this.getTimeslot());
			if(d == 0) {
				System.out.println("Unfesible solution!! BAAAAAD");
				return Double.MAX_VALUE;

			}
			if(e2.getExmID() > this.getExmID() && d < 6) {
				long nee = students.entrySet().stream().filter(s -> s.getValue().hasExam(this.getExmID())).filter(s -> s.getValue().hasExam(e2.getExmID())).collect(Collectors.toList()).size();
				sum += Math.pow(2, 5 - d) * nee;
			}
		}
		return sum / nStu;
	}

	/**
	 * @deprecated
	 * @param nStu
	 * @return
	 */
	double examCost(int nStu) {
		double sum = 0;
		for(Exam e2 : this.exmConflict) {
			int d = Math.abs(e2.getTimeslot() - this.getTimeslot());
			if(d == 0) {
				System.out.println("Unfesible solution!! BAAAAAD");
				return Double.MAX_VALUE;

			}
			if(d < 6) {
				long nee = students.entrySet().stream().filter(s -> s.getValue().hasExam(this.getExmID())).filter(s -> s.getValue().hasExam(e2.getExmID())).collect(Collectors.toList()).size();
				sum += Math.pow(2, 5 - d) * nee;
			}
		}
		return sum / nStu;

	}

	/**
	 * @deprecated
	 * @return
	 */
	int getNStuConflict() {
		int stu = 0;
		for(Exam e2 : exmConflict) {
			int d = Math.abs(e2.getTimeslot() - this.getTimeslot());
			if(d < 6) {
				int nee = students.entrySet().stream().filter(s -> s.getValue().hasExam(this.getExmID())).filter(s -> s.getValue().hasExam(e2.getExmID())).collect(Collectors.toList()).size();
				stu += nee;
			}
		}
		return stu;
	}

	// Used by Set, Map key, etc... Exams are the same based on ID only.
	@Override
	public int compareTo(Exam exam) {
		return exam.getExmID() - this.getExmID();
	}
}
