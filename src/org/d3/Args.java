/*
 * This file is part of d3.
 * 
 * d3 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * d3 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with d3.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2010 Guilhelm Savin
 */
package org.d3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.d3.tools.Time;

public class Args {
	private HashMap<String, String> innerMap;
	private HashMap<String, Args> children;

	private Args() {
		this.innerMap = new HashMap<String, String>();
		this.children = new HashMap<String, Args>();
	}

	public Args(HashMap<String, String> content) {
		this();

		for (String key : content.keySet())
			__build_args(key, content.get(key));
	}

	private void __build_args(String key, String val) {
		if (key.indexOf('.') != -1) {
			String sub = key.substring(0, key.indexOf('.'));

			if (!children.containsKey(sub))
				children.put(sub, new Args());

			children.get(sub).__build_args(key.substring(key.indexOf('.') + 1),
					val);
		} else {
			innerMap.put(key, val);
		}
	}

	public String get(String key, String def) {
		String s = get(key);
		return s == null ? def : s;
	}

	public String get(String key) {
		if (key.indexOf('.') != -1) {
			String sub = key.substring(0, key.indexOf('.'));

			if (children.containsKey(sub))
				return children.get(sub).get(
						key.substring(key.indexOf('.') + 1));
			else
				return null;
		} else {
			return innerMap.get(key);
		}
	}

	public Boolean getBoolean(String key, boolean def) {
		Boolean b = getBoolean(key);
		return b == null ? def : b;
	}

	public Boolean getBoolean(String key) {
		String s = get(key);

		if (s == null)
			return null;

		return Boolean.valueOf(s);
	}

	public Integer getInteger(String key, int def) {
		Integer i = getInteger(key);
		return i == null ? def : i;
	}

	public Integer getInteger(String key) {
		String s = get(key);

		if (s == null)
			return null;

		return Integer.valueOf(s);
	}

	public Time getTime(String key) {
		String s = get(key);

		if (s == null)
			return null;

		return Time.valueOf(s);
	}

	public boolean has(String key) {
		if (key.indexOf('.') != -1) {
			String sub = key.substring(0, key.indexOf('.'));

			if (children.containsKey(sub))
				return children.get(sub).has(
						key.substring(key.indexOf('.') + 1));
			else
				return false;
		} else
			return innerMap.containsKey(key);
	}

	public Args getArgs(Actor actor) {
		return getArgs(actor.getArgsPrefix());
	}

	public Args getArgs(String key) {
		if (key.indexOf('.') != -1) {
			String sub = key.substring(0, key.indexOf('.'));

			if (children.containsKey(sub))
				return children.get(sub).getArgs(
						key.substring(key.indexOf('.') + 1));
			else
				return new Args();
		} else
			return children.containsKey(key) ? children.get(key) : this;
	}

	public static Args parseArgs(String... args) {
		Args a = new Args();
		return parseArgs(a, args);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return toString(null);
	}

	public String toString(String prefix) {
		StringBuilder buffer = new StringBuilder();

		for (String key : innerMap.keySet()) {
			if (prefix != null)
				buffer.append(prefix).append(".");

			buffer.append(key).append(" = ").append(innerMap.get(key));
			buffer.append("\n");
		}

		for (String key : children.keySet())
			buffer.append(children.get(key).toString(
					prefix == null ? key : prefix + "." + key));

		return buffer.toString();
	}

	public static Args parseArgs(Args args, String... argToParse) {
		if (args == null)
			args = new Args();

		if (argToParse == null || argToParse.length == 0)
			return args;

		Pattern p = Pattern
				.compile("^\\s*(\\w[\\w\\d-_]*(?:[.]\\w[\\w\\d-_]*)*)\\s*=\\s*(.*)\\s*$");

		for (String s : argToParse) {
			Matcher m = p.matcher(s);
			while (m.find()) {
				args.__build_args(m.group(1), m.group(2));
			}
		}

		return args;
	}

	public static Args processFile(String url) {
		Args args = new Args();
		return processFile(args, url);
	}

	public static Args processFile(Args args, String url) {
		BufferedReader in;
		URL u = ClassLoader.getSystemResource(url);
		boolean file = false;

		if (u != null) {
			try {
				in = new BufferedReader(new InputStreamReader(u.openStream()));
			} catch (IOException e1) {
				in = null;
			}
		} else {
			try {
				in = new BufferedReader(new FileReader(url));
				file = true;
			} catch (FileNotFoundException e) {
				in = null;
			}
		}

		if (in != null) {
			try {
				String line;
				Pattern p = Pattern
						.compile("^\\s*(\\w[\\w\\d-_]*(?:[.]\\w[\\w\\d-_]*)*)\\s*=\\s*(.*)\\s*$");
				while (in.ready()) {
					line = in.readLine().trim();

					if (line.startsWith("@input")) {
						String input = line.substring("@input".length() + 1)
								.trim();
						File fInput = new File(input);

						if (!fInput.isAbsolute() && file) {
							File sourceFile = new File(url);
							String input2 = sourceFile.getParent()
									+ File.separator + input;

							Console.warning("'%s' resolves as '%s'", input,
									input2);
							input = input2;
						}

						args = processFile(args, input);
					} else if (!line.startsWith("#") && line.length() > 0) {
						Matcher m = p.matcher(line);

						if (m.matches()) {
							String key = m.group(1);
							String val = m.group(2).trim();

							if (val.matches("(\"[^\"]\"|'|^']')"))
								val = val.substring(1, val.length() - 1);

							args.__build_args(key, val);
						} else
							System.err
									.printf("[args] invalid line: %s%n", line);
					}
				}

				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return args;
		} else {
			System.err.printf("[args] unknown ressource: %s%n", url);
		}

		return null;
	}
}
