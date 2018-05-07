package uk.ac.york.cs.emu.eol.examples.execution;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OperatorEntry {
	private String oID;
	private int total_processed_mutants = 0;
	private int total_killed_mutants = 0;
	private int total_invalid_mutants = 0;
	private int total_trivial_mutants;
	private List<String> not_killed;

	public OperatorEntry(String _oid) {
		oID = _oid;
	}

	public OperatorEntry(File mutant) {
		String str = mutant.getName();
		str = str.substring(0, str.lastIndexOf("_"));
		oID = str;
	}

	public String getOID() {
		return oID;
	}

	public void incrementProcessed() {
		total_processed_mutants++;
	}

	public int getTotalProcessedMutants() {
		return total_processed_mutants;
	}

	public void incrementInvalid() {
		total_invalid_mutants++;
	}

	public int getTotalInvalidMutants() {
		return total_invalid_mutants;
	}

	public int getKilledMutants() {
		return total_killed_mutants;
	}

	public void incrementKilledMutants() {
		total_killed_mutants++;
	}

	public void addNotKilled(String mutant) {
		getAllNotKilled().add(mutant);
	}

	public List<String> getAllNotKilled() {
		if (this.not_killed == null) {
			not_killed = new ArrayList<String>();
		}
		return not_killed;
	}

	public int getTotalTrivialMutants() {
		return total_trivial_mutants;
	}
}
