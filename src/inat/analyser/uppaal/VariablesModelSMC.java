package inat.analyser.uppaal;

import inat.model.Model;
import inat.model.Property;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.util.Table;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

public class VariablesModelSMC extends VariablesModel {

	@Override
	protected void appendModel(StringBuilder out, Model m) {
		out.append("<?xml version='1.0' encoding='utf-8'?>");
		out.append(newLine);
		out.append("<!DOCTYPE nta PUBLIC '-//Uppaal Team//DTD Flat System 1.1//EN' 'http://www.it.uu.se/research/group/darts/uppaal/flat-1_1.dtd'>");
		out.append(newLine);
		out.append("<nta>");
		out.append(newLine);
		out.append("<declaration>");
		out.append(newLine);
		
		// output global declarations
		out.append("// Place global declarations here.");
		out.append(newLine);
		out.append("clock globalTime;");
		out.append(newLine);
		out.append("const int INFINITE_TIME = " + INFINITE_TIME + ";");
		out.append(newLine);
		int countReactants = 0;
		for (Reactant r : m.getReactants()) {
			if (r.get(ENABLED).as(Boolean.class)) {
				countReactants++;
			}
		}
		out.append("const int N_REACTANTS = " + countReactants + ";");
		out.append(newLine);
		out.append("broadcast chan reaction_happening[N_REACTANTS];");
		out.append(newLine);
		out.append(newLine);
		
		int reactantIndex = 0;
		for (Reactant r : m.getReactants()) {
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			r.let("index").be(reactantIndex);
			reactantIndex++; 
			this.appendReactantVariables(out, r);
		}
		out.append("</declaration>");
		
		out.append(newLine);
		out.append(newLine);
		// output templates
		this.appendTemplates(out, m);
		
		out.append(newLine);
		out.append("<system>");
		out.append(newLine);
		
		int reactionIndex = 0;
		for (Reaction r : m.getReactions()) {
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			this.appendReactionProcesses(out, m, r, reactionIndex);
			reactionIndex++;
		}
		out.append(newLine);
		out.append(newLine);

		//out.append("Crono = crono();");
		out.append(newLine);
		out.append(newLine);
		
		// compose the system
		out.append("system ");
		Iterator<Reaction> iter = m.getReactions().iterator();
		boolean first = true;
		while (iter.hasNext()) {
			Reaction r = iter.next();
			if (!r.get(ENABLED).as(Boolean.class)) continue;
			if (!first) {
				out.append(", ");
			}
			out.append(getReactionName(r));
			first = false;
		}
		//out.append(", Crono;");
		out.append(";");
		
		out.append(newLine);
		out.append(newLine);
		out.append("</system>");
		out.append(newLine);
		out.append("</nta>");
	}

	@Override
	protected void appendReactionProcesses(StringBuilder out, Model m, Reaction r, int index) {
		//NOTICE THAT index IS NOT USED HERE!!
		index = -1;
		
		if (r.get("type").as(String.class).equals("reaction1")) {
			String reactantId = r.get("reactant").as(String.class);
			out.append("//Mono-reaction on " + reactantId + " (" + m.getReactant(reactantId).get("alias").as(String.class) + ")");
			out.append(newLine);
			
			Table timesL, timesU;
			Property property = r.get("timesL");
			if (property != null) {
				timesL = property.as(Table.class);
			} else {
				timesL = r.get("times").as(Table.class);
			}
			property = r.get("timesU");
			if (property != null) {
				timesU = property.as(Table.class);
			} else {
				timesU = r.get("times").as(Table.class);
			}
			assert timesL.getColumnCount() == 1 : "Table LowerBound is (larger than one)-dimensional.";
			assert timesU.getColumnCount() == 1 : "Table UpperBound is (larger than one)-dimensional.";
			assert timesL.getRowCount() == m.getReactant(reactantId).get("levels").as(Integer.class) + 1 : "Incorrect number of rows in 'timesLower' table of '" + r + "'";
			assert timesU.getRowCount() == m.getReactant(reactantId).get("levels").as(Integer.class) + 1 : "Incorrect number of rows in 'timesUpper' table of '" + r + "'";
			
			// output times table constants for this reaction (lower bound)
			out.append("const int " + reactantId + "_tLower[" + m.getReactant(reactantId).get("levels").as(Integer.class) + "+1] := {");
			for (int i = 0; i < timesL.getRowCount() - 1; i++) {
				out.append(formatTime(timesL.get(i, 0)) + ", ");
			}
			out.append(formatTime(timesL.get(timesL.getRowCount() - 1, 0)) + "};");
			out.append(newLine);
			
			// output times table constants for this reaction (upper bound)
			out.append("const int " + reactantId + "_tUpper[" + m.getReactant(reactantId).get("levels").as(Integer.class) + "+1] := {");
			for (int i = 0; i < timesU.getRowCount() - 1; i++) {
				out.append(formatTime(timesU.get(i, 0)) + ", ");
			}
			out.append(formatTime(timesU.get(timesU.getRowCount() - 1, 0)) + "};");
			out.append(newLine);

			// output reaction instantiation
			final String name = getReactionName(r);
			out.append(name + " = Reaction_" + reactantId + "(" + reactantId + ", " + reactantId + "_tLower, "
					+ reactantId + "_tUpper, " + r.get("increment").as(Integer.class) + ", reaction_happening[" + m.getReactant(reactantId).get("index").as(Integer.class) + "]);");
			out.append(newLine);
			out.append(newLine);

		} else if (r.get("type").as(String.class).equals("reaction2")) {
			String r1Id = r.get("catalyst").as(String.class);
			String r2Id = r.get("reactant").as(String.class);
			out.append("//Reaction " + r1Id + " (" + m.getReactant(r1Id).get("alias").as(String.class) + ") " + (r.get("increment").as(Integer.class)>0?"-->":"--|") + " " + r2Id + " (" + m.getReactant(r2Id).get("alias").as(String.class) + ")");
			out.append(newLine);
			
			Table timesL, timesU;
			Property property = r.get("timesL");
			if (property != null) {
				timesL = property.as(Table.class);
			} else {
				timesL = r.get("times").as(Table.class);
			}
			property = r.get("timesU");
			if (property != null) {
				timesU = property.as(Table.class);
			} else {
				timesU = r.get("times").as(Table.class);
			}

			assert timesL.getRowCount() == m.getReactant(r2Id).get("levels").as(Integer.class) + 1 : "Incorrect number of rows in 'times lower' table of '"
					+ r + "'.";
			assert timesU.getRowCount() == m.getReactant(r2Id).get("levels").as(Integer.class) + 1 : "Incorrect number of rows in 'times upper' table of '"
				+ r + "'.";
			assert timesL.getColumnCount() == m.getReactant(r1Id).get("levels").as(Integer.class) + 1 : "Incorrect number of columns in 'times lower' table of '"
					+ r + "'.";
			assert timesU.getColumnCount() == m.getReactant(r1Id).get("levels").as(Integer.class) + 1 : "Incorrect number of columns in 'times upper' table of '"
				+ r + "'.";
			
			// output times table constant for this reaction
			out.append("const int " + r1Id + "_" + r2Id + "_r_tLower[" + m.getReactant(r2Id).get("levels").as(Integer.class) + "+1][" + m.getReactant(r1Id).get("levels").as(Integer.class) + "+1] := {");
			out.append(newLine);
			
			// for each row
			for (int row = 0; row < timesL.getRowCount(); row++) {
				out.append("\t\t{");
				
				// for each column
				for (int col = 0; col < timesL.getColumnCount(); col++) {
					out.append(formatTime(timesL.get(row, col)));
					
					// seperate value with a comma if it is not the last one
					if (col < timesL.getColumnCount() - 1) {
						out.append(", ");
					}
				}
				out.append("}");

				// end row line with a comma if it is not the last one
				if (row < timesL.getRowCount() - 1) {
					out.append(",");
				}
				out.append(newLine);
			}

			out.append("};");
			out.append(newLine);
			
			// output times table constant for this reaction
			out.append("const int " + r1Id + "_" + r2Id + "_r_tUpper[" + m.getReactant(r2Id).get("levels").as(Integer.class) + "+1][" + m.getReactant(r1Id).get("levels").as(Integer.class) + "+1] := {");
			out.append(newLine);

			// for each row
			for (int row = 0; row < timesU.getRowCount(); row++) {
				out.append("\t\t{");

				// for each column
				for (int col = 0; col < timesU.getColumnCount(); col++) {
					out.append(formatTime(timesU.get(row, col)));

					// seperate value with a comma if it is not the last one
					if (col < timesU.getColumnCount() - 1) {
						out.append(", ");
					}
				}
				out.append("}");

				// end row line with a comma if it is not the last one
				if (row < timesU.getRowCount() - 1) {
					out.append(",");
				}
				out.append(newLine);
			}

			out.append("};");
			out.append(newLine);
			out.append(newLine);

			// output process instantiation
			final String name = getReactionName(r);
			out.append(name + " = Reaction2_" + r1Id + "_" + r2Id + "(" + r1Id + ", " + r2Id + ", " + r1Id + "_" + r2Id
					+ "_r_tLower, " + r1Id + "_" + r2Id + "_r_tUpper, " + r.get("increment").as(Integer.class)
					+ ", reaction_happening[" + m.getReactant(r1Id).get("index").as(Integer.class) + "], reaction_happening[" + m.getReactant(r2Id).get("index").as(Integer.class) + "]);");
			out.append(newLine);
			out.append(newLine);
		}
	}


	@Override
	protected void appendTemplates(StringBuilder out, Model m) {
		try {
			StringWriter outString;
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document;
			Transformer tra = TransformerFactory.newInstance().newTransformer();
			tra.setOutputProperty(OutputKeys.INDENT, "yes");
			tra.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			tra.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			/*outString = new StringWriter();
			document = documentBuilder.parse(new ByteArrayInputStream(("<template><name>crono</name><declaration>int[0, 1073741821] metro := 0;</declaration><location id=\"id0\" x=\"0\" y=\"0\"><label kind=\"invariant\" x=\"-176\" y=\"-24\">globalTime&lt;=metro+1</label></location><init ref=\"id0\"/><transition><source ref=\"id0\"/><target ref=\"id0\"/><label kind=\"guard\" x=\"56\" y=\"-24\">globalTime&gt;=metro</label><label kind=\"assignment\" x=\"56\" y=\"0\">metro:=metro+1</label><nail x=\"56\" y=\"-48\"/><nail x=\"56\" y=\"48\"/></transition></template>").getBytes()));
			tra.transform(new DOMSource(document), new StreamResult(outString));
			out.append(outString.toString());
			out.append(newLine);
			out.append(newLine);*/
			for (Reaction r : m.getReactions()) {
				if (!r.get(ENABLED).as(Boolean.class)) continue;
				outString = new StringWriter();
				if (r.get("type").as(String.class).equals("reaction2")) {
					document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction2_" + r.get("catalyst").as(String.class) + "_" + r.get("reactant").as(String.class) + "</name><parameter>int &amp;reactant1, int &amp;reactant2, const int timeL[" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "+1][" + m.getReactant(r.get("catalyst").as(String.class)).get("levels").as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "+1][" + m.getReactant(r.get("catalyst").as(String.class)).get("levels").as(Integer.class) + "+1], const int delta, broadcast chan &amp;r1_reacting, broadcast chan &amp;r2_reacting</parameter><declaration>// Place local declarations here.\nclock c;\nint r1, r2;</declaration><location id=\"id0\" x=\"-1328\" y=\"-952\"><name x=\"-1338\" y=\"-982\">s2</name></location><location id=\"id1\" x=\"-1064\" y=\"-800\"><name x=\"-1074\" y=\"-830\">s4</name><urgent/></location><location id=\"id2\" x=\"-1328\" y=\"-696\"><name x=\"-1338\" y=\"-726\">s3</name><label kind=\"invariant\" x=\"-1568\" y=\"-720\">timeU[r2][r1] == INFINITE_TIME\n|| c&lt;=timeU[r2][r1]</label></location><location id=\"id3\" x=\"-1328\" y=\"-840\"><name x=\"-1352\" y=\"-864\">s1</name><urgent/></location><init ref=\"id3\"/><transition><source ref=\"id0\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1592\" y=\"-1016\">r1_reacting?</label><label kind=\"assignment\" x=\"-1592\" y=\"-1000\">c:=0</label><nail x=\"-1360\" y=\"-984\"/><nail x=\"-1616\" y=\"-984\"/><nail x=\"-1616\" y=\"-512\"/><nail x=\"-784\" y=\"-512\"/><nail x=\"-784\" y=\"-736\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1184\" y=\"-784\">r2_reacting?</label><nail x=\"-1248\" y=\"-768\"/><nail x=\"-1096\" y=\"-768\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1096\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; reactant2+delta&gt;" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1096\" y=\"-608\">r2_reacting!</label><label kind=\"assignment\" x=\"-1096\" y=\"-592\">reactant2:=" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + ",\nc:=0</label><nail x=\"-1280\" y=\"-656\"/><nail x=\"-1104\" y=\"-656\"/><nail x=\"-1104\" y=\"-560\"/><nail x=\"-848\" y=\"-560\"/><nail x=\"-848\" y=\"-704\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1576\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; reactant2+delta&lt;0</label><label kind=\"synchronisation\" x=\"-1576\" y=\"-608\">r2_reacting!</label><label kind=\"assignment\" x=\"-1576\" y=\"-592\">reactant2:=0,\nc:=0</label><nail x=\"-1416\" y=\"-656\"/><nail x=\"-1584\" y=\"-656\"/><nail x=\"-1584\" y=\"-544\"/><nail x=\"-816\" y=\"-544\"/><nail x=\"-816\" y=\"-720\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1224\" y=\"-768\">r1_reacting?</label><nail x=\"-1248\" y=\"-752\"/><nail x=\"-1088\" y=\"-752\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1384\" y=\"-648\">c&gt;=timeL[r2][r1]\n&amp;&amp; reactant2+delta&gt;=0\n&amp;&amp; reactant2+delta&lt;=" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1384\" y=\"-608\">r2_reacting!</label><label kind=\"assignment\" x=\"-1384\" y=\"-592\">reactant2:=reactant2+delta,\nc:=0</label><nail x=\"-1328\" y=\"-656\"/><nail x=\"-1392\" y=\"-656\"/><nail x=\"-1392\" y=\"-552\"/><nail x=\"-832\" y=\"-552\"/><nail x=\"-832\" y=\"-712\"/></transition><transition><source ref=\"id0\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1592\" y=\"-984\">r2_reacting?</label><label kind=\"assignment\" x=\"-1592\" y=\"-968\">c:=0</label><nail x=\"-1600\" y=\"-952\"/><nail x=\"-1600\" y=\"-528\"/><nail x=\"-800\" y=\"-528\"/><nail x=\"-800\" y=\"-728\"/></transition><transition><source ref=\"id1\"/><target ref=\"id0\"/><label kind=\"guard\" x=\"-1272\" y=\"-968\">timeL[reactant2][reactant1] == INFINITE_TIME</label><nail x=\"-952\" y=\"-800\"/><nail x=\"-952\" y=\"-952\"/></transition><transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"guard\" x=\"-1480\" y=\"-912\">timeL[reactant2][reactant1] == INFINITE_TIME</label></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1296\" y=\"-840\">timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant2][reactant1]</label><label kind=\"assignment\" x=\"-1296\" y=\"-816\">c:=timeU[reactant2][reactant1],\nr1:=reactant1,\nr2:=reactant2</label><nail x=\"-1248\" y=\"-800\"/></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1272\" y=\"-752\">(timeU[reactant2][reactant1] == INFINITE_TIME\n&amp;&amp; timeL[reactant2][reactant1] != INFINITE_TIME)\n|| (timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&lt;=timeU[reactant2][reactant1])</label><label kind=\"assignment\" x=\"-1272\" y=\"-704\">r1:=reactant1,\nr2:=reactant2</label><nail x=\"-1064\" y=\"-696\"/></transition><transition><source ref=\"id3\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1528\" y=\"-824\">timeL[reactant2][reactant1] \n  != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1448\" y=\"-792\">r1 := reactant1,\nr2 := reactant2,\nc:=0</label></transition></template>").getBytes()));
					//Alternative version, without the r1_reacting? and r2_reacting? transitions between s3 and s4: does not work very well: reactions are too much isolated
					//document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction2_" + r.get("catalyst").as(String.class) + "_" + r.get("reactant").as(String.class) + "</name><parameter>int &amp;reactant1, int &amp;reactant2, const int timeL[" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "+1][" + m.getReactant(r.get("catalyst").as(String.class)).get("levels").as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "+1][" + m.getReactant(r.get("catalyst").as(String.class)).get("levels").as(Integer.class) + "+1], const int delta, broadcast chan &amp;r1_reacting, broadcast chan &amp;r2_reacting</parameter><declaration>// Place local declarations here.\nclock c;\nint r1, r2;</declaration><location id=\"id0\" x=\"-1328\" y=\"-952\"><name x=\"-1338\" y=\"-982\">s2</name></location><location id=\"id1\" x=\"-1064\" y=\"-800\"><name x=\"-1074\" y=\"-830\">s4</name><urgent/></location><location id=\"id2\" x=\"-1328\" y=\"-696\"><name x=\"-1338\" y=\"-726\">s3</name><label kind=\"invariant\" x=\"-1568\" y=\"-720\">timeU[r2][r1] == INFINITE_TIME\n|| c&lt;=timeU[r2][r1]</label></location><location id=\"id3\" x=\"-1328\" y=\"-840\"><name x=\"-1352\" y=\"-864\">s1</name><urgent/></location><init ref=\"id3\"/><transition><source ref=\"id0\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1592\" y=\"-1016\">r1_reacting?</label><label kind=\"assignment\" x=\"-1592\" y=\"-1000\">c:=0</label><nail x=\"-1360\" y=\"-984\"/><nail x=\"-1616\" y=\"-984\"/><nail x=\"-1616\" y=\"-512\"/><nail x=\"-784\" y=\"-512\"/><nail x=\"-784\" y=\"-736\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1096\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; reactant2+delta&gt;" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1096\" y=\"-608\">r2_reacting!</label><label kind=\"assignment\" x=\"-1096\" y=\"-592\">reactant2:=" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + ",\nc:=0</label><nail x=\"-1280\" y=\"-656\"/><nail x=\"-1104\" y=\"-656\"/><nail x=\"-1104\" y=\"-560\"/><nail x=\"-848\" y=\"-560\"/><nail x=\"-848\" y=\"-704\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1576\" y=\"-640\">c&gt;=timeL[r2][r1]\n&amp;&amp; reactant2+delta&lt;0</label><label kind=\"synchronisation\" x=\"-1576\" y=\"-608\">r2_reacting!</label><label kind=\"assignment\" x=\"-1576\" y=\"-592\">reactant2:=0,\nc:=0</label><nail x=\"-1416\" y=\"-656\"/><nail x=\"-1584\" y=\"-656\"/><nail x=\"-1584\" y=\"-544\"/><nail x=\"-816\" y=\"-544\"/><nail x=\"-816\" y=\"-720\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1384\" y=\"-648\">c&gt;=timeL[r2][r1]\n&amp;&amp; reactant2+delta&gt;=0\n&amp;&amp; reactant2+delta&lt;=" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1384\" y=\"-608\">r2_reacting!</label><label kind=\"assignment\" x=\"-1384\" y=\"-592\">reactant2:=reactant2+delta,\nc:=0</label><nail x=\"-1328\" y=\"-656\"/><nail x=\"-1392\" y=\"-656\"/><nail x=\"-1392\" y=\"-552\"/><nail x=\"-832\" y=\"-552\"/><nail x=\"-832\" y=\"-712\"/></transition><transition><source ref=\"id0\"/><target ref=\"id1\"/><label kind=\"synchronisation\" x=\"-1592\" y=\"-984\">r2_reacting?</label><label kind=\"assignment\" x=\"-1592\" y=\"-968\">c:=0</label><nail x=\"-1600\" y=\"-952\"/><nail x=\"-1600\" y=\"-528\"/><nail x=\"-800\" y=\"-528\"/><nail x=\"-800\" y=\"-728\"/></transition><transition><source ref=\"id1\"/><target ref=\"id0\"/><label kind=\"guard\" x=\"-1272\" y=\"-968\">timeL[reactant2][reactant1] == INFINITE_TIME</label><nail x=\"-952\" y=\"-800\"/><nail x=\"-952\" y=\"-952\"/></transition><transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"guard\" x=\"-1480\" y=\"-912\">timeL[reactant2][reactant1] == INFINITE_TIME</label></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1296\" y=\"-840\">timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant2][reactant1]</label><label kind=\"assignment\" x=\"-1296\" y=\"-816\">c:=timeU[reactant2][reactant1],\nr1:=reactant1,\nr2:=reactant2</label><nail x=\"-1248\" y=\"-800\"/></transition><transition><source ref=\"id1\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1272\" y=\"-752\">(timeU[reactant2][reactant1] == INFINITE_TIME\n&amp;&amp; timeL[reactant2][reactant1] != INFINITE_TIME)\n|| (timeU[reactant2][reactant1] != INFINITE_TIME\n&amp;&amp; c&lt;=timeU[reactant2][reactant1])</label><label kind=\"assignment\" x=\"-1272\" y=\"-704\">r1:=reactant1,\nr2:=reactant2</label><nail x=\"-1064\" y=\"-696\"/></transition><transition><source ref=\"id3\"/><target ref=\"id2\"/><label kind=\"guard\" x=\"-1528\" y=\"-824\">timeL[reactant2][reactant1] \n  != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1448\" y=\"-792\">r1 := reactant1,\nr2 := reactant2,\nc:=0</label></transition></template>").getBytes()));
				} else {
					document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get("reactant").as(String.class) + "</name><parameter>int &amp;reactant, const int timeL[" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "+1], const int delta, broadcast chan &amp;inform_reacting</parameter><declaration>// Place local declarations here.\nclock c;\nint r;</declaration><location id=\"id4\" x=\"-1328\" y=\"-952\"><name x=\"-1338\" y=\"-982\">s2</name></location><location id=\"id5\" x=\"-1064\" y=\"-800\"><name x=\"-1074\" y=\"-830\">s4</name><urgent/></location><location id=\"id6\" x=\"-1328\" y=\"-696\"><name x=\"-1338\" y=\"-726\">s3</name><label kind=\"invariant\" x=\"-1528\" y=\"-720\">timeU[r] == INFINITE_TIME\n|| c&lt;=timeU[r]</label></location><location id=\"id7\" x=\"-1328\" y=\"-840\"><name x=\"-1352\" y=\"-864\">s1</name><urgent/></location><init ref=\"id7\"/><transition><source ref=\"id6\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1096\" y=\"-640\">c&gt;=timeL[r]\n&amp;&amp; reactant+delta&gt;" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1096\" y=\"-608\">inform_reacting!</label><label kind=\"assignment\" x=\"-1096\" y=\"-592\">reactant:=" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + ",\nc:=0</label><nail x=\"-1280\" y=\"-656\"/><nail x=\"-1104\" y=\"-656\"/><nail x=\"-1104\" y=\"-560\"/><nail x=\"-848\" y=\"-560\"/><nail x=\"-848\" y=\"-704\"/></transition><transition><source ref=\"id6\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1576\" y=\"-640\">c&gt;=timeL[r]\n&amp;&amp; reactant+delta&lt;0</label><label kind=\"synchronisation\" x=\"-1576\" y=\"-608\">inform_reacting!</label><label kind=\"assignment\" x=\"-1576\" y=\"-592\">reactant:=0,\nc:=0</label><nail x=\"-1416\" y=\"-656\"/><nail x=\"-1584\" y=\"-656\"/><nail x=\"-1584\" y=\"-544\"/><nail x=\"-816\" y=\"-544\"/><nail x=\"-816\" y=\"-720\"/></transition><transition><source ref=\"id6\"/><target ref=\"id5\"/><label kind=\"synchronisation\" x=\"-1248\" y=\"-768\">inform_reacting?</label><nail x=\"-1256\" y=\"-752\"/><nail x=\"-1120\" y=\"-752\"/></transition><transition><source ref=\"id6\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1384\" y=\"-648\">c&gt;=timeL[r]\n&amp;&amp; reactant+delta&gt;=0\n&amp;&amp; reactant+delta&lt;=" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1384\" y=\"-608\">inform_reacting!</label><label kind=\"assignment\" x=\"-1384\" y=\"-592\">reactant:=reactant+delta,\nc:=0</label><nail x=\"-1328\" y=\"-656\"/><nail x=\"-1392\" y=\"-656\"/><nail x=\"-1392\" y=\"-552\"/><nail x=\"-832\" y=\"-552\"/><nail x=\"-832\" y=\"-712\"/></transition><transition><source ref=\"id4\"/><target ref=\"id5\"/><label kind=\"synchronisation\" x=\"-1592\" y=\"-984\">inform_reacting?</label><label kind=\"assignment\" x=\"-1592\" y=\"-968\">c:=0</label><nail x=\"-1600\" y=\"-952\"/><nail x=\"-1600\" y=\"-528\"/><nail x=\"-800\" y=\"-528\"/><nail x=\"-800\" y=\"-728\"/></transition><transition><source ref=\"id5\"/><target ref=\"id4\"/><label kind=\"guard\" x=\"-1272\" y=\"-968\">timeL[reactant] == INFINITE_TIME</label><nail x=\"-952\" y=\"-800\"/><nail x=\"-952\" y=\"-952\"/></transition><transition><source ref=\"id7\"/><target ref=\"id4\"/><label kind=\"guard\" x=\"-1432\" y=\"-912\">timeL[reactant] == INFINITE_TIME</label></transition><transition><source ref=\"id5\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1296\" y=\"-840\">timeU[reactant] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant]</label><label kind=\"assignment\" x=\"-1296\" y=\"-816\">c:=timeU[reactant],\nr:=reactant</label><nail x=\"-1248\" y=\"-800\"/></transition><transition><source ref=\"id5\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1272\" y=\"-744\">(timeU[reactant] == INFINITE_TIME\n&amp;&amp; timeL[reactant] != INFINITE_TIME)\n|| (timeU[reactant] != INFINITE_TIME\n&amp;&amp; c&lt;=timeU[reactant])</label><label kind=\"assignment\" x=\"-1272\" y=\"-696\">r:=reactant</label><nail x=\"-1064\" y=\"-680\"/><nail x=\"-1280\" y=\"-680\"/></transition><transition><source ref=\"id7\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1456\" y=\"-824\">timeL[reactant] \n  != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1456\" y=\"-800\">r := reactant,\nc:=0</label></transition></template>").getBytes()));
					//Alternative version, without the inform_reacting? transition between s3 and s4: does not work very well (see comment above)
					//document = documentBuilder.parse(new ByteArrayInputStream(("<template><name x=\"5\" y=\"5\">Reaction_" + r.get("reactant").as(String.class) + "</name><parameter>int &amp;reactant, const int timeL[" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "+1], const int timeU[" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "+1], const int delta, broadcast chan &amp;inform_reacting</parameter><declaration>// Place local declarations here.\nclock c;\nint r;</declaration><location id=\"id4\" x=\"-1328\" y=\"-952\"><name x=\"-1338\" y=\"-982\">s2</name></location><location id=\"id5\" x=\"-1064\" y=\"-800\"><name x=\"-1074\" y=\"-830\">s4</name><urgent/></location><location id=\"id6\" x=\"-1328\" y=\"-696\"><name x=\"-1338\" y=\"-726\">s3</name><label kind=\"invariant\" x=\"-1528\" y=\"-720\">timeU[r] == INFINITE_TIME\n|| c&lt;=timeU[r]</label></location><location id=\"id7\" x=\"-1328\" y=\"-840\"><name x=\"-1352\" y=\"-864\">s1</name><urgent/></location><init ref=\"id7\"/><transition><source ref=\"id6\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1096\" y=\"-640\">c&gt;=timeL[r]\n&amp;&amp; reactant+delta&gt;" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1096\" y=\"-608\">inform_reacting!</label><label kind=\"assignment\" x=\"-1096\" y=\"-592\">reactant:=" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + ",\nc:=0</label><nail x=\"-1280\" y=\"-656\"/><nail x=\"-1104\" y=\"-656\"/><nail x=\"-1104\" y=\"-560\"/><nail x=\"-848\" y=\"-560\"/><nail x=\"-848\" y=\"-704\"/></transition><transition><source ref=\"id6\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1576\" y=\"-640\">c&gt;=timeL[r]\n&amp;&amp; reactant+delta&lt;0</label><label kind=\"synchronisation\" x=\"-1576\" y=\"-608\">inform_reacting!</label><label kind=\"assignment\" x=\"-1576\" y=\"-592\">reactant:=0,\nc:=0</label><nail x=\"-1416\" y=\"-656\"/><nail x=\"-1584\" y=\"-656\"/><nail x=\"-1584\" y=\"-544\"/><nail x=\"-816\" y=\"-544\"/><nail x=\"-816\" y=\"-720\"/></transition><transition><source ref=\"id6\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1384\" y=\"-648\">c&gt;=timeL[r]\n&amp;&amp; reactant+delta&gt;=0\n&amp;&amp; reactant+delta&lt;=" + m.getReactant(r.get("reactant").as(String.class)).get("levels").as(Integer.class) + "</label><label kind=\"synchronisation\" x=\"-1384\" y=\"-608\">inform_reacting!</label><label kind=\"assignment\" x=\"-1384\" y=\"-592\">reactant:=reactant+delta,\nc:=0</label><nail x=\"-1328\" y=\"-656\"/><nail x=\"-1392\" y=\"-656\"/><nail x=\"-1392\" y=\"-552\"/><nail x=\"-832\" y=\"-552\"/><nail x=\"-832\" y=\"-712\"/></transition><transition><source ref=\"id4\"/><target ref=\"id5\"/><label kind=\"synchronisation\" x=\"-1592\" y=\"-984\">inform_reacting?</label><label kind=\"assignment\" x=\"-1592\" y=\"-968\">c:=0</label><nail x=\"-1600\" y=\"-952\"/><nail x=\"-1600\" y=\"-528\"/><nail x=\"-800\" y=\"-528\"/><nail x=\"-800\" y=\"-728\"/></transition><transition><source ref=\"id5\"/><target ref=\"id4\"/><label kind=\"guard\" x=\"-1272\" y=\"-968\">timeL[reactant] == INFINITE_TIME</label><nail x=\"-952\" y=\"-800\"/><nail x=\"-952\" y=\"-952\"/></transition><transition><source ref=\"id7\"/><target ref=\"id4\"/><label kind=\"guard\" x=\"-1432\" y=\"-912\">timeL[reactant] == INFINITE_TIME</label></transition><transition><source ref=\"id5\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1296\" y=\"-840\">timeU[reactant] != INFINITE_TIME\n&amp;&amp; c&gt;timeU[reactant]</label><label kind=\"assignment\" x=\"-1296\" y=\"-816\">c:=timeU[reactant],\nr:=reactant</label><nail x=\"-1248\" y=\"-800\"/></transition><transition><source ref=\"id5\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1272\" y=\"-744\">(timeU[reactant] == INFINITE_TIME\n&amp;&amp; timeL[reactant] != INFINITE_TIME)\n|| (timeU[reactant] != INFINITE_TIME\n&amp;&amp; c&lt;=timeU[reactant])</label><label kind=\"assignment\" x=\"-1272\" y=\"-696\">r:=reactant</label><nail x=\"-1064\" y=\"-680\"/><nail x=\"-1280\" y=\"-680\"/></transition><transition><source ref=\"id7\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1456\" y=\"-824\">timeL[reactant] \n  != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1456\" y=\"-800\">r := reactant,\nc:=0</label></transition></template>").getBytes()));
				}
				tra.transform(new DOMSource(document), new StreamResult(outString));
				out.append(outString.toString());
				out.append(newLine);
				out.append(newLine);
			}
		} catch (Exception e) {
			System.err.println("Error: " + e);
			e.printStackTrace();
		}
	}
	
	@Override
	protected void appendReactantVariables(StringBuilder out, Reactant r) {
		// outputs the global variables necessary for the given reactant
		out.append("//" + r.getId() + " = " + r.get("alias").as(String.class));
		out.append(newLine);
		out.append("int " + r.getId() + " := " + r.get("initialConcentration").as(Integer.class) + ";");
		out.append(newLine);
		out.append(newLine);
	}

}
