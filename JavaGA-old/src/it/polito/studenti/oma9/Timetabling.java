package it.polito.studenti.oma9;

import io.jenetics.*;
import io.jenetics.engine.*;
import io.jenetics.util.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.function.Function;

public class Timetabling implements Problem<ISeq<IntegerGene>, IntegerGene, Double> {
	private int S;
	private int e;
	private int ts;
	private IntRange domain;
	private Data data;
	/**
	 * e×e matrix with number of conflicting students
	 */
	private static int[][] conflicts;

	/**
	 * Build problem representation
	 *
	 * @param data THE data
	 */
	Timetabling(Data data) {
		this.S = data.nStu;
		this.e = data.nExm;
		this.ts = data.nSlo;
		this.domain = IntRange.of(1, this.ts); // Estremo destro escluso?
		this.data = data;
		Timetabling.conflicts = data.conflictTable; // questo è LO SCHIFO, la classe è un singleton forzosamente ma non so che farci
	}

	public Function<ISeq<IntegerGene>, Double> fitness() {
		return schedule -> {
			int distance;

			// calcola la penalità
			double penalty = 0;
			for(int i = 0; i < this.e; i++) {
				for(int j = 0; j < i; j++) {
					// se 2 esami sono in conflitto
					if(conflicts[i][j] > 0) {
						// calcola la distanza...
						distance = Math.abs(schedule.get(i).intValue() - schedule.get(j).intValue());
						if(distance <= 5) {
							// ...che non può essere 0 altrimenti il valdiatore sta sbagliando tutto
							if(distance <= 0) {
								// TODO: remove commented-out code
								//throw new RuntimeException("SOVRAPPOSTIIIIIIIIIIIIIIIIII");
								return Double.POSITIVE_INFINITY;
							}
							// calcola la penalità
							penalty += Math.pow(2.0, 5.0 - (double) distance) * (double) conflicts[i][j];
						}
					}
				}
			}

			System.out.println("Penalty: " + (penalty / (double) this.S));
			return penalty / (double) this.S;
		};
	}

	// la documentazione di questa parte FA SCHIFO, è contraddittoria e caotica e sull'orlo dell'inesistenza,
	// ci sono voluti 20 tentativi prima di trovare qualcosa di funzionante
	public Codec<ISeq<IntegerGene>, IntegerGene> codec() {
		return Codec.of(
				this::genotypeFactory,
				gt -> gt.getChromosome().toSeq()
		);
	}

	private Genotype<IntegerGene> genotypeFactory() {
		IntegerGene[] chromosome = new IntegerGene[this.e];
		IntegerChromosome theRealChromosome;

		int[] intermediate = this.data.createFFS();
		for(int i = 0; i < intermediate.length; i++) {
			chromosome[i] = geneFactory(intermediate[i]);
		}

		theRealChromosome = IntegerChromosome.of(chromosome);
		//System.out.println("Ho creato dal nulla: " + theRealChromosome.toString());
		System.out.println("FFS created with penalty " + fitness().apply(theRealChromosome.toSeq()));

		return Genotype.of(theRealChromosome);
	}

	private IntegerGene geneFactory(int value) {
		return IntegerGene.of(value, domain);
	}


	static boolean validator(Genotype<IntegerGene> gt) {
		Map<Integer, Set<Integer>> TimeslotConflicts = new TreeMap<>();
		ISeq<IntegerGene> genes = gt.getChromosome().toSeq();

		int len = genes.length();

		for(int exam = 0; exam < len; exam++) {
			Integer timeslot = genes.get(exam).intValue();

			if(!TimeslotConflicts.containsKey(timeslot)) {
				TimeslotConflicts.put(timeslot, new TreeSet<>());
			}

			Set<Integer> sovrapposti = TimeslotConflicts.get(timeslot);

			for(int eprime : sovrapposti) {
				if(eprime == exam) {
					// TODO: replace with console.log or remove before consegnare il codice
					throw new RuntimeException("L'ottimizzazione non è ottimizzata!");
				}
				if(conflicts[exam][eprime] > 0) {
					System.out.println("Unfeasible :'(");
					//System.out.println("Invalid: " + gt);
					return false;
				}
			}

			sovrapposti.add(exam);
		}
		System.out.println("Valid");
		//System.out.println("OK: " + gt);
		return true;
	}

	static void printResult(ISeq<IntegerGene> result, String filename) {
		try {
			FileOutputStream file = new FileOutputStream(filename + ".sol");
			PrintStream Output = new PrintStream(file);

			for (int i = 0; i < result.length(); i++) {
				Output.println((i + 1) + " " + result.get(i).intValue());
			}
		} catch (IOException e) {
			System.out.println("Error writing solution to file: " + e);
			System.exit(1);
		}
	}
}
