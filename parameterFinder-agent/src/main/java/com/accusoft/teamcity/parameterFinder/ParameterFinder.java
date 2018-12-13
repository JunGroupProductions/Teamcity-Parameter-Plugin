package com.accusoft.teamcity.parameterFinder;

import java.io.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParameterFinder {
    AppAgent appAgent;
    String nameFormat;
    String valueFormat;
    String command;
    String regex;
    boolean isList;
    boolean useStdErr;
    Map<String, String> parameters;

    public ParameterFinder(String nameFormat, String valueFormat, String regex, String command,
                           Map<String, String> parameters, boolean isList, AppAgent appAgent, boolean useStdErr) {
        this.appAgent = appAgent;
        this.nameFormat = nameFormat;
        this.valueFormat = valueFormat;
        this.command = command;
        this.regex = regex;
        this.parameters = parameters;
        this.isList = isList;
        this.useStdErr = useStdErr;
        runCommand();
    }

    private void runCommand() {
        try {
            String line;
            StringBuilder output = new StringBuilder();
            BufferedReader stdInput;

            Process process = Runtime.getRuntime().exec(command);

            InputStream inputStream = useStdErr ? process.getErrorStream() : process.getInputStream();

            stdInput = new BufferedReader(new InputStreamReader(inputStream));
            while ((line = stdInput.readLine()) != null) {
                output.append(line);
            }
            appAgent.buildLogString("\t\tCommand output: " + output + "\n");
            performRegex(regex, output);
        }
        catch (Exception e) {
            appAgent.buildLogString("\t\t" + e.getMessage() + "\n");
        }
    }

    private void performRegex(String regex, StringBuilder output) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(output.toString());
        boolean matchFound = false;
        while ((isList || !matchFound) && matcher.find()) {
            matchFound = true;
            parameters.put( decorateFromMatch(nameFormat, matcher), decorateFromMatch(valueFormat, matcher));
        }
        if (!matchFound) {
            appAgent.buildLogString("\t\tRegex: " + regex + " did not return any results from the command output. Please review the regex.\n");
        }
    }

    /**
     * Given a format as input, replaces tokens of the form "$N" with the value of the Nth matching group of the
     * matcher. Does not substitute any tokens where N is greater than the number of capturing groups in the matcher.
     * @param format Format string to be interpolated
     * @param matcher Result of regular expression matching with capturing groups
     * @return The interpolated string
     */
    private String decorateFromMatch( String format, Matcher matcher ) {
        String returnValue = format;
        for ( int i = 0; i < matcher.groupCount(); i++ ) {
            returnValue = returnValue.replaceAll( "\\$" + (i + 1), matcher.group( i + 1 ) );
        }
        return returnValue;
    }
}