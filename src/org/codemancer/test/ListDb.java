// This file is part of Codemancer.
// Copyright 2014 Graham Shaw.
// Distribution and modification are permitted within the terms of the
// GNU General Public License (version 3 or any later version).

package org.codemancer.test;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.codemancer.db.Reference;
import org.codemancer.db.Line;
import org.codemancer.db.BasicBlock;
import org.codemancer.db.ExtendedBasicBlock;
import org.codemancer.db.Subroutine;
import org.codemancer.db.SsaMapping;
import org.codemancer.db.Comment;
import org.codemancer.db.Database;

class ListDb {
	public static final void main(String args[]) throws Exception {
		String projName = args[0];

		// Open connection to database.
		String dbUrl = "jdbc:derby:" + projName;
		Database db = new org.codemancer.db.jpa.Database(dbUrl);

		for (Subroutine sub: db.getSubroutines().get()) {
			System.out.printf("Subroutine %08X\n", sub.getEntryAddr());
			for (ExtendedBasicBlock ebb: db.getExtendedBasicBlocks().getMembersOf(sub)) {
				for (BasicBlock bb: db.getBasicBlocks().getMembersOf(ebb)) {
					List<Reference> references = db.getReferences().getByDstAddr(bb.getMinAddr(), bb.getMinAddr());
					if (!references.isEmpty()) {
						System.out.printf("Xref:");
						boolean first = true;
						for (Reference reference: references) {
							if (first) {
								first = false;
							} else {
								System.out.printf(",");
							}
							System.out.printf(" %08X", reference.getSrcAddr());
						}
						System.out.printf("\n");
					}
					for (Line line: db.getLines().getChanges(0, db.getCurrentRevision().get(), bb.getMinAddr(), bb.getMaxAddr())) {
						String asm = String.format("%08X\t%s", line.getMinAddr(), line.getInstruction());
						List<String> commentLines = new ArrayList<String>();
						for (Comment comment: db.getComments().get(line.getMinAddr())) {
							commentLines.addAll(Arrays.asList(comment.getContent().split("\\n")));
						}
						if (commentLines.isEmpty()) {
							System.out.println(asm);
						} else {
							for (String commentLine: commentLines) {
								System.out.printf("%s\t; %s\n", asm, commentLine);
								asm = "\t";
							}
						}
					}
					System.out.println();
				}
			}
		}
	}
}
