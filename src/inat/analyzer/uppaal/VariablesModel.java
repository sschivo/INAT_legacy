package inat.analyzer.uppaal;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import inat.model.Model;
import inat.model.Reactant;
import inat.model.Reaction;
import inat.util.Table;

/**
 * This class converts the given model into a variable based UPPAAL model.
 * 
 * @author Brend Wanders
 * 
 */
public class VariablesModel implements ModelTransformer {

	private static final int INFINITE_TIME = -1;
	private static String newLine = System.getProperty("line.separator");

	@Override
	public String transform(Model m) {
		StringBuilder out = new StringBuilder();

		this.appendModel(out, m);

		return out.toString();
	}

	private void appendModel(StringBuilder out, Model m) {
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
		out.append("const int MAX_LEVELS = " + m.getProperties().get("levels").as(Integer.class) + ";");
		out.append(newLine);
		out.append("broadcast chan update;");
		out.append(newLine);
		out.append("chan reaction_happening;");
		out.append(newLine);
		out.append("chan reaction_not_happening;");
		out.append(newLine);
		out.append("chan priority update &lt; reaction_happening;");
		out.append(newLine);
		out.append(newLine);
		for (Reactant r : m.getReactants()) {
			this.appendReactantVariables(out, r);
		}
		out.append("</declaration>");

		out.append(newLine);
		out.append(newLine);
		// output default templates (no dynamic content here)
		this.appendTemplates(out);

		out.append(newLine);
		out.append("<system>");
		out.append(newLine);
		
		out.append("Coord = Coordinator(reaction_happening, update);");
		out.append(newLine);
		out.append(newLine);

		// output the process instantiation for each reactant and reaction
		for (Reactant r : m.getReactants()) {
			this.appendReactantProcesses(out, r);
		}
		out.append(newLine);
		for (Reaction r : m.getReactions()) {
			this.appendReactionProcesses(out, m, r);
		}
		out.append(newLine);
		out.append(newLine);

		// compose the system
		out.append("system ");
		for (Reactant r : m.getReactants()) {
			out.append(r.getId() + "_reactant, ");
		}
		for (Reaction r : m.getReactions()) {
			out.append(getReactionName(r) + ", ");
		}
		out.append("Coord;");

		out.append(newLine);
		out.append(newLine);
		out.append("</system>");
		out.append(newLine);
		out.append("</nta>");
	}

	private String formatTime(int time) {
		if (time == INFINITE_TIME) {
			return "INFINITE_TIME";
		} else {
			return "" + time;
		}
	}

	/**
	 * Determines the name of a reaction process.
	 * 
	 * @param r the reaction to name
	 * @return the name of the process
	 */
	private String getReactionName(Reaction r) {
		if (r.get("type").as(String.class).equals("reaction1")) {
			// reaction1 is assumed to be a degredation reaction
			String reactantId = r.get("reactant").as(String.class);
			return reactantId + "_deg";
		} else if (r.get("type").as(String.class).equals("reaction2")) {
			String r1Id = r.get("catalyst").as(String.class);
			String r2Id = r.get("reactant").as(String.class);
			return r1Id + "_" + r2Id + "_r_" + ((r.get("increment").as(Integer.class) >= 0) ? "up" : "down");
		} else {
			return null;
		}
	}

	private void appendReactionProcesses(StringBuilder out, Model m, Reaction r) {
		if (r.get("type").as(String.class).equals("reaction1")) {
			String reactantId = r.get("reactant").as(String.class);
			Table times = r.get("times").as(Table.class);
			assert times.getColumnCount() == 1 : "Table is (larger than one)-dimensional.";
			assert times.getRowCount() == m.getProperties().get("level").as(Integer.class) + 1 : "Incorrect number of rows in 'times' table of '"
					+ r + "'";

			// output times table consants for this reaction
			out.append("const int " + reactantId + "_t[MAX_LEVELS+1] := {");
			for (int i = 0; i < times.getRowCount() - 1; i++) {
				out.append(formatTime(times.get(i, 0)) + ", ");
			}
			out.append(formatTime(times.get(times.getRowCount() - 1, 0)) + "};");
			out.append(newLine);

			// output reaction instantiation
			final String name = getReactionName(r);
			out.append(name + " = Reaction(" + reactantId + ", " + reactantId + "_nonofficial, " + reactantId + "_t, "
					+ r.get("increment").as(Integer.class) + ", update, reaction_happening);");
			out.append(newLine);

		} else if (r.get("type").as(String.class).equals("reaction2")) {
			String r1Id = r.get("catalyst").as(String.class);
			String r2Id = r.get("reactant").as(String.class);

			Table times = r.get("times").as(Table.class);

			assert times.getRowCount() == m.getProperties().get("levels").as(Integer.class) + 1 : "Incorrect number of rows in 'times' table of '"
					+ r + "'.";
			assert times.getColumnCount() == m.getProperties().get("levels").as(Integer.class) + 1 : "Incorrect number of columns in 'times' table of '"
					+ r + "'.";

			// output times table constant for this reaction
			out.append("const int " + r1Id + "_" + r2Id + "_r_t[MAX_LEVELS+1][MAX_LEVELS+1] := {");
			out.append(newLine);

			// for each row
			for (int row = 0; row < times.getRowCount(); row++) {
				out.append("\t\t{");

				// for each column
				for (int col = 0; col < times.getColumnCount(); col++) {
					out.append(formatTime(times.get(row, col)));

					// seperate value with a comma if it is not the last one
					if (col < times.getColumnCount() - 1) {
						out.append(", ");
					}
				}
				out.append("}");

				// end row line with a comma if it is not the last one
				if (row < times.getRowCount() - 1) {
					out.append(",");
				}
				out.append(newLine);
			}

			out.append("};");
			out.append(newLine);
			out.append(newLine);

			// output process instantiation
			final String name = getReactionName(r);
			out.append(name + " = Reaction2(" + r1Id + ", " + r2Id + ", " + r2Id + "_nonofficial, " + r1Id + "_" + r2Id
					+ "_r_t, " + r.get("increment").as(Integer.class)
					+ ", update, reaction_happening);");
			out.append(newLine);
			out.append(newLine);
		}
	}

	private void appendReactantProcesses(StringBuilder out, Reactant r) {
		// output process instantiation
		out.append(r.getId() + "_reactant = Reactant(" + r.getId() + ", " + r.getId() + "_nonofficial, update);");
		out.append(newLine);
		out.append(newLine);
	}

	private void appendTemplates(StringBuilder out) {
		try {
			StringWriter outString = new StringWriter();
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = documentBuilder.parse(new ByteArrayInputStream("<template><name x=\"5\" y=\"5\">Reaction2</name><parameter>int[0,MAX_LEVELS] &amp;reactant1, int[0,MAX_LEVELS] &amp;reactant2, int &amp;reactant2_nonofficial, const int time[MAX_LEVELS+1][MAX_LEVELS+1], const int delta, broadcast chan &amp;update, chan &amp;inform_reacting</parameter><declaration>clock c;</declaration><location id=\"id0\" x=\"-1328\" y=\"-472\"></location><location id=\"id1\" x=\"-1328\" y=\"-864\"></location><location id=\"id2\" x=\"-1064\" y=\"-712\"><committed/></location><location id=\"id3\" x=\"-1328\" y=\"-608\"><label kind=\"invariant\" x=\"-1560\" y=\"-616\">c&lt;=time[reactant2][reactant1]</label></location><location id=\"id4\" x=\"-1328\" y=\"-752\"><committed/></location><init ref=\"id4\"/><transition><source ref=\"id1\"/><target ref=\"id4\"/><label kind=\"synchronisation\" x=\"-1472\" y=\"-768\">update?</label><nail x=\"-1480\" y=\"-864\"/><nail x=\"-1480\" y=\"-752\"/></transition><transition><source ref=\"id3\"/><target ref=\"id2\"/><label kind=\"synchronisation\" x=\"-1216\" y=\"-680\">update?</label><nail x=\"-1240\" y=\"-664\"/><nail x=\"-1136\" y=\"-664\"/></transition><transition><source ref=\"id0\"/><target ref=\"id2\"/><label kind=\"synchronisation\" x=\"-1288\" y=\"-488\">update?</label><nail x=\"-936\" y=\"-472\"/><nail x=\"-936\" y=\"-632\"/></transition><transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1304\" y=\"-880\">time[reactant2][reactant1] == INFINITE_TIME</label><nail x=\"-976\" y=\"-712\"/><nail x=\"-976\" y=\"-864\"/></transition><transition><source ref=\"id4\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1464\" y=\"-816\">time[reactant2][reactant1]== INFINITE_TIME</label></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1296\" y=\"-752\">time[reactant2][reactant1] != INFINITE_TIME&amp;&amp; c&gt;time[reactant2][reactant1]</label><label kind=\"assignment\" x=\"-1296\" y=\"-728\">c:=time[reactant2][reactant1]</label><nail x=\"-1248\" y=\"-712\"/></transition><transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1272\" y=\"-640\">time[reactant2][reactant1] != INFINITE_TIME&amp;&amp; c&lt;=time[reactant2][reactant1]</label><nail x=\"-1064\" y=\"-608\"/></transition><transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"guard\" x=\"-1320\" y=\"-576\">c&gt;=time[reactant2][reactant1]</label><label kind=\"synchronisation\" x=\"-1320\" y=\"-560\">inform_reacting!</label><label kind=\"assignment\" x=\"-1320\" y=\"-544\">reactant2_nonofficial := reactant2_nonofficial + delta,c:=0</label></transition><transition><source ref=\"id4\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1464\" y=\"-704\">time[reactant2][reactant1]   != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1368\" y=\"-680\">c:=0</label></transition></template>".getBytes()));
			Transformer tra = TransformerFactory.newInstance().newTransformer();
			tra.setOutputProperty(OutputKeys.INDENT, "yes");
			tra.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			tra.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			tra.transform(new DOMSource(document), new StreamResult(outString));
			out.append(outString.toString());
			out.append(newLine);
			out.append(newLine);
			outString = new StringWriter();
			document = documentBuilder.parse(new ByteArrayInputStream("<template><name x=\"5\" y=\"5\">Reaction</name><parameter>int[0,MAX_LEVELS] &amp;reactant, int &amp;reactant_nonofficial, const int time[MAX_LEVELS+1], const int delta, broadcast chan &amp;update, chan &amp;inform_reacting</parameter><declaration>clock c;</declaration><location id=\"id5\" x=\"-1320\" y=\"-480\"></location><location id=\"id6\" x=\"-1320\" y=\"-920\"></location><location id=\"id7\" x=\"-1128\" y=\"-712\"><committed/></location><location id=\"id8\" x=\"-1320\" y=\"-624\"><label kind=\"invariant\" x=\"-1464\" y=\"-632\">c&lt;=time[reactant]</label></location><location id=\"id9\" x=\"-1320\" y=\"-816\"><committed/></location><init ref=\"id9\"/><transition><source ref=\"id6\"/><target ref=\"id9\"/><label kind=\"synchronisation\" x=\"-1472\" y=\"-832\">update?</label><nail x=\"-1480\" y=\"-920\"/><nail x=\"-1480\" y=\"-816\"/></transition><transition><source ref=\"id8\"/><target ref=\"id7\"/><label kind=\"synchronisation\" x=\"-1256\" y=\"-696\">update?</label><nail x=\"-1264\" y=\"-680\"/><nail x=\"-1168\" y=\"-680\"/></transition><transition><source ref=\"id5\"/><target ref=\"id7\"/><label kind=\"synchronisation\" x=\"-1256\" y=\"-496\">update?</label><nail x=\"-944\" y=\"-480\"/><nail x=\"-944\" y=\"-656\"/></transition><transition><source ref=\"id7\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1288\" y=\"-936\">time[reactant] == INFINITE_TIME</label><nail x=\"-1040\" y=\"-712\"/><nail x=\"-1040\" y=\"-920\"/></transition><transition><source ref=\"id9\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1440\" y=\"-848\">time[reactant] == INFINITE_TIME</label></transition><transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1272\" y=\"-760\">time[reactant] != INFINITE_TIME&amp;&amp; c&gt;time[reactant]</label><label kind=\"assignment\" x=\"-1272\" y=\"-728\">c:=time[reactant]</label><nail x=\"-1264\" y=\"-712\"/></transition><transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1288\" y=\"-656\">time[reactant] != INFINITE_TIME&amp;&amp; c&lt;=time[reactant]</label><nail x=\"-1128\" y=\"-624\"/></transition><transition><source ref=\"id8\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1312\" y=\"-584\">c&gt;=time[reactant]</label><label kind=\"synchronisation\" x=\"-1312\" y=\"-568\">inform_reacting!</label><label kind=\"assignment\" x=\"-1312\" y=\"-552\">reactant_nonofficial := reactant_nonofficial + delta,c:=0</label></transition><transition><source ref=\"id9\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1432\" y=\"-800\">time[reactant] != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1360\" y=\"-784\">c:=0</label><nail x=\"-1320\" y=\"-656\"/></transition></template>".getBytes()));
			tra.transform(new DOMSource(document), new StreamResult(outString));
			out.append(outString.toString());
			out.append(newLine);
			out.append(newLine);
			outString = new StringWriter();
			document = documentBuilder.parse(new ByteArrayInputStream("<template><name>Reactant</name><parameter>int[0,MAX_LEVELS] &amp;official, int &amp;nonofficial, broadcast chan &amp;update</parameter><location id=\"id10\" x=\"-416\" y=\"-104\"></location><init ref=\"id10\"/><transition><source ref=\"id10\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-536\" y=\"-248\">nonofficial&gt;MAX_LEVELS</label><label kind=\"synchronisation\" x=\"-536\" y=\"-232\">update?</label><label kind=\"assignment\" x=\"-536\" y=\"-216\">official := MAX_LEVELS, nonofficial := MAX_LEVELS</label><nail x=\"-168\" y=\"-200\"/><nail x=\"-168\" y=\"-256\"/><nail x=\"-544\" y=\"-256\"/><nail x=\"-544\" y=\"-192\"/><nail x=\"-416\" y=\"-192\"/></transition><transition><source ref=\"id10\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-496\" y=\"-48\">nonofficial&lt;0</label><label kind=\"synchronisation\" x=\"-496\" y=\"-32\">update?</label><label kind=\"assignment\" x=\"-496\" y=\"-16\">official := 0, nonofficial := 0</label><nail x=\"-416\" y=\"-56\"/><nail x=\"-504\" y=\"-56\"/><nail x=\"-504\" y=\"8\"/><nail x=\"-288\" y=\"8\"/><nail x=\"-288\" y=\"-24\"/></transition><transition><source ref=\"id10\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-680\" y=\"-176\">nonofficial&gt;=0&amp;&amp; nonofficial&lt;=MAX_LEVELS</label><label kind=\"synchronisation\" x=\"-680\" y=\"-144\">update?</label><label kind=\"assignment\" x=\"-680\" y=\"-128\">official := nonofficial</label><nail x=\"-688\" y=\"-104\"/><nail x=\"-688\" y=\"-184\"/><nail x=\"-464\" y=\"-184\"/></transition></template>".getBytes()));
			tra.transform(new DOMSource(document), new StreamResult(outString));
			out.append(outString.toString());
			out.append(newLine);
			out.append(newLine);
			outString = new StringWriter();
			document = documentBuilder.parse(new ByteArrayInputStream("<template><name>Coordinator</name><parameter>chan &amp;reaction_happening, broadcast chan &amp;update</parameter><declaration>int counter;</declaration><location id=\"id11\" x=\"-152\" y=\"-144\"><committed/></location><location id=\"id12\" x=\"-328\" y=\"-200\"></location><init ref=\"id12\"/><transition><source ref=\"id11\"/><target ref=\"id12\"/><label kind=\"synchronisation\" x=\"-312\" y=\"-112\">update!</label><nail x=\"-152\" y=\"-96\"/><nail x=\"-328\" y=\"-96\"/></transition><transition><source ref=\"id11\"/><target ref=\"id11\"/><label kind=\"synchronisation\" x=\"-80\" y=\"-160\">reaction_happening?</label><nail x=\"-88\" y=\"-184\"/><nail x=\"-88\" y=\"-112\"/></transition><transition><source ref=\"id12\"/><target ref=\"id11\"/><label kind=\"synchronisation\" x=\"-304\" y=\"-216\">reaction_happening?</label><nail x=\"-152\" y=\"-200\"/></transition></template>".getBytes()));
			tra.transform(new DOMSource(document), new StreamResult(outString));
			out.append(outString.toString());
			out.append(newLine);
			out.append(newLine);
		} catch (Exception e) {
			System.err.println("Error: " + e);
			e.printStackTrace();
		}
		/*
		out.append("<template>");
		out.append(newLine);
		out.append("<name x=\"5\" y=\"5\">Reaction2</name>");
		out.append(newLine);
		out.append("<parameter>int[0,MAX_LEVELS] &amp;reactant1, int[0,MAX_LEVELS] &amp;reactant2, int &amp;reactant2_nonofficial, const int time[MAX_LEVELS+1][MAX_LEVELS+1], const int delta, broadcast chan &amp;update, chan &amp;inform_reacting, chan &amp;inform_not_reacting</parameter>");
		out.append(newLine);
		out.append("<declaration>// Place local declarations here.");
		out.append(newLine);
		out.append("clock c;</declaration><location id=\"id0\" x=\"-1328\" y=\"-416\"></location>");
		out.append(newLine);
		out.append("<location id=\"id1\" x=\"-1328\" y=\"-864\"></location>");
		out.append(newLine);
		out.append("<location id=\"id2\" x=\"-1064\" y=\"-712\"><committed/></location>");
		out.append(newLine);
		out.append("<location id=\"id3\" x=\"-1328\" y=\"-608\"><label kind=\"invariant\" x=\"-1560\" y=\"-616\">c&lt;=time[reactant2][reactant1]</label></location>");
		out.append(newLine);
		out.append("<location id=\"id4\" x=\"-1328\" y=\"-752\"><committed/></location>");
		out.append(newLine);
		out.append("<init ref=\"id4\"/><transition><source ref=\"id1\"/><target ref=\"id0\"/><label kind=\"synchronisation\" x=\"-1640\" y=\"-896\">inform_not_reacting!</label><label kind=\"assignment\" x=\"-1640\" y=\"-880\">c:=0</label><nail x=\"-1648\" y=\"-864\"/><nail x=\"-1648\" y=\"-416\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id0\"/><target ref=\"id2\"/><label kind=\"synchronisation\" x=\"-1240\" y=\"-432\">update?</label><nail x=\"-824\" y=\"-416\"/><nail x=\"-824\" y=\"-600\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"guard\" x=\"-1640\" y=\"-544\">c&lt;time[reactant2][reactant1]</label><label kind=\"synchronisation\" x=\"-1576\" y=\"-528\">inform_not_reacting!</label><nail x=\"-1424\" y=\"-552\"/><nail x=\"-1424\" y=\"-472\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id2\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1272\" y=\"-880\">time[reactant2][reactant1] == INFINITE_TIME</label><nail x=\"-952\" y=\"-712\"/><nail x=\"-952\" y=\"-864\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id4\"/><target ref=\"id1\"/><label kind=\"guard\" x=\"-1480\" y=\"-824\">time[reactant2][reactant1] == INFINITE_TIME</label></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1296\" y=\"-752\">time[reactant2][reactant1] != INFINITE_TIME");
		out.append(newLine);
		out.append("&amp;&amp; c&gt;time[reactant2][reactant1]</label><label kind=\"assignment\" x=\"-1296\" y=\"-728\">c:=time[reactant2][reactant1]</label><nail x=\"-1248\" y=\"-712\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id2\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1272\" y=\"-640\">time[reactant2][reactant1] != INFINITE_TIME");
		out.append(newLine);
		out.append("&amp;&amp; c&lt;=time[reactant2][reactant1]</label><nail x=\"-1064\" y=\"-608\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id3\"/><target ref=\"id0\"/><label kind=\"guard\" x=\"-1224\" y=\"-544\">c&gt;=time[reactant2][reactant1]</label><label kind=\"synchronisation\" x=\"-1224\" y=\"-528\">inform_reacting!</label><label kind=\"assignment\" x=\"-1224\" y=\"-512\">reactant2_nonofficial := reactant2_nonofficial + delta,");
		out.append(newLine);
		out.append("c:=0</label><nail x=\"-1232\" y=\"-552\"/><nail x=\"-1232\" y=\"-472\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id4\"/><target ref=\"id3\"/><label kind=\"guard\" x=\"-1464\" y=\"-704\">time[reactant2][reactant1] ");
		out.append(newLine);
		out.append("  != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1368\" y=\"-680\">c:=0</label></transition>");
		out.append(newLine);
		out.append("</template>");
		out.append(newLine);
		out.append(newLine);
		out.append("  ");
		out.append("<template>");
		out.append(newLine);
		out.append("<name x=\"5\" y=\"5\">Reaction</name>");
		out.append(newLine);
		out.append("<parameter>int[0,MAX_LEVELS] &amp;reactant, int &amp;reactant_nonofficial, const int time[MAX_LEVELS+1], const int delta, broadcast chan &amp;update, chan &amp;inform_reacting, chan &amp;inform_not_reacting</parameter>");
		out.append(newLine);
		out.append("<declaration>// Place local declarations here.");
		out.append(newLine);
		out.append("clock c;</declaration><location id=\"id5\" x=\"-1320\" y=\"-424\"></location>");
		out.append(newLine);
		out.append("<location id=\"id6\" x=\"-1320\" y=\"-920\"></location>");
		out.append(newLine);
		out.append("<location id=\"id7\" x=\"-1128\" y=\"-712\"><committed/></location>");
		out.append(newLine);
		out.append("<location id=\"id8\" x=\"-1320\" y=\"-624\"><label kind=\"invariant\" x=\"-1464\" y=\"-632\">c&lt;=time[reactant]</label></location>");
		out.append(newLine);
		out.append("<location id=\"id9\" x=\"-1320\" y=\"-816\"><committed/></location>");
		out.append(newLine);
		out.append("<init ref=\"id9\"/><transition><source ref=\"id6\"/><target ref=\"id5\"/><label kind=\"synchronisation\" x=\"-1560\" y=\"-952\">inform_not_reacting!</label><label kind=\"assignment\" x=\"-1560\" y=\"-937\">c:=0</label><nail x=\"-1560\" y=\"-920\"/><nail x=\"-1560\" y=\"-424\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id5\"/><target ref=\"id7\"/><label kind=\"synchronisation\" x=\"-1256\" y=\"-440\">update?</label><nail x=\"-864\" y=\"-424\"/><nail x=\"-864\" y=\"-656\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id8\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1512\" y=\"-552\">c&lt;time[reactant]</label><label kind=\"synchronisation\" x=\"-1536\" y=\"-536\">inform_not_reacting!</label><nail x=\"-1384\" y=\"-568\"/><nail x=\"-1384\" y=\"-480\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id7\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1248\" y=\"-936\">time[reactant] == INFINITE_TIME</label><nail x=\"-1008\" y=\"-712\"/><nail x=\"-1008\" y=\"-920\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id9\"/><target ref=\"id6\"/><label kind=\"guard\" x=\"-1440\" y=\"-848\">time[reactant] == INFINITE_TIME</label></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1272\" y=\"-760\">time[reactant] != INFINITE_TIME");
		out.append(newLine);
		out.append("&amp;&amp; c&gt;time[reactant]</label><label kind=\"assignment\" x=\"-1272\" y=\"-728\">c:=time[reactant]</label><nail x=\"-1264\" y=\"-712\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id7\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1288\" y=\"-656\">time[reactant] != INFINITE_TIME");
		out.append(newLine);
		out.append("&amp;&amp; c&lt;=time[reactant]</label><nail x=\"-1128\" y=\"-624\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id8\"/><target ref=\"id5\"/><label kind=\"guard\" x=\"-1248\" y=\"-552\">c&gt;=time[reactant]</label><label kind=\"synchronisation\" x=\"-1248\" y=\"-536\">inform_reacting!</label><label kind=\"assignment\" x=\"-1248\" y=\"-520\">reactant_nonofficial := reactant_nonofficial + delta,");
		out.append(newLine);
		out.append("c:=0</label><nail x=\"-1256\" y=\"-568\"/><nail x=\"-1256\" y=\"-480\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id9\"/><target ref=\"id8\"/><label kind=\"guard\" x=\"-1432\" y=\"-800\">time[reactant] != INFINITE_TIME</label><label kind=\"assignment\" x=\"-1360\" y=\"-784\">c:=0</label><nail x=\"-1320\" y=\"-656\"/></transition>");
		out.append(newLine);
		out.append("</template>");
		out.append(newLine);
		out.append(newLine);
		out.append("<template>");
		out.append(newLine);
		out.append("<name>Reactant</name>");
		out.append(newLine);
		out.append("<parameter>int[0,MAX_LEVELS] &amp;official, int &amp;nonofficial, broadcast chan &amp;update</parameter>");
		out.append(newLine);
		out.append("<location id=\"id10\" x=\"-416\" y=\"-104\"></location>");
		out.append(newLine);
		out.append("<init ref=\"id10\"/><transition><source ref=\"id10\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-536\" y=\"-248\">nonofficial&gt;MAX_LEVELS</label><label kind=\"synchronisation\" x=\"-536\" y=\"-232\">update?</label><label kind=\"assignment\" x=\"-536\" y=\"-216\">official := MAX_LEVELS, nonofficial := MAX_LEVELS</label><nail x=\"-168\" y=\"-200\"/><nail x=\"-168\" y=\"-256\"/><nail x=\"-544\" y=\"-256\"/><nail x=\"-544\" y=\"-192\"/><nail x=\"-416\" y=\"-192\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id10\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-496\" y=\"-48\">nonofficial&lt;0</label><label kind=\"synchronisation\" x=\"-496\" y=\"-32\">update?</label><label kind=\"assignment\" x=\"-496\" y=\"-16\">official := 0, nonofficial := 0</label><nail x=\"-416\" y=\"-56\"/><nail x=\"-504\" y=\"-56\"/><nail x=\"-504\" y=\"8\"/><nail x=\"-288\" y=\"8\"/><nail x=\"-288\" y=\"-24\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id10\"/><target ref=\"id10\"/><label kind=\"guard\" x=\"-680\" y=\"-176\">nonofficial&gt;=0");
		out.append(newLine);
		out.append("&amp;&amp; nonofficial&lt;=MAX_LEVELS</label><label kind=\"synchronisation\" x=\"-680\" y=\"-144\">update?</label><label kind=\"assignment\" x=\"-680\" y=\"-128\">official := nonofficial</label><nail x=\"-688\" y=\"-104\"/><nail x=\"-688\" y=\"-184\"/><nail x=\"-464\" y=\"-184\"/></transition>");
		out.append(newLine);
		out.append("</template>");
		out.append(newLine);
		out.append(newLine);
		out.append("<template>");
		out.append(newLine);
		out.append("<name>Coordinator</name>");
		out.append(newLine);
		out.append("<parameter>chan &amp;reaction_happening, chan &amp;reaction_not_happening, broadcast chan &amp;update, broadcast chan &amp;reset[N_REACTANTS]</parameter>");
		out.append(newLine);
		out.append("<declaration>int counter;</declaration><location id=\"id11\" x=\"-376\" y=\"-112\"><committed/></location>");
		out.append(newLine);
		out.append("<location id=\"id12\" x=\"-152\" y=\"-152\"><label kind=\"invariant\" x=\"-344\" y=\"-160\">counter&lt;=N_REACTIONS</label><committed/></location>");
		out.append(newLine);
		out.append("<location id=\"id13\" x=\"-376\" y=\"-200\"></location>");
		out.append(newLine);
		out.append("<init ref=\"id13\"/><transition><source ref=\"id12\"/><target ref=\"id12\"/><label kind=\"synchronisation\" x=\"56\" y=\"-176\">reaction_not_happening?</label><label kind=\"assignment\" x=\"56\" y=\"-160\">counter:= counter +1</label><nail x=\"-136\" y=\"-200\"/><nail x=\"56\" y=\"-200\"/><nail x=\"56\" y=\"-112\"/><nail x=\"-136\" y=\"-112\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id12\"/><target ref=\"id11\"/><label kind=\"guard\" x=\"-344\" y=\"-112\">counter &gt;= N_REACTIONS</label><nail x=\"-152\" y=\"-112\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id11\"/><target ref=\"id13\"/><label kind=\"synchronisation\" x=\"-488\" y=\"-176\">update!</label><label kind=\"assignment\" x=\"-488\" y=\"-160\">counter := 0</label><nail x=\"-432\" y=\"-112\"/><nail x=\"-432\" y=\"-200\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id12\"/><target ref=\"id12\"/><label kind=\"synchronisation\" x=\"-104\" y=\"-176\">reaction_happening?</label><label kind=\"assignment\" x=\"-104\" y=\"-160\">counter := counter+1</label><nail x=\"-104\" y=\"-192\"/><nail x=\"-104\" y=\"-128\"/></transition>");
		out.append(newLine);
		out.append("<transition><source ref=\"id13\"/><target ref=\"id12\"/><label kind=\"synchronisation\" x=\"-352\" y=\"-232\">reaction_happening?</label><label kind=\"assignment\" x=\"-352\" y=\"-216\">counter := 1</label><nail x=\"-152\" y=\"-200\"/></transition>");
		out.append(newLine);
		out.append("</template>");
		out.append(newLine);
		out.append(newLine);
		out.append("<system>");
		out.append(newLine);
		out.append("// Place template instantiations here.");
		out.append(newLine);*/
	}

	private void appendReactantVariables(StringBuilder out, Reactant r) {
		// outputs the global variables necessary for the given reactant
		out.append("int[0,MAX_LEVELS] " + r.getId() + " := " + r.get("initialConcentration").as(Integer.class) + ";");
		out.append(newLine);
		out.append("int " + r.getId() + "_nonofficial := " + r.get("initialConcentration").as(Integer.class) + ";");
		out.append(newLine);
		out.append(newLine);
	}

}
