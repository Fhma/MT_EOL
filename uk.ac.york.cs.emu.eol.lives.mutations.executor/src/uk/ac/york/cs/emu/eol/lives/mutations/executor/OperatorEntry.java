package uk.ac.york.cs.emu.eol.lives.mutations.executor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OperatorEntry {
    private String oID;
    private int total_processed_mutants = 0;
    private int total_live_mutants = 0;
    private int total_equiv_mutants = 0;
    private int totol_invalid_mutants = 0;
    private List<String> killed_mutants_list;

    public OperatorEntry(String _oid) {
	oID = _oid.substring(0, _oid.lastIndexOf("_"));
    }

    public OperatorEntry(File mutant) {
	String str = mutant.getName();
	str = str.substring(0, str.lastIndexOf("_"));
	oID = str;
    }

    public String getOID() {
	return oID;
    }

    public void incrementProcessedMutants() {
	total_processed_mutants++;
    }

    public int getTotalProcessedMutants() {
	return total_processed_mutants;
    }

    public void incrementLiveMutants() {
	total_live_mutants++;
    }

    public int getTotalLiveMutants() {
	return total_live_mutants;
    }

    public void incrementEquivalentMutants() {
	total_equiv_mutants++;
    }

    public int getTotalEquivMutants() {
	return total_equiv_mutants;
    }

    public void incrementInvalidMutants() {
	totol_invalid_mutants++;
    }

    public int getTotalInvalidMutants() {
	return totol_invalid_mutants;
    }

    public void addNotKilledToAList(String mutant) {
	getAllNotKilled().add(mutant);
    }

    public List<String> getAllNotKilled() {
	if (this.killed_mutants_list == null) {
	    killed_mutants_list = new ArrayList<String>();
	}
	return killed_mutants_list;
    }
}
