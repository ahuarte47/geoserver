/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import java.util.NoSuchElementException;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.collection.DecoratingFeatureCollection;
import org.geotools.feature.collection.DecoratingFeatureIterator;
import org.geotools.geometry.jts.coordinatesequence.CoordinateSequences;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

import com.vividsolutions.jts.algorithm.CGAlgorithms;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * A feature collection which fixes the wise of the rings of the polygon features.
 * 
 * @author Alvaro Huarte
 */
class FeatureWiseFeatureCollection<T extends FeatureType, F extends Feature> extends DecoratingFeatureCollection<T, F> {
    
    /**
     * FeatureCollection to change the wise of the rings of the polygon features.
     */
    @SuppressWarnings("unchecked")
    public FeatureWiseFeatureCollection(FeatureCollection<? extends FeatureType, ? extends Feature> features) {
        super((FeatureCollection<T, F>) features);
    }
    
    /**
     * FeatureIterator to change the wise of the rings of the polygon features.
     */
    class WiseFeatureIterator extends DecoratingFeatureIterator<F> {
        
        public WiseFeatureIterator(FeatureIterator<F> iterator) {
            super(iterator);
        }
        
        @Override
        public F next() throws NoSuchElementException {
            F feature = super.next();
            
            if (feature != null) {
                final Geometry geometry = feature.getDefaultGeometryProperty()!=null ? (Geometry)feature.getDefaultGeometryProperty().getValue() : null;
                if (geometry != null) fixWiseGeometry(geometry);
            }
            return feature;
        }
        
        /**
         * Computes whether a ring defined by an array of {@link Coordinate}s is oriented counter-clockwise.
         * It is a copy of original code in JTS.
         */
        private boolean CGAlgorithms_isCCW(CoordinateSequence ring)
        {
            // # of points without closing endpoint
            int nPts = ring.size() - 1;
            // sanity check
            if (nPts < 3) return false;
            
            // find highest point
            Coordinate hiPt = ring.getCoordinate(0);
            int hiIndex = 0;
            for (int i = 1; i <= nPts; i++) {
              Coordinate p = ring.getCoordinate(i);
              if (p.y > hiPt.y) {
                hiPt = p;
                hiIndex = i;
              }
            }
            
            // find distinct point before highest point
            int iPrev = hiIndex;
            do {
              iPrev = iPrev - 1;
              if (iPrev < 0) iPrev = nPts;
            } while (ring.getCoordinate(iPrev).equals2D(hiPt) && iPrev != hiIndex);
            
            // find distinct point after highest point
            int iNext = hiIndex;
            do {
              iNext = (iNext + 1) % nPts;
            } while (ring.getCoordinate(iNext).equals2D(hiPt) && iNext != hiIndex);
            
            Coordinate prev = ring.getCoordinate(iPrev);
            Coordinate next = ring.getCoordinate(iNext);
            
            /**
             * This check catches cases where the ring contains an A-B-A configuration
             * of points. This can happen if the ring does not contain 3 distinct points
             * (including the case where the input array has fewer than 4 elements), or
             * it contains coincident line segments.
             */
            if (prev.equals2D(hiPt) || next.equals2D(hiPt) || prev.equals2D(next))
              return false;
            
            int disc = CGAlgorithms.computeOrientation(prev, hiPt, next);
            
            /**
             * If disc is exactly 0, lines are collinear. There are two possible cases:
             * (1) the lines lie along the x axis in opposite directions (2) the lines
             * lie on top of one another
             * 
             * (1) is handled by checking if next is left of prev ==> CCW (2) will never
             * happen if the ring is valid, so don't check for it (Might want to assert
             * this)
             */
            boolean isCCW = false;
            if (disc == 0) {
              // poly is CCW if prev x is right of next x
              isCCW = (prev.x > next.x);
            }
            else {
              // if area is positive, points are ordered CCW
              isCCW = (disc > 0);
            }
            return isCCW;
        }
        
        // Reverse the wise of geometry points of the specified ring.
        private boolean reverseGeometry(LineString ring) {
            if (ring.getNumPoints() > 0 && ring.isClosed()) {
                CoordinateSequence coordinateSequence = ring.getCoordinateSequence();
                CoordinateSequences.reverse(coordinateSequence);
                return true;
            }
            return false;
        }
        // Fix the wise of geometry points of the specified geomery.
        private boolean fixWiseGeometry(Geometry geometry) {
            boolean changed = false;
            
            if (geometry instanceof Polygon) {
                Polygon polygon = (Polygon)geometry;
                LineString ring = polygon.getExteriorRing();
                
                if (CGAlgorithms_isCCW(ring.getCoordinateSequence())) {
                    changed |= reverseGeometry(ring);
                    for (int i = 0, icount = polygon.getNumInteriorRing(); i < icount; i++) changed |= reverseGeometry(polygon.getInteriorRingN(i));
                }
            }
            else
            if (geometry instanceof MultiPolygon) {
                MultiPolygon mtpolygon = (MultiPolygon)geometry;
                for (int i = 0, icount = mtpolygon.getNumGeometries(); i < icount; i++) changed |= fixWiseGeometry(mtpolygon.getGeometryN(i));
            }
            return changed;
        }
    }
    
    @Override
    public FeatureIterator<F> features() {
        return new WiseFeatureIterator(delegate.features());
    }
}
