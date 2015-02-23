/*******************************************************************************
GTDbfIterator.java
Copyright (C) 2009 ETC-LUSI http://etc-lusi.eionet.europa.eu/

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*******************************************************************************/
package org.geoserver.wps.sextante;

import java.io.IOException;

import org.geotools.data.shapefile.dbf.DbaseFileReader;

import es.unex.sextante.core.Sextante;
import es.unex.sextante.dataObjects.IRecord;
import es.unex.sextante.dataObjects.IRecordsetIterator;
import es.unex.sextante.dataObjects.RecordImpl;
import es.unex.sextante.exceptions.IteratorException;

/**
 * 
 * @author Cesar Martinez Izquierdo
 *
 */
public class GTDBFIterator implements IRecordsetIterator{
	private DbaseFileReader reader = null;
	
	public GTDBFIterator(DbaseFileReader reader) {
		this.reader = reader;
	}

	public void close() {
		try {
			this.reader.close();
		} catch (IOException e) {
			Sextante.addErrorToLog(e);
		}
	}

	public boolean hasNext() {
		return reader.hasNext();
	}

	public IRecord next() throws IteratorException {
		if (reader.hasNext()) {
			try {
				return new RecordImpl(reader.readEntry());
			} catch (IOException e) {
				Sextante.addErrorToLog(e);
			}
		}
		throw new IteratorException();
	}

}
