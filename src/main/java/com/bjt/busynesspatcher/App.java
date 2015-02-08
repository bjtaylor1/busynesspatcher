package com.bjt.busynesspatcher;

import com.googlecode.jcsv.reader.internal.CSVReaderBuilder;
import com.vividsolutions.jts.geom.util.GeometryEditor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.geotools.geometry.GeneralDirectPosition;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.operation.DefaultCoordinateOperationFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.MathTransform;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import uk.me.jstott.jcoord.LatLng;
import uk.me.jstott.jcoord.OSRef;

import javax.management.StringValueExp;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class App {
	public static void main(String[] args) {
		List<File> csvFiles = new ArrayList<File>();
		for (String fileName : args) {
			File file = new File(fileName);

			if (file.getName().endsWith(".csv")) {
				csvFiles.add(file);
			}
		}


		try {
			for (File file : csvFiles) {
				CSVParser csvParser = CSVParser.parse(file, Charset.defaultCharset(), CSVFormat.DEFAULT);
				int row = 0;
				final List<Integer> indexes = new ArrayList<Integer>();
				int headerE = -1, headerN = -1, headerRoad = -1, headerCategory = -1;
				for(CSVRecord csvRecord : csvParser) {
					if(++row == 1) {
						System.out.print("insert into busyness(ref, geometry, category");
						for(int i = 0; i < csvRecord.size(); i++) {
							final String header = csvRecord.get(i);
							if(header.startsWith("Score")) {
								System.out.print(", " + header);
								indexes.add(i);
							}
							if(header.equals("S Ref E")) headerE = i;
							if(header.equals("S Ref N")) headerN = i;
							if(header.equals("Road")) headerRoad = i;
							if(header.equals("Category")) headerCategory = i;
						}
						System.out.println(") values ");
					} else {

						try {

							int easting = Integer.parseInt(csvRecord.get(headerE));
							int northing = Integer.parseInt(csvRecord.get(headerN));

							if(row > 2) System.out.print(", ");

							final int tolerance = 200;

							final OSRef bottomLeft = new OSRef(easting - tolerance, northing - tolerance);
							final OSRef topRight = new OSRef(easting + tolerance, northing + tolerance);
							final LatLng latLngBottomLeft = bottomLeft.toLatLng();
							final LatLng latLngTopRight = topRight.toLatLng();
							latLngBottomLeft.toWGS84();
							latLngTopRight.toWGS84();

							final double bottom = latLngBottomLeft.getLat(),
										left = latLngBottomLeft.getLng(),
										top = latLngTopRight.getLat(),
										right = latLngTopRight.getLng();

							final String geometryWkt = String.format(
									"POLYGON((%.5f %.5f, %.5f %.5f, %.5f %.5f, %.5f %.5f, %.5f %.5f))",
									left, bottom,
									right, bottom,
									right, top,
									left, top,
									left, bottom
									);

							final String roadRef = csvRecord.get(headerRoad);
							final String category = csvRecord.get(headerCategory);

							System.out.print(String.format("('%s', ST_Transform(ST_GeomFromText('%s', 4326), 900913), '%s'", roadRef, geometryWkt, category));
							for(Integer index : indexes){
								System.out.print(", " + csvRecord.get(index));
							}
							System.out.println(") ");

						}catch(NumberFormatException e) {
							System.err.println(String.format("(Number format exception on row %d", row));
						}
					}
				}
				System.out.println(";");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
