// This file is part of Codemancer.
// Copyright 2014 Graham Shaw.
// Distribution and modification are permitted within the terms of the
// GNU General Public License (version 3 or any later version).

package org.codemancer.test;

import java.io.RandomAccessFile;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.codemancer.loader.aof.AofFile;
import org.codemancer.loader.aof.AofHeaderChunk;
import org.codemancer.loader.aof.AofSymbolTableChunk;
import org.codemancer.loader.aof.AofArea;

class DumpAof {
	public static final void main(String args[]) throws Exception {
		PrintWriter out = new PrintWriter(System.err, true);
		RandomAccessFile file = new RandomAccessFile(args[0], "r");
		ByteBuffer image = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
		AofFile aof = new AofFile(image);
		AofHeaderChunk header = aof.getHeaderChunk();
		aof.dump(out);
		for (int i = 0; i != aof.getMaxChunks(); ++i) {
			out.println();
			aof.getChunk(i).dump(out);
		}
		for (int i = 0; i != header.getAofAreaCount(); ++i) {
			out.println();
			AofArea area = header.getAofArea(i);
			area.dump(out);
			out.println();
			for (int j = 0; j != area.getAofRelocationCount(); ++j) {
				area.getAofRelocation(j).dump(out);
			}
			if (area.getAofRelocationCount() == 0) {
				out.println("(no relocations)");
			}
		}
		out.println();
		AofSymbolTableChunk symtab = aof.getSymbolTableChunk();
		for (int i = 0; i != symtab.getAofSymbolCount(); ++i) {
			symtab.getAofSymbol(i).dump(out);
		}
	}
}
