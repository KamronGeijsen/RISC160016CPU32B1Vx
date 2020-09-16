package assembler;

import java.util.ArrayList;

public class MacroAssemblerLexer {

	private final ArrayList<String[]> macroParsed = new ArrayList<String[]>();
	private final int[] currentNumberedLabel = new int[10];
	
	public ArrayList<String[]> macroAssemblerLexer(ArrayList<String> macroAssembly) {
		for(String s : macroAssembly)
			lexer(s, macroParsed);
		return macroParsed;
	}

	public void lexer(String line, ArrayList<String[]> macroParsed) {
//		System.out.println(line);
		int len = line.length();
		if (len == 0)
			return;
		char[] chars = line.toCharArray();

		StringBuilder command = new StringBuilder();

		int i = 0;
		char c = chars[i];
		while (i < len) {
			if (c == ' ' || c == '\t') {
				while (++i < len && ((c = chars[i]) == ' ' || c == '\t'))
					;
			} else if (c == ':') {
				if (command.length() == 0)
					throw new LexException("Invalid Labelname");
				String labelName = command.toString();
				if (labelName.matches("[1-9]\\d*")) {
					int n = Integer.parseInt(labelName);
					command.append("#");
					command.append(currentNumberedLabel[n]);
					currentNumberedLabel[n]++;
				}
				macroParsed.add(new String[] { command.toString() });
				command.setLength(0);
				if (++i < len)
					c = chars[i];
			} else if (Character.isLetterOrDigit(c) || c == '$') {
				if (command.length() > 0)
					break;
				do
					command.append(c);
				while (++i < len && Character.isLetterOrDigit(c = chars[i]) || c == '.' || c == '=');
			} else if (c == '#' || c == '-') {
				break;
			} else
				throw new LexException("Did not recognize command \'" + c + "\'");
		}

		if (command.length() == 0)
			return;
		String[] separated = new String[32];
		separated[0] = command.toString();
		int index = 1;
		char separator = 0;
		StringBuilder argument = new StringBuilder();
		while (i < len) {
			if (c == ' ' || c == '\t') {
				separator = 0;
				while (++i < len && ((c = chars[i]) == ' ' || c == '\t'))
					;
			} else if (c == ',' || c == '+' || c == '*') {
				separator = c;
				while (++i < len && ((c = chars[i]) == ' ' || c == '\t'))
					;
			} else if (c == '[') {
				if (separator != 0 && separator != ' ')
					separated[index++] = Character.toString(separator);
				separated[index++] = "[";
				separator = 0;
				while (++i < len && ((c = chars[i]) == ' ' || c == '\t'))
					;

			} else if (c == ']') {
				if (separator != 0 && separator != ' ')
					throw new LexException("\']\' may not precede a separator");
				separated[index++] = "]";
				separator = 0;
				while (++i < len && ((c = chars[i]) == ' ' || c == '\t'))
					;
			} else if (Character.isLetterOrDigit(c) || c == '$' || c == '-') {
				do
					argument.append(c);
				while (++i < len && Character.isLetterOrDigit(c = chars[i]) || c == '.' || c == '=');
				if (separator != 0)
					separated[index++] = Character.toString(separator);
				separated[index++] = argument.toString();
				separator = 0;
				argument.setLength(0);
			} else if (c == '#') {
				break;
			} else
				throw new LexException("Did not recognize argument \'" + c + "\'");
		}
		if (separator != 0 && separator != ' ')
			throw new LexException("Instructions may not end with a separator \'" + separator + "\'");
		if (command.toString().startsWith("J")) {
			String labelName = separated[1];
			if (labelName.matches("[1-9]\\d*[bf]")) {
				int labelNameN = currentNumberedLabel[labelName.charAt(0) - '0'];
				separated[1] = labelName.charAt(1) == 'b' ? labelName.charAt(0) + "#" + (labelNameN - 1)
						: labelName.charAt(0) + "#" + (labelNameN);
			}
		}

		macroParsed.add(separated);
	}

	public class LexException extends RuntimeException {
		public LexException(String message) {
			super(message);
		}

		private static final long serialVersionUID = 1L;

	}
	
}
