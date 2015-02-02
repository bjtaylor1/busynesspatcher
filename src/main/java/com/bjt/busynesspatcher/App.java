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

/**
 * Hello world!
 */


public class App {
    public static void main(String[] args) {
        List<File> csvFiles = new ArrayList<File>();
        String readOsm = null;
        for (String fileName : args) {
            File file = new File(fileName);
            if(!file.exists()) {
                System.out.println(fileName + " does not exist or is not a csv file.");
                System.exit(1);
                return;
            }

            if (file.getName().endsWith(".csv")) {
                csvFiles.add(file);
            }

            if(file.getName().endsWith(".osm")) {
                readOsm = "--read-xml file=\"" + file.getName() +"\"";
            } else if (file.getName().endsWith(".osm.pbf")) {
                readOsm = "--read-pbf file=\"" + file.getName() + "\"";
            }
        }

        if(readOsm == null)  {
            System.out.print("Must specify osm or pbf file.");
            System.exit(1);
            return;
        }

        final File translationsDir = new File("translations");
        if(!translationsDir.exists()) translationsDir.mkdir();


        try {
            final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            final TransformerFactory transformerFactory = TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            for (File file : csvFiles) {
                CSVParser csvParser = CSVParser.parse(file, Charset.defaultCharset(), CSVFormat.DEFAULT);
                final Map<String,Integer> headers = new HashMap<>();
                int row = 0;
                for(CSVRecord csvRecord : csvParser) {
                    if(++row == 1) {
                        for(int i = 0; i < csvRecord.size(); i++) {
                            headers.put(csvRecord.get(i), i);
                        }
                    } else {
                        int easting = Integer.parseInt(csvRecord.get(headers.get("S Ref E")));
                        int northing = Integer.parseInt(csvRecord.get(headers.get("S Ref N")));

                        final int tolerance = 10;

                        final OSRef bottomLeft = new OSRef(easting - tolerance, northing - tolerance);
                        final OSRef topRight = new OSRef(easting + tolerance, northing + tolerance);
                        final LatLng latLngBottomLeft = bottomLeft.toLatLng();
                        final LatLng latLngTopRight = topRight.toLatLng();
                        latLngBottomLeft.toWGS84();
                        latLngTopRight.toWGS84();

                        final String bboxString = String.format("--bb left=%.4f bottom=%.4f right=%.4f top=%.4f",
                            latLngBottomLeft.getLng(), latLngBottomLeft.getLat(),
                            latLngTopRight.getLng(), latLngTopRight.getLat());
                        final String translationFile = String.format("translations/translation.%d.xml", row);
                        final String translation = String.format("--tt %s", translationFile);
                        final String output = String.format("--write-xml translated/translated.%d.osm", row);

                        final StringBuilder cmd = new StringBuilder("osmosis ")
                            .append(readOsm).append(" ")
                            .append(bboxString).append(" ")
                            .append(translation).append(" ")
                            .append(output);


                        Document translationDoc = documentBuilder.newDocument();
                        Element translationsElement, translationElement, matchElement, outputElement, roadTagElement;
                        translationDoc.appendChild(translationsElement = translationDoc.createElement("translations"));
                        translationsElement.appendChild(translationElement = translationDoc.createElement("translation"));
                        translationElement.appendChild(matchElement = translationDoc.createElement("match"));
                        translationElement.appendChild(outputElement = translationDoc.createElement("output"));
                        final String road = csvRecord.get(headers.get("Road")).trim();
                        matchElement.appendChild(roadTagElement = translationDoc.createElement("tag"));
                        roadTagElement.setAttribute("k", "ref");
                        roadTagElement.setAttribute("v", road + ".*");
                        for(Map.Entry<String,Integer> header: headers.entrySet()) {
                            if(header.getKey().startsWith("Fd")) {
                                Element outputTagElement;
                                outputElement.appendChild(outputTagElement = translationDoc.createElement("tag"));
                                final String trafficVolume = csvRecord.get(header.getValue());
                                outputTagElement.setAttribute(header.getKey(), trafficVolume);
                            }
                        }
                        final DOMSource domSource = new DOMSource(translationDoc);
                        final StreamResult streamResult =new StreamResult(new File(translationFile));
                        transformer.transform(domSource, streamResult);

                        System.out.println(cmd.toString());
                        System.err.println(String.format("%s, %s",
                            road, csvRecord.get(headers.get("ONS LA Name"))));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
