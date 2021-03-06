// This file is part of Codemancer.
// Copyright 2014 Graham Shaw.
// Distribution and modification are permitted within the terms of the
// GNU General Public License (version 3 or any later version).

package org.codemancer.analysis;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.ArrayList;

import org.codemancer.loader.ObjectFile;
import org.codemancer.cpudl.expr.Expression;
import org.codemancer.cpudl.expr.Register;
import org.codemancer.cpudl.Architecture;
import org.codemancer.cpudl.BitReader;
import org.codemancer.db.Fact;
import org.codemancer.db.Line;
import org.codemancer.db.Reference;
import org.codemancer.db.BasicBlock;
import org.codemancer.db.ExtendedBasicBlock;
import org.codemancer.db.Database;

/** A class for detecting extended basic blocks. */
public class ExtendedBasicBlockDetector {
	/** The object file to be disassembled. */
	private ObjectFile obj;

	/** A database corresponding to the object file. */
	private Database db;

	/** The architecture to be used when disassembling. */
	private Architecture arch;

	/** A list of pending unprocessed blocks. */
	private List<BasicBlock> pendingList = new ArrayList<BasicBlock>();

	/** The index of the first unprocessed block in the pending list.
	 * If no blocks are pending then this is equal to the length of the list.
	 */
	private int pendingIndex = 0;

	/** True if processing will be finished once the end of the pending list
	 * is reached, otherwise false. */
	private boolean done = false;

	/** Construct extended basic block detector.
	 * @param obj the object file to be disassembled
	 * @param db the database corresponding to the object file
	 * @param arch the architecture
	 */
	public ExtendedBasicBlockDetector(ObjectFile obj, Database db, Architecture arch) {
		this.obj = obj;
		this.db = db;
		this.arch = arch;
	}

	/** Process the next unprocessed basic block.
	 * @param pc the program counter
	 * @param links a list of possible expressions for a subroutine return address
	 * @return true if all pending blocks have been processed, otherwise false
	 */
	public boolean detectNext(Register pc, List<Expression> links) {
		if (pendingIndex == pendingList.size()) {
			if (done) return true;
			pendingList = db.getBasicBlocks().getUnprocessed(Fact.DONE_EXTENDED_BASIC_BLOCK_DETECTOR);
			pendingIndex = 0;
			if (pendingList.size() == 0) return true;
			done = true;
		}

		BasicBlock block = pendingList.get(pendingIndex);
		if (!block.isProcessed(Fact.DONE_EXTENDED_BASIC_BLOCK_DETECTOR)) {
			long addr = block.getMinAddr();
			List<Reference> references = db.getReferences().getByDstAddr(addr, addr);

			// Determine whether this basic block is part of an existing basic block.
			ExtendedBasicBlock ebb = null;
			BasicBlock prevBlock = db.getBasicBlocks().getPrevious(addr);
			if ((prevBlock != null) && prevBlock.canFallThrough()) {
				ebb = prevBlock.getExtendedBasicBlock();
			}
			for (Reference reference: references) {
				if (reference.isCodeRef()) {
					ebb = null;
				}
			}

			if (ebb == null) {
				ebb = db.getExtendedBasicBlocks().make(addr);
			}
			block.setExtendedBasicBlock(ebb);
			block.setProcessed(Fact.DONE_EXTENDED_BASIC_BLOCK_DETECTOR);
			done = false;
		}
		pendingIndex += 1;
		return false;
	}
}
