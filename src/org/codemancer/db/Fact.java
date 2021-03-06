// This file is part of Codemancer.
// Copyright 2014-2016 Graham Shaw.
// Distribution and modification are permitted within the terms of the
// GNU General Public License (version 3 or any later version).

package org.codemancer.db;

/** An interface to represent a fact stored within the database. */
public interface Fact {
	/** A constant to indicate that this fact has not been processed at all. */
	public static final int DONE_NOTHING = 0;

	/** A constant to indicate that this fact has been processed by the iterative disassembler. */
	public static final int DONE_ITERATIVE_DISASSEMBLER = 1;

	/** A constant to indicate that this fact has been processed by the basic block detector. */
	public static final int DONE_BASIC_BLOCK_DETECTOR = 2;

	/** A constant to indicate that this fact has been processed by the basic block detector. */
	public static final int DONE_EXTENDED_BASIC_BLOCK_DETECTOR = 3;

	/** A constant to indicate that this fact has been processed by the subroutine detector. */
	public static final int DONE_SUBROUTINE_DETECTOR = 4;

	/** A constant to indicate that this fact has been processed by the SSA mapper. */
	public static final int DONE_SSA_MAPPER = 5;

	/** A constant to indicate that this fact has been processed by the comment generator. */
	public static final int DONE_COMMENT_GENERATOR = 6;

	/** Get minimum database revision.
	 * @return the lowest database revision to which this fact is applicable
	 */
	long getMinRev();

	/** Get maximum database revision.
	 * @return the highest database revision to which this fact is applicable,
	 *  or -1 for all higher revisions
	 */
	long getMaxRev();

	/** Check whether this fact has been processed to a given level. */
	boolean isProcessed(int requiredLevel);

	/** Mark that this fact has been processed to a given level.
	 * The level is set irrespective of its previous value.
	 * @param processedLevel the level that has been completed
	 */
	void setProcessed(int processedLevel);

	/** Mark that this fact has not been processed to a given level.
	 * The level may be reduced by this operation but is never increased.
	 * @param processedLevel the level that has not been completed
	 */
	void setNotProcessed(int notProcessedLevel);
}
