// This file is part of Codemancer.
// Copyright 2014-2016 Graham Shaw.
// Distribution and modification are permitted within the terms of the
// GNU General Public License (version 3 or any later version).

package org.codemancer.db.jpa;

import java.util.List;
import java.util.ArrayList;
import javax.persistence.EntityManager;

/** A class to represent the collection of basic blocks in a Codemancer database. */
class BasicBlocks implements org.codemancer.db.BasicBlocks {
	/** The database to which this collection belongs. */
	private final Database db;

	/** The entity manager for the database. */
	private final EntityManager em;

	/** Construct collection of basic blocks.
	 * @param db the database
	 * @param em the entity manager for the database
	 */
	protected BasicBlocks(Database db, EntityManager em) {
		this.db = db;
		this.em = em;
	}

	public final BasicBlock make(long minAddr, long maxAddr, boolean fallThrough) {
		BasicBlock bb = new BasicBlock(db.getNextRevision().get(), -1, minAddr, maxAddr, fallThrough);
		em.persist(bb);
		return bb;
	}

	public final org.codemancer.db.BasicBlock getContaining(long addr) {
		List<BasicBlock> blocks = em.createQuery(
			"FROM BasicBlock WHERE minAddr <= :addr AND maxAddr >= :addr", BasicBlock.class)
			.setParameter("addr", addr)
			.getResultList();
		if (blocks.size() == 0) {
			return null;
		} else if (blocks.size() == 1) {
			return blocks.get(0);
		} else {
			throw new IllegalStateException("multiple basic blocks found ending at the same start address");
		}
	}

	public final org.codemancer.db.BasicBlock getPrevious(long addr) {
		List<BasicBlock> blocks = em.createQuery(
			"FROM BasicBlock WHERE maxAddr = :maxAddr", BasicBlock.class)
			.setParameter("maxAddr", addr - 1)
			.getResultList();
		if (blocks.size() == 0) {
			return null;
		} else if (blocks.size() == 1) {
			return blocks.get(0);
		} else {
			throw new IllegalStateException("multiple basic blocks found ending at the same end address");
		}
	}

	public final List<org.codemancer.db.BasicBlock> get() {
		List<BasicBlock> bbs = em.createQuery(
			"FROM BasicBlock ORDER BY minAddr", BasicBlock.class)
			.getResultList();
		return new ArrayList<org.codemancer.db.BasicBlock>(bbs);
	}

	public final List<org.codemancer.db.BasicBlock> getMembersOf(org.codemancer.db.ExtendedBasicBlock ebb) {
		List<BasicBlock> bbs = em.createQuery(
			"FROM BasicBlock WHERE ebb = :ebb", BasicBlock.class)
			.setParameter("ebb", ebb)
			.getResultList();
		return new ArrayList<org.codemancer.db.BasicBlock>(bbs);
	}

	public final List<org.codemancer.db.BasicBlock> getMembersOf(org.codemancer.db.Subroutine sub) {
		List<BasicBlock> bbs = em.createQuery(
			"FROM BasicBlock WHERE ebb.id IN (SELECT id FROM ExtendedBasicBlock WHERE subroutine = :sub)", BasicBlock.class)
			.setParameter("sub", sub)
			.getResultList();
		return new ArrayList<org.codemancer.db.BasicBlock>(bbs);
	}

	public final List<org.codemancer.db.BasicBlock> getUnprocessed(int requiredLevel) {
		List<BasicBlock> bbs = em.createQuery(
			"FROM BasicBlock WHERE processedLevel < :requiredLevel", BasicBlock.class)
			.setParameter("requiredLevel", requiredLevel)
			.getResultList();
		return new ArrayList<org.codemancer.db.BasicBlock>(bbs);
	}

	public final Long findNextDestination(long addr) {
		List<Reference> existingLines = em.createQuery(
			"FROM Reference " +
			"WHERE maxRev = -1 " +
			"AND dstAddr > :addr " +
			"ORDER BY dstAddr", Reference.class)
			.setParameter("addr", addr)
			.setMaxResults(1)
			.getResultList();
		long stopAddr = 0;
		if (!existingLines.isEmpty()) {
			stopAddr = existingLines.get(0).getDstAddr();
		}
		return stopAddr;
	}

	public final long count(long rev) {
		return em.createQuery("SELECT COUNT(minAddr) FROM BasicBlock WHERE (minRev <= :rev) AND ((maxRev >= :rev) OR (maxRev = -1))", Long.class)
			.setParameter("rev", rev)
			.getSingleResult();
	}
}
