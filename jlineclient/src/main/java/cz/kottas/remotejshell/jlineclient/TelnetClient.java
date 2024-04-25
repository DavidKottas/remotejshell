package cz.kottas.remotejshell.jlineclient;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.TerminalBuilder;


public class TelnetClient {
	public static final String HISTORY_FILE_PATH= System.getProperty("user.home") + "/.telnet_history";
	private static String last_command = "";
	private static OutputStream output;
	private static BufferedReader reader;

	static JsonNode executeCommand(String cmd) throws Exception {
		output.write(cmd.getBytes(StandardCharsets.UTF_8));
		output.write("\n".getBytes(StandardCharsets.UTF_8));
		output.flush();

		String res = reader.readLine();
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readTree(res);
	}
	static class SuggestCompleter implements Completer {
		@Override
		public void complete(LineReader lineReader, ParsedLine parsedLine, List<Candidate> list) {
			try {
				String cmd = "/suggest " + parsedLine.cursor() + " " +  parsedLine.line();
				var root = executeCommand(cmd);

				// todo also suggest all command from whole history of commands

				if (!root.has("suggest"))
					return;
				int suggestAnchor = 0;
				if (root.has("suggestAnchor"))
					suggestAnchor = root.get("suggestAnchor").asInt();
				for (var suggest : root.get("suggest")) {
					String s = suggest.asText();

					// drop all from begin to last space (if there is some space)
					String prefix = parsedLine.line().substring(0, parsedLine.cursor());
					int delimiter_pos = prefix.lastIndexOf(" ");
					if (delimiter_pos != -1)
						prefix = prefix.substring(delimiter_pos + 1, prefix.length());

					// build suggest item
					var c = prefix +
							s.substring(parsedLine.cursor() - suggestAnchor);
					list.add(new Candidate(c, s, null, null, null, null, true, 0));
				}
			} catch (Exception e) {}
		}
	}

	static void append_history(String line) {
		try {
			new File(HISTORY_FILE_PATH).createNewFile();
			try (var output = new BufferedWriter(new FileWriter(HISTORY_FILE_PATH, true))) {
				output.write(line);
				output.write("\n");
			}
		} catch (Exception e) {
			System.err.println("Cannot append to a history file: " + e.toString());
		}
	}

	static void load_history(LineReader jline) {
		try (BufferedReader reader = new BufferedReader(new FileReader(HISTORY_FILE_PATH))) {
			String line;
			while ((line = reader.readLine()) != null) {
				jline.getHistory().add(line);
				last_command = line;
			}
		} catch (Exception e) {
			System.err.println("Cannot readhistory file " + HISTORY_FILE_PATH + "? " + e.toString());
		}
	}

	public static void main(String[] args) throws Exception {
		String hostname = args[0];
		int port = Integer.parseInt(args[1]);
		// TODO cmdline --help
		try (Socket socket = new Socket(hostname, port)) {
			output = socket.getOutputStream();
			InputStream input = socket.getInputStream();
			reader = new BufferedReader(new InputStreamReader(input));

			LineReader jline = LineReaderBuilder.builder()
					.completer(new SuggestCompleter())
					.terminal(TerminalBuilder.builder().build())
					.parser(new DefaultParser())
					.build();
			load_history(jline);

			while (true) {
				try {
					String line = jline.readLine(hostname + ":" + port + " > ");
					var root = executeCommand(line);
					System.out.println(root.toString());
					if (!line.equals(last_command)) {
						last_command = line;
						append_history(line); // TODO do this only if command was successfull
					}
				} catch (UserInterruptException e) {
					// Ignore
				}
			}
		} catch (UnknownHostException ex) {
			System.err.println("Server not found: " + ex.getMessage());
		} catch (IOException ex) {
			System.err.println("I/O error: " + ex.getMessage());
		} catch (Exception ex) {
			System.err.println("Exception: " + ex.getMessage());
		}
	}
}