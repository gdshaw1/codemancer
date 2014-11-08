// This file is part of Codemancer.
// Copyright 2014 Graham Shaw.
// Distribution and modification are permitted within the terms of the
// GNU General Public License (version 3 or any later version).

package org.codemancer.test;

import java.io.RandomAccessFile;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.codemancer.loader.coff.CoffFile;
import org.codemancer.loader.coff.CoffSection;
import org.codemancer.loader.coff.CoffSymbol;

class DumpCoff {
	public static final void main(String args[]) throws Exception {
		PrintWriter out = new PrintWriter(System.err, true);
		RandomAccessFile file = new RandomAccessFile(args[0], "r");
		ByteBuffer image = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
		CoffFile coff = new CoffFile(image);
		coff.dump(out);
		for (short i = 0; i != coff.getCoffSectionCount(); ++i) {
			CoffSection sect = coff.getCoffSection(i);
			out.println();
			out.printf("Section: %d\n", i);
			sect.dump(out);
			out.println();
			for (int j = 0; j != sect.getCoffRelocationCount(); ++j) {
				sect.getCoffRelocation(j).dump(out);
			}
			if (sect.getCoffRelocationCount() == 0) {
				out.println("(no relocations)");
			}
                }
		out.println();
		for (short i = 0; i != coff.getCoffSymbolCount(); ++i) {
			CoffSymbol sym = coff.getCoffSymbol(i);
			if (sym != null) {
				sym.dump(out);
			}
                }
	}
}