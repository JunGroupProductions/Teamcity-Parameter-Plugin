package com.accusoft.teamcity.parameterFinder;

import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.AgentLifeCycleListener;
import jetbrains.buildServer.agent.BuildAgent;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AppAgent extends AgentLifeCycleAdapter {

    private StringBuilder s = null;
    Map<String, String> environmentVariables = new HashMap<String, String>();
    Map<String, String> configurationParameters = new HashMap<String, String>();

    public AppAgent(@NotNull EventDispatcher<AgentLifeCycleListener> dispatcher) {
        s = new StringBuilder();
        dispatcher.addListener(this);
    }

    @Override
    public void agentInitialized(@NotNull final BuildAgent agent) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(getClass().getResourceAsStream("/buildAgentResources/parameters.xml"));
            doc.getDocumentElement().normalize();
            NodeList nList = doc.getElementsByTagName("parameter");

            for (int i = 0; i < nList.getLength(); i++) {
                Node nNode = nList.item(i);

                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String toolName = (eElement.getElementsByTagName("tool").item(0).getTextContent());

                    String command = eElement.getElementsByTagName("command").item(0).getTextContent();
                    String regex = eElement.getElementsByTagName("regex").item(0).getTextContent();

                    // The `list` element is optional; value defaults to false if not present or if any value besides true
                    NodeList optionalElements = eElement.getElementsByTagName("list");
                    Node optionalNode = optionalElements == null ? null : optionalElements.item(0);
                    String optionalString = optionalNode == null ? null : optionalNode.getTextContent();
                    boolean isList = "true".equals( optionalString );

                    // The `name` element is optional; value defaults to toolName if not present
                    optionalElements = eElement.getElementsByTagName("name");
                    optionalNode = optionalElements == null ? null : optionalElements.item(0);
                    String nameFormat = optionalNode == null ? toolName : optionalNode.getTextContent();

                    // The `value` element is optional; value defaults to "$1" if not present
                    optionalElements = eElement.getElementsByTagName("value");
                    optionalNode = optionalElements == null ? null : optionalElements.item(0);
                    String valueFormat = optionalNode == null ? "$1" : optionalNode.getTextContent();

                    // The `type` element is optional; value defaults to "env" if not present
                    optionalElements = eElement.getElementsByTagName("type");
                    optionalNode = optionalElements == null ? null : optionalElements.item(0);
                    String type = optionalNode == null ? "env" : optionalNode.getTextContent();

                    // The `use_std_err` element is optional; value defaults to "false" if not present
                    optionalElements = eElement.getElementsByTagName("use_std_err");
                    optionalNode = optionalElements == null ? null : optionalElements.item(0);
                    optionalString = optionalNode == null ? null : optionalNode.getTextContent();
                    boolean useStdErr = "true".equals( optionalString );

                    Map<String, String> parameters = environmentVariables;
                    if ( "config".equals( type ) ) {
                        parameters = configurationParameters;
                    }

                    buildLogString("\n\t\tTOOL: " + toolName + "\n");
                    new ParameterFinder(nameFormat, valueFormat, regex, command, parameters, isList, this, useStdErr);
                }
            }
        } catch (Exception e) {
            buildLogString("\nParameterFinder: " + e.toString());
            buildLogString("\nParameterFinder: " + e.getStackTrace()[0].toString());
        } finally {
            log(s);
        }

        BuildAgentConfiguration conf = agent.getConfiguration();
        Iterator it = environmentVariables.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            conf.addEnvironmentVariable(pair.getKey().toString(), pair.getValue().toString());
            it.remove();
        }

        it = configurationParameters.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            conf.addConfigurationParameter(pair.getKey().toString(), pair.getValue().toString());
            it.remove();
        }
    }

    public void log(StringBuilder s) {
        Loggers.AGENT.info(s.toString());
    }

    public void buildLogString(String s) {
        this.s.append(s);
    }
}
